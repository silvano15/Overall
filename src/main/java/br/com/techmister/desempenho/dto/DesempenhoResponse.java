package br.com.techmister.desempenho.dto;

import br.com.techmister.desempenho.Desempenho;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DesempenhoResponse(
        Long id,
        Long partidaId,
        String adversario,
        LocalDate dataJogo,
        Long jogadorId,
        String jogadorNome,
        BigDecimal nota,
        Integer gols,
        Integer assistencias,
        Integer cartoesAmarelos,
        Integer cartoesVermelhos,
        LocalDateTime criadoEm
) {

    public static DesempenhoResponse de(Desempenho d) {
        return new DesempenhoResponse(
                d.getId(),
                d.getPartida().getId(),
                d.getPartida().getAdversario(),
                d.getPartida().getDataJogo(),
                d.getJogador().getId(),
                d.getJogador().getNome(),
                d.getNota(),
                d.getGols(),
                d.getAssistencias(),
                d.getCartoesAmarelos(),
                d.getCartoesVermelhos(),
                d.getCriadoEm()
        );
    }
}
