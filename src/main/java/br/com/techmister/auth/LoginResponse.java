package br.com.techmister.auth;

public record LoginResponse(
        String token,
        String tipoToken,
        String username,
        String perfil
) {}
