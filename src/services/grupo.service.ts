import { prisma } from '../lib/prisma.js';
import { AppError } from '../middleware/auth.js';

export class GrupoService {
  static async criar(nome: string) {
    if (!nome || !nome.trim()) {
      throw new AppError('O nome do grupo é obrigatório.');
    }
    const grupo = await prisma.grupoPelada.create({
      data: { nome: nome.trim() },
    });
    return this.montar(grupo.id, grupo.nome);
  }

  static async atualizar(id: number, nome: string) {
    if (!nome || !nome.trim()) {
      throw new AppError('O nome do grupo é obrigatório.');
    }
    const grupo = await prisma.grupoPelada.findUnique({ where: { id } });
    if (!grupo) throw new AppError('Grupo não encontrado.', 404);

    const atualizado = await prisma.grupoPelada.update({
      where: { id },
      data: { nome: nome.trim() },
    });
    return this.montar(atualizado.id, atualizado.nome);
  }

  static async excluir(id: number) {
    const grupo = await prisma.grupoPelada.findUnique({ where: { id } });
    if (!grupo) throw new AppError('Grupo não encontrado.', 404);

    const jogadoresCount = await prisma.jogador.count({ where: { grupoId: id } });
    if (jogadoresCount > 0) {
      throw new AppError('Esse grupo já tem jogadores cadastrados e não pode ser excluído.');
    }

    await prisma.grupoPelada.delete({ where: { id } });
  }

  static async listar() {
    const grupos = await prisma.grupoPelada.findMany({
      orderBy: { nome: 'asc' },
    });
    return Promise.all(grupos.map((g) => this.montar(g.id, g.nome)));
  }

  static async obter(id: number) {
    const grupo = await prisma.grupoPelada.findUnique({ where: { id } });
    if (!grupo) throw new AppError('Grupo não encontrado.', 404);
    return this.montar(grupo.id, grupo.nome);
  }

  private static async montar(id: number, nome: string) {
    const [jogadores, peladas, peladaAberta] = await Promise.all([
      prisma.jogador.count({ where: { grupoId: id, ativo: true } }),
      prisma.pelada.count({ where: { grupoId: id } }),
      prisma.pelada.findFirst({
        where: {
          grupoId: id,
          status: { not: 'FINALIZADA' },
        },
      }),
    ]);

    return {
      id,
      nome,
      jogadores,
      peladas,
      aberta: peladaAberta !== null,
    };
  }
}
