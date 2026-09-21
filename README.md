# Overall — Pelada

App para o **ADM** da pelada de fim de semana. Ninguém mais precisa instalar nada nem se cadastrar: o ADM
cadastra jogadores (nome + foto), monta a lista da semana, sorteia os times, conduz as partidas ao vivo
(cronômetro, gols, assistências, substituições, pênaltis) e o sistema calcula estatísticas, **overall** e
rankings mensal/anual — como um Pro Clubs.

Stack: Spring Boot 3 · Java 17 · MySQL + Flyway · JWT · front-end estático (JS puro, mobile-first).

## Como rodar

```bash
# MySQL: o banco "overall" é criado sozinho na primeira execução (createDatabaseIfNotExist)
mvn spring-boot:run
```

Abra `http://localhost:8080/api/` (no celular: `http://<ip-do-pc>:8080/api/`).
Login inicial: **admin / admin123** — troque na aba *Conta*.

Variáveis de ambiente: `DB_HOST`, `DB_PORT`, `DB_NAME` (padrão `overall`), `DB_USER` (padrão `root`), `DB_PASS`,
`JWT_SECRET` (**defina em produção**), `APP_TIMEZONE` (padrão `America/Recife`).
Há um MySQL pronto em `docker-compose.yml`. Swagger em `/api/swagger-ui.html`.

Testes: `mvn test` (H2 em memória rodando as mesmas migrations; não precisa de MySQL).

## Fluxo

1. **Jogadores** — nome, foto (reduzida no celular para 256px) e, se joga no gol, a marca 🧤.
   Cada jogador tem o botão **＋ Lista da semana**.
2. **Pelada** — o ADM informa data e horário. O **sorteio só é liberado no dia**.
   Escolhe *jogadores por time*; o que sobra forma um último time incompleto.
3. **Sorteio** — aleatório enquanto não há histórico (a partir de metade da lista com ≥ 3 partidas passa a
   ser **balanceado por overall**). Goleiros são espalhados entre os times.
4. **Partida** — o ADM escolhe quais times jogam, a duração e os gols que encerram.
   Cronômetro (iniciar / pausar / reiniciar / encerrar) que sobrevive a recarregar a página; a partida acaba
   sozinha por tempo ou por limite de gols. Cada time tem **Gols** (autor → assistente ou “sem assistência”)
   e **Substituição** (também *reforço* para time incompleto e escolha do goleiro).
5. **Depois da partida** — quem ganha fica; quem perde vai para o fim da fila (o time 3 sempre vem antes do 4).
   Empate: **encerrar** (os dois saem) ou **pênaltis**.
6. **Pênaltis** — três tipos:
   - *Normal*: todos os jogadores batem (cobranças por time = jogadores do maior time em campo);
   - *Pré-determinada*: o ADM escolhe quantas cobranças por time; empatou, vão às alternadas;
   - *Alternadas*: uma cobrança de cada time por vez até alguém sair na frente.
   Em todos: termina antes se um time não puder mais ser alcançado; ninguém repete antes de o time inteiro bater
   (as marcas por jogador *zeram*, mas o contador “N pênaltis batidos” continua somando).
   Pênaltis decidem quem fica na quadra, mas **não contam como gol** nem mudam o resultado (segue empate).
7. **Finalizar pelada** — resumo com ranking de times, artilheiros, assistentes, notas do dia e a variação do overall.
8. **Rankings** (aba) — mês, ano ou geral: vencedores, artilheiros, assistências, goleiros, notas e overalls.

## Front-end: design e computação gráfica

Mobile-first, sem framework (JS puro). Tema **escuro** e **claro** (Conta → Aparência; o claro ajuda ao sol) e
`prefers-reduced-motion` respeitado (sem tilt, aurora animada nem confete).

| Técnica | Onde aparece |
|---|---|
| **Matrizes 4×4 → `matrix3d`** (`js/gfx.js`, objeto `M`) | base de todas as transformações 3D abaixo |
| **Perspectiva** | cartas de jogador que giram em direção ao dedo (com reflexo); placar com os escudos "de frente um para o outro"; pódio; **campo em perspectiva** com os jogadores em pé (billboards, rotação inversa) |
| **Matriz de rotação 2D** | vértices do **radar** de atributos de cada jogador |
| **Caixa morfológica** (matriz parâmetro × opção, com nº de combinações) | sorteio (jogadores por time), regras da partida (tempo × gols), tipo de pênaltis (tipo × cobranças) |
| **Rede de interação** | Rankings → *Conexões*: grafo de assistências (assistente → autor), layout por forças, nós arrastáveis, toque destaca vizinhos; dados em `GET /rankings/conexoes` |
| **Gestalt** | proximidade (espaçamento em 3 níveis), região comum (cartões de vidro), similaridade (cor = nível/time; mesma forma = mesmo papel), continuidade (etapas da pelada e linha do tempo dos lances), fechamento (anel do cronômetro), figura-fundo (aurora desfocada; destaque no grafo) |
| **Cor e tipografia** | regra 60-30-10, cores de time com luminância equivalente, níveis de carta (Bronze/Prata/Ouro/Elite por overall), Sora + Inter, escala modular |
| **Movimento** | entrada em sequência, contador animado, confete e faixa "GOL", vibração no celular |

Arquivos: `css/theme.css` (tokens e componentes), `css/fx.css` (efeitos), `js/gfx.js` (matrizes, tilt, radar, anel, confete,
grafo), `js/ui.js` (componentes compartilhados), `js/icons.js` (ícones SVG) e um arquivo por tela.

## Overall (40–99) e nota

Atualizado só ao finalizar cada pelada. Quem estreia começa em **60**; com poucas partidas o resultado é
puxado para 60 (peso de 5 partidas), para 1 jogo bom não virar craque.

| | Peso |
|---|---|
| **Linha** | 40% aproveitamento (vitórias, empates, derrotas) · 35% gols/partida · 25% assistências/partida |
| **Goleiro** | 45% partidas sem levar gol · 30% aproveitamento · 15% derrotas evitadas · 10% gols+assistências |

As constantes ficam em `OverallCalculator` e `NotaCalculator`. A **nota** de cada jogo (3–10) parte de 6,0 e
sobe com vitória, gols, assistências e (goleiro) jogo sem sofrer gol; a nota do dia é a média das partidas.

## Decisões que tomei (fáceis de mudar)

- **Uma pelada em aberto por vez**; um único ADM (sem multi-grupo).
- **Reiniciar** partida zera cronômetro **e** placar (pede confirmação).
- Jogador que entra por substituição conta a partida e o resultado dela; o clean sheet vai para quem termina no gol.
- Jogador com histórico não é apagado: é **arquivado** (dá para reativar).
- No modo *Normal* de pênaltis com times de tamanhos diferentes, usa-se o maior time; o menor repete cobradores.

## Estrutura

```
src/main/java/br/com/overall
  auth/ security/ config/ exception/   login JWT, conta, erros (aproveitados do projeto original)
  jogador/      cadastro
  pelada/       lista, sorteio (SorteioAlgoritmo), times, finalização/resumo
  partida/      partida ao vivo, ProximaPartida (fila), PenaltisRegra
  estatistica/  tabela-fato por jogador/partida, OverallCalculator, NotaCalculator
  ranking/      rankings mensal/anual/geral
src/main/resources/db/migration/V1__schema_pelada.sql
src/main/resources/static/            front-end (index.html, css/, js/)
```
