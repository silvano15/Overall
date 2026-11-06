package br.com.overall.estatistica;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverallCalculatorTest {

    private static Estatisticas e(long partidas, long v, long e, long d, long gols, long assist, long jogosGol, long semSofrer) {
        return new Estatisticas(partidas, v, e, d, gols, assist, jogosGol, semSofrer, 0, 0);
    }

    @Test
    void semHistoricoComecaNaBase() {
        assertEquals(OverallCalculator.BASE, OverallCalculator.calcular(Estatisticas.vazia(), false));
        assertEquals(OverallCalculator.BASE, OverallCalculator.calcular(null, true));
    }

    @Test
    void umaPartidaExcelenteNaoDisparaOOverall() {
        // 1 jogo, vitória, 3 gols, 2 assistências: o "puxão" para 60 segura o número.
        int overall = OverallCalculator.calcular(e(1, 1, 0, 0, 3, 2, 0, 0), false);
        assertTrue(overall > 60 && overall < 70, "overall=" + overall);
    }

    @Test
    void craqueConsistenteChegaAlto() {
        // 20 jogos, 15 vitórias, 1 empate, 4 derrotas, 18 gols, 10 assistências
        int overall = OverallCalculator.calcular(e(20, 15, 1, 4, 18, 10, 0, 0), false);
        assertTrue(overall >= 75 && overall <= 90, "overall=" + overall);
    }

    @Test
    void pernaDePauFicaAbaixoDaBase() {
        // 20 jogos, 2 vitórias, 18 derrotas, nenhum gol
        int overall = OverallCalculator.calcular(e(20, 2, 0, 18, 0, 0, 0, 0), false);
        assertTrue(overall < 55, "overall=" + overall);
    }

    @Test
    void maisGolsMelhoraOOverall() {
        int poucos = OverallCalculator.calcular(e(10, 5, 0, 5, 2, 1, 0, 0), false);
        int muitos = OverallCalculator.calcular(e(10, 5, 0, 5, 9, 1, 0, 0), false);
        assertTrue(muitos > poucos);
    }

    @Test
    void goleiroDependeDeJogosSemSofrerGol() {
        // Mesmo aproveitamento e nenhum gol/assistência; só muda quantas vezes saiu sem levar gol.
        int semDefesa = OverallCalculator.calcular(e(10, 5, 0, 5, 0, 0, 10, 0), true);
        int muralha = OverallCalculator.calcular(e(10, 5, 0, 5, 0, 0, 10, 7), true);
        assertTrue(muralha - semDefesa >= 10, "sem=" + semDefesa + " muralha=" + muralha);
    }

    @Test
    void golsEAssistenciasPesamPoucoParaGoleiro() {
        // 90% do overall do goleiro vem de sem-sofrer + resultados; gols/assist valem só 10%.
        int semContribuicao = OverallCalculator.calcular(e(20, 10, 0, 10, 0, 0, 20, 8), true);
        int comContribuicao = OverallCalculator.calcular(e(20, 10, 0, 10, 20, 20, 20, 8), true);
        assertTrue(comContribuicao > semContribuicao);
        assertTrue(comContribuicao - semContribuicao <= 5, "diferença=" + (comContribuicao - semContribuicao));
    }

    @Test
    void sempreDentroDosLimites() {
        int maximo = OverallCalculator.calcular(e(500, 500, 0, 0, 900, 900, 500, 500), true);
        int minimo = OverallCalculator.calcular(e(500, 0, 0, 500, 0, 0, 0, 0), false);
        assertTrue(maximo <= OverallCalculator.MAXIMO);
        assertTrue(minimo >= OverallCalculator.MINIMO);
    }

    @Test
    void notaDoJogoRefleteResultadoGolsEDefesa() {
        BigDecimal fraco = NotaCalculator.calcular(Resultado.DERROTA, 0, 0, false, 3);
        BigDecimal medio = NotaCalculator.calcular(Resultado.VITORIA, 0, 0, false, 0);
        BigDecimal craque = NotaCalculator.calcular(Resultado.VITORIA, 2, 1, false, 0);
        BigDecimal goleiroSemLevar = NotaCalculator.calcular(Resultado.VITORIA, 0, 0, true, 0);
        BigDecimal goleiroVazado = NotaCalculator.calcular(Resultado.DERROTA, 0, 0, true, 4);

        assertEquals(new BigDecimal("5.60"), fraco);
        assertEquals(new BigDecimal("6.70"), medio);
        assertEquals(new BigDecimal("9.10"), craque);
        assertEquals(new BigDecimal("7.90"), goleiroSemLevar);
        assertTrue(goleiroVazado.compareTo(fraco) < 0);
    }

    @Test
    void notaNuncaSaiDe3a10() {
        assertEquals(new BigDecimal("10.00"), NotaCalculator.calcular(Resultado.VITORIA, 9, 9, true, 0));
        assertTrue(NotaCalculator.calcular(Resultado.DERROTA, 0, 0, true, 50).compareTo(new BigDecimal("3.00")) >= 0);
    }
}
