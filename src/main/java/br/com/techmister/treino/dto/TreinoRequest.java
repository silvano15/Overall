package br.com.techmister.treino.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

/**
 * Payload de entrada para criar (RF-006) ou alterar (RF-007) um treino.
 *
 * <p>Regras de validação (RF-007 CN02 do documento de estórias):
 * acertos e erros devem ser positivos ou zero — nunca negativos.
 */
public record TreinoRequest(
        @NotNull(message = "Informe o jogador")
        Long jogadorId,

        @NotNull(message = "Informe a data do treino")
        @PastOrPresent(message = "A data do treino não pode ser no futuro")
        LocalDate dataTreino,

        @NotNull(message = "Informe a quantidade de acertos")
        @PositiveOrZero(message = "Os valores devem ser positivos")
        Integer acertos,

        @NotNull(message = "Informe a quantidade de erros")
        @PositiveOrZero(message = "Os valores devem ser positivos")
        Integer erros
) {}
