import express from 'express';
import cors from 'cors';
import path from 'path';
import { fileURLToPath } from 'url';
import { authRouter, contaRouter } from './routes/auth.routes.js';
import { grupoRouter } from './routes/grupo.routes.js';
import { jogadorRouter } from './routes/jogador.routes.js';
import { peladaRouter } from './routes/pelada.routes.js';
import { partidaRouter } from './routes/partida.routes.js';
import { rankingRouter } from './routes/ranking.routes.js';
import { errorHandler } from './middleware/auth.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();

app.use(cors());
app.use(express.json({ limit: '10mb' }));

// Rotas da API sob /api
app.use('/api/auth', authRouter);
app.use('/api/conta', contaRouter);
app.use('/api/grupos', grupoRouter);
app.use('/api/jogadores', jogadorRouter);
app.use('/api/peladas', peladaRouter);
app.use('/api/partidas', partidaRouter);
app.use('/api/rankings', rankingRouter);

// Fallback de rotas diretas (caso o proxy da Vercel ou cliente envie sem o prefixo /api)
app.use('/auth', authRouter);
app.use('/conta', contaRouter);
app.use('/grupos', grupoRouter);
app.use('/jogadores', jogadorRouter);
app.use('/peladas', peladaRouter);
app.use('/partidas', partidaRouter);
app.use('/rankings', rankingRouter);

// Arquivos estáticos do frontend (para desenvolvimento local)
const publicDir = path.resolve(__dirname, '../public');
app.use(express.static(publicDir, { maxAge: '1h', etag: true }));

// Fallback para SPA (index.html)
app.get('*', (req, res, next) => {
  if (req.path.startsWith('/api') || req.path.startsWith('/auth') || req.path.startsWith('/grupos') ||
      req.path.startsWith('/jogadores') || req.path.startsWith('/peladas') ||
      req.path.startsWith('/partidas') || req.path.startsWith('/rankings')) {
    return next();
  }
  res.sendFile(path.join(publicDir, 'index.html'));
});

// Middleware central de tratamento de erros
app.use(errorHandler);

export default app;
