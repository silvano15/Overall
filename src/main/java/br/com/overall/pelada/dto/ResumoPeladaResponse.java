package br.com.overall.pelada.dto;

import br.com.overall.pelada.PeladaStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Tela final da pelada: ranking de times, artilheiros, assistentes, notas e evolução do overall. */
public record ResumoPeladaResponse(
        Long peladaId,
        LocalDate dataJogo,
        LocalTime horaJogo,
        PeladaStatus status,
        int partidasDisputadas,
        List<ClassificacaoTime> times,
        List<ItemRanking> artilheiros,
        List<ItemRanking> assistentes,
        /** Ordenado pela nota do dia. */
        List<DesempenhoJogador> jogadores
) {
    public record ClassificacaoTime(Long timeId, int numero, int jogos, int vitorias, int empates, int derrotas,
                                    int golsPro, int golsContra, int saldo) {}

    public record ItemRanking(Long jogadorId, String nome, long valor) {}

    /** overallAntes/overallDepois ficam nulos até a pelada ser finalizada. */
    public record DesempenhoJogador(Long jogadorId, String nome, int partidas, int vitorias, int empates,
                                    int derrotas, int gols, int assistencias, int semSofrer, Double nota,
                                    Integer overallAntes, Integer overallDepois) {}
}
