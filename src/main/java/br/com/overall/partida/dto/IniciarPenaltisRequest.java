package br.com.overall.partida.dto;

import br.com.overall.partida.PenaltisModo;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Como será a disputa. {@code quantidade} (cobranças por time) só é usada no modo PREDETERMINADO.
 */
public record IniciarPenaltisRequest(
        @NotNull(message = "Escolha o tipo de disputa")
        PenaltisModo modo,

        @Min(value = 1, message = "A quantidade mínima é 1 cobrança por time")
        @Max(value = 20, message = "A quantidade máxima é 20 cobranças por time")
        Integer quantidade
) {}
