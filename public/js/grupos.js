// ---------- Biblioteca de grupos: cada grupo tem seu elenco, suas peladas e seu ranking ----------

const GR = { lista: [], editandoId: null };

async function telaGrupos() {
  GR.lista = await GET('/grupos');
  desenharGrupos();
}

function desenharGrupos() {
  pintar(`
    <div style="margin:2px 2px 14px"><div class="eyebrow">Biblioteca</div><h1 class="h1 mt-2">Meus grupos</h1>
      <div class="small muted mt-2">Cada grupo tem seus jogadores, suas peladas e seu ranking.</div></div>
    ${GR.lista.length ? `<div class="card">${GR.lista.map(g => `
      <div class="p-row">
        <a class="row grow" href="#/grupos/${g.id}" style="text-decoration:none;gap:12px" onclick="GID=${g.id}">
          <span class="chip ${g.peladaAberta ? '' : 'gray'}">${g.peladaAberta ? '<span class="dot"></span>' : ic('users', 14)}</span>
          <span class="p-name">${esc(g.nome)}<span class="p-sub">${g.jogadores} jogador${g.jogadores === 1 ? '' : 'es'} · ${g.peladas} pelada${g.peladas === 1 ? '' : 's'}${g.peladaAberta ? ' · pelada em aberto' : ''}</span></span>
          <span class="muted">${ic('chevR')}</span></a>
        <button class="btn icon sm ghost" aria-label="Editar grupo" onclick="abrirFormGrupo(${g.id})">${ic('edit', 16)}</button>
      </div>`).join('')}</div>`
      : `<div class="card"><div class="empty">${arteVazia()}Nenhum grupo ainda.<br>Crie o primeiro para começar.</div></div>`}
    <button class="btn primary block lg" onclick="abrirFormGrupo()">${ic('plus')} Criar grupo de pelada</button>`);
}

function abrirFormGrupo(id) {
  const g = id ? GR.lista.find(x => x.id === id) : null;
  GR.editandoId = id || null;
  abrirSheet(g ? 'Editar grupo' : 'Novo grupo', `
    <label class="field"><span>Nome do grupo</span><input id="gr-nome" maxlength="80" placeholder="Ex.: Pelada de sábado" value="${esc(g ? g.nome : '')}"></label>
    <button class="btn primary block lg" onclick="salvarGrupo()">Salvar</button>
    ${g ? `<button class="btn danger block mt-3" onclick="excluirGrupo()">${ic('trash')} Excluir grupo</button>` : ''}`);
  setTimeout(() => { const n = document.getElementById('gr-nome'); if (n && !id) n.focus(); }, 60);
}

async function salvarGrupo() {
  const nome = document.getElementById('gr-nome').value.trim();
  if (!nome) return toast('Informe o nome do grupo.', 'error');
  try {
    if (GR.editandoId) {
      await PUT(`/grupos/${GR.editandoId}`, { nome });
      fecharSheet(); toast('Grupo atualizado!'); await telaGrupos();
    } else {
      const g = await POST('/grupos', { nome });
      fecharSheet(); toast('Grupo criado! Cadastre os jogadores.');
      location.hash = `#/grupos/${g.id}/jogadores`;
    }
  } catch (e) { erroToast(e); }
}

function excluirGrupo() {
  const id = GR.editandoId;
  confirmar('Excluir grupo', 'Só dá para excluir grupos sem jogadores cadastrados.', 'Excluir', async () => {
    try { await DEL(`/grupos/${id}`); toast('Grupo excluído.'); await telaGrupos(); } catch (e) { erroToast(e); }
  });
}
