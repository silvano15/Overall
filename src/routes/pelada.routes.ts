import { Router, Request, Response, NextFunction } from 'express';
import { PeladaService } from '../services/pelada.service.js';
import { PartidaService } from '../services/partida.service.js';
import { ExclusaoPeladaService } from '../services/exclusao.service.js';
import { authMiddleware, AppError } from '../middleware/auth.js';

export const peladaRouter = Router();
peladaRouter.use(authMiddleware);

peladaRouter.post('/', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const grupoId = parseInt(req.query.grupoId as string, 10);
    if (isNaN(grupoId)) throw new AppError('O parâmetro grupoId é obrigatório.');
    const criada = await PeladaService.criar(grupoId, req.body);
    res.status(201).json(criada);
  } catch (err) {
    next(err);
  }
});

peladaRouter.put('/:id', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const id = parseInt(req.params.id, 10);
    const atualizada = await PeladaService.atualizar(id, req.body);
    res.json(atualizada);
  } catch (err) {
    next(err);
  }
});

peladaRouter.delete('/:id', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const id = parseInt(req.params.id, 10);
    await PeladaService.excluir(id);
    res.status(204).send();
  } catch (err) {
    next(err);
  }
});

peladaRouter.post('/:id/exclusao', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const id = parseInt(req.params.id, 10);
    const { senha } = req.body;
    await ExclusaoPeladaService.excluir(id, req.user!.username, senha);
    res.status(204).send();
  } catch (err) {
    next(err);
  }
});

peladaRouter.get('/atual', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const grupoId = parseInt(req.query.grupoId as string, 10);
    if (isNaN(grupoId)) throw new AppError('O parâmetro grupoId é obrigatório.');
    const atual = await PeladaService.atual(grupoId);
    if (!atual) {
      res.status(204).send();
      return;
    }
    res.json(atual);
  } catch (err) {
    next(err);
  }
});

peladaRouter.get('/', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const grupoId = parseInt(req.query.grupoId as string, 10);
    if (isNaN(grupoId)) throw new AppError('O parâmetro grupoId é obrigatório.');
    const historico = await PeladaService.historico(grupoId);
    res.json(historico);
  } catch (err) {
    next(err);
  }
});

peladaRouter.get('/:id', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const id = parseInt(req.params.id, 10);
    const pelada = await PeladaService.obter(id);
    res.json(pelada);
  } catch (err) {
    next(err);
  }
});

peladaRouter.put('/:id/lista/:jogadorId', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const id = parseInt(req.params.id, 10);
    const jogadorId = parseInt(req.params.jogadorId, 10);
    const pelada = await PeladaService.adicionarJogador(id, jogadorId);
    res.json(pelada);
  } catch (err) {
    next(err);
  }
});

peladaRouter.delete('/:id/lista/:jogadorId', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const id = parseInt(req.params.id, 10);
    const jogadorId = parseInt(req.params.jogadorId, 10);
    const pelada = await PeladaService.removerJogador(id, jogadorId);
    res.json(pelada);
  } catch (err) {
    next(err);
  }
});

peladaRouter.post('/:id/sorteio', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const id = parseInt(req.params.id, 10);
    const pelada = await PeladaService.sortear(id, req.body);
    res.json(pelada);
  } catch (err) {
    next(err);
  }
});

peladaRouter.post('/:id/partidas', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const id = parseInt(req.params.id, 10);
    const partida = await PartidaService.criar(id, req.body);
    res.status(201).json(partida);
  } catch (err) {
    next(err);
  }
});

peladaRouter.post('/:id/finalizar', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const id = parseInt(req.params.id, 10);
    const resumo = await PeladaService.finalizar(id);
    res.json(resumo);
  } catch (err) {
    next(err);
  }
});

peladaRouter.get('/:id/resumo', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const id = parseInt(req.params.id, 10);
    const resumo = await PeladaService.resumo(id);
    res.json(resumo);
  } catch (err) {
    next(err);
  }
});
