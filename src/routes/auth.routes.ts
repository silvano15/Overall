import { Router, Request, Response, NextFunction } from 'express';
import { AuthService } from '../services/auth.service.js';
import { authMiddleware } from '../middleware/auth.js';

export const authRouter = Router();
export const contaRouter = Router();

authRouter.post('/login', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const { username, senha } = req.body;
    const resultado = await AuthService.autenticar(username, senha);
    res.json(resultado);
  } catch (err) {
    next(err);
  }
});

contaRouter.use(authMiddleware);

contaRouter.put('/senha', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const { senhaAtual, novaSenha } = req.body;
    await AuthService.alterarSenha(req.user!.username, senhaAtual, novaSenha);
    res.status(204).send();
  } catch (err) {
    next(err);
  }
});
