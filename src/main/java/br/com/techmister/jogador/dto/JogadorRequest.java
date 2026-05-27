package br.com.techmister.jogador.dto;

import br.com.techmister.jogador.Posicao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload de entrada para criar (RF-002) ou alterar (RF-003) um jogador.
 */
public record JogadorRequest(
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
        String nome,

        @NotBlank(message = "O documento é obrigatório")
        @Size(max = 20, message = "O documento deve ter no máximo 20 caracteres")
        String documento,

        @NotNull(message = "A posição é obrigatória")
        Posicao posicao,

        @Positive(message = "A altura deve ser maior que zero")
        Integer alturaCm,

        @Positive(message = "O peso deve ser maior que zero")
        BigDecimal pesoKg,

        @Past(message = "A data de nascimento deve ser no passado")
        LocalDate dataNascimento
) {}
