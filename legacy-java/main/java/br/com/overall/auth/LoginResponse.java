package br.com.overall.auth;

public record LoginResponse(
        String token,
        String tipoToken,
        String username,
        String perfil
) {}
