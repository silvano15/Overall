package br.com.overall.partida.dto;

import jakarta.validation.constraints.NotNull;

/** Gol: time que marcou, autor e (opcional) assistente — sem assistente, ex.: pênalti. */
public record GolRequest(
        @NotNull(message = "Informe o time que fez o gol")
        Long timeId,

        @NotNull(message = "Escolha quem fez o gol")
        Long autorId,

        Long assistenteId
) {}
