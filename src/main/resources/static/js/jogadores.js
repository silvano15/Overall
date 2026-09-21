// ---------- Aba Jogadores: cartas com tilt 3D, radar de atributos e lista da semana ----------

const JG = { lista: [], pelada: null, busca: '', filtro: 'todos', foto: undefined, editandoId: null };

async function telaJogadores(view) {
  const [lista, pelada] = await Promise.all([carregarJogadores(), GET('/peladas/atual')]);
  JG.lista = lista; JG.pelada = pelada;
  desenharJogadores();
}

function desenharJogadores() {
  const arquivados = JG.lista.filter(j => !j.ativo);
  const pel = JG.pelada, ativos = JG.lista.filter(j => j.ativo);
  const goleiros = ativos.filter(j => j.goleiro).length;

  pintar(`
    <div class="row between" style="margin:2px 2px 14px">
      <div><div class="eyebrow">Elenco</div><h1 class="h1 mt-2">${ativos.length} jogador${ativos.length === 1 ? '' : 'es'}</h1></div>
      <button class="btn primary" onclick="abrirFormJogador()">${ic('plus')} Novo</button>
    </div>

    ${pel && pel.status !== 'FINALIZADA'
      ? `<div class="card tight row"><span class="avatar-stack">${pel.lista.slice(0, 5).map(id => avatar(J(id), 'sm')).join('')}</span>
          <div class="grow small"><b>${pel.lista.length} na lista</b> de ${esc(fmtDataLonga(pel.dataJogo))}<br><span class="muted">Toque em “Lista” para adicionar.</span></div></div>`
      : `<div class="card tight row"><span class="chip gray">${ic('calendar', 14)}</span>
          <div class="grow small muted">Para montar a lista da semana, <a href="#/pelada" style="color:var(--brand);font-weight:600">crie a pelada</a> (data e horário).</div></div>`}

    <div style="position:relative;margin-bottom:10px">
      <span style="position:absolute;left:14px;top:15px;color:var(--text-3)">${ic('search')}</span>
      <input class="input" style="padding-left:44px" id="busca-jogador" placeholder="Buscar jogador" value="${esc(JG.busca)}" oninput="JG.busca=this.value; desenharGradeJogadores()">
    </div>
    <div class="tabs">
      ${[['todos', 'Todos', ativos.length], ['goleiros', 'Goleiros', goleiros], ['lista', 'Na lista', pel ? pel.lista.length : 0]].map(([id, nome, n]) =>
        `<button class="${JG.filtro === id ? 'on' : ''}" onclick="JG.filtro='${id}'; desenharJogadores()">${nome} <span class="faint">${n}</span></button>`).join('')}
    </div>
    <div id="grade-jogadores" data-norise></div>

    ${arquivados.length ? `<details class="card" style="margin-top:16px"><summary style="cursor:pointer;font-weight:600">Arquivados (${arquivados.length})</summary>
      <div style="margin-top:8px">${arquivados.map(j => `
        <div class="p-row">${avatar(j, 'sm')}<span class="p-name">${esc(j.nome)}</span>
          <button class="btn sm" onclick="reativarJogador(${j.id})">Reativar</button></div>`).join('')}</div></details>` : ''}`);
  desenharGradeJogadores();
}

function desenharGradeJogadores() {
  const el = document.getElementById('grade-jogadores');
  if (!el) return;
  const termo = JG.busca.trim().toLowerCase();
  const naLista = new Set(JG.pelada ? JG.pelada.lista : []);
  let ativos = JG.lista.filter(j => j.ativo && j.nome.toLowerCase().includes(termo));
  if (JG.filtro === 'goleiros') ativos = ativos.filter(j => j.goleiro);
  if (JG.filtro === 'lista') ativos = ativos.filter(j => naLista.has(j.id));
  ativos.sort((a, b) => b.overall - a.overall || a.nome.localeCompare(b.nome, 'pt-BR'));

  if (!ativos.length) {
    el.innerHTML = `<div class="empty">${arteVazia()}${JG.lista.some(j => j.ativo) ? 'Nenhum jogador encontrado.' : 'Nenhum jogador ainda.<br>Toque em <b>Novo</b> para cadastrar o primeiro.'}</div>`;
    return;
  }
  const podeLista = JG.pelada && JG.pelada.status !== 'FINALIZADA';
  el.innerHTML = `<div class="fut-grid">${ativos.map((j, k) => `
    <div class="rise" style="--i:${Math.min(k, 8)}">
      ${cartaFut(j, { onclick: `abrirJogador(${j.id})`, on: naLista.has(j.id) })}
      ${podeLista ? `<button class="btn sm block ${naLista.has(j.id) ? 'soft' : ''}" style="margin-top:8px" onclick="alternarNaListaDaSemana(${j.id})">
          ${naLista.has(j.id) ? ic('check', 16) + ' Na lista' : ic('plus', 16) + ' Lista da semana'}</button>` : ''}
    </div>`).join('')}</div>`;
  montarFx(el);
}

async function alternarNaListaDaSemana(jogadorId) {
  const pel = JG.pelada;
  if (!pel) return;
  try {
    JG.pelada = pel.lista.includes(jogadorId) ? await DEL(`/peladas/${pel.id}/lista/${jogadorId}`) : await PUT(`/peladas/${pel.id}/lista/${jogadorId}`);
    vibrar(10);
    desenharJogadores();
  } catch (e) { erroToast(e); }
}

// ---------- detalhe do jogador: carta grande + radar + matriz de números ----------

function abrirJogador(id) {
  const j = J(id), e = j.estatisticas, attrs = atributos(j);
  const naLista = JG.pelada && JG.pelada.lista.includes(id);
  const tiles = [['Jogos', e.partidas], ['Vitórias', e.vitorias], ['Empates', e.empates], ['Derrotas', e.derrotas],
    ['Gols', e.gols], ['Assist.', e.assistencias], ...(j.goleiro ? [['Sem sofrer', e.semSofrer], ['Gols sofr.', e.golsSofridos]] : []),
    ['Nota', e.partidas ? nota1(e.notaMedia) : '—']];
  abrirSheet('', `
    <div class="center" style="margin-bottom:6px"><span class="chip ${tier(j.overall) === 'elite' ? '' : 'gold'}">${TIER_NOME[tier(j.overall)]}</span></div>
    ${cartaFut(j, { big: true })}
    <div class="mt-4">${attrs
      ? radar(attrs.map(a => a[1]), attrs.map(a => a[0]))
      : `<div class="empty" style="padding:14px">Ainda sem partidas: o radar aparece depois da primeira pelada.</div>`}</div>
    <div class="tiles mt-3">${tiles.map(([r, v]) => `<div class="tile"><b>${v}</b><span>${r}</span></div>`).join('')}</div>
    <div class="btn-row mt-4">
      <button class="btn" onclick="abrirFormJogador(${id})">${ic('edit')} Editar</button>
      ${JG.pelada && JG.pelada.status !== 'FINALIZADA'
        ? `<button class="btn ${naLista ? 'soft' : 'primary'}" onclick="alternarNaListaDaSemana(${id}); fecharSheet()">${naLista ? ic('check') + ' Na lista' : ic('plus') + ' Lista'}</button>` : ''}
    </div>`);
}

// ---------- formulário (novo / editar) ----------

function abrirFormJogador(id) {
  const j = id ? J(id) : null;
  JG.editandoId = id || null;
  JG.foto = undefined;
  abrirSheet(j ? 'Editar jogador' : 'Novo jogador', `
    <div class="photo-pick">
      <span id="foto-prev">${avatar(j ? { ...j, overall: null } : { nome: '?', foto: null }, 'xl')}</span>
      <div class="col gap-2">
        <label class="btn sm" for="foto-input">${ic('camera')} Foto</label>
        <input type="file" id="foto-input" accept="image/*" onchange="lerFoto(this)">
        <button class="btn sm ghost" onclick="removerFoto()">Remover foto</button>
      </div>
    </div>
    <label class="field"><span>Nome</span><input id="jg-nome" maxlength="80" placeholder="Ex.: Carlos" value="${esc(j ? j.nome : '')}"></label>
    <label class="switch"><input type="checkbox" id="jg-goleiro" ${j && j.goleiro ? 'checked' : ''}>
      <span class="grow">${ic('glove')} Joga no gol<br><span class="small muted">Usa a fórmula de overall de goleiro</span></span></label>
    <button class="btn primary block lg" onclick="salvarJogador()">Salvar</button>
    ${j ? `<button class="btn danger block mt-3" onclick="removerJogador()">${ic('trash')} Remover jogador</button>` : ''}`);
  setTimeout(() => { const n = document.getElementById('jg-nome'); if (n && !id) n.focus(); }, 60);
}

/** Reduz a foto para um quadrado de 256px (recorte central) — mantém o banco pequeno e o app rápido. */
function lerFoto(input) {
  const arquivo = input.files && input.files[0];
  if (!arquivo) return;
  const url = URL.createObjectURL(arquivo);
  const img = new Image();
  img.onload = () => {
    const lado = Math.min(img.width, img.height), T = 256;
    const c = document.createElement('canvas');
    c.width = T; c.height = T;
    c.getContext('2d').drawImage(img, (img.width - lado) / 2, (img.height - lado) / 2, lado, lado, 0, 0, T, T);
    JG.foto = c.toDataURL('image/jpeg', 0.82);
    document.getElementById('foto-prev').innerHTML = `<span class="avatar xl"><img src="${JG.foto}" alt=""></span>`;
    URL.revokeObjectURL(url);
  };
  img.onerror = () => { URL.revokeObjectURL(url); toast('Não consegui ler essa imagem.', 'error'); };
  img.src = url;
}

function removerFoto() {
  JG.foto = '';
  const nome = document.getElementById('jg-nome').value || '?';
  document.getElementById('foto-prev').innerHTML = avatar({ nome, foto: null }, 'xl');
}

async function salvarJogador() {
  const nome = document.getElementById('jg-nome').value.trim();
  if (!nome) return toast('Informe o nome do jogador.', 'error');
  const corpo = { nome, goleiro: document.getElementById('jg-goleiro').checked };
  if (JG.foto !== undefined) corpo.foto = JG.foto;
  try {
    if (JG.editandoId) await PUT(`/jogadores/${JG.editandoId}`, corpo);
    else await POST('/jogadores', corpo);
    fecharSheet();
    toast(JG.editandoId ? 'Jogador atualizado!' : 'Jogador cadastrado!');
    await telaJogadores();
  } catch (e) { erroToast(e); }
}

function removerJogador() {
  const id = JG.editandoId, j = J(id);
  confirmar('Remover jogador', `Remover ${j.nome}? Se ele já tiver histórico, será apenas arquivado e as estatísticas continuam valendo.`,
    'Remover', async () => {
      try { await DEL(`/jogadores/${id}`); toast('Jogador removido.'); await telaJogadores(); } catch (e) { erroToast(e); }
    });
}

async function reativarJogador(id) {
  try { await POST(`/jogadores/${id}/reativar`); toast('Jogador reativado!'); await telaJogadores(); } catch (e) { erroToast(e); }
}
