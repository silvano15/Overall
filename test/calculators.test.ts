import assert from 'node:assert';
import test from 'node:test';
import { OverallCalculator, EstatisticasCalculo } from '../src/utils/overallCalculator.js';
import { NotaCalculator } from '../src/utils/notaCalculator.js';
import { SorteioAlgoritmo } from '../src/utils/sorteioAlgoritmo.js';
import { PenaltisRegra } from '../src/utils/penaltisRegra.js';
import { ProximaPartida } from '../src/utils/proximaPartida.js';

test('OverallCalculator: jogador estreante retorna BASE 60', () => {
  const over = OverallCalculator.calcular(null, false);
  assert.strictEqual(over, 60);
});

test('OverallCalculator: jogador de linha com 10 partidas', () => {
  const stats: EstatisticasCalculo = {
    partidas: 10,
    vitorias: 8,
    empates: 1,
    derrotas: 1,
    gols: 8,
    assistencias: 5,
    jogosComoGoleiro: 0,
    semSofrer: 0,
    golsSofridos: 0,
    notaMedia: 8.5,
  };
  const over = OverallCalculator.calcular(stats, false);
  assert.ok(over >= 60 && over <= 99, `Overall fora da faixa esperada: ${over}`);
});

test('NotaCalculator: partida com vitoria, 2 gols e 1 assistencia', () => {
  const nota = NotaCalculator.calcular('VITORIA', 2, 1, false, 0);
  // Base 6.0 + 0.7 (vitoria) + 1.8 (2 gols) + 0.6 (1 assistencia) = 9.1
  assert.strictEqual(nota, 9.1);
});

test('NotaCalculator: goleiro sem sofrer gol em vitoria', () => {
  const nota = NotaCalculator.calcular('VITORIA', 0, 0, true, 0);
  // Base 6.0 + 0.7 + 1.2 = 7.9
  assert.strictEqual(nota, 7.9);
});

test('SorteioAlgoritmo: divide candidatos em times de tamanho informado', () => {
  const candidatos = Array.from({ length: 14 }, (_, i) => ({
    id: i + 1,
    overall: 60 + (i % 20),
  }));
  const times = SorteioAlgoritmo.sortear(candidatos, 5, true);
  // 14 jogadores divididos por times de 5 -> 3 times (5, 5, 4)
  assert.strictEqual(times.length, 3);
  assert.strictEqual(times[0].length, 5);
  assert.strictEqual(times[1].length, 5);
  assert.strictEqual(times[2].length, 4);
});

test('PenaltisRegra: decide vencedor quando diferenca inalcancavel', () => {
  // 3 rodadas. A bateu 2 e fez 2 gols. B bateu 2 e fez 0 gols.
  // Falta 1 cobranca para B (maximo 1 gol). A ja venceu.
  const v = PenaltisRegra.vencedor(2, 2, 2, 0, 3);
  assert.strictEqual(v, 1);
});

test('ProximaPartida: vencedor fica e desafiante vem da fila', () => {
  const historico = [
    { numeroA: 1, numeroB: 2, fica: 1 }, // time 1 venceu
  ];
  // 3 times. Fila original [1, 2, 3]. Time 1 ficou, time 2 foi pro fim -> Fila [3, 2].
  const prox = ProximaPartida.sugerir(3, historico);
  assert.deepStrictEqual(prox, [1, 3]);
});
