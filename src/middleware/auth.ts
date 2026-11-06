import { Request, Response, NextFunction } from 'express';
import jwt from 'jsonwebtoken';
import { ENV } from '../config/env.js';

export interface AuthUser {
  username: string;
  perfil: string;
}

declare global {
  namespace Express {
    interface Request {
      user?: AuthUser;
    }
  }
}

export function authMiddleware(req: Request, res: Response, next: NextFunction): void {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    res.status(401).json({ mensagem: 'Acesso não autorizado. Token ausente.' });
    return;
  }

  const token = authHeader.substring(7);
  try {
    const payload = jwt.verify(token, ENV.JWT_SECRET) as any;
    req.user = {
      username: payload.sub || payload.username,
      perfil: payload.perfil || 'ADMIN',
    };
    next();
  } catch (err) {
    res.status(401).json({ mensagem: 'Sessão expirada. Entre novamente.' });
  }
}

export class AppError extends Error {
  statusCode: number;

  constructor(message: string, statusCode = 400) {
    super(message);
    this.statusCode = statusCode;
    Object.setPrototypeOf(this, AppError.prototype);
  }
}

export function errorHandler(err: any, req: Request, res: Response, next: NextFunction): void {
  console.error('API Error:', err);
  const status = err.statusCode || err.status || 500;
  const mensagem = err.message || 'Ocorreu um erro interno no servidor.';
  res.status(status).json({ mensagem });
}
