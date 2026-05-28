const API = 'http://localhost:8080/api';
let token = null;
let perfil = null;
let jogadoresList = [];
let partidasList = [];

function toast(msg, type='success') {
  const t = document.getElementById('toast');
  t.textContent = msg; t.className = type; t.style.display = 'block';
  setTimeout(() => t.style.display = 'none', 3500);
}

function fecharModal(id) { document.getElementById(id).classList.remove('open'); }
function abrirModal(id) { document.getElementById(id).classList.add('open'); }

function confirmar(msg, fn, btnText = 'Remover', btnClass = 'btn-danger') {
  document.getElementById('confirm-msg').textContent = msg;
  const btn = document.getElementById('confirm-ok-btn');
  btn.textContent = btnText;
  btn.className = `btn ${btnClass}`;
  btn.onclick = () => { fecharModal('modal-confirm'); fn(); };
  abrirModal('modal-confirm');
}

function fmt(d) { return d ? d.split('T')[0] : '—'; }
function fmtPos(p) {
  const m = { GOLEIRO:'Goleiro', LATERAL_DIREITO:'Lat. Dir.', LATERAL_ESQUERDO:'Lat. Esq.',
    ZAGUEIRO:'Zagueiro', VOLANTE:'Volante', MEIO_CAMPO:'Meia', MEIA_ATACANTE:'Meia Atac.',
    PONTA_DIREITA:'Ponta Dir.', PONTA_ESQUERDA:'Ponta Esq.', ATACANTE:'Atacante' };
  return m[p] || p;
}

async function req(method, path, body) {
  const opts = { method, headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` } };
  if (body) opts.body = JSON.stringify(body);
  const res = await fetch(API + path, opts);
  const data = res.status !== 204 ? await res.json().catch(() => ({})) : {};
  if (!res.ok) throw new Error(data.mensagem || `Erro ${res.status}`);
  return data;
}

const views = { dashboard:'Dashboard', jogadores:'Jogadores', treinos:'Treinos',
  comportamentos:'Comportamentos', partidas:'Partidas', desempenhos:'Desempenhos', timeideal:'Time Ideal' };
const subs = { dashboard:'Visão geral do sistema', jogadores:'Gestão do elenco (RF-002 a 005)',
  treinos:'Estatísticas de acertos e erros (RF-006 a 009)', comportamentos:'Notas comportamentais (RF-010 a 013)',
  partidas:'Calendário de jogos (RF-014 a 017)', desempenhos:'Scout dos atletas (RF-018 a 021)',
  timeideal:'Escalação automática semanal (RF-022)' };

function navigate(id) {
  document.querySelectorAll('.view').forEach(v => v.classList.remove('active'));
  document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
  document.getElementById(`view-${id}`).classList.add('active');
  document.querySelector(`[onclick="navigate('${id}')"]`)?.classList.add('active');
  document.getElementById('topbar-title').textContent = views[id];
  document.getElementById('topbar-sub').textContent = subs[id];

  const loaders = {
    jogadores: typeof carregarJogadores === 'function' ? carregarJogadores : null,
    treinos: typeof carregarTreinos === 'function' ? carregarTreinos : null,
    comportamentos: typeof carregarComportamentos === 'function' ? carregarComportamentos : null,
    partidas: typeof carregarPartidas === 'function' ? carregarPartidas : null,
    desempenhos: typeof carregarDesempenhos === 'function' ? carregarDesempenhos : null
  };

  if (loaders[id]) loaders[id]();
}

// ─── JOGADORES (RF-002 a RF-005) ───

async function carregarJogadores() {
  const el = document.getElementById('tabela-jogadores');
  if (el) el.innerHTML = '<div class="loading"><span class="spinner"></span>Carregando...</div>';
  try {
    const lista = await req('GET', '/jogadores');
    jogadoresList = lista;
    if (!lista.length) {
      el.innerHTML = '<div class="empty-state"><div class="empty-icon">🏃</div><p>Nenhum jogador cadastrado</p></div>';
      return;
    }
    el.innerHTML = `<table>
      <thead><tr><th>Nome</th><th>Documento</th><th>Posição</th><th>Altura</th><th>Peso</th><th>Status</th><th>Ações</th></tr></thead>
      <tbody>${lista.map(j => `<tr>
        <td><strong>${j.nome}</strong></td>
        <td style="font-family:'JetBrains Mono',monospace;font-size:12px">${j.documento}</td>
        <td><span class="badge badge-blue">${fmtPos(j.posicao)}</span></td>
        <td>${j.alturaCm ? j.alturaCm + 'cm' : '—'}</td>
        <td>${j.pesoKg ? j.pesoKg + 'kg' : '—'}</td>
        <td><span class="badge ${j.ativo ? 'badge-green' : 'badge-red'}">${j.ativo ? 'Ativo' : 'Inativo'}</span></td>
        <td style="display:flex;gap:6px">
          <button class="btn btn-icon btn-sm" onclick="editarJogador(${j.id})">✏️</button>
          <button class="btn btn-icon btn-sm" onclick="removerJogador(${j.id},'${j.nome.replace(/'/g, "\\'")}')">🗑️</button>
        </td>
      </tr>`).join('')}</tbody>
    </table>`;
  } catch (e) {
    el.innerHTML = `<div class="empty-state"><p style="color:var(--red)">${e.message}</p></div>`;
  }
}

function abrirModalJogador(j = {}) {
  document.getElementById('modal-jogador-title').textContent = j.id ? 'Editar Jogador' : 'Novo Jogador';
  document.getElementById('jogador-id').value = j.id || '';
  document.getElementById('jogador-nome').value = j.nome || '';
  document.getElementById('jogador-documento').value = j.documento || '';
  document.getElementById('jogador-posicao').value = j.posicao || '';
  document.getElementById('jogador-altura').value = j.alturaCm || '';
  document.getElementById('jogador-peso').value = j.pesoKg || '';
  document.getElementById('jogador-nascimento').value = j.dataNascimento || '';
  abrirModal('modal-jogador');
}

function editarJogador(id) {
  const j = jogadoresList.find(x => x.id === id);
  if (j) abrirModalJogador(j);
}

async function salvarJogador() {
  const id = document.getElementById('jogador-id').value;
  const body = {
    nome: document.getElementById('jogador-nome').value,
    documento: document.getElementById('jogador-documento').value,
    posicao: document.getElementById('jogador-posicao').value,
    alturaCm: parseInt(document.getElementById('jogador-altura').value) || null,
    pesoKg: parseFloat(document.getElementById('jogador-peso').value) || null,
    dataNascimento: document.getElementById('jogador-nascimento').value || null,
  };
  try {
    if (id) await req('PUT', `/jogadores/${id}`, body);
    else await req('POST', '/jogadores', body);
    fecharModal('modal-jogador');
    toast(id ? 'Jogador atualizado!' : 'Jogador cadastrado!');
    carregarJogadores();
    carregarDashboard();
  } catch (e) { toast(e.message, 'error'); }
}

function removerJogador(id, nome) {
  confirmar(`Remover o jogador "${nome}"?`, async () => {
    try {
      await req('DELETE', `/jogadores/${id}`);
      toast('Jogador removido!');
      carregarJogadores();
      carregarDashboard();
    } catch (e) { toast(e.message, 'error'); }
  });
}

async function carregarJogadoresCache() {
  try { jogadoresList = await req('GET', '/jogadores'); } catch (e) {}
}