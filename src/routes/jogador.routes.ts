import { Router, Request, Response, NextFunction } from 'express';
import { JogadorService } from '../services/jogador.service.js';
import { authMiddleware, AppError } from '../middleware/auth.js';

export const jogadorRouter = Router();
jogadorRouter.use(authMiddleware);

jogadorRouter.post('/', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const grupoId = parseInt(req.query.grupoId as string, 10);
    if (isNaN(grupoId)) throw new AppError('O parâmetro grupoId é obrigatório.');
    const criado = await JogadorService.cadastrar(grupoId, req.body);
    res.status(201).json(criado);
  } catch (err) {
    next(err);
  }
});

jogadorRouter.put('/:id', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const id = parseInt(req.params.id, 10);
    const atualizado = await JogadorService.alterar(id, req.body);
    res.json(atualizado);
  } catch (err) {
    next(err);
  }
});

jogadorRouter.delete('/:id', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const id = parseInt(req.params.id, 10);
    await JogadorService.remover(id);
    res.status(204).send();
  } catch (err) {
    next(err);
  }
});

jogadorRouter.post('/:id/reativar', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const id = parseInt(req.params.id, 10);
    const reativado = await JogadorService.reativar(id);
    res.json(reativado);
  } catch (err) {
    next(err);
  }
});

jogadorRouter.get('/', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const grupoId = parseInt(req.query.grupoId as string, 10);
    if (isNaN(grupoId)) throw new AppError('O parâmetro grupoId é obrigatório.');
    const lista = await JogadorService.listar(grupoId);
    res.json(lista);
  } catch (err) {
    next(err);
  }
});

jogadorRouter.get('/:id', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const id = parseInt(req.params.id, 10);
    const jogador = await JogadorService.buscar(id);
    res.json(jogador);
  } catch (err) {
    next(err);
  }
});
