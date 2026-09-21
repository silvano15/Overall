package br.com.overall.pelada;

import br.com.overall.estatistica.EstatisticaAgregador;
import br.com.overall.estatistica.EstatisticaPartida;
import br.com.overall.estatistica.EstatisticaRepository;
import br.com.overall.estatistica.Estatisticas;
import br.com.overall.estatistica.OverallCalculator;
import br.com.overall.estatistica.Resultado;
import br.com.overall.exception.RecursoNaoEncontradoException;
import br.com.overall.exception.RegraDeNegocioException;
import br.com.overall.jogador.Jogador;
import br.com.overall.jogador.JogadorRepository;
import br.com.overall.partida.Gol;
import br.com.overall.partida.GolRepository;
import br.com.overall.partida.Partida;
import br.com.overall.partida.PartidaJogadorRepository;
import br.com.overall.partida.PartidaRepository;
import br.com.overall.partida.PartidaStatus;
import br.com.overall.partida.ProximaPartida;
import br.com.overall.pelada.dto.PeladaListaItem;
import br.com.overall.pelada.dto.PeladaRequest;
import br.com.overall.pelada.dto.PeladaResponse;
import br.com.overall.pelada.dto.ResumoPeladaResponse;
import br.com.overall.pelada.dto.SorteioRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PeladaService {

    /** Um jogador só "conta como tendo dados" com essa quantidade de partidas... */
    static final int MIN_JOGOS_CONFIAVEL = 3;
    /** ...e o sorteio só passa a usar overall quando essa fração da lista tem dados. */
    static final double FRACAO_MINIMA_COM_DADOS = 0.5;

    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");

    private final PeladaRepository peladaRepository;
    private final PeladaJogadorRepository peladaJogadorRepository;
    private final TimePeladaRepository timePeladaRepository;
    private final TimePeladaJogadorRepository timePeladaJogadorRepository;
    private final JogadorRepository jogadorRepository;
    private final PartidaRepository partidaRepository;
    private final PartidaJogadorRepository partidaJogadorRepository;
    private final GolRepository golRepository;
    private final EstatisticaRepository estatisticaRepository;
    private final EstatisticaAgregador agregador;
    private final Clock clock;

    public PeladaService(PeladaRepository peladaRepository,
                         PeladaJogadorRepository peladaJogadorRepository,
                         TimePeladaRepository timePeladaRepository,
                         TimePeladaJogadorRepository timePeladaJogadorRepository,
                         JogadorRepository jogadorRepository,
                         PartidaRepository partidaRepository,
                         PartidaJogadorRepository partidaJogadorRepository,
                         GolRepository golRepository,
                         EstatisticaRepository estatisticaRepository,
                         EstatisticaAgregador agregador,
                         Clock clock) {
        this.peladaRepository = peladaRepository;
        this.peladaJogadorRepository = peladaJogadorRepository;
        this.timePeladaRepository = timePeladaRepository;
        this.timePeladaJogadorRepository = timePeladaJogadorRepository;
        this.jogadorRepository = jogadorRepository;
        this.partidaRepository = partidaRepository;
        this.partidaJogadorRepository = partidaJogadorRepository;
        this.golRepository = golRepository;
        this.estatisticaRepository = estatisticaRepository;
        this.agregador = agregador;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ CRUD da pelada

    @Transactional
    public PeladaResponse criar(PeladaRequest req) {
        peladaRepository.findFirstByStatusNotOrderByDataJogoAscIdAsc(PeladaStatus.FINALIZADA).ifPresent(aberta -> {
            throw new RegraDeNegocioException("Já existe uma pelada em aberto (" + aberta.getDataJogo().format(DIA_MES)
                    + "). Finalize ou exclua essa antes de criar outra.");
        });
        validarData(req.dataJogo());
        return montar(peladaRepository.save(new Pelada(req.dataJogo(), req.horaJogo())));
    }

    @Transactional
    public PeladaResponse atualizar(Long id, PeladaRequest req) {
        Pelada pelada = buscar(id);
        if (pelada.getStatus() != PeladaStatus.ABERTA && pelada.getStatus() != PeladaStatus.SORTEADA)
            throw new RegraDeNegocioException("Não é possível alterar a data de uma pelada já iniciada.");
        validarData(req.dataJogo());
        pelada.setDataJogo(req.dataJogo());
        pelada.setHoraJogo(req.horaJogo());
        return montar(peladaRepository.save(pelada));
    }

    @Transactional
    public void excluir(Long id) {
        Pelada pelada = buscar(id);
        if (pelada.getStatus() == PeladaStatus.FINALIZADA || partidaRepository.existsByPeladaId(id))
            throw new RegraDeNegocioException("Esta pelada já tem partidas registradas e não pode ser excluída.");
        limparTimes(id);
        peladaJogadorRepository.deleteByPeladaId(id);
        peladaRepository.delete(pelada);
    }

    @Transactional(readOnly = true)
    public Optional<PeladaResponse> atual() {
        return peladaRepository.findFirstByStatusNotOrderByDataJogoAscIdAsc(PeladaStatus.FINALIZADA)
                .map(this::montar);
    }

    @Transactional(readOnly = true)
    public PeladaResponse obter(Long id) {
        return montar(buscar(id));
    }

    @Transactional(readOnly = true)
    public List<PeladaListaItem> historico() {
        return peladaRepository.findAllByOrderByDataJogoDescIdDesc().stream()
                .map(p -> new PeladaListaItem(p.getId(), p.getDataJogo(), p.getHoraJogo(), p.getStatus(),
                        peladaJogadorRepository.findByPeladaId(p.getId()).size(),
                        partidaRepository.findByPeladaIdOrderByOrdemAsc(p.getId()).size()))
                .toList();
    }

    // ------------------------------------------------------------------ lista da pelada

    @Transactional
    public PeladaResponse adicionarJogador(Long peladaId, Long jogadorId) {
        Pelada pelada = buscar(peladaId);
        if (pelada.getStatus() == PeladaStatus.FINALIZADA)
            throw new RegraDeNegocioException("Esta pelada já foi finalizada.");
        Jogador jogador = jogadorRepository.findById(jogadorId).orElseThrow(
                () -> new RecursoNaoEncontradoException("Jogador não encontrado."));
        if (!jogador.isAtivo())
            throw new RegraDeNegocioException("Este jogador está arquivado. Reative-o para adicionar à lista.");
        if (!peladaJogadorRepository.existsByPeladaIdAndJogadorId(peladaId, jogadorId))
            peladaJogadorRepository.save(new PeladaJogador(peladaId, jogadorId));
        return montar(pelada);
    }

    @Transactional
    public PeladaResponse removerJogador(Long peladaId, Long jogadorId) {
        Pelada pelada = buscar(peladaId);
        if (pelada.getStatus() == PeladaStatus.FINALIZADA)
            throw new RegraDeNegocioException("Esta pelada já foi finalizada.");
        boolean emTime = timePeladaJogadorRepository.findByTimeIdIn(idsDosTimes(peladaId)).stream()
                .anyMatch(m -> m.getJogadorId().equals(jogadorId));
        if (emTime || partidaJogadorRepository.participouNaPelada(peladaId, jogadorId))
            throw new RegraDeNegocioException("Este jogador já está em um time. Re-sorteie ou use a substituição.");
        peladaJogadorRepository.findByPeladaIdAndJogadorId(peladaId, jogadorId)
                .ifPresent(peladaJogadorRepository::delete);
        return montar(pelada);
    }

    // ------------------------------------------------------------------ sorteio

    @Transactional
    public PeladaResponse sortear(Long peladaId, SorteioRequest req) {
        Pelada pelada = buscar(peladaId);
        if (pelada.getStatus() != PeladaStatus.ABERTA && pelada.getStatus() != PeladaStatus.SORTEADA)
            throw new RegraDeNegocioException("Os times só podem ser sorteados antes da primeira partida.");
        LocalDate hoje = LocalDate.now(clock);
        if (pelada.getDataJogo().isAfter(hoje))
            throw new RegraDeNegocioException("O sorteio só é liberado no dia da pelada ("
                    + pelada.getDataJogo().format(DIA_MES) + ").");

        int tamanho = req.jogadoresPorTime();
        List<Long> ids = peladaJogadorRepository.findByPeladaId(peladaId).stream()
                .map(PeladaJogador::getJogadorId).toList();
        if (SorteioAlgoritmo.quantidadeDeTimes(ids.size(), tamanho) < 2)
            throw new RegraDeNegocioException("Com " + ids.size() + " jogadores e " + tamanho
                    + " por time não dá para formar 2 times. Adicione jogadores ou reduza o tamanho do time.");

        List<Jogador> jogadores = jogadorRepository.findAllById(ids);
        Map<Long, Estatisticas> historico = agregador.geral();
        long comDados = jogadores.stream()
                .filter(j -> historico.getOrDefault(j.getId(), Estatisticas.vazia()).partidas() >= MIN_JOGOS_CONFIAVEL)
                .count();
        boolean balancear = comDados >= Math.ceil(jogadores.size() * FRACAO_MINIMA_COM_DADOS);

        List<SorteioAlgoritmo.Candidato> candidatos = jogadores.stream()
                .map(j -> new SorteioAlgoritmo.Candidato(j.getId(), j.getOverall(), j.isGoleiro())).toList();
        List<List<Long>> resultado = SorteioAlgoritmo.sortear(candidatos, tamanho, balancear, ThreadLocalRandom.current());

        limparTimes(peladaId);
        timePeladaRepository.flush(); // apaga os antigos antes de inserir (índice único pelada+número)
        for (int i = 0; i < resultado.size(); i++) {
            TimePelada time = timePeladaRepository.save(new TimePelada(peladaId, i + 1));
            for (Long jogadorId : resultado.get(i))
                timePeladaJogadorRepository.save(new TimePeladaJogador(time.getId(), jogadorId));
        }

        pelada.setJogadoresPorTime(tamanho);
        pelada.setModoSorteio(balancear ? ModoSorteio.OVERALL : ModoSorteio.ALEATORIO);
        pelada.setStatus(PeladaStatus.SORTEADA);
        return montar(peladaRepository.save(pelada));
    }

    // ------------------------------------------------------------------ finalização e resumo

    @Transactional
    public ResumoPeladaResponse finalizar(Long id) {
        Pelada pelada = buscar(id);
        if (pelada.getStatus() != PeladaStatus.EM_ANDAMENTO)
            throw new RegraDeNegocioException("Só é possível finalizar uma pelada que já teve partidas.");
        if (partidaRepository.existsByPeladaIdAndStatusNot(id, PartidaStatus.ENCERRADA))
            throw new RegraDeNegocioException("Encerre a partida em aberto antes de finalizar a pelada.");

        List<EstatisticaPartida> linhas = estatisticaRepository.findByPeladaId(id);
        Map<Long, Estatisticas> antes = agregador.geralExceto(id);
        Map<Long, Estatisticas> depois = agregador.geral();
        Map<Long, Jogador> jogadores = mapaDeJogadores(peladaJogadorRepository.findByPeladaId(id).stream()
                .map(PeladaJogador::getJogadorId).toList());

        for (PeladaJogador pj : peladaJogadorRepository.findByPeladaId(id)) {
            Jogador j = jogadores.get(pj.getJogadorId());
            int ovAntes = OverallCalculator.calcular(antes.get(j.getId()), j.isGoleiro());
            int ovDepois = OverallCalculator.calcular(depois.get(j.getId()), j.isGoleiro());
            pj.setOverallAntes(ovAntes);
            pj.setOverallDepois(ovDepois);
            pj.setNota(mediaDasNotas(linhas, j.getId()));
            peladaJogadorRepository.save(pj);
            j.setOverall(ovDepois);
            jogadorRepository.save(j);
        }

        pelada.setStatus(PeladaStatus.FINALIZADA);
        pelada.setFinalizadaEm(LocalDateTime.now(clock));
        peladaRepository.save(pelada);
        return resumo(id);
    }

    @Transactional(readOnly = true)
    public ResumoPeladaResponse resumo(Long id) {
        Pelada pelada = buscar(id);
        List<EstatisticaPartida> linhas = estatisticaRepository.findByPeladaId(id);
        List<PeladaJogador> lista = peladaJogadorRepository.findByPeladaId(id);
        Map<Long, PeladaJogador> listaPorJogador = lista.stream()
                .collect(Collectors.toMap(PeladaJogador::getJogadorId, Function.identity()));

        Set<Long> ids = new HashSet<>(listaPorJogador.keySet());
        linhas.forEach(l -> ids.add(l.getJogadorId()));
        Map<Long, Jogador> jogadores = mapaDeJogadores(ids);

        // Desempenho por jogador no dia
        Map<Long, List<EstatisticaPartida>> porJogador = linhas.stream()
                .collect(Collectors.groupingBy(EstatisticaPartida::getJogadorId));
        List<ResumoPeladaResponse.DesempenhoJogador> desempenhos = new ArrayList<>();
        for (Long jogadorId : ids) {
            List<EstatisticaPartida> ls = porJogador.getOrDefault(jogadorId, List.of());
            if (ls.isEmpty() && pelada.getStatus() != PeladaStatus.FINALIZADA) continue;
            PeladaJogador pj = listaPorJogador.get(jogadorId);
            BigDecimal nota = mediaDasNotas(linhas, jogadorId);
            desempenhos.add(new ResumoPeladaResponse.DesempenhoJogador(
                    jogadorId, jogadores.get(jogadorId).getNome(), ls.size(),
                    contar(ls, Resultado.VITORIA), contar(ls, Resultado.EMPATE), contar(ls, Resultado.DERROTA),
                    ls.stream().mapToInt(EstatisticaPartida::getGols).sum(),
                    ls.stream().mapToInt(EstatisticaPartida::getAssistencias).sum(),
                    (int) ls.stream().filter(EstatisticaPartida::isSemSofrer).count(),
                    nota == null ? null : nota.doubleValue(),
                    pj == null ? null : pj.getOverallAntes(), pj == null ? null : pj.getOverallDepois()));
        }
        // Só aparecem no resumo quem jogou (ou, na pelada finalizada, quem estava na lista)
        desempenhos.sort(Comparator
                .comparing((ResumoPeladaResponse.DesempenhoJogador d) -> d.nota() == null ? -1.0 : d.nota()).reversed()
                .thenComparing(ResumoPeladaResponse.DesempenhoJogador::nome));

        List<ResumoPeladaResponse.ItemRanking> artilheiros = desempenhos.stream()
                .filter(d -> d.gols() > 0)
                .sorted(Comparator.comparingInt(ResumoPeladaResponse.DesempenhoJogador::gols).reversed()
                        .thenComparingInt(ResumoPeladaResponse.DesempenhoJogador::partidas))
                .map(d -> new ResumoPeladaResponse.ItemRanking(d.jogadorId(), d.nome(), d.gols())).toList();
        List<ResumoPeladaResponse.ItemRanking> assistentes = desempenhos.stream()
                .filter(d -> d.assistencias() > 0)
                .sorted(Comparator.comparingInt(ResumoPeladaResponse.DesempenhoJogador::assistencias).reversed()
                        .thenComparingInt(ResumoPeladaResponse.DesempenhoJogador::partidas))
                .map(d -> new ResumoPeladaResponse.ItemRanking(d.jogadorId(), d.nome(), d.assistencias())).toList();

        return new ResumoPeladaResponse(pelada.getId(), pelada.getDataJogo(), pelada.getHoraJogo(), pelada.getStatus(),
                (int) partidaRepository.findByPeladaIdOrderByOrdemAsc(id).stream()
                        .filter(p -> p.getStatus() == PartidaStatus.ENCERRADA).count(),
                classificacaoDosTimes(id), artilheiros, assistentes, desempenhos);
    }

    /** Times ordenados por vitórias (o "time mais vencedor" da pelada), depois saldo e gols pró. */
    private List<ResumoPeladaResponse.ClassificacaoTime> classificacaoDosTimes(Long peladaId) {
        List<TimePelada> times = timePeladaRepository.findByPeladaIdOrderByNumeroAsc(peladaId);
        List<Partida> encerradas = partidaRepository.findByPeladaIdOrderByOrdemAsc(peladaId).stream()
                .filter(p -> p.getStatus() == PartidaStatus.ENCERRADA).toList();
        Map<Long, List<Gol>> golsPorPartida = golRepository
                .findByPartidaIdIn(encerradas.stream().map(Partida::getId).toList()).stream()
                .collect(Collectors.groupingBy(Gol::getPartidaId));

        Map<Long, int[]> acc = new HashMap<>(); // [jogos, v, e, d, gp, gc]
        times.forEach(t -> acc.put(t.getId(), new int[6]));
        for (Partida p : encerradas) {
            List<Gol> gols = golsPorPartida.getOrDefault(p.getId(), List.of());
            int a = (int) gols.stream().filter(g -> g.getTimeId().equals(p.getTimeAId())).count();
            int b = gols.size() - a;
            aplicar(acc.get(p.getTimeAId()), a, b);
            aplicar(acc.get(p.getTimeBId()), b, a);
        }
        return times.stream()
                .map(t -> {
                    int[] c = acc.get(t.getId());
                    return new ResumoPeladaResponse.ClassificacaoTime(t.getId(), t.getNumero(),
                            c[0], c[1], c[2], c[3], c[4], c[5], c[4] - c[5]);
                })
                .filter(t -> t.jogos() > 0)
                .sorted(Comparator.comparingInt(ResumoPeladaResponse.ClassificacaoTime::vitorias).reversed()
                        .thenComparing(Comparator.comparingInt(ResumoPeladaResponse.ClassificacaoTime::saldo).reversed())
                        .thenComparing(Comparator.comparingInt(ResumoPeladaResponse.ClassificacaoTime::golsPro).reversed())
                        .thenComparingInt(ResumoPeladaResponse.ClassificacaoTime::numero))
                .toList();
    }

    private static void aplicar(int[] c, int pro, int contra) {
        c[0]++;
        if (pro > contra) c[1]++; else if (pro == contra) c[2]++; else c[3]++;
        c[4] += pro;
        c[5] += contra;
    }

    private static int contar(List<EstatisticaPartida> ls, Resultado r) {
        return (int) ls.stream().filter(l -> l.getResultado() == r).count();
    }

    private static BigDecimal mediaDasNotas(List<EstatisticaPartida> linhas, Long jogadorId) {
        List<BigDecimal> notas = linhas.stream().filter(l -> l.getJogadorId().equals(jogadorId))
                .map(EstatisticaPartida::getNota).toList();
        if (notas.isEmpty()) return null;
        return notas.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(notas.size()), 2, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------------ montagem da resposta

    private PeladaResponse montar(Pelada pelada) {
        Long id = pelada.getId();
        List<PeladaJogador> lista = peladaJogadorRepository.findByPeladaId(id);
        List<TimePelada> times = timePeladaRepository.findByPeladaIdOrderByNumeroAsc(id);
        Map<Long, List<TimePeladaJogador>> membros = timePeladaJogadorRepository
                .findByTimeIdIn(times.stream().map(TimePelada::getId).toList()).stream()
                .collect(Collectors.groupingBy(TimePeladaJogador::getTimeId));

        Set<Long> ids = new HashSet<>();
        lista.forEach(l -> ids.add(l.getJogadorId()));
        membros.values().forEach(ms -> ms.forEach(m -> ids.add(m.getJogadorId())));
        Map<Long, Jogador> jogadores = mapaDeJogadores(ids);

        List<Long> listaOrdenada = lista.stream().map(PeladaJogador::getJogadorId)
                .sorted(Comparator.comparing((Long j) -> jogadores.get(j).getNome(), String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<PeladaResponse.Time> timesDto = times.stream().map(t -> {
            List<PeladaResponse.Integrante> integrantes = membros.getOrDefault(t.getId(), List.of()).stream()
                    .map(m -> jogadores.get(m.getJogadorId()))
                    .sorted(Comparator.comparingInt(Jogador::getOverall).reversed())
                    .map(j -> new PeladaResponse.Integrante(j.getId(), j.getOverall(), j.isGoleiro()))
                    .toList();
            double media = integrantes.stream().mapToInt(PeladaResponse.Integrante::overall).average().orElse(0);
            return new PeladaResponse.Time(t.getId(), t.getNumero(), Math.round(media * 10) / 10.0, integrantes);
        }).toList();

        Map<Long, TimePelada> timesPorId = times.stream().collect(Collectors.toMap(TimePelada::getId, Function.identity()));
        List<Partida> partidas = partidaRepository.findByPeladaIdOrderByOrdemAsc(id);
        Map<Long, List<Gol>> golsPorPartida = golRepository
                .findByPartidaIdIn(partidas.stream().map(Partida::getId).toList()).stream()
                .collect(Collectors.groupingBy(Gol::getPartidaId));

        List<PeladaResponse.PartidaResumo> partidasDto = new ArrayList<>();
        Long emAberto = null;
        List<ProximaPartida.Encerrada> historico = new ArrayList<>();
        for (Partida p : partidas) {
            List<Gol> gols = golsPorPartida.getOrDefault(p.getId(), List.of());
            int golsA = (int) gols.stream().filter(g -> g.getTimeId().equals(p.getTimeAId())).count();
            int golsB = gols.size() - golsA;
            partidasDto.add(new PeladaResponse.PartidaResumo(p.getId(), p.getOrdem(), p.getStatus(),
                    p.getTimeAId(), timesPorId.get(p.getTimeAId()).getNumero(), golsA,
                    p.getTimeBId(), timesPorId.get(p.getTimeBId()).getNumero(), golsB,
                    p.getVencedorId(), p.getVencedorPenaltisId()));
            if (p.getStatus() != PartidaStatus.ENCERRADA) {
                emAberto = p.getId();
            } else {
                Long fica = p.quemFicaId();
                historico.add(new ProximaPartida.Encerrada(timesPorId.get(p.getTimeAId()).getNumero(),
                        timesPorId.get(p.getTimeBId()).getNumero(),
                        fica == null ? null : timesPorId.get(fica).getNumero()));
            }
        }

        PeladaResponse.Sugestao sugestao = null;
        boolean sorteada = pelada.getStatus() == PeladaStatus.SORTEADA || pelada.getStatus() == PeladaStatus.EM_ANDAMENTO;
        if (sorteada && emAberto == null) {
            Map<Integer, Long> idPorNumero = times.stream()
                    .collect(Collectors.toMap(TimePelada::getNumero, TimePelada::getId));
            sugestao = ProximaPartida.sugerir(times.size(), historico)
                    .map(n -> new PeladaResponse.Sugestao(idPorNumero.get(n[0]), idPorNumero.get(n[1])))
                    .orElse(null);
        }

        boolean sorteioLiberado = (pelada.getStatus() == PeladaStatus.ABERTA || pelada.getStatus() == PeladaStatus.SORTEADA)
                && !pelada.getDataJogo().isAfter(LocalDate.now(clock));

        return new PeladaResponse(id, pelada.getDataJogo(), pelada.getHoraJogo(), pelada.getStatus(),
                pelada.getJogadoresPorTime(), pelada.getModoSorteio(), pelada.getDuracaoMinutos(),
                pelada.getGolsParaEncerrar(), sorteioLiberado, listaOrdenada, timesDto, partidasDto, emAberto, sugestao);
    }

    // ------------------------------------------------------------------ helpers

    private Pelada buscar(Long id) {
        return peladaRepository.findById(id).orElseThrow(
                () -> new RecursoNaoEncontradoException("Pelada não encontrada."));
    }

    private void validarData(LocalDate data) {
        if (data.isBefore(LocalDate.now(clock)))
            throw new RegraDeNegocioException("A data da pelada não pode estar no passado.");
    }

    private List<Long> idsDosTimes(Long peladaId) {
        return timePeladaRepository.findByPeladaIdOrderByNumeroAsc(peladaId).stream().map(TimePelada::getId).toList();
    }

    private void limparTimes(Long peladaId) {
        List<Long> timeIds = idsDosTimes(peladaId);
        if (timeIds.isEmpty()) return;
        timePeladaJogadorRepository.deleteByTimeIdIn(timeIds);
        timePeladaRepository.deleteByPeladaId(peladaId);
    }

    private Map<Long, Jogador> mapaDeJogadores(java.util.Collection<Long> ids) {
        return jogadorRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Jogador::getId, Function.identity()));
    }
}
