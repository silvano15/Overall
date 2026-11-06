import { Router, Request, Response, NextFunction } from 'express';
import { GrupoService } from '../services/grupo.service.js';
import { authMiddleware } from '../middleware/auth.js';

export const grupoRouter = Router();
grupoRouter.use(authMiddleware);

grupoRouter.post('/', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const { nome } = req.body;
    const criado = await GrupoService.criar(nome);
    res.status(201).json(criado);
  } catch (err) {
    next(err);
  }
});

grupoRouter.put('/:id', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const id = parseInt(req.params.id, 10);
    const { nome } = req.body;
    const atualizado = await GrupoService.atualizar(id, nome);
    res.json(atualizado);
  } catch (err) {
    next(err);
  }
});

grupoRouter.delete('/:id', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const id = parseInt(req.params.id, 10);
    await GrupoService.excluir(id);
    res.status(204).send();
  } catch (err) {
    next(err);
  }
});

grupoRouter.get('/', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const lista = await GrupoService.listar();
    res.json(lista);
  } catch (err) {
    next(err);
  }
});

grupoRouter.get('/:id', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const id = parseInt(req.params.id, 10);
    const grupo = await GrupoService.obter(id);
    res.json(grupo);
  } catch (err) {
    next(err);
  }
});
