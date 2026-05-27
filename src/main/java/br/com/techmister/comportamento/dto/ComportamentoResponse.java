package br.com.techmister.comportamento.dto;

import br.com.techmister.comportamento.Comportamento;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ComportamentoResponse(
        Long id,
        Long jogadorId,
        String jogadorNome,
        LocalDate dataAvaliacao,
        Integer notaFoco,
        Integer notaDisciplina,
        Integer notaAtitude,
        LocalDateTime criadoEm
) {

    public static ComportamentoResponse de(Comportamento c) {
        return new ComportamentoResponse(
                c.getId(),
                c.getJogador().getId(),
                c.getJogador().getNome(),
                c.getDataAvaliacao(),
                c.getNotaFoco(),
                c.getNotaDisciplina(),
                c.getNotaAtitude(),
                c.getCriadoEm()
        );
    }
}
