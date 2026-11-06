import { Router, Request, Response, NextFunction } from 'express';
import { RankingService } from '../services/ranking.service.js';
import { authMiddleware, AppError } from '../middleware/auth.js';

export const rankingRouter = Router();
rankingRouter.use(authMiddleware);

rankingRouter.get('/', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const grupoId = parseInt(req.query.grupoId as string, 10);
    if (isNaN(grupoId)) throw new AppError('O parâmetro grupoId é obrigatório.');

    const periodo = (req.query.periodo as string) || 'MES';
    const ano = req.query.ano ? parseInt(req.query.ano as string, 10) : undefined;
    const mes = req.query.mes ? parseInt(req.query.mes as string, 10) : undefined;

    const ranking = await RankingService.ranking(grupoId, periodo, ano, mes);
    res.json(ranking);
  } catch (err) {
    next(err);
  }
});

rankingRouter.get('/conexoes', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const grupoId = parseInt(req.query.grupoId as string, 10);
    if (isNaN(grupoId)) throw new AppError('O parâmetro grupoId é obrigatório.');

    const periodo = (req.query.periodo as string) || 'MES';
    const ano = req.query.ano ? parseInt(req.query.ano as string, 10) : undefined;
    const mes = req.query.mes ? parseInt(req.query.mes as string, 10) : undefined;

    const conexoes = await RankingService.conexoes(grupoId, periodo, ano, mes);
    res.json(conexoes);
  } catch (err) {
    next(err);
  }
});
