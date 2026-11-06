package br.com.overall.partida.dto;

import jakarta.validation.constraints.NotNull;

/** Cobrança de pênalti: o time da vez é definido pelo sistema (alternado); o ADM diz quem bateu e se fez. */
public record PenaltiRequest(
        @NotNull(message = "Escolha quem cobrou")
        Long jogadorId,

        @NotNull(message = "Informe se foi gol")
        Boolean convertido
) {}
