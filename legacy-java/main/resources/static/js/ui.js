// =====================================================================
//  Overall — componentes de interface compartilhados
// =====================================================================

/** Escapa texto para HTML (nomes de jogadores vêm do usuário). */
function esc(s) {
  return String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}
function vibrar(ms) { try { if (navigator.vibrate && !REDUZIDO) navigator.vibrate(ms); } catch (e) { /* sem suporte */ } }

// ---------- tema (auto / escuro / claro) ----------
function temaSalvo() { try { return localStorage.getItem('overall_tema') || 'auto'; } catch (e) { return 'auto'; } }
function aplicarTema(t) {
  document.documentElement.dataset.theme = t;
  try { localStorage.setItem('overall_tema', t); } catch (e) { /* ignora */ }
  const cor = getComputedStyle(document.documentElement).getPropertyValue('--bg').trim();
  const meta = document.querySelector('meta[name="theme-color"]');
  if (meta && cor) meta.content = cor;
}
document.documentElement.dataset.theme = temaSalvo();

// ---------- toast ----------
let toastTimer = null;
function toast(msg, tipo) {
  const t = document.getElementById('toast');
  t.className = tipo === 'error' ? 'error' : '';
  t.innerHTML = `${ic(tipo === 'error' ? 'alert' : 'check')}<span>${esc(msg)}</span>`;
  t.hidden = false;
  t.style.animation = 'none'; void t.offsetWidth; t.style.animation = '';
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { t.hidden = true; }, 3200);
  if (tipo === 'error') vibrar([30, 40, 30]);
}
const erroToast = (e) => toast(e && e.message ? e.message : String(e), 'error');

// ---------- gaveta (bottom sheet) com gesto de arrastar para fechar ----------
function abrirSheet(titulo, html) {
  document.getElementById('sheet-title').textContent = titulo;
  document.getElementById('sheet-body').innerHTML = html;
  document.getElementById('sheet-overlay').hidden = false;
  document.getElementById('sheet').style.transform = '';
  montarFx(document.getElementById('sheet'));
}
function fecharSheet() { document.getElementById('sheet-overlay').hidden = true; }
document.addEventListener('click', e => { if (e.target.id === 'sheet-overlay') fecharSheet(); });
(function gestoDaGaveta() {
  const sheet = document.getElementById('sheet');
  if (!sheet) return;
  let y0 = null, dy = 0;
  const alvo = (e) => e.target.closest('.sheet-grip, .sheet-header');
  sheet.addEventListener('pointerdown', e => { if (alvo(e)) { y0 = e.clientY; dy = 0; sheet.style.transition = 'none'; } });
  addEventListener('pointermove', e => {
    if (y0 === null) return;
    dy = Math.max(0, e.clientY - y0);
    sheet.style.transform = `translateY(${dy}px)`;
  });
  const fim = () => {
    if (y0 === null) return;
    sheet.style.transition = 'transform .3s var(--ease)';
    if (dy > 110) fecharSheet(); else sheet.style.transform = '';
    y0 = null;
  };
  addEventListener('pointerup', fim); addEventListener('pointercancel', fim);
})();

/** Confirmação dentro da gaveta. */
function confirmar(titulo, mensagem, textoBotao, acao, perigo = true) {
  window.__confirmarAcao = acao;
  abrirSheet(titulo, `
    <p class="muted" style="margin-bottom:18px">${esc(mensagem)}</p>
    <div class="btn-row">
      <button class="btn" onclick="fecharSheet()">Cancelar</button>
      <button class="btn ${perigo ? 'danger' : 'primary'}" onclick="fecharSheet(); window.__confirmarAcao()">${esc(textoBotao)}</button>
    </div>`);
}

// ---------- pintura da tela: entrada em sequência + efeitos ----------
function pintar(html) {
  const v = document.getElementById('view');
  v.innerHTML = html;
  if (v.dataset.entrada === '1') {
    [...v.children].filter(el => !el.hasAttribute('data-norise')).forEach((el, i) => { el.classList.add('rise'); el.style.setProperty('--i', Math.min(i, 9)); });
    v.dataset.entrada = '0';
  }
  montarFx(v);
}
function esqueleto() {
  return `<div class="sk" style="height:150px;border-radius:22px;margin-bottom:14px"></div>
    <div class="sk" style="height:96px;border-radius:22px;margin-bottom:14px"></div>
    <div class="sk" style="height:200px;border-radius:22px"></div>`;
}
function arteVazia() {
  return `<svg class="art" viewBox="0 0 120 90" fill="none" stroke="currentColor" stroke-width="2" opacity=".55" aria-hidden="true">
    <rect x="8" y="10" width="104" height="70" rx="8"/><path d="M60 10v70"/><circle cx="60" cy="45" r="13"/>
    <path d="M8 30h16v30H8M112 30H96v30h16"/><circle cx="60" cy="45" r="2.5" fill="currentColor"/></svg>`;
}

// ---------- formatação ----------
const DIAS = ['domingo', 'segunda', 'terça', 'quarta', 'quinta', 'sexta', 'sábado'];
const MESES = ['janeiro', 'fevereiro', 'março', 'abril', 'maio', 'junho', 'julho', 'agosto', 'setembro', 'outubro', 'novembro', 'dezembro'];
const capital = (s) => s.charAt(0).toUpperCase() + s.slice(1);

function parseData(iso) { const [a, m, d] = iso.split('-').map(Number); return new Date(a, m - 1, d); }
function fmtData(iso) { const d = parseData(iso); return `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}`; }
function fmtDataLonga(iso) { const d = parseData(iso); return `${capital(DIAS[d.getDay()])}, ${d.getDate()} de ${MESES[d.getMonth()]}`; }
function fmtHora(h) { return h ? h.substring(0, 5) : ''; }
function fmtMs(ms) { const s = Math.max(0, Math.ceil(ms / 1000)); return `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`; }
function fmtMinuto(ms) { return `${Math.floor(ms / 60000) + 1}'`; }
function hojeISO() { const d = new Date(); return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`; }
function nota1(n) { return n == null ? '—' : Number(n).toFixed(1).replace('.', ','); }
function diasAte(iso) { return Math.round((parseData(iso) - parseData(hojeISO())) / 86400000); }
function contagem(iso) {
  const d = diasAte(iso);
  return d === 0 ? 'É hoje!' : d === 1 ? 'Amanhã' : d > 1 ? `Em ${d} dias` : 'Data passada';
}

// ---------- grupo atual: tudo (jogadores, peladas, rankings) é escopado por grupo ----------
let GID = null;
try { GID = Number(localStorage.getItem('overall_gid')) || null; } catch (e) { /* armazenamento bloqueado */ }

// ---------- jogadores (cache) ----------
let JOGADORES = new Map();
async function carregarJogadores() {
  const lista = await GET(`/jogadores?grupoId=${GID}`);
  JOGADORES = new Map(lista.map(j => [j.id, j]));
  return lista;
}
function J(id) {
  return JOGADORES.get(id) || { id, nome: 'Jogador #' + id, foto: null, goleiro: false, overall: 60, ativo: false,
    estatisticas: { partidas: 0, vitorias: 0, empates: 0, derrotas: 0, gols: 0, assistencias: 0, jogosComoGoleiro: 0, semSofrer: 0, golsSofridos: 0, notaMedia: 0 } };
}
function iniciais(nome) {
  const p = String(nome).trim().split(/\s+/);
  return ((p[0] || '?')[0] + (p.length > 1 ? p[p.length - 1][0] : '')).toUpperCase();
}

// ---------- níveis de carta por overall (similaridade: mesma cor = mesmo patamar) ----------
function tier(n) { return n >= 80 ? 'elite' : n >= 65 ? 'gold' : n >= 55 ? 'silver' : 'bronze'; }
const TIER_NOME = { bronze: 'Bronze', silver: 'Prata', gold: 'Ouro', elite: 'Elite' };
function tierCor(n) { return { bronze: '#D8956A', silver: '#C4D0DB', gold: '#F3CD70', elite: '#7BF0D8' }[tier(n)]; }

function avatar(j, tam) {
  const t = j.overall != null ? tier(j.overall) : '';
  const cls = 'avatar' + (tam ? ' ' + tam : '');
  return j.foto
    ? `<span class="${cls}" data-tier="${t}"><img src="${esc(j.foto)}" alt=""></span>`
    : `<span class="${cls}" data-tier="${t}">${esc(iniciais(j.nome))}</span>`;
}
function ovr(n, grande) { return `<span class="ovr${grande ? ' big' : ''}" data-tier="${tier(n)}">${n}</span>`; }

/** Seis atributos (0–99) derivados do histórico — alimentam o radar. */
function atributos(j) {
  const e = j.estatisticas;
  if (!e || !e.partidas) return null;
  const p = e.partidas, cl = (v) => Math.max(0, Math.min(99, v * 99));
  const aproveitamento = (3 * e.vitorias + e.empates) / (3 * p);
  return [
    ['FIN', cl(e.gols / p / 0.8)],
    ['CRI', cl(e.assistencias / p / 0.5)],
    ['VIT', cl(aproveitamento)],
    ['NOT', cl((e.notaMedia - 3) / 7)],
    j.goleiro ? ['DEF', cl(e.semSofrer / Math.max(1, e.jogosComoGoleiro) / 0.7)] : ['RES', cl(1 - e.derrotas / p)],
    ['EXP', cl(p / 20)]
  ];
}
function perfil(j) {
  const e = j.estatisticas;
  if (j.goleiro) return 'GOL';
  if (!e || !e.partidas) return 'NOVO';
  return e.gols >= e.assistencias ? 'ATA' : 'MEI';
}

/** Carta do jogador: tilt 3D com reflexo, borda e brilho por nível. */
function cartaFut(j, o = {}) {
  const e = j.estatisticas || { partidas: 0, gols: 0, assistencias: 0, semSofrer: 0, vitorias: 0 };
  const t = tier(j.overall);
  const stats = j.goleiro
    ? [['JOG', e.partidas], ['SSG', e.semSofrer], ['VIT', e.vitorias]]
    : [['JOG', e.partidas], ['GOL', e.gols], ['AST', e.assistencias]];
  return `<div class="fut${o.big ? ' big' : ''}${o.mvp ? ' mvp' : ''}${o.on ? ' on' : ''}" data-tier="${t}" ${o.tilt === false ? '' : 'data-tilt'}
      ${o.onclick ? `role="button" tabindex="0" onclick="${o.onclick}"` : ''} style="${o.estilo || ''}">
    ${t === 'elite' ? '<span class="holo"></span>' : ''}
    <span class="fut-check">${ic('check', 15)}</span>
    <div class="fut-top">
      <div><div class="fut-ovr">${j.overall}</div><div class="fut-role">${perfil(j)}</div></div>
      <span class="fut-badge">${j.goleiro ? ic('glove', 20) : ic('ball', 20)}</span>
    </div>
    <div class="fut-photo">${avatar({ ...j, overall: null }, '')}</div>
    <div class="fut-name">${esc(j.nome)}</div>
    <div class="fut-stats">${stats.map(([r, v]) => `<div><b>${v}</b><span>${r}</span></div>`).join('')}</div>
  </div>`;
}

// ---------- times ----------
const CORES = ['#FF5A5F', '#4C8DFF', '#FFB020', '#2FD07A', '#A66BFF', '#FF5FA8', '#22D3C5', '#FF8A3D'];
const NOMES_COR = ['Vermelho', 'Azul', 'Amarelo', 'Verde', 'Roxo', 'Rosa', 'Turquesa', 'Laranja'];
const corDoTime = (n) => CORES[(n - 1) % CORES.length];
const nomeDoCorTime = (n) => NOMES_COR[(n - 1) % NOMES_COR.length];
const nomeDoTime = (n) => `Time ${n}`;

function clarear(hex, f) {
  const n = parseInt(hex.slice(1), 16), r = n >> 16, g = (n >> 8) & 255, b = n & 255;
  const m = (c) => Math.round(c + (255 - c) * f);
  return `rgb(${m(r)},${m(g)},${m(b)})`;
}

/** Escudo do time: cor própria com degradê, brilho de vidro e o número dentro. */
function escudo(numero, altura, brilho) {
  const h = altura || 44, w = Math.round(h * 0.875), c = corDoTime(numero);
  return `<svg class="shield${brilho ? ' shield-fx' : ''}" style="--team:${c}" viewBox="0 0 84 96" width="${w}" height="${h}" aria-label="${nomeDoTime(numero)}">
    <defs><linearGradient id="sg${numero}" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${clarear(c, .32)}"/><stop offset=".55" stop-color="${c}"/><stop offset="1" stop-color="${c}" stop-opacity=".82"/></linearGradient></defs>
    <path d="M42 3 L78 14 V46 C78 68 62 85 42 93 C22 85 6 68 6 46 V14 Z" fill="url(#sg${numero})" stroke="rgba(255,255,255,.9)" stroke-width="3"/>
    <path d="M42 8 L73 17.5 V30 C58 23 26 23 11 30 V17.5 Z" fill="rgba(255,255,255,.2)"/>
    <text x="42" y="63" text-anchor="middle" font-family="Sora, sans-serif" font-weight="800" font-size="44" fill="#fff"
      stroke="rgba(0,0,0,.35)" stroke-width="3" paint-order="stroke">${numero}</text></svg>`;
}

const STATUS_PELADA = { ABERTA: 'Lista aberta', SORTEADA: 'Times sorteados', EM_ANDAMENTO: 'Em andamento', FINALIZADA: 'Finalizada' };

// ---------- caixa morfológica (matriz de parâmetros × opções) ----------
// Cada linha é um parâmetro; cada coluna, uma opção. A configuração é uma célula por linha, e o rodapé mostra
// a combinação escolhida e quantas combinações existem no "espaço de projeto".
const CX = {};
function caixa(id, def) { CX[id] = def; return caixaHtml(id); }
function cxEstado(id) { return Object.fromEntries(CX[id].linhas.map(l => [l.k, l.v])); }
function cxAtiva(id, l) { return !l.ativa || l.ativa(cxEstado(id)); }
function caixaHtml(id) {
  const d = CX[id], est = cxEstado(id);
  const linhas = d.linhas.map(l => {
    const off = !cxAtiva(id, l), noPadrao = l.opcoes.some(o => o.v === l.v);
    const opts = l.opcoes.map((o, i) =>
      `<button type="button" class="cx-opt" aria-pressed="${l.v === o.v}" onclick="cxEscolher('${id}','${l.k}',${i})">${o.r}${o.sub ? `<small>${o.sub}</small>` : ''}</button>`).join('');
    const custom = l.custom
      ? `<input class="cx-custom${noPadrao ? '' : ' on'}" type="number" inputmode="numeric" min="${l.custom.min}" max="${l.custom.max}" placeholder="outro"
           value="${noPadrao ? '' : l.v}" onchange="cxCustom('${id}','${l.k}',this.value)" aria-label="Outro valor para ${esc(l.rotulo)}">` : '';
    return `<div class="cx-row${off ? ' off' : ''}"><div class="cx-lbl">${l.rotulo}${l.dica ? `<small>${l.dica}</small>` : ''}</div><div class="cx-opts">${opts}${custom}</div></div>`;
  }).join('');
  const ativos = d.linhas.filter(l => cxAtiva(id, l));
  const combos = ativos.reduce((a, l) => a * (l.opcoes.length + (l.custom ? 1 : 0)), 1);
  const resumo = d.resumo ? d.resumo(est)
    : ativos.map(l => `<span class="chip">${esc(l.fmt ? l.fmt(l.v) : l.v)}</span>`).join('');
  return `<div class="cx" id="cx-${id}">${linhas}
    <div class="cx-foot"><div class="cx-combo">${resumo}</div><div class="cx-count">1 de <b>${combos}</b> combinações</div></div></div>`;
}
function cxAtualizar(id) {
  const el = document.getElementById('cx-' + id);
  if (el) el.outerHTML = caixaHtml(id);
  vibrar(8);
  if (CX[id].aoMudar) CX[id].aoMudar(cxEstado(id));
}
function cxEscolher(id, k, i) { const l = CX[id].linhas.find(x => x.k === k); l.v = l.opcoes[i].v; cxAtualizar(id); }
function cxCustom(id, k, val) {
  const l = CX[id].linhas.find(x => x.k === k), n = parseInt(val, 10);
  if (!isNaN(n) && n >= l.custom.min && n <= l.custom.max) l.v = n;
  cxAtualizar(id);
}
