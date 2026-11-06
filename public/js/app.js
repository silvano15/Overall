// ---------- Roteamento por hash, login, conta e tema ----------

/** Função de limpeza da tela atual (ex.: para o cronômetro da partida). */
let aoSair = null;

// Carregamento sob demanda (Google PRPL / Code-Splitting)
const _modulosCarregados = new Set();
function carregarModulo(arquivo) {
  if (_modulosCarregados.has(arquivo)) return Promise.resolve();
  return new Promise((resolve, reject) => {
    const s = document.createElement('script');
    s.src = `js/${arquivo}.js`;
    s.onload = () => { _modulosCarregados.add(arquivo); resolve(); };
    s.onerror = reject;
    document.head.appendChild(s);
  });
}

// Prefetch em segundo plano no tempo ocioso do navegador
function prefetchModulos() {
  const modulos = ['gfx', 'pelada', 'jogadores', 'partida', 'rankings'];
  const carregarProximo = () => {
    if (!modulos.length) return;
    const m = modulos.shift();
    carregarModulo(m).finally(() => {
      if ('requestIdleCallback' in window) requestIdleCallback(carregarProximo);
      else setTimeout(carregarProximo, 200);
    });
  };
  if ('requestIdleCallback' in window) requestIdleCallback(carregarProximo);
  else setTimeout(carregarProximo, 500);
}

// Rotas com `g: true` vivem dentro de um grupo: o 1º número da URL é o id do grupo (vira GID).
const ROTAS = [
  { re: /^#\/grupos$/, tab: null, titulo: 'Grupos', modulos: [], tela: (v) => telaGrupos(v) },
  { re: /^#\/grupos\/(\d+)$/, g: true, tab: 'pelada', titulo: 'Grupo', modulos: ['gfx', 'pelada'], tela: (v) => telaPelada(v) },
  { re: /^#\/grupos\/(\d+)\/times$/, g: true, tab: 'pelada', titulo: 'Times', modulos: ['gfx', 'pelada'], tela: (v) => telaPelada(v, 'times') },
  { re: /^#\/grupos\/(\d+)\/peladas$/, g: true, tab: 'pelada', titulo: 'Peladas', modulos: ['gfx', 'pelada'], tela: () => telaBibliotecaPeladas() },
  { re: /^#\/grupos\/(\d+)\/jogadores$/, g: true, tab: 'jogadores', titulo: 'Jogadores', modulos: ['gfx', 'jogadores'], tela: (v) => telaJogadores(v) },
  { re: /^#\/grupos\/(\d+)\/rankings$/, g: true, tab: 'rankings', titulo: 'Rankings', modulos: ['gfx', 'rankings'], tela: (v) => telaRankings(v) },
  { re: /^#\/conta$/, tab: 'conta', titulo: 'Conta', modulos: [], tela: (v) => telaConta(v) },
  { re: /^#\/grupos\/(\d+)\/partida\/(\d+)$/, g: true, tab: 'pelada', titulo: 'Ao vivo', modulos: ['gfx', 'partida'], tela: (v, id) => telaPartida(v, Number(id)) },
  { re: /^#\/grupos\/(\d+)\/resumo\/(\d+)$/, g: true, tab: 'pelada', titulo: 'Resumo', modulos: ['gfx', 'pelada'], tela: (v, id) => telaResumo(v, Number(id)) }
];
const ABAS = ['pelada', 'jogadores', 'rankings', 'conta'];

/**
 * Atualiza a barra de navegação inferior:
 * Na tela inicial (Biblioteca de grupos), mostra apenas "Pelada" e "Conta".
 * Dentro de um grupo, expande para as 4 abas: Pelada, Jogadores, Rankings e Conta.
 */
function atualizarNav(rota) {
  const hash = location.hash || '#/grupos';
  const rotaAtual = rota || ROTAS.find(r => r.re.test(hash)) || ROTAS[0];
  const naBiblioteca = !rotaAtual.g && rotaAtual.tab !== 'conta';

  const nav = document.querySelector('.nav');
  if (nav) {
    nav.classList.toggle('mode-grupos', naBiblioteca);
    nav.style.setProperty('--nav-cols', naBiblioteca ? '2' : '4');
  }

  const base = GID ? `#/grupos/${GID}` : '#/grupos';
  const destino = {
    pelada: naBiblioteca ? '#/grupos' : base,
    jogadores: GID ? base + '/jogadores' : '#/grupos',
    rankings: GID ? base + '/rankings' : '#/grupos',
    conta: '#/conta'
  };
  document.querySelectorAll('.nav a').forEach(a => {
    a.href = destino[a.dataset.tab] || '#/grupos';
  });
}

async function rotear() {
  if (!token) return;
  if (aoSair) { aoSair(); aoSair = null; }
  fecharSheet();
  const hash = location.hash || '#/grupos';
  const rota = ROTAS.find(r => r.re.test(hash)) || ROTAS[0];
  const args = (hash.match(rota.re) || []).slice(1);
  if (rota.g) {
    GID = Number(args.shift());
    try { localStorage.setItem('overall_gid', String(GID)); } catch (e) { /* ignora */ }
  }
  const naBiblioteca = !rota.g && rota.tab !== 'conta';
  atualizarNav(rota);

  document.querySelectorAll('.nav a').forEach(a => {
    let ativo = false;
    if (naBiblioteca) {
      ativo = (a.dataset.tab === 'pelada' && rota.tab === null) || (a.dataset.tab === rota.tab);
    } else {
      ativo = (a.dataset.tab === rota.tab);
    }
    a.classList.toggle('active', ativo);
  });

  const idx = naBiblioteca
    ? (rota.tab === 'conta' ? 1 : 0)
    : Math.max(0, ABAS.indexOf(rota.tab));

  const nav = document.querySelector('.nav');
  if (nav) {
    nav.style.setProperty('--idx', idx);
    const pill = nav.querySelector('.nav-pill');
    if (pill) pill.style.opacity = '1';
  }
  document.getElementById('topbar-title').textContent = rota.titulo;

  const view = document.getElementById('view');
  view.dataset.entrada = '1';
  view.innerHTML = esqueleto();
  window.scrollTo(0, 0);
  try {
    if (rota.modulos && rota.modulos.length) {
      await Promise.all(rota.modulos.map(carregarModulo));
    }
    await rota.tela(view, ...args);
  } catch (e) {
    if (!token) return;
    view.innerHTML = `<div class="card"><div class="empty">${arteVazia()}${esc(e.message)}
      <div class="mt-4"><button class="btn" onclick="rotear()">Tentar de novo</button></div></div></div>`;
  }
}
window.addEventListener('hashchange', rotear);

// ---------- login ----------

async function doLogin() {
  const username = document.getElementById('login-username').value.trim();
  const senha = document.getElementById('login-password').value;
  const err = document.getElementById('login-error');
  err.hidden = true;
  try {
    const res = await fetch(`${API}/auth/login`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username, senha }) });
    if (!res.ok) throw new Error(res.status === 401 ? 'Usuário ou senha inválidos.' : 'Não consegui entrar. Tente novamente.');
    salvarToken((await res.json()).token);
    document.getElementById('login-password').value = '';
    mostrarApp();
  } catch (e) {
    err.textContent = e.message; err.hidden = false; vibrar([30, 40, 30]);
  }
}

function mostrarApp() {
  document.getElementById('login-screen').hidden = true;
  document.getElementById('app').hidden = false;
  if (!location.hash) location.hash = '#/grupos';
  rotear();
  prefetchModulos();
}

function sair() {
  if (aoSair) { aoSair(); aoSair = null; }
  salvarToken(null);
  fecharSheet();
  document.getElementById('app').hidden = true;
  document.getElementById('login-screen').hidden = false;
}

// ---------- conta ----------

function telaConta() {
  const t = temaSalvo();
  const temas = [['auto', 'Auto', 'grid'], ['dark', 'Escuro', 'moon'], ['light', 'Claro', 'sun']];
  const barra = (rotulo, pct, k) => `<div style="margin:10px 0"><div class="row between small"><span>${rotulo}</span><b>${pct}%</b></div>
    <div class="bar"><i style="--w:${pct}%;--k:${k}"></i></div></div>`;
  pintar(`
    <div style="margin:2px 2px 14px"><div class="eyebrow">Conta</div><h1 class="h1 mt-2">Ajustes</h1></div>

    <div class="card"><div class="card-head"><h2 class="h3">Aparência</h2></div>
      <div class="seg" style="--n:3;--idx:${temas.findIndex(x => x[0] === t)};margin-bottom:8px"><span class="seg-pill"></span>
        ${temas.map(([id, nome, i]) => `<button class="${t === id ? 'on' : ''}" onclick="mudarTema('${id}')">${ic(i, 16)} ${nome}</button>`).join('')}</div>
      <div class="small muted">O tema claro ajuda a enxergar o placar sob o sol do campo.</div></div>

    <div class="card"><div class="card-head"><h2 class="h3">Alterar senha</h2></div>
      <div class="small muted" style="margin-bottom:12px">Troque a senha padrão (<b>admin123</b>) no primeiro acesso.</div>
      <label class="field"><span>Senha atual</span><input type="password" id="senha-atual" autocomplete="current-password"></label>
      <label class="field"><span>Nova senha (mín. 6 caracteres)</span><input type="password" id="senha-nova" autocomplete="new-password"></label>
      <label class="field"><span>Repita a nova senha</span><input type="password" id="senha-nova2" autocomplete="new-password"></label>
      <button class="btn primary block" onclick="alterarSenha()">Salvar nova senha</button></div>

    <div class="card"><div class="card-head"><h2 class="h3">Como o overall é calculado</h2></div>
      <div class="small muted">De 40 a 99, atualizado ao finalizar a pelada. Quem estreia começa em 60 e se afasta conforme joga.</div>
      <div class="eyebrow mt-4">Jogador de linha</div>
      ${barra('Aproveitamento (V · E · D)', 40, 0)}${barra('Gols por partida', 35, 1)}${barra('Assistências por partida', 25, 2)}
      <div class="eyebrow mt-4">Goleiro <span class="faint">— 90% defesa e resultado</span></div>
      ${barra('Partidas sem levar gol', 45, 0)}${barra('Aproveitamento', 30, 1)}${barra('Derrotas evitadas', 15, 2)}${barra('Gols e assistências', 10, 3)}
      <div class="eyebrow mt-4">Níveis da carta</div>
      <div class="row wrap gap-2 mt-2">${[[40, 'Bronze <55'], [58, 'Prata 55–64'], [70, 'Ouro 65–79'], [85, 'Elite 80+']].map(([n, r]) => `<span class="row gap-2">${ovr(n)}<span class="small muted">${r}</span></span>`).join('')}</div>
      <p class="small muted mt-4">A <b>nota</b> do jogo (3 a 10) é separada do overall. Os pênaltis decidem quem fica na quadra, mas não contam como gol.</p></div>

    <a class="btn block" href="#/grupos" style="margin-bottom:12px">${ic('grid')} Trocar de grupo</a>

    <button class="btn danger block" onclick="sair()">${ic('logout')} Sair</button>`);
}

function mudarTema(t) { aplicarTema(t); vibrar(8); telaConta(); }

async function alterarSenha() {
  const atual = document.getElementById('senha-atual').value, nova = document.getElementById('senha-nova').value;
  if (nova !== document.getElementById('senha-nova2').value) return toast('As senhas novas não conferem.', 'error');
  try {
    await PUT('/conta/senha', { senhaAtual: atual, novaSenha: nova });
    toast('Senha alterada!');
    ['senha-atual', 'senha-nova', 'senha-nova2'].forEach(id => { document.getElementById(id).value = ''; });
  } catch (e) { erroToast(e); }
}

// ---------- login em 3D: o cartão gira levemente em direção ao ponteiro ----------
(function loginEmPerspectiva() {
  const card = document.querySelector('.login-card');
  if (!card || REDUZIDO || typeof M === 'undefined') return;
  document.getElementById('login-screen').addEventListener('pointermove', (e) => {
    const nx = (e.clientX / innerWidth - .5) * 2, ny = (e.clientY / innerHeight - .5) * 2;
    card.style.transform = M.css(M.chain(M.perspective(1100), M.rotX(-ny * 6), M.rotY(nx * 8)));
  });
})();

document.querySelectorAll('.nav a').forEach(a => a.insertAdjacentHTML('afterbegin', ic(a.dataset.ic)));
document.getElementById('login-mark').innerHTML = logoMark(84, 'lg-login');
document.getElementById('topbar-logo').innerHTML = logoMark(26, 'lg-top');
aplicarTema(temaSalvo());

// ---------- início ----------
if (token) mostrarApp();
