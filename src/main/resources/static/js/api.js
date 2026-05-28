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

// CORREÇÃO SU004: Função de confirmar agora aceita botões customizados (Ex: Inativar)
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
  
  // Roteador de carga (os arquivos JS carregarão as suas devidas funções)
  const loaders = { jogadores: carregarJogadores, treinos: typeof carregarTreinos === 'function' ? carregarTreinos : null,
    comportamentos: typeof carregarComportamentos === 'function' ? carregarComportamentos : null, 
    partidas: typeof carregarPartidas === 'function' ? carregarPartidas : null, 
    desempenhos: typeof carregarDesempenhos === 'function' ? carregarDesempenhos : null };
    
  if (loaders[id]) loaders[id]();
}