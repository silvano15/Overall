import app from './app.js';
import { ENV } from './config/env.js';

app.listen(ENV.PORT, () => {
  console.log(`=========================================`);
  console.log(` Overall - Pelada (Node.js/TypeScript)   `);
  console.log(` Servidor rodando em: http://localhost:${ENV.PORT}`);
  console.log(`=========================================`);
});
