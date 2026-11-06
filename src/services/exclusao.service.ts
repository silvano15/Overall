import bcrypt from 'bcryptjs';
import { prisma } from '../lib/prisma.js';
import { AppError } from '../middleware/auth.js';
import { OverallCalculator, EstatisticasCalculo, estatisticasVazia } from '../utils/overallCalculator.js';

export class ExclusaoPeladaService {
  static async excluir(peladaId: number, username: string, senha: string) {
    if (!senha) throw new AppError('Senha incorreta.');

    const usuario = await prisma.usuario.findUnique({ where: { username } });
    if (!usuario) throw new AppError('Senha incorreta.');

    const senhaOk = await bcrypt.compare(senha, usuario.senha);
    if (!senhaOk) throw new AppError('Senha incorreta.');

    const pelada = await prisma.pelada.findUnique({ where: { id: peladaId } });
    if (!pelada) throw new AppError('Pelada não encontrada.', 404);

    const grupoId = pelada.grupoId;

    // Remove partidas e dependências
    const partidas = await prisma.partida.findMany({ where: { peladaId } });
    const partidaIds = partidas.map((p) => p.id);

    if (partidaIds.length > 0) {
      await prisma.gol.deleteMany({ where: { partidaId: { in: partidaIds } } });
      await prisma.substituicao.deleteMany({ where: { partidaId: { in: partidaIds } } });
      await prisma.penalti.deleteMany({ where: { partidaId: { in: partidaIds } } });
      await prisma.partidaJogador.deleteMany({ where: { partidaId: { in: partidaIds } } });
      await prisma.estatisticaPartida.deleteMany({ where: { partidaId: { in: partidaIds } } });
      await prisma.partida.deleteMany({ where: { id: { in: partidaIds } } });
    }

    const times = await prisma.timePelada.findMany({ where: { peladaId } });
    const timeIds = times.map((t) => t.id);
    if (timeIds.length > 0) {
      await prisma.timePeladaJogador.deleteMany({ where: { timeId: { in: timeIds } } });
      await prisma.timePelada.deleteMany({ where: { id: { in: timeIds } } });
    }

    await prisma.peladaJogador.deleteMany({ where: { peladaId } });
    await prisma.pelada.delete({ where: { id: peladaId } });

    // Reconstrói histórico de overall
    await this.refazerHistoricoDeOverall(grupoId);
  }

  private static async refazerHistoricoDeOverall(grupoId: number) {
    const jogadores = await prisma.jogador.findMany({ where: { grupoId } });
    const jogadoresMap = new Map<number, any>();
    for (const j of jogadores) jogadoresMap.set(j.id, j);

    const finalizadas = await prisma.pelada.findMany({
      where: { grupoId, status: 'FINALIZADA' },
      orderBy: [{ dataJogo: 'asc' }, { id: 'asc' }],
    });

    const acumulado = new Map<number, EstatisticasCalculo>();

    for (const p of finalizadas) {
      const lista = await prisma.peladaJogador.findMany({ where: { peladaId: p.id } });
      const antes = new Map<number, number>();

      for (const pj of lista) {
        const j = jogadoresMap.get(pj.jogadorId);
        const est = acumulado.get(pj.jogadorId) || null;
        antes.set(pj.jogadorId, OverallCalculator.calcular(est, j?.goleiro ?? false));
      }

      const estatisticas = await prisma.estatisticaPartida.findMany({ where: { peladaId: p.id } });
      for (const e of estatisticas) {
        let est = acumulado.get(e.jogadorId);
        if (!est) {
          est = estatisticasVazia();
          acumulado.set(e.jogadorId, est);
        }
        est.partidas++;
        if (e.resultado === 'VITORIA') est.vitorias++;
        else if (e.resultado === 'EMPATE') est.empates++;
        else if (e.resultado === 'DERROTA') est.derrotas++;
        est.gols += e.gols;
        est.assistencias += e.assistencias;
        if (e.goleiro) {
          est.jogosComoGoleiro++;
          est.golsSofridos += e.golsSofridos;
          if (e.semSofrer) est.semSofrer++;
        }
      }

      for (const pj of lista) {
        const j = jogadoresMap.get(pj.jogadorId);
        const est = acumulado.get(pj.jogadorId) || null;
        const depois = OverallCalculator.calcular(est, j?.goleiro ?? false);

        await prisma.peladaJogador.update({
          where: { id: pj.id },
          data: {
            overallAntes: antes.get(pj.jogadorId),
            overallDepois: depois,
          },
        });
      }
    }

    for (const j of jogadores) {
      const est = acumulado.get(j.id) || null;
      const overAtual = OverallCalculator.calcular(est, j.goleiro);
      await prisma.jogador.update({
        where: { id: j.id },
        data: { overall: overAtual },
      });
    }
  }
}
