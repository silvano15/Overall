package br.com.overall.ranking.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Rede de interação de um período: quem serve quem. Cada aresta é "assistente → autor do gol" e o peso é
 * quantas vezes essa dupla marcou junta. Os nós trazem o necessário para desenhar o grafo.
 */
public record ConexoesResponse(
        String periodo,
        LocalDate de,
        LocalDate ate,
        List<No> nos,
        List<Aresta> arestas
) {
    public record No(Long jogadorId, String nome, int overall, long gols, long assistencias) {}

    public record Aresta(Long assistenteId, Long autorId, long quantidade) {}
}
