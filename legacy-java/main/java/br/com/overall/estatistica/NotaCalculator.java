package br.com.overall.estatistica;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Nota (3,0–10,0) de um jogador em UMA partida, estilo "nota do jogo" do Pro Clubs.
 * Não entra no overall (que já usa gols/assistências/resultados) — serve para o ranking de melhores notas.
 *
 * <pre>
 * base 6,0
 * resultado: vitória +0,7 · empate +0,1 · derrota −0,4
 * gols +0,9 cada (máx. +3,0) · assistências +0,6 cada (máx. +2,0)
 * goleiro: +1,2 sem levar gol; −0,25 por gol sofrido (máx. −1,5)
 * </pre>
 */
public final class NotaCalculator {

    private NotaCalculator() {}

    public static BigDecimal calcular(Resultado resultado, int gols, int assistencias,
                                      boolean goleiro, int golsSofridos) {
        double nota = 6.0;
        nota += switch (resultado) {
            case VITORIA -> 0.7;
            case EMPATE -> 0.1;
            case DERROTA -> -0.4;
        };
        nota += Math.min(3.0, 0.9 * gols);
        nota += Math.min(2.0, 0.6 * assistencias);
        if (goleiro) {
            nota += golsSofridos == 0 ? 1.2 : -Math.min(1.5, 0.25 * golsSofridos);
        }
        nota = Math.max(3.0, Math.min(10.0, nota));
        return BigDecimal.valueOf(nota).setScale(2, RoundingMode.HALF_UP);
    }
}
