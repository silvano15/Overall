package br.com.overall.partida.dto;

import jakarta.validation.constraints.NotNull;

/** Troca de jogador. 'saiuId' nulo = reforço para completar um time incompleto. */
public record SubstituicaoRequest(
        @NotNull(message = "Informe o time")
        Long timeId,

        Long saiuId,

        @NotNull(message = "Escolha quem vai entrar")
        Long entrouId
) {}
