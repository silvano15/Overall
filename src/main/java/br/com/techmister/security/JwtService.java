package br.com.techmister.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

/**
 * Gera e valida tokens JWT.
 *
 * <p>O token é o "crachá" que o cliente apresenta em cada requisição depois do login.
 * Carregamos dentro dele o username e a data de expiração — e assinamos com uma chave
 * secreta para que ninguém consiga forjar.
 */
@Service
public class JwtService {

    private final SecretKey chave;
    private final long expiracaoMs;

    public JwtService(@Value("${techmister.jwt.secret}") String secret,
                      @Value("${techmister.jwt.expiration-hours}") long expiracaoHoras) {
        // A chave precisa ter no mínimo 256 bits para HS256.
        // Se o segredo do application.yml for curto, este construtor lançará exceção na inicialização.
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        this.chave = Keys.hmacShaKeyFor(bytes);
        this.expiracaoMs = expiracaoHoras * 60 * 60 * 1000;
    }

    /** Gera um token JWT para o usuário autenticado. */
    public String gerarToken(UserDetails usuario) {
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + expiracaoMs);

        return Jwts.builder()
                .subject(usuario.getUsername())
                .issuedAt(agora)
                .expiration(expiracao)
                .signWith(chave)
                .compact();
    }

    public String extrairUsername(String token) {
        return extrairClaim(token, Claims::getSubject);
    }

    public boolean tokenValido(String token, UserDetails usuario) {
        try {
            final String username = extrairUsername(token);
            return username.equals(usuario.getUsername()) && !expirou(token);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean expirou(String token) {
        return extrairClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extrairClaim(String token, Function<Claims, T> extrator) {
        Claims claims = Jwts.parser()
                .verifyWith(chave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return extrator.apply(claims);
    }
}
