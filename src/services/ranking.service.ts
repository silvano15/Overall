import { prisma } from '../lib/prisma.js';
import { AppError } from '../middleware/auth.js';
import { EstatisticaService } from './estatistica.service.js';
import { EstatisticasCalculo } from '../utils/overallCalculator.js';

export class RankingService {
  private static readonly LIMITE = 30;

  static intervalo(periodoStr: string, ano?: number, mes?: number): [Date, Date] {
    const hoje = new Date();
    const p = periodoStr.toUpperCase();

    if (p === 'MES') {
      const a = ano ?? hoje.getFullYear();
      const m = mes !== undefined ? mes - 1 : hoje.getMonth();
      const inicio = new Date(Date.UTC(a, m, 1, 0, 0, 0));
      const fim = new Date(Date.UTC(a, m + 1, 0, 23, 59, 59, 999));
      return [inicio, fim];
    } else if (p === 'ANO') {
      const a = ano ?? hoje.getFullYear();
      const inicio = new Date(Date.UTC(a, 0, 1, 0, 0, 0));
      const fim = new Date(Date.UTC(a, 11, 31, 23, 59, 59, 999));
      return [inicio, fim];
    } else if (p === 'GERAL') {
      const inicio = new Date(Date.UTC(2000, 0, 1, 0, 0, 0));
      const fim = new Date(Date.UTC(2999, 11, 31, 23, 59, 59, 999));
      return [inicio, fim];
    } else {
      throw new AppError('Período inválido. Use MES, ANO ou GERAL.');
    }
  }

  static async ranking(grupoId: number, periodoStr: string, ano?: number, mes?: number) {
    const [de, ate] = this.intervalo(periodoStr, ano, mes);

    const [statsMap, peladasCount, jogadores] = await Promise.all([
      EstatisticaService.agregarPorPeriodo(grupoId, de, ate),
      prisma.estatisticaPartida.findMany({
        where: {
          pelada: { grupoId },
          dataJogo: { gte: de, lte: ate },
        },
        distinct: ['peladaId'],
      }),
      prisma.jogador.findMany({
        where: { grupoId },
      }),
    ]);

    const jogadoresMap = new Map<number, any>();
    for (const j of jogadores) jogadoresMap.set(j.id, j);

    const ordenar = (
      incluir: (e: EstatisticasCalculo) => boolean,
      valorFn: (e: EstatisticasCalculo) => number,
      detalheFn: (e: EstatisticasCalculo) => string,
      maisPartidasDesempata: boolean
    ) => {
      const lista: any[] = [];
      for (const [jogadorId, est] of statsMap.entries()) {
        const j = jogadoresMap.get(jogadorId);
        if (!j || !incluir(est)) continue;

        lista.push({
          jogadorId,
          nome: j.nome,
          valor: Math.round(valorFn(est) * 100) / 100,
          partidas: est.partidas,
          detalhe: detalheFn(est),
        });
      }

      lista.sort((a, b) => {
        if (b.valor !== a.valor) return b.valor - a.valor;
        if (maisPartidasDesempata) {
          if (b.partidas !== a.partidas) return b.partidas - a.partidas;
        } else {
          if (a.partidas !== b.partidas) return a.partidas - b.partidas;
        }
        return a.nome.localeCompare(b.nome, 'pt-BR', { sensitivity: 'base' });
      });

      return lista.slice(0, this.LIMITE);
    };

    const vencedores = ordenar(
      (e) => e.vitorias > 0,
      (e) => e.vitorias,
      (e) => e.partidas + ' jogos',
      false
    );

    const goleadores = ordenar(
      (e) => e.gols > 0,
      (e) => e.gols,
      (e) => e.partidas + ' jogos',
      false
    );

    const assistentes = ordenar(
      (e) => e.assistencias > 0,
      (e) => e.assistencias,
      (e) => e.partidas + ' jogos',
      false
    );

    const goleiros = ordenar(
      (e) => e.jogosComoGoleiro > 0,
      (e) => e.semSofrer,
      (e) => `${e.jogosComoGoleiro} jogos no gol · ${e.golsSofridos} gols sofridos`,
      false
    );

    const notas = ordenar(
      (e) => e.partidas > 0,
      (e) => e.notaMedia,
      (e) => e.partidas + ' jogos',
      true
    );

    // Overalls no período
    const peladaJogadoresNoPeriodo = await prisma.peladaJogador.findMany({
      where: {
        pelada: {
          grupoId,
          status: 'FINALIZADA',
          dataJogo: { gte: de, lte: ate },
        },
      },
      include: {
        pelada: true,
      },
      orderBy: [{ pelada: { dataJogo: 'asc' } }, { id: 'asc' }],
    });

    const primeiroAntes = new Map<number, number>();
    const ultimoDepois = new Map<number, number>();

    for (const pj of peladaJogadoresNoPeriodo) {
      if (!statsMap.has(pj.jogadorId)) continue;
      if (pj.overallAntes !== null && !primeiroAntes.has(pj.jogadorId)) {
        primeiroAntes.set(pj.jogadorId, pj.overallAntes);
      }
      if (pj.overallDepois !== null) {
        ultimoDepois.set(pj.jogadorId, pj.overallDepois);
      }
    }

    const overalls: any[] = [];
    for (const [jogadorId, depois] of ultimoDepois.entries()) {
      const j = jogadoresMap.get(jogadorId);
      if (!j) continue;
      const antes = primeiroAntes.get(jogadorId) ?? depois;
      const variacao = depois - antes;
      const detalhe = variacao > 0 ? `▲ +${variacao}` : variacao < 0 ? `▼ ${variacao}` : '=';

      overalls.push({
        jogadorId,
        nome: j.nome,
        valor: depois,
        partidas: statsMap.get(jogadorId)?.partidas ?? 0,
        detalhe,
      });
    }

    overalls.sort((a, b) => {
      if (b.valor !== a.valor) return b.valor - a.valor;
      return a.nome.localeCompare(b.nome, 'pt-BR', { sensitivity: 'base' });
    });

    return {
      periodo: periodoStr.toUpperCase(),
      de: de.toISOString().slice(0, 10),
      ate: ate.toISOString().slice(0, 10),
      peladas: peladasCount.length,
      vencedores,
      goleadores,
      assistentes,
      goleiros,
      notas,
      overalls: overalls.slice(0, this.LIMITE),
    };
  }

  static async conexoes(grupoId: number, periodoStr: string, ano?: number, mes?: number) {
    const [de, ate] = this.intervalo(periodoStr, ano, mes);

    const [statsMap, golsComAssist, jogadores] = await Promise.all([
      EstatisticaService.agregarPorPeriodo(grupoId, de, ate),
      prisma.gol.findMany({
        where: {
          partida: {
            pelada: {
              grupoId,
              dataJogo: { gte: de, lte: ate },
            },
          },
          assistenteId: { not: null },
        },
      }),
      prisma.jogador.findMany({
        where: { grupoId },
      }),
    ]);

    const duplasMap = new Map<string, { assistenteId: number; autorId: number; quantidade: number }>();
    for (const g of golsComAssist) {
      if (!g.assistenteId) continue;
      const chave = `${g.assistenteId}-${g.autorId}`;
      const d = duplasMap.get(chave);
      if (d) {
        d.quantidade++;
      } else {
        duplasMap.set(chave, {
          assistenteId: g.assistenteId,
          autorId: g.autorId,
          quantidade: 1,
        });
      }
    }

    const arestas = Array.from(duplasMap.values());
    arestas.sort((a, b) => b.quantidade - a.quantidade);

    const idsRelevantes = new Set<number>();
    for (const a of arestas) {
      idsRelevantes.add(a.assistenteId);
      idsRelevantes.add(a.autorId);
    }
    for (const [id, est] of statsMap.entries()) {
      if (est.gols > 0 || est.assistencias > 0) {
        idsRelevantes.add(id);
      }
    }

    const jogadoresMap = new Map<number, any>();
    for (const j of jogadores) jogadoresMap.set(j.id, j);

    const nos: any[] = [];
    for (const id of idsRelevantes) {
      const j = jogadoresMap.get(id);
      if (!j) continue;
      const est = statsMap.get(id);
      nos.push({
        jogadorId: j.id,
        nome: j.nome,
        overall: j.overall,
        gols: est?.gols ?? 0,
        assistencias: est?.assistencias ?? 0,
      });
    }

    nos.sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR', { sensitivity: 'base' }));

    return {
      periodo: periodoStr.toUpperCase(),
      de: de.toISOString().slice(0, 10),
      ate: ate.toISOString().slice(0, 10),
      nos,
      arestas,
    };
  }
}
