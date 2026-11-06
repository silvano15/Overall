package br.com.overall.partida.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Configuração de uma partida: quem joga, quantos minutos dura e quantos gols a encerram. */
public record PartidaRequest(
        @NotNull(message = "Escolha o time A")
        Long timeAId,

        @NotNull(message = "Escolha o time B")
        Long timeBId,

        @NotNull(message = "Informe a duração em minutos")
        @Min(value = 1, message = "A duração mínima é 1 minuto")
        @Max(value = 90, message = "A duração máxima é 90 minutos")
        Integer duracaoMinutos,

        @NotNull(message = "Informe quantos gols encerram a partida")
        @Min(value = 1, message = "É preciso ao menos 1 gol")
        @Max(value = 99, message = "Limite de gols inválido")
        Integer golsParaEncerrar
) {}
