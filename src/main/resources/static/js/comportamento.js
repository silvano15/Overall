async function carregarComportamentos() {
  const el = document.getElementById('tabela-comportamentos');
  el.innerHTML = '<div class="loading"><span class="spinner"></span>Carregando...</div>';
  try {
    const lista = await req('GET','/comportamentos');
    if (!lista.length) { el.innerHTML = '<div class="empty-state"><div class="empty-icon">🧠</div><p>Nenhum comportamento registrado</p></div>'; return; }
    el.innerHTML = `<table>
      <thead><tr><th>Jogador</th><th>Data</th><th>Foco</th><th>Disciplina</th><th>Atitude</th><th>Média</th><th>Ações</th></tr></thead>
      <tbody>${lista.map(c => {
        const med = ((c.notaFoco+c.notaDisciplina+c.notaAtitude)/3).toFixed(1);
        return `<tr>
          <td><strong>${c.jogadorNome}</strong></td>
          <td>${fmt(c.dataAvaliacao)}</td>
          <td><span class="badge badge-blue">${c.notaFoco}</span></td>
          <td><span class="badge badge-blue">${c.notaDisciplina}</span></td>
          <td><span class="badge badge-blue">${c.notaAtitude}</span></td>
          <td><span class="badge ${med>=7?'badge-green':med>=5?'badge-yellow':'badge-red'}">${med}</span></td>
          <td style="display:flex;gap:6px">
            <button class="btn btn-icon btn-sm" onclick="editarComp(${c.id})">✏️</button>
            <button class="btn btn-icon btn-sm" onclick="removerComp(${c.id})">🗑️</button>
          </td>
        </tr>`;
      }).join('')}</tbody>
    </table>`;
  } catch(e) { el.innerHTML = `<div class="empty-state"><p style="color:var(--red)">${e.message}</p></div>`; }
}

let compsList = [];
async function abrirModalComportamento(c={}) {
  document.getElementById('modal-comp-title').textContent = c.id ? 'Editar Comportamento' : 'Nova Avaliação';
  document.getElementById('comp-id').value = c.id||'';
  document.getElementById('comp-data').value = c.dataAvaliacao||new Date().toISOString().split('T')[0];
  document.getElementById('comp-foco').value = c.notaFoco!=null ? c.notaFoco : '';
  document.getElementById('comp-disciplina').value = c.notaDisciplina!=null ? c.notaDisciplina : '';
  document.getElementById('comp-atitude').value = c.notaAtitude!=null ? c.notaAtitude : '';
  const sel = document.getElementById('comp-jogador');
  sel.innerHTML = '<option value="">Selecione...</option>';
  if (!jogadoresList.length) await carregarJogadoresCache();
  jogadoresList.forEach(j => sel.innerHTML += `<option value="${j.id}" ${c.jogadorId==j.id?'selected':''}>${j.nome}</option>`);
  abrirModal('modal-comportamento');
}

async function editarComp(id) {
  if (!compsList.length) compsList = await req('GET','/comportamentos').catch(()=>[]);
  const c = compsList.find(x=>x.id===id); if(c) abrirModalComportamento(c);
}

async function salvarComportamento() {
  const id = document.getElementById('comp-id').value;
  const body = { jogadorId: parseInt(document.getElementById('comp-jogador').value),
    dataAvaliacao: document.getElementById('comp-data').value,
    notaFoco: parseInt(document.getElementById('comp-foco').value),
    notaDisciplina: parseInt(document.getElementById('comp-disciplina').value),
    notaAtitude: parseInt(document.getElementById('comp-atitude').value) };
  try {
    if (id) await req('PUT',`/comportamentos/${id}`, body);
    else await req('POST','/comportamentos', body);
    fecharModal('modal-comportamento'); toast(id?'Avaliação atualizada!':'Avaliação cadastrada!');
    carregarComportamentos();
  } catch(e) { toast(e.message,'error'); }
}

function removerComp(id) {
  confirmar('Remover esta avaliação comportamental?', async () => {
    try { await req('DELETE',`/comportamentos/${id}`); toast('Avaliação removida!'); carregarComportamentos(); }
    catch(e) { toast(e.message,'error'); }
  });
}