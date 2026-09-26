package br.com.overall.grupo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Nome do grupo de pelada (ex.: "Pelada do trabalho", "Galera da rua"). */
public record GrupoRequest(
        @NotBlank(message = "O nome do grupo é obrigatório")
        @Size(max = 80, message = "O nome deve ter no máximo 80 caracteres")
        String nome
) {}
