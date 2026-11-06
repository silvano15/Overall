export interface EstatisticasCalculo {
  partidas: number;
  vitorias: number;
  empates: number;
  derrotas: number;
  gols: number;
  assistencias: number;
  jogosComoGoleiro: number;
  semSofrer: number;
  golsSofridos: number;
  notaMedia: number;
}

export function estatisticasVazia(): EstatisticasCalculo {
  return {
    partidas: 0,
    vitorias: 0,
    empates: 0,
    derrotas: 0,
    gols: 0,
    assistencias: 0,
    jogosComoGoleiro: 0,
    semSofrer: 0,
    golsSofridos: 0,
    notaMedia: 0,
  };
}

export class OverallCalculator {
  static readonly BASE = 60;
  static readonly MINIMO = 40;
  static readonly MAXIMO = 99;
  static readonly PARTIDAS_DE_CONFIANCA = 5;

  static readonly GOLS_POR_JOGO_MAXIMO = 0.8;
  static readonly ASSIST_POR_JOGO_MAXIMO = 0.5;
  static readonly SEM_SOFRER_MAXIMO = 0.7;
  static readonly GOLS_ASSIST_GOLEIRO_MAXIMO = 0.5;

  static calcular(e: EstatisticasCalculo | null, goleiro: boolean): number {
    if (!e || e.partidas === 0) return this.BASE;
    const p = e.partidas;
    const aproveitamento = (3.0 * e.vitorias + e.empates) / (3.0 * p);

    let bruto: number;
    if (goleiro) {
      const semSofrer = this.limitar(e.semSofrer / p / this.SEM_SOFRER_MAXIMO);
      const derrotasEvitadas = 1.0 - e.derrotas / p;
      const contribuicao = this.limitar((e.gols + e.assistencias) / p / this.GOLS_ASSIST_GOLEIRO_MAXIMO);
      bruto = 0.45 * semSofrer + 0.30 * aproveitamento + 0.15 * derrotasEvitadas + 0.10 * contribuicao;
    } else {
      const gols = this.limitar(e.gols / p / this.GOLS_POR_JOGO_MAXIMO);
      const assistencias = this.limitar(e.assistencias / p / this.ASSIST_POR_JOGO_MAXIMO);
      bruto = 0.40 * aproveitamento + 0.35 * gols + 0.25 * assistencias;
    }

    const semAmostra = this.MINIMO + (this.MAXIMO - this.MINIMO) * this.limitar(bruto);
    const comConfianca = (p * semAmostra + this.PARTIDAS_DE_CONFIANCA * this.BASE) / (p + this.PARTIDAS_DE_CONFIANCA);
    return Math.round(Math.max(this.MINIMO, Math.min(this.MAXIMO, comConfianca)));
  }

  private static limitar(v: number): number {
    return Math.max(0.0, Math.min(1.0, v));
  }
}
