package br.com.overall.partida;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * Sugere quem joga a próxima partida, com a regra da pelada:
 * <ul>
 *   <li>quem ganha fica na quadra; quem perde vai para o fim da fila;</li>
 *   <li>o desafiante é o primeiro da fila — então, por exemplo, o time 3 sempre vem antes do time 4;</li>
 *   <li>se a partida terminou empatada e foi encerrada, os dois saem (menor número na frente na fila);</li>
 *   <li>se o empate foi para os pênaltis, o vencedor dos pênaltis fica.</li>
 * </ul>
 * É só uma sugestão: o ADM pode escolher outro confronto (times incompletos, por exemplo).
 */
public final class ProximaPartida {

    /** @param fica número do time que segue na quadra, ou null se os dois saíram. */
    public record Encerrada(int numeroA, int numeroB, Integer fica) {}

    private ProximaPartida() {}

    /** @return {numeroA, numeroB} sugeridos, ou vazio se ainda não há times suficientes. */
    public static Optional<int[]> sugerir(int numTimes, List<Encerrada> historico) {
        Deque<Integer> fila = new ArrayDeque<>();
        for (int i = 1; i <= numTimes; i++) fila.addLast(i);
        Integer naQuadra = null;

        for (Encerrada m : historico) {
            fila.remove(Integer.valueOf(m.numeroA()));
            fila.remove(Integer.valueOf(m.numeroB()));
            // Quem estava na quadra mas foi "pulado" pelo ADM vai para o fim da fila.
            if (naQuadra != null && naQuadra != m.numeroA() && naQuadra != m.numeroB()) fila.addLast(naQuadra);

            if (m.fica() != null) {
                int perdedor = m.fica() == m.numeroA() ? m.numeroB() : m.numeroA();
                fila.addLast(perdedor);
                naQuadra = m.fica();
            } else {
                fila.addLast(Math.min(m.numeroA(), m.numeroB()));
                fila.addLast(Math.max(m.numeroA(), m.numeroB()));
                naQuadra = null;
            }
        }

        if (naQuadra != null) {
            return fila.isEmpty() ? Optional.empty() : Optional.of(new int[]{naQuadra, fila.peekFirst()});
        }
        if (fila.size() < 2) return Optional.empty();
        Integer primeiro = fila.pollFirst();
        return Optional.of(new int[]{primeiro, fila.peekFirst()});
    }
}
