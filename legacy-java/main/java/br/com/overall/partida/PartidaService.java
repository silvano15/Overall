package br.com.overall.partida;

import br.com.overall.estatistica.EstatisticaPartida;
import br.com.overall.estatistica.EstatisticaRepository;
import br.com.overall.estatistica.NotaCalculator;
import br.com.overall.estatistica.Resultado;
import br.com.overall.exception.RecursoNaoEncontradoException;
import br.com.overall.exception.RegraDeNegocioException;
import br.com.overall.jogador.Jogador;
import br.com.overall.jogador.JogadorRepository;
import br.com.overall.partida.dto.GoleiroRequest;
import br.com.overall.partida.dto.IniciarPenaltisRequest;
import br.com.overall.partida.dto.GolRequest;
import br.com.overall.partida.dto.PartidaRequest;
import br.com.overall.partida.dto.PartidaResponse;
import br.com.overall.partida.dto.PenaltiRequest;
import br.com.overall.partida.dto.SubstituicaoRequest;
import br.com.overall.pelada.Pelada;
import br.com.overall.pelada.PeladaJogadorRepository;
import br.com.overall.pelada.PeladaRepository;
import br.com.overall.pelada.PeladaStatus;
import br.com.overall.pelada.TimePelada;
import br.com.overall.pelada.TimePeladaJogador;
import br.com.overall.pelada.TimePeladaJogadorRepository;
import br.com.overall.pelada.TimePeladaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Regras da partida ao vivo: cronômetro, gols, substituições, empate/pênaltis e fechamento das estatísticas.
 *
 * <p>Toda operação começa chamando {@link #carregar(Long)}, que encerra sozinha a partida cujo tempo acabou
 * (mesmo que ninguém estivesse com a tela aberta na hora).
 */
@Service
public class PartidaService {

    private final PartidaRepository partidaRepository;
    private final PartidaJogadorRepository partidaJogadorRepository;
    private final GolRepository golRepository;
    private final SubstituicaoRepository substituicaoRepository;
    private final PenaltiRepository penaltiRepository;
    private final PeladaRepository peladaRepository;
    private final PeladaJogadorRepository peladaJogadorRepository;
    private final TimePeladaRepository timePeladaRepository;
    private final TimePeladaJogadorRepository timePeladaJogadorRepository;
    private final JogadorRepository jogadorRepository;
    private final EstatisticaRepository estatisticaRepository;
    private final Clock clock;

    public PartidaService(PartidaRepository partidaRepository,
                          PartidaJogadorRepository partidaJogadorRepository,
                          GolRepository golRepository,
                          SubstituicaoRepository substituicaoRepository,
                          PenaltiRepository penaltiRepository,
                          PeladaRepository peladaRepository,
                          PeladaJogadorRepository peladaJogadorRepository,
                          TimePeladaRepository timePeladaRepository,
                          TimePeladaJogadorRepository timePeladaJogadorRepository,
                          JogadorRepository jogadorRepository,
                          EstatisticaRepository estatisticaRepository,
                          Clock clock) {
        this.partidaRepository = partidaRepository;
        this.partidaJogadorRepository = partidaJogadorRepository;
        this.golRepository = golRepository;
        this.substituicaoRepository = substituicaoRepository;
        this.penaltiRepository = penaltiRepository;
        this.peladaRepository = peladaRepository;
        this.peladaJogadorRepository = peladaJogadorRepository;
        this.timePeladaRepository = timePeladaRepository;
        this.timePeladaJogadorRepository = timePeladaJogadorRepository;
        this.jogadorRepository = jogadorRepository;
        this.estatisticaRepository = estatisticaRepository;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ criação

    @Transactional
    public PartidaResponse criar(Long peladaId, PartidaRequest req) {
        Pelada pelada = peladaRepository.findById(peladaId).orElseThrow(
                () -> new RecursoNaoEncontradoException("Pelada não encontrada."));
        if (pelada.getStatus() == PeladaStatus.ABERTA)
            throw new RegraDeNegocioException("Sorteie os times antes de criar uma partida.");
        if (pelada.getStatus() == PeladaStatus.FINALIZADA)
            throw new RegraDeNegocioException("Esta pelada já foi finalizada.");
        if (partidaRepository.existsByPeladaIdAndStatusNot(peladaId, PartidaStatus.ENCERRADA))
            throw new RegraDeNegocioException("Já existe uma partida em aberto. Encerre-a antes de criar outra.");
        if (req.timeAId().equals(req.timeBId()))
            throw new RegraDeNegocioException("Escolha dois times diferentes.");

        TimePelada a = timeDaPelada(req.timeAId(), peladaId);
        TimePelada b = timeDaPelada(req.timeBId(), peladaId);

        int ordem = partidaRepository.findFirstByPeladaIdOrderByOrdemDesc(peladaId)
                .map(p -> p.getOrdem() + 1).orElse(1);
        Partida partida = partidaRepository.save(new Partida(peladaId, ordem, a.getId(), b.getId(),
                req.duracaoMinutos() * 60, req.golsParaEncerrar()));

        escalar(partida, a);
        escalar(partida, b);

        pelada.setStatus(PeladaStatus.EM_ANDAMENTO);
        pelada.setDuracaoMinutos(req.duracaoMinutos());
        pelada.setGolsParaEncerrar(req.golsParaEncerrar());
        peladaRepository.save(pelada);
        return montar(partida);
    }

    /**
     * Escalação inicial = integrantes do time no sorteio (só jogadores de linha; goleiros não entram
     * no sorteio). Ninguém começa marcado como goleiro: o ADM escolhe quem vai para o gol de cada time
     * antes de iniciar a partida, em {@link #definirGoleiro}.
     */
    private void escalar(Partida partida, TimePelada time) {
        List<Long> ids = timePeladaJogadorRepository.findByTimeIdIn(List.of(time.getId())).stream()
                .map(TimePeladaJogador::getJogadorId).toList();
        List<Jogador> jogadores = jogadorRepository.findAllById(ids).stream()
                .sorted(Comparator.comparing(Jogador::getNome, String.CASE_INSENSITIVE_ORDER)).toList();
        for (Jogador j : jogadores)
            partidaJogadorRepository.save(new PartidaJogador(partida.getId(), time.getId(), j.getId(), false));
    }

    // ------------------------------------------------------------------ consulta

    @Transactional
    public PartidaResponse obter(Long id) {
        return montar(carregar(id));
    }

    // ------------------------------------------------------------------ cronômetro

    @Transactional
    public PartidaResponse iniciar(Long id) {
        Partida p = carregar(id);
        exigir(p, PartidaStatus.AGUARDANDO);
        p.setStatus(PartidaStatus.EM_ANDAMENTO);
        p.setRetomadaEmMs(clock.millis());
        return montar(partidaRepository.save(p));
    }

    @Transactional
    public PartidaResponse pausar(Long id) {
        Partida p = carregar(id);
        exigir(p, PartidaStatus.EM_ANDAMENTO);
        congelarRelogio(p);
        p.setStatus(PartidaStatus.PAUSADA);
        return montar(partidaRepository.save(p));
    }

    @Transactional
    public PartidaResponse retomar(Long id) {
        Partida p = carregar(id);
        exigir(p, PartidaStatus.PAUSADA);
        p.setStatus(PartidaStatus.EM_ANDAMENTO);
        p.setRetomadaEmMs(clock.millis());
        return montar(partidaRepository.save(p));
    }

    /**
     * Reinicia do zero: cronômetro 0, placar 0 e volta a AGUARDANDO. As escalações atuais são mantidas
     * (quem já tinha saído numa substituição deixa de constar como participante).
     */
    @Transactional
    public PartidaResponse reiniciar(Long id) {
        Partida p = carregar(id);
        if (p.getStatus() != PartidaStatus.EM_ANDAMENTO && p.getStatus() != PartidaStatus.PAUSADA
                && p.getStatus() != PartidaStatus.EMPATE_PENDENTE)
            throw new RegraDeNegocioException("Só é possível reiniciar uma partida em andamento, pausada ou empatada.");
        golRepository.deleteByPartidaId(id);
        substituicaoRepository.deleteByPartidaId(id);
        partidaJogadorRepository.deleteAll(
                partidaJogadorRepository.findByPartidaId(id).stream().filter(j -> !j.isEmCampo()).toList());
        p.setAcumuladoMs(0);
        p.setRetomadaEmMs(null);
        p.setStatus(PartidaStatus.AGUARDANDO);
        return montar(partidaRepository.save(p));
    }

    /** Encerra manualmente (aperto do botão): vale o placar do momento. */
    @Transactional
    public PartidaResponse encerrar(Long id) {
        Partida p = carregar(id);
        if (!p.getStatus().emJogo())
            throw new RegraDeNegocioException("A partida não está em andamento.");
        congelarRelogio(p);
        fimDoTempo(p);
        return montar(p);
    }

    /** Descarta uma partida que ainda não começou (criada por engano). */
    @Transactional
    public void cancelar(Long id) {
        Partida p = carregar(id);
        exigir(p, PartidaStatus.AGUARDANDO);
        partidaJogadorRepository.deleteAll(partidaJogadorRepository.findByPartidaId(id));
        partidaRepository.delete(p);
        if (!partidaRepository.existsByPeladaId(p.getPeladaId())) {
            peladaRepository.findById(p.getPeladaId()).ifPresent(pel -> {
                pel.setStatus(PeladaStatus.SORTEADA);
                peladaRepository.save(pel);
            });
        }
    }

    // ------------------------------------------------------------------ gols

    @Transactional
    public PartidaResponse registrarGol(Long id, GolRequest req) {
        Partida p = carregar(id);
        if (!p.getStatus().emJogo())
            throw new RegraDeNegocioException("Só é possível marcar gol com a partida em andamento ou pausada.");
        exigirTimeDaPartida(p, req.timeId());

        List<PartidaJogador> emCampo = partidaJogadorRepository.findByPartidaId(id).stream()
                .filter(j -> j.isEmCampo() && j.getTimeId().equals(req.timeId())).toList();
        if (emCampo.stream().noneMatch(j -> j.getJogadorId().equals(req.autorId())))
            throw new RegraDeNegocioException("O autor do gol precisa estar em campo neste time.");
        if (req.assistenteId() != null) {
            if (req.assistenteId().equals(req.autorId()))
                throw new RegraDeNegocioException("O assistente não pode ser o próprio autor do gol.");
            if (emCampo.stream().noneMatch(j -> j.getJogadorId().equals(req.assistenteId())))
                throw new RegraDeNegocioException("O assistente precisa estar em campo neste time.");
        }

        golRepository.save(new Gol(id, req.timeId(), req.autorId(), req.assistenteId(),
                Math.min(p.decorridoMs(clock.millis()), p.duracaoMs())));

        // Chegou ao limite de gols: a partida acaba na hora.
        if (golRepository.countByPartidaIdAndTimeId(id, req.timeId()) >= p.getGolsParaEncerrar()) {
            congelarRelogio(p);
            fimDoTempo(p);
        }
        return montar(p);
    }

    @Transactional
    public PartidaResponse desfazerGol(Long id, Long golId) {
        Partida p = carregar(id);
        if (!p.getStatus().emJogo())
            throw new RegraDeNegocioException("Só é possível desfazer gols com a partida em andamento ou pausada.");
        Gol gol = golRepository.findById(golId)
                .filter(g -> g.getPartidaId().equals(id))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Gol não encontrado."));
        golRepository.delete(gol);
        return montar(p);
    }

    // ------------------------------------------------------------------ substituições e goleiro

    @Transactional
    public PartidaResponse substituir(Long id, SubstituicaoRequest req) {
        Partida p = carregar(id);
        exigirAberta(p);
        exigirTimeDaPartida(p, req.timeId());

        List<PartidaJogador> todos = partidaJogadorRepository.findByPartidaId(id);
        PartidaJogador saiu = null;
        if (req.saiuId() != null) {
            saiu = todos.stream()
                    .filter(j -> j.getJogadorId().equals(req.saiuId()) && j.isEmCampo() && j.getTimeId().equals(req.timeId()))
                    .findFirst()
                    .orElseThrow(() -> new RegraDeNegocioException("Quem sai precisa estar em campo neste time."));
            if (req.saiuId().equals(req.entrouId()))
                throw new RegraDeNegocioException("Escolha um jogador diferente para entrar.");
        }
        if (!peladaJogadorRepository.existsByPeladaIdAndJogadorId(p.getPeladaId(), req.entrouId()))
            throw new RegraDeNegocioException("Quem entra precisa estar na lista da pelada.");
        PartidaJogador entrou = todos.stream()
                .filter(j -> j.getJogadorId().equals(req.entrouId())).findFirst().orElse(null);
        if (entrou != null && entrou.isEmCampo())
            throw new RegraDeNegocioException("Esse jogador já está em campo.");

        boolean herdaGoleiro = saiu != null && saiu.isGoleiro();
        if (saiu != null) {
            if (p.getStatus() == PartidaStatus.AGUARDANDO) {
                // Ajuste de escalação antes do apito: quem saiu nunca chegou a jogar.
                partidaJogadorRepository.delete(saiu);
            } else {
                saiu.setEmCampo(false);
                saiu.setGoleiro(false);
                partidaJogadorRepository.save(saiu);
            }
        }
        if (entrou == null) {
            entrou = new PartidaJogador(id, req.timeId(), req.entrouId(), herdaGoleiro);
        } else {
            entrou.setTimeId(req.timeId());
            entrou.setEmCampo(true);
            entrou.setGoleiro(herdaGoleiro);
        }
        partidaJogadorRepository.save(entrou);

        if (p.getStatus() != PartidaStatus.AGUARDANDO) {
            substituicaoRepository.save(new Substituicao(id, req.timeId(),
                    req.saiuId(), req.entrouId(), Math.min(p.decorridoMs(clock.millis()), p.duracaoMs())));
        }
        return montar(p);
    }

    /**
     * Define quem está no gol de um time. Qualquer jogador da lista da pelada pode agarrar — não precisa
     * já estar escalado nesse time (nem em nenhum): se for de fora, ele "entra" só para o gol, sem que
     * ninguém precise sair. O desempenho dele nessa partida conta no overall normalmente.
     */
    @Transactional
    public PartidaResponse definirGoleiro(Long id, GoleiroRequest req) {
        Partida p = carregar(id);
        exigirAberta(p);
        exigirTimeDaPartida(p, req.timeId());

        List<PartidaJogador> todos = partidaJogadorRepository.findByPartidaId(id);
        todos.stream().filter(j -> j.getTimeId().equals(req.timeId()) && j.isGoleiro()).forEach(j -> {
            j.setGoleiro(false);
            partidaJogadorRepository.save(j);
        });

        if (req.jogadorId() == null) return montar(p);

        PartidaJogador alvo = todos.stream()
                .filter(j -> j.getJogadorId().equals(req.jogadorId())).findFirst().orElse(null);
        if (alvo != null) {
            if (alvo.isEmCampo() && !alvo.getTimeId().equals(req.timeId()))
                throw new RegraDeNegocioException("Esse jogador já está em campo no outro time.");
            alvo.setTimeId(req.timeId());
            alvo.setEmCampo(true);
            alvo.setGoleiro(true);
            partidaJogadorRepository.save(alvo);
        } else {
            if (!peladaJogadorRepository.existsByPeladaIdAndJogadorId(p.getPeladaId(), req.jogadorId()))
                throw new RegraDeNegocioException("Esse jogador precisa estar na lista da pelada.");
            partidaJogadorRepository.save(new PartidaJogador(id, req.timeId(), req.jogadorId(), true));
        }
        return montar(p);
    }

    // ------------------------------------------------------------------ empate e pênaltis

    @Transactional
    public PartidaResponse encerrarEmpate(Long id) {
        Partida p = carregar(id);
        exigir(p, PartidaStatus.EMPATE_PENDENTE);
        encerrar(p, null);
        return montar(p);
    }

    /**
     * Abre a disputa de pênaltis no modo escolhido:
     * NORMAL = todos batem (cobranças por time = jogadores do maior time em campo);
     * PREDETERMINADO = o ADM informa quantas cobranças por time; ALTERNADO = direto nas alternadas.
     * Empatadas as cobranças previstas, a disputa segue nas alternadas.
     */
    @Transactional
    public PartidaResponse iniciarPenaltis(Long id, IniciarPenaltisRequest req) {
        Partida p = carregar(id);
        exigir(p, PartidaStatus.EMPATE_PENDENTE);
        int rodadas = switch (req.modo()) {
            case NORMAL -> {
                List<PartidaJogador> emCampo = partidaJogadorRepository.findByPartidaId(id).stream()
                        .filter(PartidaJogador::isEmCampo).toList();
                long a = emCampo.stream().filter(j -> j.getTimeId().equals(p.getTimeAId())).count();
                long b = emCampo.size() - a;
                yield (int) Math.max(1, Math.max(a, b));
            }
            case PREDETERMINADO -> {
                if (req.quantidade() == null)
                    throw new RegraDeNegocioException("Informe quantas cobranças cada time vai bater.");
                yield req.quantidade();
            }
            case ALTERNADO -> 0;
        };
        p.setPenaltisModo(req.modo());
        p.setPenaltisRodadas(rodadas);
        p.setStatus(PartidaStatus.PENALTIS);
        return montar(partidaRepository.save(p));
    }

    @Transactional
    public PartidaResponse registrarPenalti(Long id, PenaltiRequest req) {
        Partida p = carregar(id);
        exigir(p, PartidaStatus.PENALTIS);
        List<Penalti> cobrancas = penaltiRepository.findByPartidaIdOrderByOrdemAsc(id);
        List<PartidaJogador> participantes = partidaJogadorRepository.findByPartidaId(id);
        PartidaResponse.Penaltis situacao = situacaoPenaltis(p, cobrancas, participantes);
        if (situacao.decididoParaTimeId() != null)
            throw new RegraDeNegocioException("A disputa já está decidida: confirme o vencedor ou desfaça a última cobrança.");

        int ordem = cobrancas.size() + 1;
        Long timeId = situacao.proximoTimeId();
        boolean cobradorDoTime = participantes.stream()
                .anyMatch(j -> j.isEmCampo() && j.getTimeId().equals(timeId) && j.getJogadorId().equals(req.jogadorId()));
        if (!cobradorDoTime)
            throw new RegraDeNegocioException("O cobrador precisa ser um jogador do time da vez.");
        List<Long> jaBateram = timeId.equals(p.getTimeAId()) ? situacao.cicloA() : situacao.cicloB();
        if (jaBateram.contains(req.jogadorId()))
            throw new RegraDeNegocioException("Esse jogador já bateu neste ciclo: só repete depois que todos do time baterem.");

        penaltiRepository.save(new Penalti(id, ordem, timeId, req.jogadorId(), req.convertido()));
        return montar(p);
    }

    @Transactional
    public PartidaResponse desfazerPenalti(Long id) {
        Partida p = carregar(id);
        exigir(p, PartidaStatus.PENALTIS);
        Penalti ultimo = penaltiRepository.findFirstByPartidaIdOrderByOrdemDesc(id)
                .orElseThrow(() -> new RegraDeNegocioException("Ainda não há cobranças para desfazer."));
        penaltiRepository.delete(ultimo);
        return montar(p);
    }

    /** Confirma o vencedor da disputa (que fica na quadra) e fecha a partida; no placar segue empate. */
    @Transactional
    public PartidaResponse encerrarPenaltis(Long id) {
        Partida p = carregar(id);
        exigir(p, PartidaStatus.PENALTIS);
        Long vencedor = situacaoPenaltis(p, penaltiRepository.findByPartidaIdOrderByOrdemAsc(id),
                partidaJogadorRepository.findByPartidaId(id)).decididoParaTimeId();
        if (vencedor == null)
            throw new RegraDeNegocioException("A disputa de pênaltis ainda não tem vencedor.");
        p.setVencedorPenaltisId(vencedor);
        encerrar(p, null);
        return montar(p);
    }

    // ------------------------------------------------------------------ internos

    /** Carrega a partida e, se o tempo acabou, aplica o fim do tempo regulamentar. */
    private Partida carregar(Long id) {
        Partida p = partidaRepository.findById(id).orElseThrow(
                () -> new RecursoNaoEncontradoException("Partida não encontrada."));
        if (p.getStatus() == PartidaStatus.EM_ANDAMENTO && p.decorridoMs(clock.millis()) >= p.duracaoMs()) {
            p.setAcumuladoMs(p.duracaoMs());
            p.setRetomadaEmMs(null);
            fimDoTempo(p);
        }
        return p;
    }

    private void congelarRelogio(Partida p) {
        p.setAcumuladoMs(Math.min(p.decorridoMs(clock.millis()), p.duracaoMs()));
        p.setRetomadaEmMs(null);
    }

    /** Fim do tempo (ou do limite de gols): quem tem mais gols vence; empate espera a decisão do ADM. */
    private void fimDoTempo(Partida p) {
        long a = golRepository.countByPartidaIdAndTimeId(p.getId(), p.getTimeAId());
        long b = golRepository.countByPartidaIdAndTimeId(p.getId(), p.getTimeBId());
        if (a == b) {
            p.setStatus(PartidaStatus.EMPATE_PENDENTE);
            partidaRepository.save(p);
        } else {
            p.setVencedorId(a > b ? p.getTimeAId() : p.getTimeBId());
            encerrar(p, p.getVencedorId());
        }
    }

    /** Fecha a partida e grava uma linha de estatística para cada participante. */
    private void encerrar(Partida p, Long vencedorId) {
        p.setVencedorId(vencedorId);
        p.setStatus(PartidaStatus.ENCERRADA);
        p.setRetomadaEmMs(null);
        p.setEncerradaEm(LocalDateTime.now(clock));
        partidaRepository.save(p);
        registrarEstatisticas(p);
    }

    private void registrarEstatisticas(Partida p) {
        Pelada pelada = peladaRepository.findById(p.getPeladaId()).orElseThrow();
        List<Gol> gols = golRepository.findByPartidaIdOrderByIdAsc(p.getId());
        long golsA = gols.stream().filter(g -> g.getTimeId().equals(p.getTimeAId())).count();
        long golsB = gols.size() - golsA;

        for (PartidaJogador pj : partidaJogadorRepository.findByPartidaId(p.getId())) {
            boolean doTimeA = pj.getTimeId().equals(p.getTimeAId());
            long pro = doTimeA ? golsA : golsB;
            long contra = doTimeA ? golsB : golsA;
            Resultado resultado = pro > contra ? Resultado.VITORIA : pro == contra ? Resultado.EMPATE : Resultado.DERROTA;
            int golsDele = (int) gols.stream().filter(g -> g.getAutorId().equals(pj.getJogadorId())).count();
            int assistencias = (int) gols.stream()
                    .filter(g -> pj.getJogadorId().equals(g.getAssistenteId())).count();
            estatisticaRepository.save(new EstatisticaPartida(p.getId(), p.getPeladaId(), pelada.getDataJogo(),
                    pj.getJogadorId(), pj.getTimeId(), resultado, golsDele, assistencias, pj.isGoleiro(), (int) contra,
                    NotaCalculator.calcular(resultado, golsDele, assistencias, pj.isGoleiro(), (int) contra)));
        }
    }

    private TimePelada timeDaPelada(Long timeId, Long peladaId) {
        return timePeladaRepository.findById(timeId)
                .filter(t -> t.getPeladaId().equals(peladaId))
                .orElseThrow(() -> new RegraDeNegocioException("Time inválido para esta pelada."));
    }

    private void exigir(Partida p, PartidaStatus esperado) {
        if (p.getStatus() != esperado)
            throw new RegraDeNegocioException("Ação indisponível: a partida está " + p.getStatus().name().toLowerCase().replace('_', ' ') + ".");
    }

    private void exigirAberta(Partida p) {
        if (p.getStatus() != PartidaStatus.AGUARDANDO && !p.getStatus().emJogo())
            throw new RegraDeNegocioException("A partida não permite alterar escalações agora.");
    }

    private void exigirTimeDaPartida(Partida p, Long timeId) {
        if (!p.getTimeAId().equals(timeId) && !p.getTimeBId().equals(timeId))
            throw new RegraDeNegocioException("Esse time não está na partida.");
    }

    // ------------------------------------------------------------------ resposta

    private PartidaResponse.Penaltis situacaoPenaltis(Partida p, List<Penalti> cobrancas,
                                                      List<PartidaJogador> participantes) {
        int cobrancasA = 0, cobrancasB = 0, golsA = 0, golsB = 0;
        List<Long> batedoresA = new ArrayList<>(), batedoresB = new ArrayList<>();
        for (Penalti c : cobrancas) {
            if (c.getTimeId().equals(p.getTimeAId())) {
                cobrancasA++; batedoresA.add(c.getJogadorId()); if (c.isConvertido()) golsA++;
            } else {
                cobrancasB++; batedoresB.add(c.getJogadorId()); if (c.isConvertido()) golsB++;
            }
        }
        int rodadas = p.getPenaltisRodadas();
        int v = PenaltisRegra.vencedor(cobrancasA, cobrancasB, golsA, golsB, rodadas);
        Long decidido = v > 0 ? p.getTimeAId() : v < 0 ? p.getTimeBId() : null;
        Long proximo = decidido != null ? null : (cobrancas.size() % 2 == 0 ? p.getTimeAId() : p.getTimeBId());

        // "Ciclo": quem já bateu desde que o time inteiro bateu pela última vez.
        Set<Long> elencoA = elencoEmCampo(participantes, p.getTimeAId());
        Set<Long> elencoB = elencoEmCampo(participantes, p.getTimeBId());
        List<Long> cicloA = List.copyOf(PenaltisRegra.jaBateramNoCiclo(batedoresA, elencoA));
        List<Long> cicloB = List.copyOf(PenaltisRegra.jaBateramNoCiclo(batedoresB, elencoB));

        List<PartidaResponse.Cobranca> lista = cobrancas.stream()
                .map(c -> new PartidaResponse.Cobranca(c.getOrdem(), c.getTimeId(), c.getJogadorId(), c.isConvertido()))
                .toList();
        return new PartidaResponse.Penaltis(golsA, golsB, cobrancasA, cobrancasB, p.getPenaltisModo(), rodadas,
                PenaltisRegra.emAlternadas(cobrancasA, cobrancasB, rodadas),
                proximo, decidido, cicloA, cicloB, lista);
    }

    private static Set<Long> elencoEmCampo(List<PartidaJogador> participantes, Long timeId) {
        return participantes.stream().filter(j -> j.isEmCampo() && j.getTimeId().equals(timeId))
                .map(PartidaJogador::getJogadorId).collect(Collectors.toSet());
    }

    private PartidaResponse montar(Partida p) {
        long agora = clock.millis();
        Map<Long, TimePelada> times = timePeladaRepository.findAllById(List.of(p.getTimeAId(), p.getTimeBId()))
                .stream().collect(Collectors.toMap(TimePelada::getId, Function.identity()));
        List<Gol> gols = golRepository.findByPartidaIdOrderByIdAsc(p.getId());
        List<PartidaJogador> participantes = partidaJogadorRepository.findByPartidaId(p.getId());

        PartidaResponse.Lado ladoA = lado(p.getTimeAId(), times, gols, participantes);
        PartidaResponse.Lado ladoB = lado(p.getTimeBId(), times, gols, participantes);

        List<PartidaResponse.GolItem> golItens = gols.stream()
                .map(g -> new PartidaResponse.GolItem(g.getId(), g.getTimeId(), g.getAutorId(), g.getAssistenteId(), g.getTempoMs()))
                .toList();
        List<PartidaResponse.SubstituicaoItem> subs = substituicaoRepository.findByPartidaIdOrderByIdAsc(p.getId()).stream()
                .map(s -> new PartidaResponse.SubstituicaoItem(s.getId(), s.getTimeId(), s.getSaiuId(), s.getEntrouId(), s.getTempoMs()))
                .toList();

        PartidaResponse.Penaltis penaltis = null;
        if (p.getStatus() == PartidaStatus.PENALTIS || p.getVencedorPenaltisId() != null)
            penaltis = situacaoPenaltis(p, penaltiRepository.findByPartidaIdOrderByOrdemAsc(p.getId()), participantes);

        long decorrido = Math.min(p.decorridoMs(agora), p.duracaoMs());
        return new PartidaResponse(p.getId(), p.getPeladaId(), p.getOrdem(), p.getStatus(),
                p.getDuracaoSegundos(), p.getGolsParaEncerrar(), decorrido, p.relogioCorrendo(), agora,
                ladoA, ladoB, p.getVencedorId(), p.getVencedorPenaltisId(), golItens, subs, penaltis);
    }

    private PartidaResponse.Lado lado(Long timeId, Map<Long, TimePelada> times, List<Gol> gols,
                                      List<PartidaJogador> participantes) {
        int golsDoTime = (int) gols.stream().filter(g -> g.getTimeId().equals(timeId)).count();
        List<PartidaResponse.Atleta> atletas = participantes.stream()
                .filter(j -> j.getTimeId().equals(timeId))
                .map(j -> new PartidaResponse.Atleta(j.getJogadorId(), j.isGoleiro(), j.isEmCampo()))
                .toList();
        return new PartidaResponse.Lado(timeId, times.get(timeId).getNumero(), golsDoTime, atletas);
    }
}
