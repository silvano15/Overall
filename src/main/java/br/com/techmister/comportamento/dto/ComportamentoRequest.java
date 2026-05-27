package br.com.techmister.comportamento.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

/**
 * Payload de entrada de comportamento. RF-010 / RF-011:
 * as notas devem estar entre 0 e 10.
 */
public record ComportamentoRequest(
        @NotNull(message = "Informe o jogador")
        Long jogadorId,

        @NotNull(message = "Informe a data da avaliação")
        @PastOrPresent(message = "A data da avaliação não pode ser no futuro")
        LocalDate dataAvaliacao,

        @NotNull(message = "Informe a nota de foco")
        @Min(value = 0, message = "As notas devem estar entre 0 e 10")
        @Max(value = 10, message = "As notas devem estar entre 0 e 10")
        Integer notaFoco,

        @NotNull(message = "Informe a nota de disciplina")
        @Min(value = 0, message = "As notas devem estar entre 0 e 10")
        @Max(value = 10, message = "As notas devem estar entre 0 e 10")
        Integer notaDisciplina,

        @NotNull(message = "Informe a nota de atitude")
        @Min(value = 0, message = "As notas devem estar entre 0 e 10")
        @Max(value = 10, message = "As notas devem estar entre 0 e 10")
        Integer notaAtitude
) {}
