package br.com.overall.ranking;

import br.com.overall.estatistica.EstatisticaAgregador;
import br.com.overall.estatistica.EstatisticaRepository;
import br.com.overall.estatistica.Estatisticas;
import br.com.overall.exception.RegraDeNegocioException;
import br.com.overall.jogador.Jogador;
import br.com.overall.jogador.JogadorRepository;
import br.com.overall.partida.GolRepository;
import br.com.overall.pelada.PeladaJogadorRepository;
import br.com.overall.ranking.dto.ConexoesResponse;
import br.com.overall.ranking.dto.RankingResponse;
import br.com.overall.ranking.dto.RankingResponse.Item;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Rankings mensal, anual e geral: mais vencedores, goleadores, assistentes, melhores goleiros,
 * melhores notas e maiores overalls. Tudo é agregado da tabela de estatísticas por partida.
 */
@Service
public class RankingService {

    public enum Periodo { MES, ANO, GERAL }

    private static final int LIMITE = 30;

    private final EstatisticaAgregador agregador;
    private final EstatisticaRepository estatisticaRepository;
    private final JogadorRepository jogadorRepository;
    private final PeladaJogadorRepository peladaJogadorRepository;
    private final GolRepository golRepository;
    private final Clock clock;

    public RankingService(EstatisticaAgregador agregador,
                          EstatisticaRepository estatisticaRepository,
                          JogadorRepository jogadorRepository,
                          PeladaJogadorRepository peladaJogadorRepository,
                          GolRepository golRepository,
                          Clock clock) {
        this.agregador = agregador;
        this.estatisticaRepository = estatisticaRepository;
        this.jogadorRepository = jogadorRepository;
        this.peladaJogadorRepository = peladaJogadorRepository;
        this.golRepository = golRepository;
        this.clock = clock;
    }

    /** Início e fim do período pedido (mês/ano em relação a "hoje" quando não informados). */
    private LocalDate[] intervalo(Periodo periodo, Integer ano, Integer mes) {
        LocalDate hoje = LocalDate.now(clock);
        return switch (periodo) {
            case MES -> {
                YearMonth ym = YearMonth.of(ano == null ? hoje.getYear() : ano, mes == null ? hoje.getMonthValue() : mes);
                yield new LocalDate[]{ym.atDay(1), ym.atEndOfMonth()};
            }
            case ANO -> {
                int a = ano == null ? hoje.getYear() : ano;
                yield new LocalDate[]{LocalDate.of(a, 1, 1), LocalDate.of(a, 12, 31)};
            }
            default -> new LocalDate[]{LocalDate.of(2000, 1, 1), LocalDate.of(2999, 12, 31)};
        };
    }

    /**
     * Rede de assistências do período: nós = quem marcou ou serviu alguém; arestas = "assistente → autor" com a
     * quantidade de gols da dupla.
     */
    @Transactional(readOnly = true)
    public ConexoesResponse conexoes(Periodo periodo, Integer ano, Integer mes) {
        LocalDate[] i = intervalo(periodo, ano, mes);
        Map<Long, Estatisticas> stats = agregador.periodo(i[0], i[1]);

        List<ConexoesResponse.Aresta> arestas = golRepository.duplasDeAssistencia(i[0], i[1]).stream()
                .map(l -> new ConexoesResponse.Aresta(((Number) l[0]).longValue(), ((Number) l[1]).longValue(),
                        ((Number) l[2]).longValue()))
                .sorted(Comparator.comparingLong(ConexoesResponse.Aresta::quantidade).reversed())
                .toList();

        Set<Long> ids = new HashSet<>();
        arestas.forEach(a -> { ids.add(a.assistenteId()); ids.add(a.autorId()); });
        stats.forEach((id, e) -> { if (e.gols() > 0 || e.assistencias() > 0) ids.add(id); });

        List<ConexoesResponse.No> nos = jogadorRepository.findAllById(ids).stream()
                .map(j -> {
                    Estatisticas e = stats.getOrDefault(j.getId(), Estatisticas.vazia());
                    return new ConexoesResponse.No(j.getId(), j.getNome(), j.getOverall(), e.gols(), e.assistencias());
                })
                .sorted(Comparator.comparing(ConexoesResponse.No::nome, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return new ConexoesResponse(periodo.name(), i[0], i[1], nos, arestas);
    }

    @Transactional(readOnly = true)
    public RankingResponse ranking(Periodo periodo, Integer ano, Integer mes) {
        LocalDate[] intervalo = intervalo(periodo, ano, mes);
        LocalDate de = intervalo[0], ate = intervalo[1];

        Map<Long, Estatisticas> stats = agregador.periodo(de, ate);
        Map<Long, Jogador> jogadores = jogadorRepository.findAllById(stats.keySet()).stream()
                .collect(Collectors.toMap(Jogador::getId, Function.identity()));

        List<Item> vencedores = ordenar(stats, jogadores, e -> e.vitorias() > 0,
                Estatisticas::vitorias, e -> e.partidas() + " jogos", false);
        List<Item> goleadores = ordenar(stats, jogadores, e -> e.gols() > 0,
                Estatisticas::gols, e -> e.partidas() + " jogos", false);
        List<Item> assistentes = ordenar(stats, jogadores, e -> e.assistencias() > 0,
                Estatisticas::assistencias, e -> e.partidas() + " jogos", false);
        List<Item> goleiros = ordenar(stats, jogadores, e -> e.jogosComoGoleiro() > 0,
                Estatisticas::semSofrer,
                e -> e.jogosComoGoleiro() + " jogos no gol · " + e.golsSofridos() + " gols sofridos", false);
        List<Item> notas = ordenar(stats, jogadores, e -> e.partidas() > 0,
                Estatisticas::notaMedia, e -> e.partidas() + " jogos", true);

        return new RankingResponse(periodo.name(), de, ate, estatisticaRepository.contarPeladas(de, ate),
                vencedores, goleadores, assistentes, goleiros, notas, overalls(de, ate, stats, jogadores));
    }

    public static Periodo periodo(String texto) {
        try {
            return Periodo.valueOf(texto.toUpperCase());
        } catch (Exception e) {
            throw new RegraDeNegocioException("Período inválido. Use MES, ANO ou GERAL.");
        }
    }

    // Ordena por 'valor' (decrescente). Empate: menos partidas primeiro (mesmo resultado em menos jogos é melhor);
    // em notas, mais partidas primeiro (média mais confiável).
    private List<Item> ordenar(Map<Long, Estatisticas> stats, Map<Long, Jogador> jogadores,
                               java.util.function.Predicate<Estatisticas> incluir,
                               ToNumber<Estatisticas> valor, Function<Estatisticas, String> detalhe,
                               boolean maisPartidasDesempata) {
        Comparator<Item> comparador = Comparator.comparingDouble(Item::valor).reversed();
        comparador = maisPartidasDesempata
                ? comparador.thenComparing(Comparator.comparingLong(Item::partidas).reversed())
                : comparador.thenComparingLong(Item::partidas);
        return stats.entrySet().stream()
                .filter(e -> incluir.test(e.getValue()) && jogadores.containsKey(e.getKey()))
                .map(e -> new Item(e.getKey(), jogadores.get(e.getKey()).getNome(),
                        arredondar(valor.aplicar(e.getValue())), e.getValue().partidas(), detalhe.apply(e.getValue())))
                .sorted(comparador.thenComparing(Item::nome, String.CASE_INSENSITIVE_ORDER))
                .limit(LIMITE)
                .toList();
    }

    /** Overall no fim do período (da última pelada finalizada em que jogou) e a variação no período. */
    private List<Item> overalls(LocalDate de, LocalDate ate, Map<Long, Estatisticas> stats, Map<Long, Jogador> jogadores) {
        Map<Long, Integer> primeiroAntes = new HashMap<>();
        Map<Long, Integer> ultimoDepois = new HashMap<>();
        for (Object[] linha : peladaJogadorRepository.evolucaoDoOverall(de, ate)) {
            Long jogadorId = ((Number) linha[0]).longValue();
            if (!stats.containsKey(jogadorId)) continue; // só quem jogou alguma partida no período
            if (linha[1] != null) primeiroAntes.putIfAbsent(jogadorId, ((Number) linha[1]).intValue());
            ultimoDepois.put(jogadorId, ((Number) linha[2]).intValue());
        }
        return ultimoDepois.entrySet().stream()
                .filter(e -> jogadores.containsKey(e.getKey()))
                .map(e -> {
                    int variacao = e.getValue() - primeiroAntes.getOrDefault(e.getKey(), e.getValue());
                    String detalhe = variacao > 0 ? "▲ +" + variacao : variacao < 0 ? "▼ " + variacao : "=";
                    return new Item(e.getKey(), jogadores.get(e.getKey()).getNome(), e.getValue(),
                            stats.get(e.getKey()).partidas(), detalhe);
                })
                .sorted(Comparator.comparingDouble(Item::valor).reversed()
                        .thenComparing(Item::nome, String.CASE_INSENSITIVE_ORDER))
                .limit(LIMITE)
                .toList();
    }

    private static double arredondar(double v) { return Math.round(v * 100.0) / 100.0; }

    /** Extrator numérico (long ou double) sem boxing manual nas chamadas. */
    @FunctionalInterface
    private interface ToNumber<T> {
        double aplicar(T t);
    }
}
