import { Router, Request, Response, NextFunction } from 'express';
import { PartidaService } from '../services/partida.service.js';
import { authMiddleware } from '../middleware/auth.js';

export const partidaRouter = Router();
partidaRouter.use(authMiddleware);

partidaRouter.get('/:id', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const id = parseInt(req.params.id, 10);
    const partida = await PartidaService.obter(id);
    res.json(partida);
  } catch (err) {
    next(err);
  }
});

partidaRouter.delete('/:id', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const id = parseInt(req.params.id, 10);
    await PartidaService.cancelar(id);
    res.status(204).send();
  } catch (err) {
    next(err);
  }
});

partidaRouter.post('/:id/iniciar', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const id = parseInt(req.params.id, 10);
    const partida = await PartidaService.iniciar(id);
    res.json(partida);
  } catch (err) {
    next(err);
  }
});

partidaRouter.post('/:id/pausar', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const id = parseInt(req.params.id, 10);
    const partida = await PartidaService.pausar(id);
    res.json(partida);
  } catch (err) {
    next(err);
  }
});

partidaRouter.post('/:id/retomar', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const id = parseInt(req.params.id, 10);
    const partida = await PartidaService.retomar(id);
    res.json(partida);
  } catch (err) {
    next(err);
  }
});

partidaRouter.post('/:id/reiniciar', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const id = parseInt(req.params.id, 10);
    const partida = await PartidaService.reiniciar(id);
    res.json(partida);
  } catch (err) {
    next(err);
  }
});

partidaRouter.post('/:id/encerrar', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const id = parseInt(req.params.id, 10);
    const partida = await PartidaService.encerrar(id);
    res.json(partida);
  } catch (err) {
    next(err);
  }
});

partidaRouter.post('/:id/gols', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const id = parseInt(req.params.id, 10);
    const partida = await PartidaService.registrarGol(id, req.body);
    res.json(partida);
  } catch (err) {
    next(err);
  }
});

partidaRouter.delete('/:id/gols/:golId', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const id = parseInt(req.params.id, 10);
    const golId = parseInt(req.params.golId, 10);
    const partida = await PartidaService.desfazerGol(id, golId);
    res.json(partida);
  } catch (err) {
    next(err);
  }
});

partidaRouter.post('/:id/substituicoes', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const id = parseInt(req.params.id, 10);
    const partida = await PartidaService.substituir(id, req.body);
    res.json(partida);
  } catch (err) {
    next(err);
  }
});

partidaRouter.put('/:id/goleiro', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const id = parseInt(req.params.id, 10);
    const partida = await PartidaService.definirGoleiro(id, req.body);
    res.json(partida);
  } catch (err) {
    next(err);
  }
});

partidaRouter.post('/:id/empate/encerrar', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const id = parseInt(req.params.id, 10);
    const partida = await PartidaService.encerrarEmpate(id);
    res.json(partida);
  } catch (err) {
    next(err);
  }
});

partidaRouter.post('/:id/penaltis/iniciar', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const id = parseInt(req.params.id, 10);
    const partida = await PartidaService.iniciarPenaltis(id, req.body);
    res.json(partida);
  } catch (err) {
    next(err);
  }
});

partidaRouter.post('/:id/penaltis', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const id = parseInt(req.params.id, 10);
    const partida = await PartidaService.registrarPenalti(id, req.body);
    res.json(partida);
  } catch (err) {
    next(err);
  }
});

partidaRouter.delete('/:id/penaltis/ultimo', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const id = parseInt(req.params.id, 10);
    const partida = await PartidaService.desfazerPenalti(id);
    res.json(partida);
  } catch (err) {
    next(err);
  }
});

partidaRouter.post('/:id/penaltis/encerrar', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const id = parseInt(req.params.id, 10);
    const partida = await PartidaService.encerrarPenaltis(id);
    res.json(partida);
  } catch (err) {
    next(err);
  }
});
