package br.com.overall.pelada.dto;

import br.com.overall.partida.PartidaStatus;
import br.com.overall.pelada.ModoSorteio;
import br.com.overall.pelada.PeladaStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Tudo que a tela da pelada precisa em uma chamada: lista, times sorteados, partidas e a sugestão
 * de quem joga a seguir. Só ids de jogador: nomes e fotos o cliente já tem em cache (GET /jogadores).
 */
public record PeladaResponse(
        Long id,
        LocalDate dataJogo,
        LocalTime horaJogo,
        PeladaStatus status,
        Integer jogadoresPorTime,
        ModoSorteio modoSorteio,
        Integer duracaoMinutos,
        Integer golsParaEncerrar,
        /** O sorteio só é liberado a partir do dia da pelada. */
        boolean sorteioLiberado,
        List<Long> lista,
        List<Time> times,
        List<PartidaResumo> partidas,
        /** Partida ainda não encerrada (o ADM deve voltar a ela), ou null. */
        Long partidaEmAbertoId,
        Sugestao proximaSugerida
) {
    public record Time(Long id, int numero, double overallMedio, List<Integrante> jogadores) {}

    public record Integrante(Long jogadorId, int overall, boolean goleiro) {}

    public record PartidaResumo(
            Long id, int ordem, PartidaStatus status,
            Long timeAId, int timeANumero, int golsA,
            Long timeBId, int timeBNumero, int golsB,
            Long vencedorId, Long vencedorPenaltisId) {}

    public record Sugestao(Long timeAId, Long timeBId) {}
}
