package br.com.techmister.timeideal.dto;

import br.com.techmister.jogador.Posicao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TimeIdealResponse(
        LocalDate inicioSemana,
        LocalDate fimSemana,
        List<JogadorAvaliado> titulares,
        List<JogadorAvaliado> reservas,
        String alerta
) {

    public record JogadorAvaliado(
            Long jogadorId,
            String nome,
            Posicao posicao,
            BigDecimal mediaFinal,
            BigDecimal mediaTreino,
            BigDecimal mediaComportamento,
            BigDecimal mediaDesempenho
    ) {}
}
