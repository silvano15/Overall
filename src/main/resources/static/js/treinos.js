async function carregarTreinos() {
  const el = document.getElementById('tabela-treinos');
  el.innerHTML = '<div class="loading"><span class="spinner"></span>Carregando...</div>';
  try {
    const lista = await req('GET','/treinos');
    if (!lista.length) { el.innerHTML = '<div class="empty-state"><div class="empty-icon">⚽</div><p>Nenhum treino registrado nesta semana</p></div>'; return; }
    el.innerHTML = `<table>
      <thead><tr><th>Jogador</th><th>Data</th><th>Acertos</th><th>Erros</th><th>Aproveit.</th><th>Ações</th></tr></thead>
      <tbody>${lista.map(t => {
        const tot = t.acertos+t.erros; const pct = tot ? Math.round(t.acertos/tot*100) : 0;
        return `<tr>
          <td><strong>${t.jogadorNome}</strong></td>
          <td>${fmt(t.dataTreino)}</td>
          <td><span class="badge badge-green">${t.acertos}</span></td>
          <td><span class="badge badge-red">${t.erros}</span></td>
          <td><span class="badge ${pct>=70?'badge-green':pct>=50?'badge-yellow':'badge-red'}">${pct}%</span></td>
          <td style="display:flex;gap:6px">
            <button class="btn btn-icon btn-sm" onclick="editarTreino(${t.id})">✏️</button>
            <button class="btn btn-icon btn-sm" onclick="removerTreino(${t.id})">🗑️</button>
          </td>
        </tr>`;
      }).join('')}</tbody>
    </table>`;
  } catch(e) { el.innerHTML = `<div class="empty-state"><p style="color:var(--red)">${e.message}</p></div>`; }
}

let treinosList = [];
async function abrirModalTreino(t={}) {
  document.getElementById('modal-treino-title').textContent = t.id ? 'Editar Treino' : 'Novo Treino';
  document.getElementById('treino-id').value = t.id||'';
  document.getElementById('treino-data').value = t.dataTreino||new Date().toISOString().split('T')[0];
  document.getElementById('treino-acertos').value = t.acertos||'';
  document.getElementById('treino-erros').value = t.erros||'';
  const sel = document.getElementById('treino-jogador');
  sel.innerHTML = '<option value="">Selecione...</option>';
  if (!jogadoresList.length) await carregarJogadoresCache();
  jogadoresList.forEach(j => sel.innerHTML += `<option value="${j.id}" ${t.jogadorId==j.id?'selected':''}>${j.nome}</option>`);
  abrirModal('modal-treino');
}

async function editarTreino(id) {
  if (!treinosList.length) treinosList = await req('GET','/treinos').catch(()=>[]);
  const t = treinosList.find(x=>x.id===id); if(t) abrirModalTreino(t);
}

async function salvarTreino() {
  const id = document.getElementById('treino-id').value;
  const body = { jogadorId: parseInt(document.getElementById('treino-jogador').value),
    dataTreino: document.getElementById('treino-data').value,
    acertos: parseInt(document.getElementById('treino-acertos').value)||0,
    erros: parseInt(document.getElementById('treino-erros').value)||0 };
  try {
    if (id) await req('PUT',`/treinos/${id}`, body);
    else await req('POST','/treinos', body);
    fecharModal('modal-treino'); toast(id?'Treino atualizado!':'Treino cadastrado!');
    carregarTreinos(); carregarDashboard();
  } catch(e) { toast(e.message,'error'); }
}

function removerTreino(id) {
  confirmar('Remover este treino?', async () => {
    try { await req('DELETE',`/treinos/${id}`); toast('Treino removido!'); carregarTreinos(); }
    catch(e) { toast(e.message,'error'); }
  });
}