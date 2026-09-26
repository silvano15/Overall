package br.com.overall.estatistica;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Converte o resultado bruto das queries de agregação em {@link Estatisticas} por jogador.
 * Colunas devolvidas pelas queries (na ordem): jogadorId, partidas, vitórias, empates, derrotas, gols,
 * assistências, jogosComoGoleiro, semSofrer, golsSofridos, notaMedia.
 */
@Component
public class EstatisticaAgregador {

    private static final LocalDate INICIO = LocalDate.of(2000, 1, 1);
    private static final LocalDate FIM = LocalDate.of(2999, 12, 31);

    private final EstatisticaRepository repository;

    public EstatisticaAgregador(EstatisticaRepository repository) {
        this.repository = repository;
    }

    public Map<Long, Estatisticas> geral(Long grupoId) { return periodo(grupoId, INICIO, FIM); }

    public Map<Long, Estatisticas> periodo(Long grupoId, LocalDate de, LocalDate ate) {
        return converter(repository.agregarPorPeriodo(grupoId, de, ate));
    }

    public Map<Long, Estatisticas> geralExceto(Long grupoId, Long peladaId) {
        return converter(repository.agregarExceto(grupoId, peladaId));
    }

    private static Map<Long, Estatisticas> converter(List<Object[]> linhas) {
        Map<Long, Estatisticas> mapa = new HashMap<>();
        for (Object[] c : linhas) {
            mapa.put(((Number) c[0]).longValue(), new Estatisticas(
                    n(c[1]), n(c[2]), n(c[3]), n(c[4]), n(c[5]), n(c[6]), n(c[7]), n(c[8]), n(c[9]),
                    c[10] == null ? 0 : ((Number) c[10]).doubleValue()));
        }
        return mapa;
    }

    private static long n(Object o) { return o == null ? 0 : ((Number) o).longValue(); }
}
