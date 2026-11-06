export class PenaltisRegra {
  static vencedor(
    cobrancasA: number,
    cobrancasB: number,
    golsA: number,
    golsB: number,
    rodadas: number
  ): number {
    if (cobrancasA === cobrancasB && cobrancasA >= rodadas) {
      return golsA > golsB ? 1 : golsA < golsB ? -1 : 0;
    }
    if (cobrancasA <= rodadas && cobrancasB <= rodadas) {
      if (golsA > golsB + (rodadas - cobrancasB)) return 1;
      if (golsB > golsA + (rodadas - cobrancasA)) return -1;
    }
    return 0;
  }

  static emAlternadas(cobrancasA: number, cobrancasB: number, rodadas: number): boolean {
    return cobrancasA >= rodadas && cobrancasB >= rodadas;
  }

  static jaBateramNoCiclo(batedoresEmOrdem: number[], elenco: Set<number>): Set<number> {
    const usados = new Set<number>();
    for (const jogador of batedoresEmOrdem) {
      usados.add(jogador);
      let timeInteiroBateu = true;
      for (const e of elenco) {
        if (!usados.has(e)) {
          timeInteiroBateu = false;
          break;
        }
      }
      if (timeInteiroBateu) usados.clear();
    }
    return usados;
  }
}
