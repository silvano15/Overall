package br.com.overall.estatistica;

/**
 * Totais de um jogador (ou de um período). É a entrada do {@link OverallCalculator}
 * e também o que a API devolve para montar o "card" do jogador.
 *
 * @param jogosComoGoleiro partidas em que ele terminou no gol
 * @param semSofrer        partidas como goleiro sem levar gol
 * @param golsSofridos     gols levados nas partidas como goleiro
 * @param notaMedia        média das notas por partida (0 se não jogou)
 */
public record Estatisticas(
        long partidas,
        long vitorias,
        long empates,
        long derrotas,
        long gols,
        long assistencias,
        long jogosComoGoleiro,
        long semSofrer,
        long golsSofridos,
        double notaMedia
) {
    public static Estatisticas vazia() {
        return new Estatisticas(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
}
