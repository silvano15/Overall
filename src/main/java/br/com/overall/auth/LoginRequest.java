package br.com.overall.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Informe o usuário")
        String username,

        @NotBlank(message = "Informe a senha")
        String senha
) {}
