# Overall — Pelada (Pronto para Vercel)

App mobile-first para o **ADM** da pelada de fim de semana. Ninguém mais precisa instalar nada nem se cadastrar: o ADM cadastra jogadores (nome + foto), monta a lista da semana, sorteia os times, conduz as partidas ao vivo (cronômetro, gols, assistências, substituições, pênaltis) e o sistema calcula estatísticas, **overall** e rankings mensal/anual — como um Pro Clubs.

Reestruturado para **deploy 1-click no Vercel** com backend **Node.js + TypeScript**, **Prisma ORM** e banco de dados **PostgreSQL** na nuvem (Supabase, Neon ou Vercel Postgres).

---

## 🚀 Como rodar localmente

### 1. Pré-requisitos
- Node.js 18+ (ou utilize o Node portátil incluso)
- PostgreSQL (ou SQLite para testes)

### 2. Instalação e execução
```bash
# 1. Instale as dependências
npm install

# 2. Configure suas variáveis de ambiente no arquivo .env
cp .env.example .env

# 3. Gere o Prisma Client e sincronize o banco de dados
npm run prisma:generate
npm run prisma:push

# 4. Crie o grupo padrão e o admin de bootstrap (admin / admin123)
npm run prisma:seed

# 5. Inicie em modo de desenvolvimento
npm run dev
```

Abra `http://localhost:8080/` no navegador ou no celular.

---

## ☁️ Como hospedar no Vercel

Consulte o [Guia Completo de Deploy no Vercel](./VERCEL_DEPLOY.md).

Em resumo:
1. Conecte o repositório no [Vercel](https://vercel.com).
2. Configure as variáveis de ambiente:
   - `DATABASE_URL`: URL do seu banco PostgreSQL (Supabase ou Neon).
   - `JWT_SECRET`: Chave secreta JWT.
   - `APP_TIMEZONE`: `America/Recife`.
3. Clique em **Deploy**.
4. Rode `npm run prisma:push` e `npm run prisma:seed` apontando para o seu banco de produção.

---

## 📱 Fluxo da Pelada

1. **Jogadores** — nome, foto e posição de goleiro 🧤. Botão **＋ Lista da semana**.
2. **Pelada** — o ADM informa data e horário. O sorteio é liberado no dia do jogo.
3. **Sorteio** — aleatório ou balanceado por overall com histórico.
4. **Partida ao Vivo** — cronômetro persistente, gols com assistências, substituições e escolha do goleiro.
5. **Pênaltis** — Normal (todos batem), Pré-determinada (N cobranças) ou Alternadas.
6. **Finalizar Pelada** — resumo com notas do dia e atualização do overall (40–99).
7. **Rankings** — mês, ano ou geral: vitórias, artilheiros, assistências, goleiros, notas e rede de conexões.

---

## 📂 Estrutura do Projeto

```
Overall/
├── public/                 # Front-end estático (HTML, CSS 3D matrix, JS puro, mobile-first)
├── api/
│   └── index.ts            # Entrypoint das Vercel Serverless Functions
├── prisma/
│   ├── schema.prisma       # Modelagem relacional Prisma (PostgreSQL)
│   └── seed.ts             # Criação do grupo inicial e admin padrão
├── src/
│   ├── app.ts              # Aplicação Express (APIs REST sob /api)
│   ├── server.ts           # Servidor de desenvolvimento local
│   ├── config/env.ts       # Configurações de ambiente
│   ├── middleware/auth.ts  # Autenticação JWT e tratamento centralizado de erros
│   ├── routes/             # Endpoints /api (auth, grupos, jogadores, peladas, partidas, rankings)
│   ├── services/           # Lógica de negócio e persistência
│   └── utils/              # Calculadoras de Overall, Nota, Sorteio, Pênaltis e Fila
├── vercel.json             # Roteamento otimizado para a Vercel
└── package.json
```
