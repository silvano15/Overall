package br.com.techmister.desempenho.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record DesempenhoRequest(
        @NotNull(message = "Informe a partida")
        Long partidaId,

        @NotNull(message = "Informe o jogador")
        Long jogadorId,

        @NotNull(message = "Informe a nota")
        @DecimalMin(value = "0.0", message = "A nota deve estar entre 0 e 10")
        @DecimalMax(value = "10.0", message = "A nota deve estar entre 0 e 10")
        BigDecimal nota,

        @NotNull
        @PositiveOrZero(message = "Gols deve ser zero ou positivo")
        Integer gols,

        @NotNull
        @PositiveOrZero(message = "Assistências deve ser zero ou positivo")
        Integer assistencias,

        @NotNull
        @PositiveOrZero(message = "Cartões amarelos deve ser zero ou positivo")
        Integer cartoesAmarelos,

        @NotNull
        @PositiveOrZero(message = "Cartões vermelhos deve ser zero ou positivo")
        Integer cartoesVermelhos
) {}
