package br.com.overall.pelada;

import br.com.overall.pelada.SorteioAlgoritmo.Candidato;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SorteioAlgoritmoTest {

    private static List<Candidato> jogadores(int[] overalls, Set<Integer> indicesGoleiros) {
        List<Candidato> lista = new ArrayList<>();
        for (int i = 0; i < overalls.length; i++)
            lista.add(new Candidato((long) i, overalls[i], indicesGoleiros.contains(i)));
        return lista;
    }

    private static double media(List<Long> time, List<Candidato> todos) {
        return time.stream().mapToInt(id -> todos.get(id.intValue()).overall()).average().orElse(0);
    }

    private static double amplitudeDeMedias(List<List<Long>> times, List<Candidato> todos) {
        double min = Double.MAX_VALUE, max = -1;
        for (List<Long> t : times) {
            double m = media(t, todos);
            min = Math.min(min, m);
            max = Math.max(max, m);
        }
        return max - min;
    }

    @Test
    void todosOsJogadoresAparecemUmaVezEOsTimesCheiosVemPrimeiro() {
        List<Candidato> todos = jogadores(new int[]{60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60}, Set.of());
        List<List<Long>> times = SorteioAlgoritmo.sortear(todos, 5, false, new Random(1));

        assertEquals(3, times.size());
        assertEquals(5, times.get(0).size());
        assertEquals(5, times.get(1).size());
        assertEquals(3, times.get(2).size(), "o que sobra forma o último time, incompleto");
        Set<Long> vistos = new HashSet<>();
        times.forEach(vistos::addAll);
        assertEquals(13, vistos.size());
    }

    @Test
    void goleirosSaoSeparadosEntreOsTimes() {
        for (long seed = 0; seed < 50; seed++) {
            List<Candidato> todos = jogadores(new int[]{60, 60, 60, 60, 60, 60, 60, 60}, Set.of(0, 1));
            List<List<Long>> times = SorteioAlgoritmo.sortear(todos, 4, seed % 2 == 0, new Random(seed));
            long comGoleiro = times.stream().filter(t -> t.contains(0L) || t.contains(1L)).count();
            assertEquals(2, comGoleiro, "seed " + seed + " deixou os dois goleiros juntos");
            assertTrue(!(times.get(0).contains(0L) && times.get(0).contains(1L)));
        }
    }

    @Test
    void modoBalanceadoAproximaAsMediasDosTimes() {
        int[] overalls = {90, 85, 80, 75, 70, 65, 62, 60, 55, 50, 48, 45};
        List<Candidato> todos = jogadores(overalls, Set.of());
        double somaBalanceado = 0, somaAleatorio = 0;
        int rodadas = 40;
        for (int seed = 0; seed < rodadas; seed++) {
            somaBalanceado += amplitudeDeMedias(SorteioAlgoritmo.sortear(todos, 4, true, new Random(seed)), todos);
            somaAleatorio += amplitudeDeMedias(SorteioAlgoritmo.sortear(todos, 4, false, new Random(seed)), todos);
        }
        double mediaBalanceado = somaBalanceado / rodadas;
        double mediaAleatorio = somaAleatorio / rodadas;
        assertTrue(mediaBalanceado < 2.0, "amplitude média balanceada = " + mediaBalanceado);
        assertTrue(mediaBalanceado < mediaAleatorio / 3,
                "balanceado=" + mediaBalanceado + " aleatório=" + mediaAleatorio);
    }

    @Test
    void modoBalanceadoNaoRepeteSempreOsMesmosTimes() {
        int[] overalls = {80, 75, 70, 65, 60, 55, 50, 45};
        List<Candidato> todos = jogadores(overalls, Set.of());
        Set<Set<Long>> primeirosTimes = new HashSet<>();
        for (int seed = 0; seed < 30; seed++) {
            primeirosTimes.add(new HashSet<>(SorteioAlgoritmo.sortear(todos, 4, true, new Random(seed)).get(0)));
        }
        assertTrue(primeirosTimes.size() > 3, "variedade insuficiente: " + primeirosTimes.size());
    }

    @Test
    void goleiroFicaEmTimesDiferentesTambemNoModoBalanceado() {
        List<Candidato> todos = jogadores(new int[]{99, 40, 80, 70, 60, 50, 90, 30, 55, 45}, Set.of(0, 6));
        for (int seed = 0; seed < 30; seed++) {
            List<List<Long>> times = SorteioAlgoritmo.sortear(todos, 5, true, new Random(seed));
            assertTrue(times.get(0).contains(0L) ^ times.get(0).contains(6L));
        }
    }

    @Test
    void precisaDeAoMenosDoisTimes() {
        List<Candidato> todos = jogadores(new int[]{60, 60, 60}, Set.of());
        assertThrows(IllegalArgumentException.class, () -> SorteioAlgoritmo.sortear(todos, 5, false, new Random()));
    }
}
