package br.com.overall.pelada.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/** Data e horário em que a pelada acontece. */
public record PeladaRequest(
        @NotNull(message = "Informe a data da pelada")
        LocalDate dataJogo,

        @NotNull(message = "Informe o horário da pelada")
        LocalTime horaJogo
) {}
