package br.com.techmister.partida.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PartidaRequest(
        @NotBlank(message = "Informe o adversário da partida")
        @Size(max = 120, message = "O nome do adversário deve ter no máximo 120 caracteres")
        String adversario,

        @NotNull(message = "Informe a data da partida")
        LocalDate dataJogo,

        @PositiveOrZero(message = "Gols pró deve ser zero ou positivo")
        Integer golsPro,

        @PositiveOrZero(message = "Gols contra deve ser zero ou positivo")
        Integer golsContra,

        @Size(max = 120, message = "O local deve ter no máximo 120 caracteres")
        String local
) {}
