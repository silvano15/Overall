package br.com.overall.jogador.dto;

import br.com.overall.estatistica.Estatisticas;
import br.com.overall.jogador.Jogador;

public record JogadorResponse(
        Long id,
        String nome,
        String foto,
        boolean goleiro,
        boolean ativo,
        int overall,
        Estatisticas estatisticas
) {
    public static JogadorResponse de(Jogador j, Estatisticas e) {
        return new JogadorResponse(j.getId(), j.getNome(), j.getFoto(), j.isGoleiro(), j.isAtivo(),
                j.getOverall(), e == null ? Estatisticas.vazia() : e);
    }
}
