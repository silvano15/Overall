import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import { prisma } from '../lib/prisma.js';
import { ENV } from '../config/env.js';
import { AppError } from '../middleware/auth.js';

export class AuthService {
  static async autenticar(username: string, senha: string) {
    if (!username || !senha) {
      throw new AppError('Usuário e senha são obrigatórios.');
    }

    const usuario = await prisma.usuario.findUnique({
      where: { username },
    });

    if (!usuario || !usuario.ativo) {
      throw new AppError('Credenciais inválidas.', 401);
    }

    const senhaValida = await bcrypt.compare(senha, usuario.senha);
    if (!senhaValida) {
      throw new AppError('Credenciais inválidas.', 401);
    }

    const expiresInSeconds = ENV.JWT_EXPIRATION_HOURS * 3600;
    const token = jwt.sign(
      {
        sub: usuario.username,
        perfil: usuario.perfil,
      },
      ENV.JWT_SECRET,
      { expiresIn: expiresInSeconds }
    );

    return {
      token,
      tipo: 'Bearer',
      username: usuario.username,
      perfil: usuario.perfil,
    };
  }

  static async alterarSenha(username: string, senhaAtual: string, novaSenha: string) {
    if (!senhaAtual || !novaSenha) {
      throw new AppError('Senha atual e nova senha são obrigatórias.');
    }

    const usuario = await prisma.usuario.findUnique({
      where: { username },
    });

    if (!usuario) {
      throw new AppError('Usuário não encontrado.', 404);
    }

    const confere = await bcrypt.compare(senhaAtual, usuario.senha);
    if (!confere) {
      throw new AppError('A senha atual está incorreta.');
    }

    const hash = await bcrypt.hash(novaSenha, 10);
    await prisma.usuario.update({
      where: { username },
      data: { senha: hash },
    });
  }
}
