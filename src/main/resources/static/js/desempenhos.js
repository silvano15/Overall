async function carregarDesempenhos() {
  const el = document.getElementById('tabela-desempenhos');
  el.innerHTML = '<div class="loading"><span class="spinner"></span>Carregando...</div>';
  try {
    const lista = await req('GET','/desempenhos');
    if (!lista.length) { el.innerHTML = '<div class="empty-state"><div class="empty-icon">📈</div><p>Nenhum desempenho registrado</p></div>'; return; }
    el.innerHTML = `<table>
      <thead><tr><th>Jogador</th><th>Partida</th><th>Nota</th><th>Gols</th><th>Assist.</th><th>Cartões</th><th>Ações</th></tr></thead>
      <tbody>${lista.map(d => `<tr>
        <td><strong>${d.jogadorNome}</strong></td>
        <td>${d.adversario} <span style="color:var(--text3);font-size:11px">${fmt(d.dataJogo)}</span></td>
        <td><span class="badge ${d.nota>=7?'badge-green':d.nota>=5?'badge-yellow':'badge-red'}">${d.nota}</span></td>
        <td>${d.gols}</td>
        <td>${d.assistencias}</td>
        <td><span style="color:var(--yellow)">${d.cartoesAmarelos}🟨</span> <span style="color:var(--red)">${d.cartoesVermelhos}🟥</span></td>
        <td style="display:flex;gap:6px">
          <button class="btn btn-icon btn-sm" onclick="editarDesemp(${d.id})">✏️</button>
          <button class="btn btn-icon btn-sm" onclick="removerDesemp(${d.id})">🗑️</button>
        </td>
      </tr>`).join('')}</tbody>
    </table>`;
  } catch(e) { el.innerHTML = `<div class="empty-state"><p style="color:var(--red)">${e.message}</p></div>`; }
}

let desempList = [];
async function abrirModalDesempenho(d={}) {
  document.getElementById('modal-desemp-title').textContent = d.id ? 'Editar Desempenho' : 'Novo Desempenho';
  document.getElementById('desemp-id').value = d.id||'';
  document.getElementById('desemp-nota').value = d.nota||'';
  document.getElementById('desemp-gols').value = d.gols||0;
  document.getElementById('desemp-assist').value = d.assistencias||0;
  document.getElementById('desemp-amarelos').value = d.cartoesAmarelos||0;
  document.getElementById('desemp-vermelhos').value = d.cartoesVermelhos||0;
  
  if (!jogadoresList.length) await carregarJogadoresCache();
  if (!partidasList.length) partidasList = await req('GET','/partidas').catch(()=>[]);
  
  const sj = document.getElementById('desemp-jogador');
  sj.innerHTML = '<option value="">Selecione...</option>';
  jogadoresList.forEach(j => sj.innerHTML += `<option value="${j.id}" ${d.jogadorId==j.id?'selected':''}>${j.nome}</option>`);
  
  const sp = document.getElementById('desemp-partida');
  sp.innerHTML = '<option value="">Selecione...</option>';
  partidasList.forEach(p => sp.innerHTML += `<option value="${p.id}" ${d.partidaId==p.id?'selected':''}>${p.adversario} (${fmt(p.dataJogo)})</option>`);
  
  abrirModal('modal-desempenho');
}

async function editarDesemp(id) {
  if (!desempList.length) desempList = await req('GET','/desempenhos').catch(()=>[]);
  const d = desempList.find(x=>x.id===id); if(d) abrirModalDesempenho(d);
}

async function salvarDesempenho() {
  const id = document.getElementById('desemp-id').value;
  const body = { partidaId: parseInt(document.getElementById('desemp-partida').value),
    jogadorId: parseInt(document.getElementById('desemp-jogador').value),
    nota: parseFloat(document.getElementById('desemp-nota').value),
    gols: parseInt(document.getElementById('desemp-gols').value)||0,
    assistencias: parseInt(document.getElementById('desemp-assist').value)||0,
    cartoesAmarelos: parseInt(document.getElementById('desemp-amarelos').value)||0,
    cartoesVermelhos: parseInt(document.getElementById('desemp-vermelhos').value)||0 };
  try {
    if (id) await req('PUT',`/desempenhos/${id}`, body);
    else await req('POST','/desempenhos', body);
    fecharModal('modal-desempenho'); toast(id?'Desempenho atualizado!':'Desempenho cadastrado!');
    carregarDesempenhos(); carregarDashboard();
  } catch(e) { toast(e.message,'error'); }
}

function removerDesemp(id) {
  confirmar('Remover este desempenho?', async () => {
    try { await req('DELETE',`/desempenhos/${id}`); toast('Desempenho removido!'); carregarDesempenhos(); }
    catch(e) { toast(e.message,'error'); }
  });
}