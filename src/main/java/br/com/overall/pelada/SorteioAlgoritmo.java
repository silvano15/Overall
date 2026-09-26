package br.com.overall.pelada;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Divide os jogadores de linha da lista em times.
 *
 * <p>Goleiros não entram aqui: eles não ocupam vaga em nenhum time (ver {@link br.com.overall.pelada.PeladaService}),
 * já que quem vai para o gol é escolhido partida a partida, não no sorteio.</p>
 *
 * <ul>
 *   <li>Times cheios (com {@code tamanhoTime} jogadores) primeiro; o que sobrar forma o último time,
 *       incompleto — como acontece na pelada de verdade.</li>
 *   <li>Modo aleatório: só embaralha.</li>
 *   <li>Modo balanceado: sorteia várias divisões aleatórias e melhora cada uma trocando jogadores
 *       de times diferentes enquanto isso aproximar a média de overall dos times. Fica o melhor resultado.
 *       Como parte de divisões aleatórias, o resultado varia a cada sorteio sem deixar de ser equilibrado.</li>
 * </ul>
 */
public final class SorteioAlgoritmo {

    public record Candidato(Long id, int overall) {}

    private static final int TENTATIVAS_BALANCEADO = 60;

    private SorteioAlgoritmo() {}

    /** Quantidade de times que a lista gera para o tamanho informado. */
    public static int quantidadeDeTimes(int jogadores, int tamanhoTime) {
        return (jogadores + tamanhoTime - 1) / tamanhoTime;
    }

    /** @return uma lista de ids de jogadores por time; o índice + 1 é o número do time. */
    public static List<List<Long>> sortear(List<Candidato> candidatos, int tamanhoTime,
                                           boolean balancear, Random rnd) {
        if (tamanhoTime < 1) throw new IllegalArgumentException("Tamanho do time deve ser ao menos 1.");
        int n = candidatos.size();
        int numTimes = quantidadeDeTimes(n, tamanhoTime);
        if (numTimes < 2) throw new IllegalArgumentException("A lista precisa formar ao menos 2 times.");

        int[] capacidade = new int[numTimes];
        java.util.Arrays.fill(capacidade, tamanhoTime);
        if (n % tamanhoTime != 0) capacidade[numTimes - 1] = n % tamanhoTime;

        double media = candidatos.stream().mapToInt(Candidato::overall).average().orElse(0);

        List<List<Candidato>> melhor = null;
        double melhorPontuacao = Double.MAX_VALUE;
        int tentativas = balancear ? TENTATIVAS_BALANCEADO : 1;
        for (int t = 0; t < tentativas; t++) {
            List<List<Candidato>> times = montarAleatorio(candidatos, capacidade, rnd);
            if (balancear) melhorar(times, media);
            double pontuacao = balancear ? pontuar(times, media) : 0;
            if (melhor == null || pontuacao < melhorPontuacao) {
                melhor = times;
                melhorPontuacao = pontuacao;
            }
        }

        List<List<Long>> resultado = new ArrayList<>();
        for (List<Candidato> time : melhor) resultado.add(time.stream().map(Candidato::id).toList());
        return resultado;
    }

    private static List<List<Candidato>> montarAleatorio(List<Candidato> candidatos, int[] capacidade, Random rnd) {
        int numTimes = capacidade.length;
        List<Candidato> demais = new ArrayList<>(candidatos);
        Collections.shuffle(demais, rnd);

        List<List<Candidato>> times = new ArrayList<>();
        for (int i = 0; i < numTimes; i++) times.add(new ArrayList<>());

        int proximo = 0;
        for (int t = 0; t < numTimes; t++) {
            while (times.get(t).size() < capacidade[t]) times.get(t).add(demais.get(proximo++));
        }
        return times;
    }

    /** Hill-climbing: troca pares de jogadores entre times enquanto a pontuação cair. */
    private static void melhorar(List<List<Candidato>> times, double media) {
        double atual = pontuar(times, media);
        boolean melhorou = true;
        while (melhorou) {
            melhorou = false;
            for (int i = 0; i < times.size(); i++) {
                for (int j = i + 1; j < times.size(); j++) {
                    List<Candidato> ti = times.get(i), tj = times.get(j);
                    for (int a = 0; a < ti.size(); a++) {
                        for (int b = 0; b < tj.size(); b++) {
                            Candidato ca = ti.get(a), cb = tj.get(b);
                            ti.set(a, cb); tj.set(b, ca);
                            double nova = pontuar(times, media);
                            if (nova < atual - 1e-9) { atual = nova; melhorou = true; }
                            else { ti.set(a, ca); tj.set(b, cb); }
                        }
                    }
                }
            }
        }
    }

    /** Soma ponderada (pelo tamanho do time) do quadrado do desvio da média do time em relação à média geral. */
    static double pontuar(List<List<Candidato>> times, double mediaGeral) {
        double total = 0;
        for (List<Candidato> time : times) {
            if (time.isEmpty()) continue;
            double mediaTime = time.stream().mapToInt(Candidato::overall).average().orElse(0);
            total += time.size() * Math.pow(mediaTime - mediaGeral, 2);
        }
        return total;
    }
}
