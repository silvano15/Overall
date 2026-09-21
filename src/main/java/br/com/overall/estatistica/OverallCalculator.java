package br.com.overall.estatistica;

/**
 * Calcula o overall (40–99) de um jogador a partir do histórico dele.
 *
 * <h3>Jogador de linha</h3>
 * <ul>
 *   <li>40% aproveitamento: (3·vitórias + empates) / (3·partidas) — já embute as derrotas;</li>
 *   <li>35% gols por partida (0,8 gol/jogo já vale nota máxima);</li>
 *   <li>25% assistências por partida (0,5/jogo vale nota máxima).</li>
 * </ul>
 *
 * <h3>Goleiro — 90% do peso vem de defesa e resultado</h3>
 * <ul>
 *   <li>45% partidas sem levar gol (70% dos jogos sem sofrer vale nota máxima);</li>
 *   <li>30% aproveitamento (vitórias e empates);</li>
 *   <li>15% derrotas evitadas (1 − derrotas/partidas);</li>
 *   <li>10% gols + assistências por partida.</li>
 * </ul>
 *
 * <h3>Poucas partidas</h3>
 * Com pouca amostra o número seria enganoso (1 jogo com gol = craque). Por isso o resultado é
 * puxado para {@link #BASE} com um peso de {@link #PARTIDAS_DE_CONFIANCA} partidas: quem estreia
 * começa em 60 e só se afasta de 60 conforme joga.
 */
public final class OverallCalculator {

    public static final int BASE = 60;
    public static final int MINIMO = 40;
    public static final int MAXIMO = 99;
    public static final int PARTIDAS_DE_CONFIANCA = 5;

    // Referências que valem "nota máxima" em cada quesito.
    static final double GOLS_POR_JOGO_MAXIMO = 0.8;
    static final double ASSIST_POR_JOGO_MAXIMO = 0.5;
    static final double SEM_SOFRER_MAXIMO = 0.7;
    static final double GOLS_ASSIST_GOLEIRO_MAXIMO = 0.5;

    private OverallCalculator() {}

    public static int calcular(Estatisticas e, boolean goleiro) {
        if (e == null || e.partidas() == 0) return BASE;
        double p = e.partidas();
        double aproveitamento = (3.0 * e.vitorias() + e.empates()) / (3.0 * p);

        double bruto;
        if (goleiro) {
            double semSofrer = limitar(e.semSofrer() / p / SEM_SOFRER_MAXIMO);
            double derrotasEvitadas = 1.0 - e.derrotas() / p;
            double contribuicao = limitar((e.gols() + e.assistencias()) / p / GOLS_ASSIST_GOLEIRO_MAXIMO);
            bruto = 0.45 * semSofrer + 0.30 * aproveitamento + 0.15 * derrotasEvitadas + 0.10 * contribuicao;
        } else {
            double gols = limitar(e.gols() / p / GOLS_POR_JOGO_MAXIMO);
            double assistencias = limitar(e.assistencias() / p / ASSIST_POR_JOGO_MAXIMO);
            bruto = 0.40 * aproveitamento + 0.35 * gols + 0.25 * assistencias;
        }

        double semAmostra = MINIMO + (MAXIMO - MINIMO) * limitar(bruto);
        double comConfianca = (p * semAmostra + PARTIDAS_DE_CONFIANCA * BASE) / (p + PARTIDAS_DE_CONFIANCA);
        return (int) Math.round(Math.max(MINIMO, Math.min(MAXIMO, comConfianca)));
    }

    private static double limitar(double v) { return Math.max(0.0, Math.min(1.0, v)); }
}
