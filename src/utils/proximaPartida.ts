export interface PartidaEncerradaHistorico {
  numeroA: number;
  numeroB: number;
  fica: number | null;
}

export class ProximaPartida {
  static sugerir(
    numTimes: number,
    historico: PartidaEncerradaHistorico[]
  ): [number, number] | null {
    const fila: number[] = [];
    for (let i = 1; i <= numTimes; i++) fila.push(i);
    let naQuadra: number | null = null;

    for (const m of historico) {
      const idxA = fila.indexOf(m.numeroA);
      if (idxA !== -1) fila.splice(idxA, 1);
      const idxB = fila.indexOf(m.numeroB);
      if (idxB !== -1) fila.splice(idxB, 1);

      if (naQuadra !== null && naQuadra !== m.numeroA && naQuadra !== m.numeroB) {
        fila.push(naQuadra);
      }

      if (m.fica !== null) {
        const perdedor = m.fica === m.numeroA ? m.numeroB : m.numeroA;
        fila.push(perdedor);
        naQuadra = m.fica;
      } else {
        fila.push(Math.min(m.numeroA, m.numeroB));
        fila.push(Math.max(m.numeroA, m.numeroB));
        naQuadra = null;
      }
    }

    if (naQuadra !== null) {
      return fila.length === 0 ? null : [naQuadra, fila[0]];
    }
    if (fila.length < 2) return null;
    const primeiro = fila.shift()!;
    return [primeiro, fila[0]];
  }
}
