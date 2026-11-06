import { prisma } from '../lib/prisma.js';
import { AppError } from '../middleware/auth.js';
import { PenaltisRegra } from '../utils/penaltisRegra.js';
import { NotaCalculator, ResultadoTipo } from '../utils/notaCalculator.js';

export interface PartidaInput {
  timeAId: number;
  timeBId: number;
  duracaoSegundos: number;
  golsParaEncerrar: number;
}

export interface GolInput {
  timeId: number;
  autorId: number;
  assistenteId?: number | null;
}

export interface SubstituicaoInput {
  timeId: number;
  saiuId?: number | null;
  entrouId: number;
}

export interface GoleiroInput {
  timeId: number;
  jogadorId: number;
}

export interface IniciarPenaltisInput {
  modo: string; // NORMAL | PREDETERMINADO | ALTERNADO
  rodadas?: number;
}

export interface PenaltiInput {
  timeId: number;
  jogadorId: number;
  convertido: boolean;
}

export class PartidaService {
  static async criar(peladaId: number, req: PartidaInput) {
    const pelada = await prisma.pelada.findUnique({ where: { id: peladaId } });
    if (!pelada) throw new AppError('Pelada não encontrada.', 404);
    if (pelada.status === 'ABERTA') {
      throw new AppError('Sorteie os times antes de criar uma partida.');
    }
    if (pelada.status === 'FINALIZADA') {
      throw new AppError('Esta pelada já foi finalizada.');
    }

    const aberta = await prisma.partida.findFirst({
      where: {
        peladaId,
        status: { not: 'ENCERRADA' },
      },
    });
    if (aberta) {
      throw new AppError('Já existe uma partida em andamento nesta pelada.');
    }

    const timeA = await prisma.timePelada.findFirst({
      where: { id: req.timeAId, peladaId },
      include: { jogadores: { include: { jogador: true } } },
    });
    const timeB = await prisma.timePelada.findFirst({
      where: { id: req.timeBId, peladaId },
      include: { jogadores: { include: { jogador: true } } },
    });
    if (!timeA || !timeB || timeA.id === timeB.id) {
      throw new AppError('Times inválidos para a partida.');
    }

    const ultima = await prisma.partida.findFirst({
      where: { peladaId },
      orderBy: { ordem: 'desc' },
    });
    const ordem = (ultima?.ordem ?? 0) + 1;

    const partida = await prisma.partida.create({
      data: {
        peladaId,
        ordem,
        timeAId: timeA.id,
        timeBId: timeB.id,
        duracaoSegundos: req.duracaoSegundos,
        golsParaEncerrar: req.golsParaEncerrar,
        status: 'AGUARDANDO',
        acumuladoMs: BigInt(0),
      },
    });

    // Escala os jogadores do time A
    for (const tpj of timeA.jogadores) {
      await prisma.partidaJogador.create({
        data: {
          partidaId: partida.id,
          timeId: timeA.id,
          jogadorId: tpj.jogadorId,
          goleiro: tpj.jogador.goleiro,
          emCampo: true,
        },
      });
    }

    // Escala os jogadores do time B
    for (const tpj of timeB.jogadores) {
      await prisma.partidaJogador.create({
        data: {
          partidaId: partida.id,
          timeId: timeB.id,
          jogadorId: tpj.jogadorId,
          goleiro: tpj.jogador.goleiro,
          emCampo: true,
        },
      });
    }

    await prisma.pelada.update({
      where: { id: peladaId },
      data: {
        status: 'EM_ANDAMENTO',
        duracaoMinutos: Math.round(req.duracaoSegundos / 60),
        golsParaEncerrar: req.golsParaEncerrar,
      },
    });

    return this.obter(partida.id);
  }

  static async obter(id: number) {
    const partida = await this.carregarEAtualizarTempo(id);
    return this.montar(partida);
  }

  static async cancelar(id: number) {
    const partida = await prisma.partida.findUnique({ where: { id } });
    if (!partida) throw new AppError('Partida não encontrada.', 404);
    if (partida.status !== 'AGUARDANDO') {
      throw new AppError('Não é possível cancelar uma partida que já foi iniciada.');
    }
    await prisma.partida.delete({ where: { id } });
  }

  static async iniciar(id: number) {
    const partida = await this.carregarEAtualizarTempo(id);
    if (partida.status !== 'AGUARDANDO') {
      throw new AppError('A partida já foi iniciada.');
    }

    const agora = Date.now();
    const atualizada = await prisma.partida.update({
      where: { id },
      data: {
        status: 'EM_ANDAMENTO',
        retomadaEmMs: BigInt(agora),
      },
    });

    return this.montar(atualizada);
  }

  static async pausar(id: number) {
    const partida = await this.carregarEAtualizarTempo(id);
    if (partida.status !== 'EM_ANDAMENTO') {
      throw new AppError('A partida não está em andamento para ser pausada.');
    }

    const agora = Date.now();
    const trecho = partida.retomadaEmMs ? agora - Number(partida.retomadaEmMs) : 0;
    const acumulado = Number(partida.acumuladoMs) + Math.max(0, trecho);

    const atualizada = await prisma.partida.update({
      where: { id },
      data: {
        status: 'PAUSADA',
        acumuladoMs: BigInt(acumulado),
        retomadaEmMs: null,
      },
    });

    return this.montar(atualizada);
  }

  static async retomar(id: number) {
    const partida = await this.carregarEAtualizarTempo(id);
    if (partida.status !== 'PAUSADA') {
      throw new AppError('A partida não está pausada para ser retomada.');
    }

    const agora = Date.now();
    const atualizada = await prisma.partida.update({
      where: { id },
      data: {
        status: 'EM_ANDAMENTO',
        retomadaEmMs: BigInt(agora),
      },
    });

    return this.montar(atualizada);
  }

  static async reiniciar(id: number) {
    const partida = await prisma.partida.findUnique({ where: { id } });
    if (!partida) throw new AppError('Partida não encontrada.', 404);
    if (partida.status === 'ENCERRADA') {
      throw new AppError('A partida já foi encerrada.');
    }

    await prisma.gol.deleteMany({ where: { partidaId: id } });
    await prisma.substituicao.deleteMany({ where: { partidaId: id } });
    await prisma.penalti.deleteMany({ where: { partidaId: id } });

    const atualizada = await prisma.partida.update({
      where: { id },
      data: {
        status: 'AGUARDANDO',
        acumuladoMs: BigInt(0),
        retomadaEmMs: null,
        vencedorId: null,
        vencedorPenaltisId: null,
        penaltisModo: null,
        penaltisRodadas: null,
      },
    });

    return this.montar(atualizada);
  }

  static async encerrar(id: number) {
    const partida = await this.carregarEAtualizarTempo(id);
    if (partida.status === 'ENCERRADA') return this.montar(partida);

    const agora = Date.now();
    const decorrido = this.decorridoMs(partida, agora);
    const gols = await prisma.gol.findMany({ where: { partidaId: id } });
    const golsA = gols.filter((g) => g.timeId === partida.timeAId).length;
    const golsB = gols.filter((g) => g.timeId === partida.timeBId).length;

    if (golsA === golsB) {
      const atualizada = await prisma.partida.update({
        where: { id },
        data: {
          status: 'EMPATE_PENDENTE',
          acumuladoMs: BigInt(Math.min(decorrido, partida.duracaoSegundos * 1000)),
          retomadaEmMs: null,
        },
      });
      return this.montar(atualizada);
    }

    const vencedorId = golsA > golsB ? partida.timeAId : partida.timeBId;
    const atualizada = await prisma.partida.update({
      where: { id },
      data: {
        status: 'ENCERRADA',
        vencedorId,
        acumuladoMs: BigInt(Math.min(decorrido, partida.duracaoSegundos * 1000)),
        retomadaEmMs: null,
        encerradaEm: new Date(),
      },
    });

    await this.registrarEstatisticas(atualizada);
    return this.montar(atualizada);
  }

  static async registrarGol(id: number, req: GolInput) {
    const partida = await this.carregarEAtualizarTempo(id);
    if (partida.status !== 'EM_ANDAMENTO') {
      throw new AppError('Gols só podem ser registrados enquanto a partida está em andamento.');
    }
    if (req.timeId !== partida.timeAId && req.timeId !== partida.timeBId) {
      throw new AppError('Time não pertence à partida.');
    }

    const agora = Date.now();
    const tempoMs = this.decorridoMs(partida, agora);

    await prisma.gol.create({
      data: {
        partidaId: id,
        timeId: req.timeId,
        autorId: req.autorId,
        assistenteId: req.assistenteId ?? null,
        tempoMs: BigInt(tempoMs),
      },
    });

    const gols = await prisma.gol.findMany({ where: { partidaId: id } });
    const golsDoTime = gols.filter((g) => g.timeId === req.timeId).length;

    // Checa limite de gols
    if (golsDoTime >= partida.golsParaEncerrar) {
      return this.encerrar(id);
    }

    return this.montar(partida);
  }

  static async desfazerGol(id: number, golId: number) {
    const partida = await this.carregarEAtualizarTempo(id);
    if (partida.status === 'ENCERRADA') {
      throw new AppError('Não é possível alterar gols de uma partida encerrada.');
    }

    await prisma.gol.delete({ where: { id: golId } });

    // Se estava em EMPATE_PENDENTE por ter acabado os gols, volta para EM_ANDAMENTO ou PAUSADA
    if (partida.status === 'EMPATE_PENDENTE') {
      const atualizada = await prisma.partida.update({
        where: { id },
        data: { status: 'PAUSADA' },
      });
      return this.montar(atualizada);
    }

    return this.montar(partida);
  }

  static async substituir(id: number, req: SubstituicaoInput) {
    const partida = await this.carregarEAtualizarTempo(id);
    if (partida.status === 'ENCERRADA') {
      throw new AppError('A partida já foi encerrada.');
    }
    if (req.timeId !== partida.timeAId && req.timeId !== partida.timeBId) {
      throw new AppError('Time não pertence à partida.');
    }

    const agora = Date.now();
    const tempoMs = this.decorridoMs(partida, agora);

    if (req.saiuId) {
      await prisma.partidaJogador.updateMany({
        where: { partidaId: id, jogadorId: req.saiuId },
        data: { emCampo: false },
      });
    }

    await prisma.partidaJogador.upsert({
      where: {
        uk_pjg: { partidaId: id, jogadorId: req.entrouId },
      },
      create: {
        partidaId: id,
        timeId: req.timeId,
        jogadorId: req.entrouId,
        emCampo: true,
        goleiro: false,
      },
      update: {
        emCampo: true,
      },
    });

    await prisma.substituicao.create({
      data: {
        partidaId: id,
        timeId: req.timeId,
        saiuId: req.saiuId ?? null,
        entrouId: req.entrouId,
        tempoMs: BigInt(tempoMs),
      },
    });

    return this.montar(partida);
  }

  static async definirGoleiro(id: number, req: GoleiroInput) {
    const partida = await this.carregarEAtualizarTempo(id);
    if (partida.status === 'ENCERRADA') {
      throw new AppError('A partida já foi encerrada.');
    }
    if (req.timeId !== partida.timeAId && req.timeId !== partida.timeBId) {
      throw new AppError('Time não pertence à partida.');
    }

    // Marca todos os outros desse time como não goleiro
    await prisma.partidaJogador.updateMany({
      where: { partidaId: id, timeId: req.timeId },
      data: { goleiro: false },
    });

    // Define o goleiro
    await prisma.partidaJogador.updateMany({
      where: { partidaId: id, timeId: req.timeId, jogadorId: req.jogadorId },
      data: { goleiro: true },
    });

    return this.montar(partida);
  }

  static async encerrarEmpate(id: number) {
    const partida = await this.carregarEAtualizarTempo(id);
    if (partida.status !== 'EMPATE_PENDENTE') {
      throw new AppError('A partida não está aguardando desfecho de empate.');
    }

    const atualizada = await prisma.partida.update({
      where: { id },
      data: {
        status: 'ENCERRADA',
        vencedorId: null,
        vencedorPenaltisId: null,
        encerradaEm: new Date(),
      },
    });

    await this.registrarEstatisticas(atualizada);
    return this.montar(atualizada);
  }

  static async iniciarPenaltis(id: number, req: IniciarPenaltisInput) {
    const partida = await this.carregarEAtualizarTempo(id);
    if (partida.status !== 'EMPATE_PENDENTE') {
      throw new AppError('Disputa de pênaltis só é permitida em caso de empate pendente.');
    }

    const participantes = await prisma.partidaJogador.findMany({ where: { partidaId: id } });
    const emCampoA = participantes.filter((j) => j.timeId === partida.timeAId && j.emCampo).length;
    const emCampoB = participantes.filter((j) => j.timeId === partida.timeBId && j.emCampo).length;

    let rodadas = 3;
    if (req.modo === 'NORMAL') {
      rodadas = Math.max(emCampoA, emCampoB);
    } else if (req.modo === 'PREDETERMINADO') {
      rodadas = req.rodadas && req.rodadas > 0 ? req.rodadas : 3;
    } else if (req.modo === 'ALTERNADO') {
      rodadas = 0;
    }

    const atualizada = await prisma.partida.update({
      where: { id },
      data: {
        status: 'PENALTIS',
        penaltisModo: req.modo,
        penaltisRodadas: rodadas,
      },
    });

    return this.montar(atualizada);
  }

  static async registrarPenalti(id: number, req: PenaltiInput) {
    const partida = await this.carregarEAtualizarTempo(id);
    if (partida.status !== 'PENALTIS') {
      throw new AppError('A partida não está na disputa de pênaltis.');
    }

    const cobrancas = await prisma.penalti.findMany({
      where: { partidaId: id },
      orderBy: { ordem: 'asc' },
    });

    const proximaOrdem = cobrancas.length + 1;
    const esperadoTimeId = proximaOrdem % 2 === 1 ? partida.timeAId : partida.timeBId;
    if (req.timeId !== esperadoTimeId) {
      throw new AppError('Não é a vez deste time bater o pênalti.');
    }

    await prisma.penalti.create({
      data: {
        partidaId: id,
        ordem: proximaOrdem,
        timeId: req.timeId,
        jogadorId: req.jogadorId,
        convertido: req.convertido,
      },
    });

    return this.montar(partida);
  }

  static async desfazerPenalti(id: number) {
    const partida = await this.carregarEAtualizarTempo(id);
    if (partida.status !== 'PENALTIS') {
      throw new AppError('A partida não está na disputa de pênaltis.');
    }

    const ultimo = await prisma.penalti.findFirst({
      where: { partidaId: id },
      orderBy: { ordem: 'desc' },
    });
    if (ultimo) {
      await prisma.penalti.delete({ where: { id: ultimo.id } });
    }

    return this.montar(partida);
  }

  static async encerrarPenaltis(id: number) {
    const partida = await this.carregarEAtualizarTempo(id);
    if (partida.status !== 'PENALTIS') {
      throw new AppError('A partida não está na disputa de pênaltis.');
    }

    const cobrancas = await prisma.penalti.findMany({
      where: { partidaId: id },
      orderBy: { ordem: 'asc' },
    });

    let golsA = 0, golsB = 0, cobrA = 0, cobrB = 0;
    for (const c of cobrancas) {
      if (c.timeId === partida.timeAId) {
        cobrA++;
        if (c.convertido) golsA++;
      } else {
        cobrB++;
        if (c.convertido) golsB++;
      }
    }

    const v = PenaltisRegra.vencedor(cobrA, cobrB, golsA, golsB, partida.penaltisRodadas ?? 0);
    if (v === 0) {
      throw new AppError('A disputa de pênaltis ainda não tem um vencedor definido.');
    }

    const vencedorPenaltisId = v > 0 ? partida.timeAId : partida.timeBId;
    const atualizada = await prisma.partida.update({
      where: { id },
      data: {
        status: 'ENCERRADA',
        vencedorId: null, // Pênaltis não alteram o empate no placar da partida
        vencedorPenaltisId,
        encerradaEm: new Date(),
      },
    });

    await this.registrarEstatisticas(atualizada);
    return this.montar(atualizada);
  }

  // --- helpers internos

  private static decorridoMs(partida: any, agora: number): number {
    let decorrido = Number(partida.acumuladoMs);
    if (partida.retomadaEmMs) {
      decorrido += agora - Number(partida.retomadaEmMs);
    }
    return decorrido;
  }

  private static async carregarEAtualizarTempo(id: number) {
    const partida = await prisma.partida.findUnique({ where: { id } });
    if (!partida) throw new AppError('Partida não encontrada.', 404);

    if (partida.status === 'EM_ANDAMENTO') {
      const agora = Date.now();
      const decorrido = this.decorridoMs(partida, agora);
      const duracaoMs = partida.duracaoSegundos * 1000;

      if (decorrido >= duracaoMs) {
        // Encerra ou vai para empate pendente por tempo
        const gols = await prisma.gol.findMany({ where: { partidaId: id } });
        const golsA = gols.filter((g) => g.timeId === partida.timeAId).length;
        const golsB = gols.filter((g) => g.timeId === partida.timeBId).length;

        if (golsA === golsB) {
          return prisma.partida.update({
            where: { id },
            data: {
              status: 'EMPATE_PENDENTE',
              acumuladoMs: BigInt(duracaoMs),
              retomadaEmMs: null,
            },
          });
        } else {
          const vencedorId = golsA > golsB ? partida.timeAId : partida.timeBId;
          const encerrada = await prisma.partida.update({
            where: { id },
            data: {
              status: 'ENCERRADA',
              vencedorId,
              acumuladoMs: BigInt(duracaoMs),
              retomadaEmMs: null,
              encerradaEm: new Date(),
            },
          });
          await this.registrarEstatisticas(encerrada);
          return encerrada;
        }
      }
    }

    return partida;
  }

  private static async registrarEstatisticas(partida: any) {
    const pelada = await prisma.pelada.findUnique({ where: { id: partida.peladaId } });
    if (!pelada) return;

    const gols = await prisma.gol.findMany({ where: { partidaId: partida.id } });
    const golsA = gols.filter((g) => g.timeId === partida.timeAId).length;
    const golsB = gols.filter((g) => g.timeId === partida.timeBId).length;

    const participantes = await prisma.partidaJogador.findMany({
      where: { partidaId: partida.id },
    });

    for (const pj of participantes) {
      const doTimeA = pj.timeId === partida.timeAId;
      const pro = doTimeA ? golsA : golsB;
      const contra = doTimeA ? golsB : golsA;

      const resultado: ResultadoTipo =
        pro > contra ? 'VITORIA' : pro === contra ? 'EMPATE' : 'DERROTA';

      const golsDele = gols.filter((g) => g.autorId === pj.jogadorId).length;
      const assistencias = gols.filter((g) => g.assistenteId === pj.jogadorId).length;
      const semSofrer = pj.goleiro && contra === 0;

      const nota = NotaCalculator.calcular(resultado, golsDele, assistencias, pj.goleiro, contra);

      await prisma.estatisticaPartida.upsert({
        where: {
          uk_est: { partidaId: partida.id, jogadorId: pj.jogadorId },
        },
        create: {
          partidaId: partida.id,
          peladaId: partida.peladaId,
          dataJogo: pelada.dataJogo,
          jogadorId: pj.jogadorId,
          timeId: pj.timeId,
          resultado,
          gols: golsDele,
          assistencias,
          goleiro: pj.goleiro,
          golsSofridos: pj.goleiro ? contra : 0,
          semSofrer,
          nota,
        },
        update: {
          resultado,
          gols: golsDele,
          assistencias,
          goleiro: pj.goleiro,
          golsSofridos: pj.goleiro ? contra : 0,
          semSofrer,
          nota,
        },
      });
    }
  }

  private static async montar(p: any) {
    const agora = Date.now();
    const duracaoMs = p.duracaoSegundos * 1000;
    const decorrido = Math.min(this.decorridoMs(p, agora), duracaoMs);
    const relogioCorrendo = p.status === 'EM_ANDAMENTO' && p.retomadaEmMs !== null;

    const [times, gols, participantes, substituicoes, cobrancas] = await Promise.all([
      prisma.timePelada.findMany({ where: { id: { in: [p.timeAId, p.timeBId] } } }),
      prisma.gol.findMany({ where: { partidaId: p.id }, orderBy: { id: 'asc' } }),
      prisma.partidaJogador.findMany({ where: { partidaId: p.id } }),
      prisma.substituicao.findMany({ where: { partidaId: p.id }, orderBy: { id: 'asc' } }),
      prisma.penalti.findMany({ where: { partidaId: p.id }, orderBy: { ordem: 'asc' } }),
    ]);

    const timesMap = new Map<number, number>();
    for (const t of times) timesMap.set(t.id, t.numero);

    const timeAAtletas = participantes
      .filter((j) => j.timeId === p.timeAId)
      .map((j) => ({
        jogadorId: j.jogadorId,
        goleiro: j.goleiro,
        emCampo: j.emCampo,
      }));
    const timeBAtletas = participantes
      .filter((j) => j.timeId === p.timeBId)
      .map((j) => ({
        jogadorId: j.jogadorId,
        goleiro: j.goleiro,
        emCampo: j.emCampo,
      }));

    const golsA = gols.filter((g) => g.timeId === p.timeAId).length;
    const golsB = gols.filter((g) => g.timeId === p.timeBId).length;

    let situacaoPenaltis: any = null;
    if (p.status === 'PENALTIS' || p.vencedorPenaltisId !== null) {
      let cobrancasA = 0, cobrancasB = 0, pGolsA = 0, pGolsB = 0;
      const batedoresA: number[] = [];
      const batedoresB: number[] = [];

      for (const c of cobrancas) {
        if (c.timeId === p.timeAId) {
          cobrancasA++;
          batedoresA.push(c.jogadorId);
          if (c.convertido) pGolsA++;
        } else {
          cobrancasB++;
          batedoresB.push(c.jogadorId);
          if (c.convertido) pGolsB++;
        }
      }

      const rodadas = p.penaltisRodadas ?? 0;
      const v = PenaltisRegra.vencedor(cobrancasA, cobrancasB, pGolsA, pGolsB, rodadas);
      const decidido = v > 0 ? p.timeAId : v < 0 ? p.timeBId : null;
      const proximo = decidido !== null ? null : (cobrancas.length % 2 === 0 ? p.timeAId : p.timeBId);

      const elencoA = new Set(timeAAtletas.filter((a) => a.emCampo).map((a) => a.jogadorId));
      const elencoB = new Set(timeBAtletas.filter((a) => a.emCampo).map((a) => a.jogadorId));
      const cicloA = Array.from(PenaltisRegra.jaBateramNoCiclo(batedoresA, elencoA));
      const cicloB = Array.from(PenaltisRegra.jaBateramNoCiclo(batedoresB, elencoB));

      situacaoPenaltis = {
        golsA: pGolsA,
        golsB: pGolsB,
        cobrancasA,
        cobrancasB,
        modo: p.penaltisModo,
        rodadas,
        alternadas: PenaltisRegra.emAlternadas(cobrancasA, cobrancasB, rodadas),
        proximoTimeId: proximo,
        decididoParaTimeId: decidido,
        cicloA,
        cicloB,
        cobrancas: cobrancas.map((c) => ({
          ordem: c.ordem,
          timeId: c.timeId,
          jogadorId: c.jogadorId,
          convertido: c.convertido,
        })),
      };
    }

    return {
      id: p.id,
      peladaId: p.peladaId,
      ordem: p.ordem,
      status: p.status,
      duracaoSegundos: p.duracaoSegundos,
      golsParaEncerrar: p.golsParaEncerrar,
      decorridoMs: decorrido,
      relogioCorrendo,
      agoraMs: agora,
      timeA: {
        timeId: p.timeAId,
        numero: timesMap.get(p.timeAId) ?? 1,
        gols: golsA,
        jogadores: timeAAtletas,
      },
      timeB: {
        timeId: p.timeBId,
        numero: timesMap.get(p.timeBId) ?? 2,
        gols: golsB,
        jogadores: timeBAtletas,
      },
      vencedorId: p.vencedorId,
      vencedorPenaltisId: p.vencedorPenaltisId,
      gols: gols.map((g) => ({
        id: g.id,
        timeId: g.timeId,
        autorId: g.autorId,
        assistenteId: g.assistenteId,
        tempoMs: Number(g.tempoMs),
      })),
      substituicoes: substituicoes.map((s) => ({
        id: s.id,
        timeId: s.timeId,
        saiuId: s.saiuId,
        entrouId: s.entrouId,
        tempoMs: Number(s.tempoMs),
      })),
      penaltis: situacaoPenaltis,
    };
  }
}
