import { PrismaClient } from '@prisma/client';
import bcrypt from 'bcryptjs';

const prisma = new PrismaClient();

async function main() {
  console.log('Executando seed...');

  // 1. Grupo inicial
  let grupo = await prisma.grupoPelada.findFirst();
  if (!grupo) {
    grupo = await prisma.grupoPelada.create({
      data: {
        nome: 'Minha Pelada',
      },
    });
    console.log('Grupo inicial criado:', grupo.nome);
  }

  // 2. Usuário administrador de bootstrap (admin / admin123)
  const adminExistente = await prisma.usuario.findUnique({
    where: { username: 'admin' },
  });

  if (!adminExistente) {
    const senhaHash = await bcrypt.hash('admin123', 10);
    const admin = await prisma.usuario.create({
      data: {
        username: 'admin',
        senha: senhaHash,
        nomeCompleto: 'Administrador',
        perfil: 'ADMIN',
        ativo: true,
      },
    });
    console.log('Administrador criado com sucesso:', admin.username);
  } else {
    console.log('Administrador já existe.');
  }

  console.log('Seed concluído com sucesso!');
}

main()
  .catch((e) => {
    console.error('Erro durante o seed:', e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
