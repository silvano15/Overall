package br.com.overall.partida.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Define o goleiro de um time na partida. 'jogadorId' nulo = time sem goleiro definido.
 * Não precisa ser um goleiro "fixo" (cadastro) nem já estar escalado nesse time: qualquer jogador
 * da lista da pelada pode ser escolhido para o gol.
 */
public record GoleiroRequest(
        @NotNull(message = "Informe o time")
        Long timeId,

        Long jogadorId
) {}
