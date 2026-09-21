// ---------- Roteamento por hash, login, conta e tema ----------

/** Função de limpeza da tela atual (ex.: para o cronômetro da partida). */
let aoSair = null;

const ROTAS = [
  { re: /^#\/pelada$/, tab: 'pelada', titulo: 'Pelada', tela: (v) => telaPelada(v) },
  { re: /^#\/jogadores$/, tab: 'jogadores', titulo: 'Jogadores', tela: (v) => telaJogadores(v) },
  { re: /^#\/rankings$/, tab: 'rankings', titulo: 'Rankings', tela: (v) => telaRankings(v) },
  { re: /^#\/conta$/, tab: 'conta', titulo: 'Conta', tela: (v) => telaConta(v) },
  { re: /^#\/partida\/(\d+)$/, tab: 'pelada', titulo: 'Ao vivo', tela: (v, id) => telaPartida(v, Number(id)) },
  { re: /^#\/resumo\/(\d+)$/, tab: 'pelada', titulo: 'Resumo', tela: (v, id) => telaResumo(v, Number(id)) }
];
const ABAS = ['pelada', 'jogadores', 'rankings', 'conta'];

async function rotear() {
  if (!token) return;
  if (aoSair) { aoSair(); aoSair = null; }
  fecharSheet();
  const hash = location.hash || '#/pelada';
  const rota = ROTAS.find(r => r.re.test(hash)) || ROTAS[0];
  const args = (hash.match(rota.re) || []).slice(1);

  document.querySelectorAll('.nav a').forEach(a => a.classList.toggle('active', a.dataset.tab === rota.tab));
  document.querySelector('.nav').style.setProperty('--idx', ABAS.indexOf(rota.tab));
  document.getElementById('topbar-title').textContent = rota.titulo;

  const view = document.getElementById('view');
  view.dataset.entrada = '1';
  view.innerHTML = esqueleto();
  window.scrollTo(0, 0);
  try {
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
  if (!location.hash) location.hash = '#/pelada';
  rotear();
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
  if (!card || REDUZIDO) return;
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
