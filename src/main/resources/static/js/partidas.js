async function carregarPartidas() {
  const el = document.getElementById('tabela-partidas');
  el.innerHTML = '<div class="loading"><span class="spinner"></span>Carregando...</div>';
  try {
    const lista = await req('GET','/partidas');
    partidasList = lista;
    if (!lista.length) { el.innerHTML = '<div class="empty-state"><div class="empty-icon">🏟️</div><p>Nenhuma partida cadastrada</p></div>'; return; }
    el.innerHTML = `<table>
      <thead><tr><th>Adversário</th><th>Data</th><th>Local</th><th>Placar</th><th>Ações</th></tr></thead>
      <tbody>${lista.map(p => {
        const placar = (p.golsPro!=null&&p.golsContra!=null) ? `${p.golsPro} × ${p.golsContra}` : '—';
        const badge = p.golsPro>p.golsContra?'badge-green':p.golsPro<p.golsContra?'badge-red':'badge-yellow';
        return `<tr>
          <td><strong>${p.adversario}</strong></td>
          <td>${fmt(p.dataJogo)}</td>
          <td>${p.local||'—'}</td>
          <td>${placar!=='—'?`<span class="badge ${badge}">${placar}</span>`:'—'}</td>
          <td style="display:flex;gap:6px">
            <button class="btn btn-icon btn-sm" onclick="editarPartida(${p.id})">✏️</button>
            <button class="btn btn-icon btn-sm" onclick="removerPartida(${p.id},'${p.adversario}')">🗑️</button>
          </td>
        </tr>`;
      }).join('')}</tbody>
    </table>`;
  } catch(e) { el.innerHTML = `<div class="empty-state"><p style="color:var(--red)">${e.message}</p></div>`; }
}

async function abrirModalPartida(p={}) {
  document.getElementById('modal-partida-title').textContent = p.id ? 'Editar Partida' : 'Nova Partida';
  document.getElementById('partida-id').value = p.id||'';
  document.getElementById('partida-adversario').value = p.adversario||'';
  document.getElementById('partida-data').value = p.dataJogo||'';
  document.getElementById('partida-local').value = p.local||'';
  document.getElementById('partida-golspro').value = p.golsPro!=null?p.golsPro:'';
  document.getElementById('partida-golscontra').value = p.golsContra!=null?p.golsContra:'';
  abrirModal('modal-partida');
}

async function editarPartida(id) {
  const p = partidasList.find(x=>x.id===id); if(p) abrirModalPartida(p);
}

async function salvarPartida() {
  const id = document.getElementById('partida-id').value;
  const body = { adversario: document.getElementById('partida-adversario').value,
    dataJogo: document.getElementById('partida-data').value,
    local: document.getElementById('partida-local').value||null,
    golsPro: document.getElementById('partida-golspro').value!==''?parseInt(document.getElementById('partida-golspro').value):null,
    golsContra: document.getElementById('partida-golscontra').value!==''?parseInt(document.getElementById('partida-golscontra').value):null };
  try {
    if (id) await req('PUT',`/partidas/${id}`, body);
    else await req('POST','/partidas', body);
    fecharModal('modal-partida'); toast(id?'Partida atualizada!':'Partida cadastrada!');
    carregarPartidas(); carregarDashboard();
  } catch(e) { toast(e.message,'error'); }
}

function removerPartida(id, adv) {
  confirmar(`Remover a partida contra "${adv}"?`, async () => {
    try { await req('DELETE',`/partidas/${id}`); toast('Partida removida!'); carregarPartidas(); carregarDashboard(); }
    catch(e) { toast(e.message,'error'); }
  });
}