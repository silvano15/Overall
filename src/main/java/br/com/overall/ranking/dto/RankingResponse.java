package br.com.overall.ranking.dto;

import java.time.LocalDate;
import java.util.List;

/** Rankings de um período (mês, ano ou geral). Cada lista já vem ordenada. */
public record RankingResponse(
        String periodo,
        LocalDate de,
        LocalDate ate,
        /** Quantidade de peladas com partidas registradas no período. */
        long peladas,
        List<Item> vencedores,
        List<Item> goleadores,
        List<Item> assistentes,
        List<Item> goleiros,
        List<Item> notas,
        List<Item> overalls
) {
    /**
     * @param valor    o que ordena o ranking (vitórias, gols, gols sem sofrer, nota média, overall...)
     * @param partidas partidas disputadas no período
     * @param detalhe  texto de apoio (ex.: "12 jogos · 3 gols sofridos", "▲ +4")
     */
    public record Item(Long jogadorId, String nome, double valor, long partidas, String detalhe) {}
}
