import { prisma } from '../lib/prisma.js';
import { AppError } from '../middleware/auth.js';
import { SorteioAlgoritmo } from '../utils/sorteioAlgoritmo.js';
import { ProximaPartida, PartidaEncerradaHistorico } from '../utils/proximaPartida.js';
import { EstatisticaService } from './estatistica.service.js';
import { OverallCalculator, EstatisticasCalculo, estatisticasVazia } from '../utils/overallCalculator.js';

export interface PeladaInput {
  dataJogo: string;
  horaJogo: string;
  jogadoresPorTime?: number | null;
  modoSorteio?: string | null;
  duracaoMinutos?: number | null;
  golsParaEncerrar?: number | null;
}

export interface SorteioInput {
  jogadoresPorTime: number;
  modoSorteio?: string | null;
}

export class PeladaService {
  static async criar(grupoId: number, req: PeladaInput) {
    const grupo = await prisma.grupoPelada.findUnique({ where: { id: grupoId } });
    if (!grupo) throw new AppError('Grupo não encontrado.', 404);

    const aberta = await prisma.pelada.findFirst({
      where: {
        grupoId,
        status: { not: 'FINALIZADA' },
      },
    });
    if (aberta) {
      throw new AppError('Já existe uma pelada em andamento ou aberta.');
    }

    const dataObj = new Date(req.dataJogo + 'T12:00:00Z');
    const hojeStr = new Date().toISOString().slice(0, 10);
    if (req.dataJogo < hojeStr) {
      throw new AppError('A data da pelada não pode estar no passado.');
    }

    const pelada = await prisma.pelada.create({
      data: {
        grupoId,
        dataJogo: dataObj,
        horaJogo: req.horaJogo ? req.horaJogo.slice(0, 5) : '08:00',
        status: 'ABERTA',
        jogadoresPorTime: req.jogadoresPorTime ?? null,
        modoSorteio: req.modoSorteio ?? null,
        duracaoMinutos: req.duracaoMinutos ?? null,
        golsParaEncerrar: req.golsParaEncerrar ?? null,
      },
    });

    return this.obter(pelada.id);
  }

  static async atualizar(id: number, req: PeladaInput) {
    const pelada = await prisma.pelada.findUnique({ where: { id } });
    if (!pelada) throw new AppError('Pelada não encontrada.', 404);

    const partidasCount = await prisma.partida.count({ where: { peladaId: id } });
    if (partidasCount > 0) {
      throw new AppError('Não é possível alterar data/horário após iniciar as partidas.');
    }

    const dataObj = new Date(req.dataJogo + 'T12:00:00Z');
    const atualizada = await prisma.pelada.update({
      where: { id },
      data: {
        dataJogo: dataObj,
        horaJogo: req.horaJogo ? req.horaJogo.slice(0, 5) : pelada.horaJogo,
        jogadoresPorTime: req.jogadoresPorTime ?? pelada.jogadoresPorTime,
        modoSorteio: req.modoSorteio ?? pelada.modoSorteio,
        duracaoMinutos: req.duracaoMinutos ?? pelada.duracaoMinutos,
        golsParaEncerrar: req.golsParaEncerrar ?? pelada.golsParaEncerrar,
      },
    });

    return this.obter(atualizada.id);
  }

  static async excluir(id: number) {
    const pelada = await prisma.pelada.findUnique({ where: { id } });
    if (!pelada) throw new AppError('Pelada não encontrada.', 404);

    const partidasCount = await prisma.partida.count({ where: { peladaId: id } });
    if (partidasCount > 0) {
      throw new AppError('Esta pelada já tem partidas registradas e não pode ser excluída aqui.');
    }

    await prisma.pelada.delete({ where: { id } });
  }

  static async atual(grupoId: number) {
    const pelada = await prisma.pelada.findFirst({
      where: {
        grupoId,
        status: { not: 'FINALIZADA' },
      },
      orderBy: [{ dataJogo: 'asc' }, { id: 'asc' }],
    });

    if (!pelada) return null;
    return this.obter(pelada.id);
  }

  static async historico(grupoId: number) {
    const peladas = await prisma.pelada.findMany({
      where: {
        grupoId,
        status: 'FINALIZADA',
      },
      orderBy: [{ dataJogo: 'desc' }, { id: 'desc' }],
      include: {
        _count: {
          select: {
            jogadores: true,
            partidas: true,
          },
        },
      },
    });

    return peladas.map((p) => ({
      id: p.id,
      dataJogo: p.dataJogo.toISOString().slice(0, 10),
      horaJogo: p.horaJogo,
      status: p.status,
      totalJogadores: p._count.jogadores,
      totalPartidas: p._count.partidas,
      finalizadaEm: p.finalizadaEm ? p.finalizadaEm.toISOString() : null,
    }));
  }

  static async obter(id: number) {
    const pelada = await prisma.pelada.findUnique({
      where: { id },
      include: {
        jogadores: {
          include: { jogador: true },
        },
        times: {
          orderBy: { numero: 'asc' },
          include: {
            jogadores: {
              include: { jogador: true },
            },
          },
        },
        partidas: {
          orderBy: { ordem: 'asc' },
          include: {
            gols: true,
          },
        },
      },
    });

    if (!pelada) throw new AppError('Pelada não encontrada.', 404);

    const listaOrdenada = pelada.jogadores
      .slice()
      .sort((a, b) => a.jogador.nome.localeCompare(b.jogador.nome, 'pt-BR', { sensitivity: 'base' }))
      .map((pj) => pj.jogadorId);

    const timesDto = pelada.times.map((t) => {
      const integrantes = t.jogadores
        .slice()
        .sort((a, b) => b.jogador.overall - a.jogador.overall)
        .map((m) => ({
          jogadorId: m.jogadorId,
          overall: m.jogador.overall,
          goleiro: m.jogador.goleiro,
        }));

      const media =
        integrantes.length > 0
          ? integrantes.reduce((acc, i) => acc + i.overall, 0) / integrantes.length
          : 0;

      return {
        id: t.id,
        numero: t.numero,
        overallMedio: Math.round(media * 10) / 10,
        jogadores: integrantes,
      };
    });

    const timesPorId = new Map<number, number>();
    for (const t of pelada.times) {
      timesPorId.set(t.id, t.numero);
    }

    const partidasDto: any[] = [];
    let partidaEmAbertoId: number | null = null;
    const historicoParaSugestao: PartidaEncerradaHistorico[] = [];

    for (const p of pelada.partidas) {
      const golsA = p.gols.filter((g) => g.timeId === p.timeAId).length;
      const golsB = p.gols.filter((g) => g.timeId === p.timeBId).length;

      partidasDto.push({
        id: p.id,
        ordem: p.ordem,
        status: p.status,
        timeAId: p.timeAId,
        timeANumero: timesPorId.get(p.timeAId) ?? 0,
        golsA,
        timeBId: p.timeBId,
        timeBNumero: timesPorId.get(p.timeBId) ?? 0,
        golsB,
        vencedorId: p.vencedorId,
        vencedorPenaltisId: p.vencedorPenaltisId,
      });

      if (p.status !== 'ENCERRADA') {
        partidaEmAbertoId = p.id;
      } else {
        const fica = p.vencedorPenaltisId ?? p.vencedorId;
        historicoParaSugestao.push({
          numeroA: timesPorId.get(p.timeAId) ?? 0,
          numeroB: timesPorId.get(p.timeBId) ?? 0,
          fica: fica ? (timesPorId.get(fica) ?? null) : null,
        });
      }
    }

    let proximaSugerida: { timeAId: number; timeBId: number } | null = null;
    const sorteada = pelada.status === 'SORTEADA' || pelada.status === 'EM_ANDAMENTO';
    if (sorteada && partidaEmAbertoId === null && pelada.times.length >= 2) {
      const idPorNumero = new Map<number, number>();
      for (const t of pelada.times) idPorNumero.set(t.numero, t.id);

      const sugestao = ProximaPartida.sugerir(pelada.times.length, historicoParaSugestao);
      if (sugestao) {
        proximaSugerida = {
          timeAId: idPorNumero.get(sugestao[0])!,
          timeBId: idPorNumero.get(sugestao[1])!,
        };
      }
    }

    const hojeStr = new Date().toISOString().slice(0, 10);
    const dataJogoStr = pelada.dataJogo.toISOString().slice(0, 10);
    const sorteioLiberado =
      (pelada.status === 'ABERTA' || pelada.status === 'SORTEADA') &&
      dataJogoStr <= hojeStr;

    return {
      id: pelada.id,
      grupoId: pelada.grupoId,
      dataJogo: dataJogoStr,
      horaJogo: pelada.horaJogo,
      status: pelada.status,
      jogadoresPorTime: pelada.jogadoresPorTime,
      modoSorteio: pelada.modoSorteio,
      duracaoMinutos: pelada.duracaoMinutos,
      golsParaEncerrar: pelada.golsParaEncerrar,
      sorteioLiberado,
      lista: listaOrdenada,
      times: timesDto,
      partidas: partidasDto,
      partidaEmAbertoId,
      proximaSugerida,
    };
  }

  static async adicionarJogador(peladaId: number, jogadorId: number) {
    const pelada = await prisma.pelada.findUnique({ where: { id: peladaId } });
    if (!pelada) throw new AppError('Pelada não encontrada.', 404);
    if (pelada.status === 'FINALIZADA') throw new AppError('Pelada já finalizada.');

    await prisma.peladaJogador.upsert({
      where: {
        uk_pj: { peladaId, jogadorId },
      },
      create: { peladaId, jogadorId },
      update: {},
    });

    return this.obter(peladaId);
  }

  static async removerJogador(peladaId: number, jogadorId: number) {
    const pelada = await prisma.pelada.findUnique({ where: { id: peladaId } });
    if (!pelada) throw new AppError('Pelada não encontrada.', 404);
    if (pelada.status === 'FINALIZADA') throw new AppError('Pelada já finalizada.');

    await prisma.peladaJogador.deleteMany({
      where: { peladaId, jogadorId },
    });

    return this.obter(peladaId);
  }

  static async sortear(peladaId: number, req: SorteioInput) {
    const pelada = await prisma.pelada.findUnique({
      where: { id: peladaId },
      include: {
        jogadores: { include: { jogador: true } },
      },
    });
    if (!pelada) throw new AppError('Pelada não encontrada.', 404);
    if (pelada.status === 'FINALIZADA') throw new AppError('Pelada já finalizada.');

    const hojeStr = new Date().toISOString().slice(0, 10);
    const dataJogoStr = pelada.dataJogo.toISOString().slice(0, 10);
    if (dataJogoStr > hojeStr) {
      throw new AppError('O sorteio só é liberado no dia do jogo.');
    }

    const jogadoresLinha = pelada.jogadores
      .map((pj) => pj.jogador)
      .filter((j) => !j.goleiro);

    if (jogadoresLinha.length < 2) {
      throw new AppError('A lista precisa de ao menos 2 jogadores de linha.');
    }

    const tamanhoTime = req.jogadoresPorTime;
    const numTimes = SorteioAlgoritmo.quantidadeDeTimes(jogadoresLinha.length, tamanhoTime);
    if (numTimes < 2) {
      throw new AppError('A lista de jogadores de linha não forma ao menos 2 times.');
    }

    const statsMap = await EstatisticaService.agregarGeral(pelada.grupoId);
    let confiaveis = 0;
    for (const j of jogadoresLinha) {
      const s = statsMap.get(j.id);
      if (s && s.partidas >= 3) confiaveis++;
    }

    const temHistoricoSuficiente = confiaveis >= jogadoresLinha.length * 0.5;
    const balancear = req.modoSorteio === 'OVERALL' && temHistoricoSuficiente;

    const candidatos = jogadoresLinha.map((j) => ({ id: j.id, overall: j.overall }));
    const timesSorteados = SorteioAlgoritmo.sortear(candidatos, tamanhoTime, balancear);

    // Remove times anteriores se houver
    await prisma.timePelada.deleteMany({ where: { peladaId } });

    // Salva novos times
    for (let i = 0; i < timesSorteados.length; i++) {
      const numero = i + 1;
      const time = await prisma.timePelada.create({
        data: {
          peladaId,
          numero,
        },
      });

      for (const jogadorId of timesSorteados[i]) {
        await prisma.timePeladaJogador.create({
          data: {
            timeId: time.id,
            jogadorId,
          },
        });
      }
    }

    await prisma.pelada.update({
      where: { id: peladaId },
      data: {
        status: 'SORTEADA',
        jogadoresPorTime: tamanhoTime,
        modoSorteio: balancear ? 'OVERALL' : 'ALEATORIO',
      },
    });

    return this.obter(peladaId);
  }

  static async finalizar(peladaId: number) {
    const pelada = await prisma.pelada.findUnique({
      where: { id: peladaId },
      include: {
        jogadores: { include: { jogador: true } },
        partidas: true,
      },
    });
    if (!pelada) throw new AppError('Pelada não encontrada.', 404);

    const temPartidaAberta = pelada.partidas.some((p) => p.status !== 'ENCERRADA');
    if (temPartidaAberta) {
      throw new AppError('Encerre a partida em andamento antes de finalizar a pelada.');
    }

    // Calcula overall "antes" (estatísticas acumuladas exceto esta pelada)
    const statsAntes = await EstatisticaService.agregarExceto(pelada.grupoId, peladaId);
    // Calcula overall "depois" (estatísticas acumuladas incluindo esta pelada)
    const statsDepois = await EstatisticaService.agregarGeral(pelada.grupoId);

    // Atualiza pelada_jogador e jogador
    for (const pj of pelada.jogadores) {
      const j = pj.jogador;
      const sAntes = statsAntes.get(j.id) || null;
      const sDepois = statsDepois.get(j.id) || null;

      const overAntes = OverallCalculator.calcular(sAntes, j.goleiro);
      const overDepois = OverallCalculator.calcular(sDepois, j.goleiro);

      // Nota do dia: média das notas das estatísticas desta pelada
      const statsDestaPelada = await prisma.estatisticaPartida.findMany({
        where: { peladaId, jogadorId: j.id },
      });
      const mediaNota =
        statsDestaPelada.length > 0
          ? statsDestaPelada.reduce((acc, s) => acc + Number(s.nota), 0) / statsDestaPelada.length
          : null;

      await prisma.peladaJogador.update({
        where: { id: pj.id },
        data: {
          overallAntes: overAntes,
          overallDepois: overDepois,
          nota: mediaNota !== null ? Math.round(mediaNota * 100) / 100 : null,
        },
      });

      await prisma.jogador.update({
        where: { id: j.id },
        data: { overall: overDepois },
      });
    }

    await prisma.pelada.update({
      where: { id: peladaId },
      data: {
        status: 'FINALIZADA',
        finalizadaEm: new Date(),
      },
    });

    return this.resumo(peladaId);
  }

  static async resumo(peladaId: number) {
    const pelada = await prisma.pelada.findUnique({
      where: { id: peladaId },
      include: {
        times: { orderBy: { numero: 'asc' } },
        partidas: {
          where: { status: 'ENCERRADA' },
          include: { gols: true },
        },
        jogadores: {
          include: { jogador: true },
        },
      },
    });
    if (!pelada) throw new AppError('Pelada não encontrada.', 404);

    // 1. Classificação dos times
    const timesMap = new Map<number, {
      timeId: number;
      numero: number;
      jogos: number;
      vitorias: number;
      empates: number;
      derrotas: number;
      golsPro: number;
      golsContra: number;
      saldo: number;
    }>();

    for (const t of pelada.times) {
      timesMap.set(t.id, {
        timeId: t.id,
        numero: t.numero,
        jogos: 0,
        vitorias: 0,
        empates: 0,
        derrotas: 0,
        golsPro: 0,
        golsContra: 0,
        saldo: 0,
      });
    }

    for (const p of pelada.partidas) {
      const timeA = timesMap.get(p.timeAId);
      const timeB = timesMap.get(p.timeBId);
      if (!timeA || !timeB) continue;

      const golsA = p.gols.filter((g) => g.timeId === p.timeAId).length;
      const golsB = p.gols.filter((g) => g.timeId === p.timeBId).length;

      timeA.jogos++;
      timeB.jogos++;
      timeA.golsPro += golsA;
      timeA.golsContra += golsB;
      timeB.golsPro += golsB;
      timeB.golsContra += golsA;

      if (golsA > golsB) {
        timeA.vitorias++;
        timeB.derrotas++;
      } else if (golsB > golsA) {
        timeB.vitorias++;
        timeA.derrotas++;
      } else {
        timeA.empates++;
        timeB.empates++;
      }
    }

    const classificacaoTimes = Array.from(timesMap.values()).map((t) => ({
      ...t,
      saldo: t.golsPro - t.golsContra,
    }));
    classificacaoTimes.sort((a, b) => {
      const ptsA = a.vitorias * 3 + a.empates;
      const ptsB = b.vitorias * 3 + b.empates;
      if (ptsB !== ptsA) return ptsB - ptsA;
      if (b.vitorias !== a.vitorias) return b.vitorias - a.vitorias;
      if (b.saldo !== a.saldo) return b.saldo - a.saldo;
      return b.golsPro - a.golsPro;
    });

    // 2. Artilheiros e assistentes
    const golsContagem = new Map<number, number>();
    const assistsContagem = new Map<number, number>();

    for (const p of pelada.partidas) {
      for (const g of p.gols) {
        golsContagem.set(g.autorId, (golsContagem.get(g.autorId) || 0) + 1);
        if (g.assistenteId) {
          assistsContagem.set(g.assistenteId, (assistsContagem.get(g.assistenteId) || 0) + 1);
        }
      }
    }

    const jogadoresMap = new Map<number, string>();
    for (const pj of pelada.jogadores) {
      jogadoresMap.set(pj.jogador.id, pj.jogador.nome);
    }

    const artilheiros = Array.from(golsContagem.entries())
      .map(([jogadorId, valor]) => ({
        jogadorId,
        nome: jogadoresMap.get(jogadorId) || '',
        valor,
      }))
      .sort((a, b) => b.valor - a.valor || a.nome.localeCompare(b.nome));

    const assistentes = Array.from(assistsContagem.entries())
      .map(([jogadorId, valor]) => ({
        jogadorId,
        nome: jogadoresMap.get(jogadorId) || '',
        valor,
      }))
      .sort((a, b) => b.valor - a.valor || a.nome.localeCompare(b.nome));

    // 3. Desempenho dos jogadores
    const estatisticasPartidas = await prisma.estatisticaPartida.findMany({
      where: { peladaId },
    });

    const desempenhos = pelada.jogadores.map((pj) => {
      const statsDoJogador = estatisticasPartidas.filter((s) => s.jogadorId === pj.jogadorId);
      const partidas = statsDoJogador.length;
      let vitorias = 0, empates = 0, derrotas = 0, gols = 0, assistencias = 0, semSofrer = 0;

      for (const s of statsDoJogador) {
        if (s.resultado === 'VITORIA') vitorias++;
        else if (s.resultado === 'EMPATE') empates++;
        else if (s.resultado === 'DERROTA') derrotas++;
        gols += s.gols;
        assistencias += s.assistencias;
        if (s.semSofrer) semSofrer++;
      }

      return {
        jogadorId: pj.jogador.id,
        nome: pj.jogador.nome,
        partidas,
        vitorias,
        empates,
        derrotas,
        gols,
        assistencias,
        semSofrer,
        nota: pj.nota !== null ? Number(pj.nota) : null,
        overallAntes: pj.overallAntes,
        overallDepois: pj.overallDepois,
      };
    });

    desempenhos.sort((a, b) => (b.nota ?? 0) - (a.nota ?? 0) || a.nome.localeCompare(b.nome));

    return {
      peladaId: pelada.id,
      dataJogo: pelada.dataJogo.toISOString().slice(0, 10),
      horaJogo: pelada.horaJogo,
      status: pelada.status,
      partidasDisputadas: pelada.partidas.length,
      times: classificacaoTimes,
      artilheiros,
      assistentes,
      jogadores: desempenhos,
    };
  }
}
