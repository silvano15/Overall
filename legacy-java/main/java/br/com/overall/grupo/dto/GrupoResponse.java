package br.com.overall.grupo.dto;

/** Card do grupo na biblioteca: nome + números rápidos, sem precisar entrar no grupo. */
public record GrupoResponse(
        Long id,
        String nome,
        long jogadores,
        long peladas,
        boolean peladaAberta
) {}
