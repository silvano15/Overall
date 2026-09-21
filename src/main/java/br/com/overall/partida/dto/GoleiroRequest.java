package br.com.overall.partida.dto;

import jakarta.validation.constraints.NotNull;

/** Define o goleiro de um time na partida. 'jogadorId' nulo = time sem goleiro fixo. */
public record GoleiroRequest(
        @NotNull(message = "Informe o time")
        Long timeId,

        Long jogadorId
) {}
