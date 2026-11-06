import { prisma } from '../lib/prisma.js';
import { EstatisticasCalculo, estatisticasVazia } from '../utils/overallCalculator.js';

export class EstatisticaService {
  static async agregarPorPeriodo(
    grupoId: number,
    de: Date,
    ate: Date
  ): Promise<Map<number, EstatisticasCalculo>> {
    const stats = await prisma.estatisticaPartida.findMany({
      where: {
        pelada: { grupoId },
        dataJogo: {
          gte: de,
          lte: ate,
        },
      },
    });

    return this.computarStats(stats);
  }

  static async agregarGeral(grupoId: number): Promise<Map<number, EstatisticasCalculo>> {
    const stats = await prisma.estatisticaPartida.findMany({
      where: {
        pelada: { grupoId },
      },
    });

    return this.computarStats(stats);
  }

  static async agregarExceto(
    grupoId: number,
    peladaId: number
  ): Promise<Map<number, EstatisticasCalculo>> {
    const stats = await prisma.estatisticaPartida.findMany({
      where: {
        pelada: {
          grupoId,
          id: { not: peladaId },
        },
      },
    });

    return this.computarStats(stats);
  }

  private static computarStats(stats: any[]): Map<number, EstatisticasCalculo> {
    const mapa = new Map<number, EstatisticasCalculo>();

    const notasJogador = new Map<number, number[]>();

    for (const s of stats) {
      let est = mapa.get(s.jogadorId);
      if (!est) {
        est = estatisticasVazia();
        mapa.set(s.jogadorId, est);
      }

      est.partidas++;
      if (s.resultado === 'VITORIA') est.vitorias++;
      else if (s.resultado === 'EMPATE') est.empates++;
      else if (s.resultado === 'DERROTA') est.derrotas++;

      est.gols += s.gols;
      est.assistencias += s.assistencias;

      if (s.goleiro) {
        est.jogosComoGoleiro++;
        est.golsSofridos += s.golsSofridos;
        if (s.semSofrer) est.semSofrer++;
      }

      let notas = notasJogador.get(s.jogadorId);
      if (!notas) {
        notas = [];
        notasJogador.set(s.jogadorId, notas);
      }
      notas.push(Number(s.nota));
    }

    for (const [jogadorId, notas] of notasJogador.entries()) {
      const est = mapa.get(jogadorId)!;
      const media = notas.reduce((a, b) => a + b, 0) / (notas.length || 1);
      est.notaMedia = Math.round(media * 100) / 100;
    }

    return mapa;
  }
}
