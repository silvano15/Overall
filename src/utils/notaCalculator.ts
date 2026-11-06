export type ResultadoTipo = 'VITORIA' | 'EMPATE' | 'DERROTA';

export class NotaCalculator {
  static calcular(
    resultado: ResultadoTipo,
    gols: number,
    assistencias: number,
    goleiro: boolean,
    golsSofridos: number
  ): number {
    let nota = 6.0;
    switch (resultado) {
      case 'VITORIA':
        nota += 0.7;
        break;
      case 'EMPATE':
        nota += 0.1;
        break;
      case 'DERROTA':
        nota -= 0.4;
        break;
    }
    nota += Math.min(3.0, 0.9 * gols);
    nota += Math.min(2.0, 0.6 * assistencias);
    if (goleiro) {
      nota += golsSofridos === 0 ? 1.2 : -Math.min(1.5, 0.25 * golsSofridos);
    }
    nota = Math.max(3.0, Math.min(10.0, nota));
    return Math.round(nota * 100) / 100;
  }
}
