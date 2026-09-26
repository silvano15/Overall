package br.com.overall.pelada;

import br.com.overall.estatistica.EstatisticaPartida;
import br.com.overall.estatistica.EstatisticaRepository;
import br.com.overall.estatistica.Estatisticas;
import br.com.overall.estatistica.OverallCalculator;
import br.com.overall.exception.RecursoNaoEncontradoException;
import br.com.overall.exception.RegraDeNegocioException;
import br.com.overall.jogador.Jogador;
import br.com.overall.jogador.JogadorRepository;
import br.com.overall.partida.GolRepository;
import br.com.overall.partida.Partida;
import br.com.overall.partida.PartidaJogadorRepository;
import br.com.overall.partida.PartidaRepository;
import br.com.overall.partida.PenaltiRepository;
import br.com.overall.partida.SubstituicaoRepository;
import br.com.overall.usuario.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Apaga uma pelada inteira (partidas, gols, estatísticas...), inclusive finalizada. Como é destrutivo e
 * irreversível, exige a senha do usuário logado: quem pegar o celular desbloqueado não apaga o histórico.
 */
@Service
public class ExclusaoPeladaService {

    private final PeladaRepository peladaRepository;
    private final PeladaJogadorRepository peladaJogadorRepository;
    private final TimePeladaRepository timePeladaRepository;
    private final TimePeladaJogadorRepository timePeladaJogadorRepository;
    private final PartidaRepository partidaRepository;
    private final PartidaJogadorRepository partidaJogadorRepository;
    private final GolRepository golRepository;
    private final SubstituicaoRepository substituicaoRepository;
    private final PenaltiRepository penaltiRepository;
    private final EstatisticaRepository estatisticaRepository;
    private final JogadorRepository jogadorRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public ExclusaoPeladaService(PeladaRepository peladaRepository,
                                 PeladaJogadorRepository peladaJogadorRepository,
                                 TimePeladaRepository timePeladaRepository,
                                 TimePeladaJogadorRepository timePeladaJogadorRepository,
                                 PartidaRepository partidaRepository,
                                 PartidaJogadorRepository partidaJogadorRepository,
                                 GolRepository golRepository,
                                 SubstituicaoRepository substituicaoRepository,
                                 PenaltiRepository penaltiRepository,
                                 EstatisticaRepository estatisticaRepository,
                                 JogadorRepository jogadorRepository,
                                 UsuarioRepository usuarioRepository,
                                 PasswordEncoder passwordEncoder) {
        this.peladaRepository = peladaRepository;
        this.peladaJogadorRepository = peladaJogadorRepository;
        this.timePeladaRepository = timePeladaRepository;
        this.timePeladaJogadorRepository = timePeladaJogadorRepository;
        this.partidaRepository = partidaRepository;
        this.partidaJogadorRepository = partidaJogadorRepository;
        this.golRepository = golRepository;
        this.substituicaoRepository = substituicaoRepository;
        this.penaltiRepository = penaltiRepository;
        this.estatisticaRepository = estatisticaRepository;
        this.jogadorRepository = jogadorRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void excluir(Long peladaId, String username, String senha) {
        var usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RegraDeNegocioException("Senha incorreta."));
        if (!passwordEncoder.matches(senha, usuario.getSenha()))
            throw new RegraDeNegocioException("Senha incorreta.");

        Pelada pelada = peladaRepository.findById(peladaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pelada não encontrada."));

        // Ordem das chaves estrangeiras: do mais "de baixo" para a própria pelada.
        List<Partida> partidas = partidaRepository.findByPeladaIdOrderByOrdemAsc(peladaId);
        for (Partida p : partidas) {
            golRepository.deleteByPartidaId(p.getId());
            substituicaoRepository.deleteByPartidaId(p.getId());
            penaltiRepository.deleteByPartidaId(p.getId());
            partidaJogadorRepository.deleteByPartidaId(p.getId());
        }
        estatisticaRepository.deleteByPeladaId(peladaId);
        partidaRepository.flush();
        partidaRepository.deleteAll(partidas);
        partidaRepository.flush();

        List<Long> timeIds = timePeladaRepository.findByPeladaIdOrderByNumeroAsc(peladaId).stream()
                .map(TimePelada::getId).toList();
        if (!timeIds.isEmpty()) {
            timePeladaJogadorRepository.deleteByTimeIdIn(timeIds);
            timePeladaRepository.deleteByPeladaId(peladaId);
            timePeladaRepository.flush();
        }
        peladaJogadorRepository.deleteByPeladaId(peladaId);
        peladaRepository.delete(pelada);
        peladaRepository.flush();

        refazerHistoricoDeOverall(pelada.getGrupoId());
    }

    /**
     * Reconstrói o overall do grupo só com as peladas finalizadas que restaram, em ordem cronológica:
     * o "antes/depois" de cada pelada seguinte e o overall atual de cada jogador deixam de carregar
     * qualquer efeito da pelada apagada.
     */
    private void refazerHistoricoDeOverall(Long grupoId) {
        Map<Long, Jogador> jogadores = new HashMap<>();
        jogadorRepository.findByGrupoId(grupoId).forEach(j -> jogadores.put(j.getId(), j));

        List<Pelada> finalizadas = new ArrayList<>(peladaRepository.findByGrupoIdOrderByDataJogoDescIdDesc(grupoId).stream()
                .filter(p -> p.getStatus() == PeladaStatus.FINALIZADA).toList());
        Collections.reverse(finalizadas); // da mais antiga para a mais nova

        Map<Long, long[]> acumulado = new HashMap<>();
        for (Pelada p : finalizadas) {
            List<PeladaJogador> lista = peladaJogadorRepository.findByPeladaId(p.getId());
            Map<Long, Integer> antes = new HashMap<>();
            for (PeladaJogador pj : lista) antes.put(pj.getJogadorId(), overall(jogadores.get(pj.getJogadorId()), acumulado));
            for (EstatisticaPartida e : estatisticaRepository.findByPeladaId(p.getId())) somar(acumulado, e);
            for (PeladaJogador pj : lista) {
                pj.setOverallAntes(antes.get(pj.getJogadorId()));
                pj.setOverallDepois(overall(jogadores.get(pj.getJogadorId()), acumulado));
                peladaJogadorRepository.save(pj);
            }
        }
        for (Jogador j : jogadores.values()) {
            j.setOverall(overall(j, acumulado));
            jogadorRepository.save(j);
        }
    }

    private static int overall(Jogador j, Map<Long, long[]> acumulado) {
        long[] a = acumulado.get(j.getId());
        Estatisticas e = a == null ? Estatisticas.vazia()
                : new Estatisticas(a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8], 0);
        return OverallCalculator.calcular(e, j.isGoleiro());
    }

    private static void somar(Map<Long, long[]> acumulado, EstatisticaPartida e) {
        long[] a = acumulado.computeIfAbsent(e.getJogadorId(), k -> new long[9]);
        a[0]++;
        switch (e.getResultado()) {
            case VITORIA -> a[1]++;
            case EMPATE -> a[2]++;
            case DERROTA -> a[3]++;
        }
        a[4] += e.getGols();
        a[5] += e.getAssistencias();
        if (e.isGoleiro()) { a[6]++; a[8] += e.getGolsSofridos(); if (e.isSemSofrer()) a[7]++; }
    }
}
