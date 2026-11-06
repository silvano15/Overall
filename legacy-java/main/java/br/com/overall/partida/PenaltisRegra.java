package br.com.overall.partida;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Regras da disputa de pênaltis.
 *
 * <ul>
 *   <li><b>Fase prevista</b>: cada time bate {@code rodadas} cobranças, alternadas. Termina antes se um time
 *       não puder mais ser alcançado pelo outro.</li>
 *   <li><b>Alternadas</b> (desempate): empatado após as {@code rodadas}, cada time bate uma cobrança por
 *       vez; vence quem estiver na frente quando os dois tiverem batido o mesmo número de cobranças.
 *       Com {@code rodadas = 0} a disputa já começa nas alternadas.</li>
 *   <li><b>Ciclo</b>: um jogador só bate de novo depois que todos os colegas em campo já bateram; quando o
 *       time inteiro bate, as marcas por jogador "zeram" (mas o total de cobranças continua contando).</li>
 * </ul>
 */
public final class PenaltisRegra {

    private PenaltisRegra() {}

    /**
     * @param cobrancasA cobranças já batidas pelo time A (idem B)
     * @param golsA      gols convertidos pelo time A (idem B)
     * @param rodadas    cobranças previstas por time antes das alternadas
     * @return 1 se A venceu, -1 se B venceu, 0 se a disputa ainda não está decidida
     */
    public static int vencedor(int cobrancasA, int cobrancasB, int golsA, int golsB, int rodadas) {
        if (cobrancasA == cobrancasB && cobrancasA >= rodadas) return Integer.compare(golsA, golsB);
        if (cobrancasA <= rodadas && cobrancasB <= rodadas) {
            if (golsA > golsB + (rodadas - cobrancasB)) return 1;
            if (golsB > golsA + (rodadas - cobrancasA)) return -1;
        }
        return 0;
    }

    /** As duas equipes já bateram todas as cobranças previstas e seguem nas alternadas (ou começaram nelas). */
    public static boolean emAlternadas(int cobrancasA, int cobrancasB, int rodadas) {
        return cobrancasA >= rodadas && cobrancasB >= rodadas;
    }

    /**
     * Quem já bateu no ciclo atual de um time.
     *
     * @param batedoresEmOrdem jogadores do time, na ordem em que bateram
     * @param elenco           jogadores do time em campo
     */
    public static Set<Long> jaBateramNoCiclo(List<Long> batedoresEmOrdem, Set<Long> elenco) {
        Set<Long> usados = new HashSet<>();
        for (Long jogador : batedoresEmOrdem) {
            usados.add(jogador);
            if (usados.containsAll(elenco)) usados.clear(); // time inteiro bateu: novo ciclo
        }
        return usados;
    }
}
