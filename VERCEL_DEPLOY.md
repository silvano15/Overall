# Guia de Deploy no Vercel — Overall Pelada

Este guia orienta passo a passo como colocar seu sistema de pelada no ar na **Vercel** com banco de dados **PostgreSQL na nuvem** (gratuito via Supabase, Neon ou Vercel Postgres).

---

## 1. Criar o Banco de Dados PostgreSQL (Gratuito)

Recomendamos utilizar o **Neon** (https://neon.tech) ou **Supabase** (https://supabase.com):

1. Crie uma conta gratuita no [Neon](https://neon.tech) ou [Supabase](https://supabase.com).
2. Crie um novo projeto (ex: `overall-pelada`).
3. Copie a string de conexão (`Connection String` ou `URI`), que tem o formato:
   ```text
   postgresql://usuario:senha@ep-xyz.us-east-1.aws.neon.tech/neondb?sslmode=require
   ```

---

## 2. Configurar o Projeto no Vercel

1. Envie o código para o seu repositório GitHub.
2. Acesse [vercel.com](https://vercel.com) e faça login.
3. Clique em **"Add New..."** → **"Project"** e selecione o repositório `Overall`.
4. Em **Build and Output Settings**:
   - **Framework Preset**: `Other`
   - **Build Command**: `npm run build`
   - **Output Directory**: (deixe em branco ou padrão)
5. Em **Environment Variables**, adicione as seguintes variáveis:
   - `DATABASE_URL`: a URL de conexão do PostgreSQL obtida no passo 1.
   - `JWT_SECRET`: uma chave secreta aleatória com pelo menos 32 caracteres (ex: `chave-secreta-overall-pelada-2026-xyz`).
   - `APP_TIMEZONE`: `America/Recife` (ou o fuso horário da sua região).
6. Clique em **Deploy**.

---

## 3. Criar as Tabelas e o Usuário Inicial (Seed)

Após o primeiro deploy, execute o push do schema e o seed para criar o administrador:

No terminal da sua máquina (com a mesma `DATABASE_URL` no `.env`):
```bash
# Cria todas as tabelas no PostgreSQL na nuvem
npx prisma db push

# Cria o grupo inicial 'Minha Pelada' e o admin padrão:
# Usuário: admin
# Senha:   admin123
npx prisma db seed
```

Alternativamente, você pode usar o comando integrado:
```bash
npm run prisma:push
npm run prisma:seed
```

---

## 4. Acessando a Aplicação

Abra o domínio gerado pela Vercel (ex: `https://overall-pelada.vercel.app`).
1. Faça login com:
   - **Usuário**: `admin`
   - **Senha**: `admin123`
2. Na aba **Conta**, altere a senha imediatamente para sua segurança.
3. Cadastre os jogadores com foto, crie a pelada da semana e conduza as partidas ao vivo diretamente pelo celular!
