package br.com.techmister.timeideal;

import br.com.techmister.comportamento.Comportamento;
import br.com.techmister.comportamento.ComportamentoRepository;
import br.com.techmister.desempenho.Desempenho;
import br.com.techmister.desempenho.DesempenhoRepository;
import br.com.techmister.exception.RegraDeNegocioException;
import br.com.techmister.jogador.Jogador;
import br.com.techmister.jogador.JogadorRepository;
import br.com.techmister.jogador.Posicao;
import br.com.techmister.timeideal.dto.TimeIdealResponse;
import br.com.techmister.timeideal.dto.TimeIdealResponse.JogadorAvaliado;
import br.com.techmister.treino.Treino;
import br.com.techmister.treino.TreinoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TimeIdealService {

    private final JogadorRepository jogadorRepository;
    private final TreinoRepository treinoRepository;
    private final ComportamentoRepository comportamentoRepository;
    private final DesempenhoRepository desempenhoRepository;

    private static final int TAM_TITULARES = 11;

    @Autowired
    public TimeIdealService(JogadorRepository jogadorRepository, TreinoRepository treinoRepository,
                            ComportamentoRepository comportamentoRepository, DesempenhoRepository desempenhoRepository) {
        this.jogadorRepository = jogadorRepository;
        this.treinoRepository = treinoRepository;
        this.comportamentoRepository = comportamentoRepository;
        this.desempenhoRepository = desempenhoRepository;
    }

    @Transactional(readOnly = true)
    public TimeIdealResponse gerarDaSemanaAtual() {
        LocalDate hoje = LocalDate.now();
        LocalDate inicio = hoje.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate fim = hoje.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        return gerarParaSemana(inicio, fim);
    }

    public TimeIdealResponse gerarParaSemana(LocalDate inicio, LocalDate fim) {
        List<Treino> treinos = treinoRepository.findByDataTreinoBetween(inicio, fim);
        List<Comportamento> comportamentos = comportamentoRepository.findByDataAvaliacaoBetween(inicio, fim);
        List<Desempenho> desempenhos = desempenhoRepository.findByPartidaDataJogoBetween(inicio, fim);

        if (treinos.isEmpty() && desempenhos.isEmpty())
            throw new RegraDeNegocioException("Dados insuficientes para gerar o Time Ideal. Registre mais treinos e desempenhos.");

        // Agrupando com cast explícito para Map<Long, List<...>>
        Map<Long, List<Treino>> treinosPorJogador = new HashMap<>();
        for (Treino t : treinos) {
            Long jid = t.getJogador().getId();
            treinosPorJogador.computeIfAbsent(jid, k -> new ArrayList<>()).add(t);
        }
        Map<Long, List<Comportamento>> compsPorJogador = new HashMap<>();
        for (Comportamento c : comportamentos) {
            Long jid = c.getJogador().getId();
            compsPorJogador.computeIfAbsent(jid, k -> new ArrayList<>()).add(c);
        }
        Map<Long, List<Desempenho>> desempenhosPorJogador = new HashMap<>();
        for (Desempenho d : desempenhos) {
            Long jid = d.getJogador().getId();
            desempenhosPorJogador.computeIfAbsent(jid, k -> new ArrayList<>()).add(d);
        }

        List<Jogador> elenco = jogadorRepository.findAll().stream()
                .filter(j -> Boolean.TRUE.equals(j.getAtivo())).toList();

        List<JogadorAvaliado> avaliados = new ArrayList<>();
        for (Jogador j : elenco) {
            List<Treino> tj = treinosPorJogador.getOrDefault(j.getId(), Collections.emptyList());
            List<Comportamento> cj = compsPorJogador.getOrDefault(j.getId(), Collections.emptyList());
            List<Desempenho> dj = desempenhosPorJogador.getOrDefault(j.getId(), Collections.emptyList());
            JogadorAvaliado av = avaliar(j, tj, cj, dj);
            if (av.mediaFinal().compareTo(BigDecimal.ZERO) > 0) avaliados.add(av);
        }

        if (avaliados.isEmpty())
            throw new RegraDeNegocioException("Dados insuficientes para gerar o Time Ideal. Registre mais treinos e desempenhos.");

        avaliados.sort((a, b) -> b.mediaFinal().compareTo(a.mediaFinal()));

        List<JogadorAvaliado> titulares = avaliados.size() >= TAM_TITULARES
                ? avaliados.subList(0, TAM_TITULARES) : avaliados;
        List<JogadorAvaliado> reservas = avaliados.size() > TAM_TITULARES
                ? avaliados.subList(TAM_TITULARES, avaliados.size()) : Collections.emptyList();

        String alerta = detectarDesequilibrio(titulares);
        return new TimeIdealResponse(inicio, fim, titulares, reservas, alerta);
    }

    private JogadorAvaliado avaliar(Jogador j, List<Treino> treinos, List<Comportamento> comps, List<Desempenho> desempenhos) {
        BigDecimal mediaTreino = mediaTreinos(treinos);
        BigDecimal mediaComp = mediaComportamentos(comps);
        BigDecimal mediaDesemp = mediaDesempenhos(desempenhos);
        BigDecimal soma = mediaDesemp.multiply(BigDecimal.valueOf(3))
                .add(mediaTreino.multiply(BigDecimal.valueOf(2))).add(mediaComp);
        BigDecimal mediaFinal = soma.divide(BigDecimal.valueOf(6), 2, RoundingMode.HALF_UP);
        return new JogadorAvaliado(j.getId(), j.getNome(), j.getPosicao(), mediaFinal, mediaTreino, mediaComp, mediaDesemp);
    }

    private BigDecimal mediaTreinos(List<Treino> treinos) {
        if (treinos.isEmpty()) return BigDecimal.ZERO;
        BigDecimal soma = BigDecimal.ZERO; int contados = 0;
        for (Treino t : treinos) {
            int total = t.getAcertos() + t.getErros();
            if (total == 0) continue;
            BigDecimal nota = BigDecimal.valueOf(t.getAcertos()).multiply(BigDecimal.TEN)
                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
            soma = soma.add(nota); contados++;
        }
        if (contados == 0) return BigDecimal.ZERO;
        return soma.divide(BigDecimal.valueOf(contados), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal mediaComportamentos(List<Comportamento> comps) {
        if (comps.isEmpty()) return BigDecimal.ZERO;
        BigDecimal soma = BigDecimal.ZERO;
        for (Comportamento c : comps) {
            BigDecimal media = BigDecimal.valueOf(c.getNotaFoco() + c.getNotaDisciplina() + c.getNotaAtitude())
                    .divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
            soma = soma.add(media);
        }
        return soma.divide(BigDecimal.valueOf(comps.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal mediaDesempenhos(List<Desempenho> desempenhos) {
        if (desempenhos.isEmpty()) return BigDecimal.ZERO;
        BigDecimal soma = BigDecimal.ZERO;
        for (Desempenho d : desempenhos) soma = soma.add(d.getNota());
        return soma.divide(BigDecimal.valueOf(desempenhos.size()), 2, RoundingMode.HALF_UP);
    }

    private String detectarDesequilibrio(List<JogadorAvaliado> titulares) {
        Set<Posicao> posicoes = new HashSet<>();
        for (JogadorAvaliado j : titulares) posicoes.add(j.posicao());
        boolean temGoleiro = posicoes.contains(Posicao.GOLEIRO);
        boolean temDefesa = posicoes.contains(Posicao.ZAGUEIRO) || posicoes.contains(Posicao.LATERAL_DIREITO) || posicoes.contains(Posicao.LATERAL_ESQUERDO);
        boolean temMeio = posicoes.contains(Posicao.VOLANTE) || posicoes.contains(Posicao.MEIO_CAMPO) || posicoes.contains(Posicao.MEIA_ATACANTE);
        boolean temAtaque = posicoes.contains(Posicao.ATACANTE) || posicoes.contains(Posicao.PONTA_DIREITA) || posicoes.contains(Posicao.PONTA_ESQUERDA);
        if (titulares.size() < TAM_TITULARES || !temGoleiro || !temDefesa || !temMeio || !temAtaque)
            return "Atenção: A escalação gerada apresenta desequilíbrio tático nas posições. Recomendada a substituição manual.";
        return null;
    }
}
