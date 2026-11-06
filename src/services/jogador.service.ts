import { prisma } from '../lib/prisma.js';
import { AppError } from '../middleware/auth.js';
import { EstatisticaService } from './estatistica.service.js';
import { estatisticasVazia } from '../utils/overallCalculator.js';

export interface JogadorInput {
  nome: string;
  foto?: string | null;
  goleiro?: boolean;
}

export class JogadorService {
  static async cadastrar(grupoId: number, req: JogadorInput) {
    if (!req.nome || !req.nome.trim()) {
      throw new AppError('O nome do jogador é obrigatório.');
    }

    const grupo = await prisma.grupoPelada.findUnique({ where: { id: grupoId } });
    if (!grupo) throw new AppError('Grupo não encontrado.', 404);

    const foto = req.foto && req.foto.trim() ? req.foto.trim() : null;
    const jogador = await prisma.jogador.create({
      data: {
        grupoId,
        nome: req.nome.trim(),
        foto,
        goleiro: Boolean(req.goleiro),
        overall: 60,
      },
    });

    return {
      id: jogador.id,
      nome: jogador.nome,
      foto: jogador.foto,
      goleiro: jogador.goleiro,
      ativo: jogador.ativo,
      overall: jogador.overall,
      estatisticas: estatisticasVazia(),
    };
  }

  static async alterar(id: number, req: JogadorInput) {
    const jogador = await prisma.jogador.findUnique({ where: { id } });
    if (!jogador) throw new AppError('Jogador não encontrado.', 404);

    const updateData: any = {};
    if (req.nome !== undefined) {
      if (!req.nome.trim()) throw new AppError('O nome do jogador é obrigatório.');
      updateData.nome = req.nome.trim();
    }
    if (req.foto !== undefined) {
      updateData.foto = req.foto && req.foto.trim() ? req.foto.trim() : null;
    }
    if (req.goleiro !== undefined) {
      updateData.goleiro = Boolean(req.goleiro);
    }

    const salvo = await prisma.jogador.update({
      where: { id },
      data: updateData,
    });

    const statsMap = await EstatisticaService.agregarGeral(salvo.grupoId);
    return {
      id: salvo.id,
      nome: salvo.nome,
      foto: salvo.foto,
      goleiro: salvo.goleiro,
      ativo: salvo.ativo,
      overall: salvo.overall,
      estatisticas: statsMap.get(salvo.id) || estatisticasVazia(),
    };
  }

  static async remover(id: number) {
    const jogador = await prisma.jogador.findUnique({ where: { id } });
    if (!jogador) throw new AppError('Jogador não encontrado.', 404);

    const [temPelada, temStats] = await Promise.all([
      prisma.peladaJogador.findFirst({ where: { jogadorId: id } }),
      prisma.estatisticaPartida.findFirst({ where: { jogadorId: id } }),
    ]);

    if (temPelada || temStats) {
      await prisma.jogador.update({
        where: { id },
        data: { ativo: false },
      });
    } else {
      await prisma.jogador.delete({ where: { id } });
    }
  }

  static async reativar(id: number) {
    const jogador = await prisma.jogador.findUnique({ where: { id } });
    if (!jogador) throw new AppError('Jogador não encontrado.', 404);

    const salvo = await prisma.jogador.update({
      where: { id },
      data: { ativo: true },
    });

    const statsMap = await EstatisticaService.agregarGeral(salvo.grupoId);
    return {
      id: salvo.id,
      nome: salvo.nome,
      foto: salvo.foto,
      goleiro: salvo.goleiro,
      ativo: salvo.ativo,
      overall: salvo.overall,
      estatisticas: statsMap.get(salvo.id) || estatisticasVazia(),
    };
  }

  static async listar(grupoId: number) {
    const [jogadores, statsMap] = await Promise.all([
      prisma.jogador.findMany({
        where: { grupoId },
        orderBy: { nome: 'asc' },
      }),
      EstatisticaService.agregarGeral(grupoId),
    ]);

    jogadores.sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR', { sensitivity: 'base' }));

    return jogadores.map((j) => ({
      id: j.id,
      nome: j.nome,
      foto: j.foto,
      goleiro: j.goleiro,
      ativo: j.ativo,
      overall: j.overall,
      estatisticas: statsMap.get(j.id) || estatisticasVazia(),
    }));
  }

  static async buscar(id: number) {
    const jogador = await prisma.jogador.findUnique({ where: { id } });
    if (!jogador) throw new AppError('Jogador não encontrado.', 404);

    const statsMap = await EstatisticaService.agregarGeral(jogador.grupoId);
    return {
      id: jogador.id,
      nome: jogador.nome,
      foto: jogador.foto,
      goleiro: jogador.goleiro,
      ativo: jogador.ativo,
      overall: jogador.overall,
      estatisticas: statsMap.get(jogador.id) || estatisticasVazia(),
    };
  }
}
