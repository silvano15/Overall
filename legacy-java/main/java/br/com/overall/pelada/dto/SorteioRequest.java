package br.com.overall.pelada.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SorteioRequest(
        @NotNull(message = "Informe quantos jogadores por time")
        @Min(value = 1, message = "Cada time precisa de ao menos 1 jogador")
        @Max(value = 20, message = "No máximo 20 jogadores por time")
        Integer jogadoresPorTime
) {}
