package br.com.overall.partida;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PenaltisRegraTest {

    @Test
    void comecoDaDisputaNaoTemVencedor() {
        assertEquals(0, PenaltisRegra.vencedor(0, 0, 0, 0, 5));
        assertEquals(0, PenaltisRegra.vencedor(1, 0, 1, 0, 5));
        assertEquals(0, PenaltisRegra.vencedor(3, 3, 2, 2, 5));
    }

    @Test
    void terminaAntesQuandoOAdversarioNaoAlcancaMais() {
        // 5 cobranças por time. A fez 3 e B errou 3: B só pode chegar a 2 → A venceu.
        assertEquals(1, PenaltisRegra.vencedor(3, 3, 3, 0, 5));
        // 3x0 com A já tendo batido 3 e B 2: B ainda chega a 3 → não decide.
        assertEquals(0, PenaltisRegra.vencedor(3, 2, 3, 0, 5));
        assertEquals(1, PenaltisRegra.vencedor(4, 4, 4, 1, 5));
        assertEquals(-1, PenaltisRegra.vencedor(4, 4, 2, 4, 5));
    }

    @Test
    void oNumeroDeCobrancasPrevistasMudaComOModo() {
        // Time de 3 jogadores (modo normal): 3 cobranças cada.
        assertEquals(1, PenaltisRegra.vencedor(2, 2, 2, 0, 3));   // B só pode chegar a 1
        assertEquals(0, PenaltisRegra.vencedor(2, 2, 1, 0, 3));   // B ainda alcança
        assertEquals(1, PenaltisRegra.vencedor(3, 3, 2, 1, 3));   // todos bateram: maior placar
        // Pré-determinada com 1 cobrança cada.
        assertEquals(1, PenaltisRegra.vencedor(1, 1, 1, 0, 1));
        assertEquals(0, PenaltisRegra.vencedor(1, 1, 1, 1, 1));   // empatou → alternadas
    }

    @Test
    void empatadoAposAsPrevistasSegueNasAlternadas() {
        assertEquals(0, PenaltisRegra.vencedor(5, 5, 3, 3, 5));
        assertEquals(0, PenaltisRegra.vencedor(6, 5, 4, 3, 5));   // A bateu a 6ª e B ainda vai bater
        assertEquals(1, PenaltisRegra.vencedor(6, 6, 4, 3, 5));   // B perdeu: A venceu
        assertEquals(0, PenaltisRegra.vencedor(6, 6, 4, 4, 5));   // segue
        assertEquals(-1, PenaltisRegra.vencedor(7, 7, 4, 5, 5));
    }

    @Test
    void modoAlternadoComecaDiretoNasAlternadas() {
        // rodadas = 0: cada par de cobranças decide se alguém ficou na frente.
        assertEquals(0, PenaltisRegra.vencedor(0, 0, 0, 0, 0));
        assertEquals(0, PenaltisRegra.vencedor(1, 0, 1, 0, 0));   // B ainda vai bater
        assertEquals(0, PenaltisRegra.vencedor(1, 1, 1, 1, 0));   // as duas fizeram: segue
        assertEquals(1, PenaltisRegra.vencedor(1, 1, 1, 0, 0));
        assertEquals(-1, PenaltisRegra.vencedor(3, 3, 2, 3, 0));
        assertTrue(PenaltisRegra.emAlternadas(0, 0, 0));
    }

    @Test
    void sabeQuandoEstaNasAlternadas() {
        assertFalse(PenaltisRegra.emAlternadas(2, 2, 3));
        assertFalse(PenaltisRegra.emAlternadas(3, 2, 3));
        assertTrue(PenaltisRegra.emAlternadas(3, 3, 3));
        assertTrue(PenaltisRegra.emAlternadas(4, 3, 3));
    }

    @Test
    void cicloZeraQuandoOTimeInteiroBateMasOTotalContinua() {
        Set<Long> elenco = Set.of(1L, 2L, 3L, 4L);
        assertEquals(Set.of(), PenaltisRegra.jaBateramNoCiclo(List.of(), elenco));
        assertEquals(Set.of(1L, 2L), PenaltisRegra.jaBateramNoCiclo(List.of(1L, 2L), elenco));
        // os 4 bateram → ciclo zerado
        assertEquals(Set.of(), PenaltisRegra.jaBateramNoCiclo(List.of(1L, 2L, 3L, 4L), elenco));
        // e o próximo ciclo já começa contando
        assertEquals(Set.of(3L), PenaltisRegra.jaBateramNoCiclo(List.of(1L, 2L, 3L, 4L, 3L), elenco));
        assertEquals(Set.of(3L, 1L), PenaltisRegra.jaBateramNoCiclo(List.of(4L, 2L, 3L, 1L, 3L, 1L), elenco));
    }
}
