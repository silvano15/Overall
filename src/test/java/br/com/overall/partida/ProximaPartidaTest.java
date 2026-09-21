package br.com.overall.partida;

import br.com.overall.partida.ProximaPartida.Encerrada;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProximaPartidaTest {

    @Test
    void semHistoricoComecaPelosDoisPrimeiros() {
        assertArrayEquals(new int[]{1, 2}, ProximaPartida.sugerir(4, List.of()).orElseThrow());
    }

    @Test
    void vencedorFicaEODesafianteEOMenorNumeroDaFila() {
        // 1x2, o 1 ganha -> 1 x 3
        assertArrayEquals(new int[]{1, 3},
                ProximaPartida.sugerir(4, List.of(new Encerrada(1, 2, 1))).orElseThrow());
    }

    @Test
    void time3SempreVemAntesDoTime4() {
        // 1x2 (1 ganha), 1x3 (3 ganha) -> quem fica é o 3 e o desafiante é o 4 (o 2 e o 1 vão para o fim da fila)
        List<Encerrada> historico = List.of(new Encerrada(1, 2, 1), new Encerrada(1, 3, 3));
        assertArrayEquals(new int[]{3, 4}, ProximaPartida.sugerir(4, historico).orElseThrow());
    }

    @Test
    void perdedorVoltaParaOFimDaFila() {
        // 1x2 (2 ganha), 2x3 (2 ganha) -> 2 x 4; depois 2x4 (4 ganha) -> 4 x 1 (o 1 esperava desde o começo)
        List<Encerrada> historico = List.of(new Encerrada(1, 2, 2), new Encerrada(2, 3, 2), new Encerrada(2, 4, 4));
        assertArrayEquals(new int[]{4, 1}, ProximaPartida.sugerir(4, historico).orElseThrow());
    }

    @Test
    void empateEncerradoTiraOsDoisEChamaOsProximosDaFila() {
        // 1x2 empatou e encerrou -> 3 x 4
        assertArrayEquals(new int[]{3, 4},
                ProximaPartida.sugerir(4, List.of(new Encerrada(1, 2, null))).orElseThrow());
    }

    @Test
    void empateNosPenaltisMantemQuemGanhou() {
        // 1x2 empatou, 2 ganhou nos pênaltis e fica -> 2 x 3
        assertArrayEquals(new int[]{2, 3},
                ProximaPartida.sugerir(4, List.of(new Encerrada(1, 2, 2))).orElseThrow());
    }

    @Test
    void comTresTimesEEmpateOsDoisQueSaemEntramNaFila() {
        // 1x2 empate -> fila 3,1,2 -> 3 x 1
        assertArrayEquals(new int[]{3, 1},
                ProximaPartida.sugerir(3, List.of(new Encerrada(1, 2, null))).orElseThrow());
    }

    @Test
    void comDoisTimesSempreSugereOMesmoConfronto() {
        Optional<int[]> sugestao = ProximaPartida.sugerir(2, List.of(new Encerrada(1, 2, 1)));
        assertArrayEquals(new int[]{1, 2}, sugestao.orElseThrow());
        assertArrayEquals(new int[]{1, 2},
                ProximaPartida.sugerir(2, List.of(new Encerrada(1, 2, null))).orElseThrow());
    }

    @Test
    void adminPodeComecarPorOutroConfrontoPorqueUmTimeEstaIncompleto() {
        // Time 4 incompleto: ADM começa 2 x 3 e o 3 ganha -> desafiante é o 1 (primeiro da fila restante)
        assertArrayEquals(new int[]{3, 1},
                ProximaPartida.sugerir(4, List.of(new Encerrada(2, 3, 3))).orElseThrow());
    }

    @Test
    void semTimesSuficientesNaoSugere() {
        assertTrue(ProximaPartida.sugerir(1, List.of()).isEmpty());
        assertTrue(ProximaPartida.sugerir(0, List.of()).isEmpty());
    }
}
