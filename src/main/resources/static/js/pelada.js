// ---------- Aba Pelada: lista, sorteio, times (cards/campo), partidas e resumo ----------

const PEL = { verElenco: false, grupo: null, rk: null, p: null, buscaPicker: '', vista: 'cards', timeCampo: null, par: [], tam: null, dur: null, gols: null };

async function telaPelada(view, pagina) {
  PEL.pagina = pagina || 'principal';
  await carregarJogadores();
  [PEL.grupo, PEL.rk] = await Promise.all([GET(`/grupos/${GID}`),
    GET(`/rankings?grupoId=${GID}&periodo=GERAL`).catch(() => null)]);
  PEL.p = await GET(`/peladas/atual?grupoId=${GID}`);
  if (!PEL.p) return desenharSemPelada();
  PEL.par = [];
  desenharPelada();
}

// ---------- sem pelada em aberto ----------

async function desenharSemPelada() {
  const historico = (await GET(`/peladas?grupoId=${GID}`)).filter(p => p.status === 'FINALIZADA');
  pintar(`
    ${cabecalhoGrupo()}
    <div class="card hero">
      <div class="empty" style="padding-top:8px">${arteVazia()}
        <h2 class="h2">Nenhuma pelada marcada</h2>
        <p class="muted mt-2">Defina o dia e o horário para abrir a lista da semana.</p></div>
      <div class="field-row">
        <label class="field"><span>Data</span><input type="date" id="nova-data" value="${hojeISO()}"></label>
        <label class="field"><span>Horário</span><input type="time" id="nova-hora" value="08:00"></label>
      </div>
      <button class="btn primary block lg" onclick="criarPelada()">${ic('plus')} Criar pelada</button>
    </div>
    ${blocoRankingResumo()}
    ${historico.length ? `<div class="card"><div class="card-head"><h2 class="h3">Peladas anteriores</h2><span class="chip gray">${historico.length}</span></div>
      ${historico.slice(0, 15).map(h => `
        <a class="p-row" href="#/grupos/${GID}/resumo/${h.id}" style="text-decoration:none">
          <span class="chip gray">${ic('calendar', 14)}</span>
          <span class="p-name">${esc(fmtDataLonga(h.dataJogo))}<span class="p-sub">${h.jogadores} jogadores · ${h.partidas} partidas</span></span>
          <span class="muted">${ic('chevR')}</span></a>`).join('')}
      ${historico.length > 15 ? `<a class="btn sm ghost block mt-2" href="#/grupos/${GID}/peladas">Ver todas (${historico.length})</a>` : ''}</div>` : ''}`);
}

// ---------- biblioteca: todas as peladas já criadas (ativa + histórico) ----------

async function telaBibliotecaPeladas() {
  const todas = await GET(`/peladas?grupoId=${GID}`);
  const ordenadas = todas.slice().sort((a, b) => {
    const ativaA = a.status !== 'FINALIZADA', ativaB = b.status !== 'FINALIZADA';
    if (ativaA !== ativaB) return ativaA ? -1 : 1;
    return b.dataJogo.localeCompare(a.dataJogo) || b.id - a.id;
  });
  pintar(`
    <a href="#/grupos/${GID}" class="btn sm ghost" style="margin-bottom:10px">${ic('left', 16)} Pelada</a>
    <div style="margin:2px 2px 14px"><div class="eyebrow">Biblioteca</div><h1 class="h1 mt-2">Todas as peladas</h1></div>
    ${ordenadas.length ? `<div class="card">
      ${ordenadas.map(p => {
        const ativa = p.status !== 'FINALIZADA';
        const destino = ativa ? `#/grupos/${GID}` : `#/grupos/${GID}/resumo/${p.id}`;
        return `<a class="p-row" href="${destino}" style="text-decoration:none">
          <span class="chip ${ativa ? '' : 'gray'}">${ativa ? '<span class="dot"></span>' : ic('calendar', 14)}</span>
          <span class="p-name">${esc(fmtDataLonga(p.dataJogo))}
            <span class="p-sub">${STATUS_PELADA[p.status]} · ${p.jogadores} jogadores · ${p.partidas} partida${p.partidas === 1 ? '' : 's'}</span></span>
          <span class="muted">${ic('chevR')}</span></a>`;
      }).join('')}
    </div>` : `<div class="card"><div class="empty">${arteVazia()}Nenhuma pelada criada ainda.</div></div>`}`);
}

async function criarPelada() {
  const dataJogo = document.getElementById('nova-data').value;
  const horaJogo = document.getElementById('nova-hora').value;
  if (!dataJogo || !horaJogo) return toast('Informe data e horário.', 'error');
  try {
    await POST(`/peladas?grupoId=${GID}`, { dataJogo, horaJogo });
    toast('Pelada criada! Agora monte a lista.');
    document.getElementById('view').dataset.entrada = '1';
    telaPelada();
  } catch (e) { erroToast(e); }
}

// ---------- pelada em aberto ----------

function desenharPelada() {
  const p = PEL.p;
  const aberta = p.status === 'ABERTA', sorteada = p.status === 'SORTEADA' || p.status === 'EM_ANDAMENTO';
  const temPartidas = p.partidas.length > 0;
  const banner = p.partidaEmAbertoId ? `
      <a class="banner" href="#/grupos/${GID}/partida/${p.partidaEmAbertoId}" style="display:block;text-decoration:none">
        <div class="row"><span class="chip"><span class="dot"></span> Ao vivo</span><div class="grow"><h3>Partida em andamento</h3>
        <div class="muted small">Toque para voltar ao placar e ao cronômetro.</div></div>${ic('chevR')}</div></a>` : '';

  if (PEL.pagina === 'times') {
    if (p.status !== 'SORTEADA' || peladaIniciada(p)) { location.hash = `#/grupos/${GID}`; return; }
    return desenharPaginaTimes(p);
  }

  // Pelada iniciada: tela enxuta só com o que se usa entre uma partida e outra.
  if (peladaIniciada(p)) {
    pintar(`
      ${banner}
      <div class="card tight row between">
        <div><div class="eyebrow">${esc(PEL.grupo ? PEL.grupo.nome : 'Grupo')}</div>
          <div class="h3 mt-2">${esc(fmtDataLonga(p.dataJogo))}</div></div>
        <div class="row gap-2"><span class="chip ${p.status === 'EM_ANDAMENTO' ? '' : 'gray'}">${p.status === 'EM_ANDAMENTO' ? '<span class="dot"></span>' : ''}${STATUS_PELADA[p.status]}</span>
          <button class="btn sm" onclick="PEL.verElenco=!PEL.verElenco; desenharPelada()">${ic('users', 16)} ${PEL.verElenco ? 'Ocultar' : 'Times'}</button></div>
      </div>
      ${PEL.verElenco ? blocoTimes(p) : ''}
      ${!p.partidaEmAbertoId ? blocoNovaPartida(p) : ''}
      ${temPartidas ? blocoPartidas(p) : ''}
      ${!temPartidas ? `<button class="btn block" onclick="voltarAosTimes()">${ic('left', 16)} Voltar à lista e aos times</button>` : ''}
      ${p.status === 'EM_ANDAMENTO' && !p.partidaEmAbertoId
        ? `<button class="btn primary block lg" onclick="finalizarPelada()">${ic('flag')} Finalizar pelada</button>` : ''}`);
    return;
  }

  pintar(`
    ${cabecalhoGrupo()}
    ${banner}
    ${blocoHero(p, temPartidas)}
    ${blocoLista(p)}
    ${aberta || p.status === 'SORTEADA' ? blocoSorteio(p) : ''}
    ${p.status === 'SORTEADA' ? blocoTimesResumo(p) : ''}
    ${blocoRankingResumo()}`);
  if (document.getElementById('previa-sorteio')) atualizarPrevia();
}

/** "Iniciada" = já tem partida, ou o ADM tocou em Iniciar pelada (lembrado neste aparelho). */
function peladaIniciada(p) {
  if (p.status === 'EM_ANDAMENTO') return true;
  if (p.status !== 'SORTEADA') return false;
  try { return localStorage.getItem('overall_ini_' + p.id) === '1'; } catch (e) { return false; }
}
function marcarIniciada(id, sim) {
  try { sim ? localStorage.setItem('overall_ini_' + id, '1') : localStorage.removeItem('overall_ini_' + id); } catch (e) { /* ignora */ }
}
function iniciarPelada() {
  marcarIniciada(PEL.p.id, true); PEL.verElenco = false; PEL.par = [];
  vibrar(12); location.hash = `#/grupos/${GID}`;
}
function voltarAosTimes() {
  marcarIniciada(PEL.p.id, false); location.hash = `#/grupos/${GID}/times`;
}

function blocoHero(p, temPartidas) {
  // Continuidade: as quatro etapas formam um trilho único que se preenche conforme a pelada avança.
  const idx = p.status === 'FINALIZADA' ? 4 : p.status === 'EM_ANDAMENTO' ? 2 : p.status === 'SORTEADA' ? 2 : (p.lista.length >= 2 ? 1 : 0);
  const etapas = [['Lista', 'users'], ['Sorteio', 'dice'], ['Jogos', 'ball'], ['Final', 'flag']];
  return `<div class="card hero">
    <div class="row between"><span class="chip ${p.status === 'EM_ANDAMENTO' ? '' : 'gray'}">${p.status === 'EM_ANDAMENTO' ? '<span class="dot"></span>' : ''}${STATUS_PELADA[p.status]}</span>
      <div class="row gap-2">
        <a class="btn icon sm" aria-label="Biblioteca de peladas" href="#/grupos/${GID}/peladas">${ic('grid')}</a>
        ${!temPartidas ? `<button class="btn icon sm" aria-label="Alterar data" onclick="editarPelada()">${ic('edit')}</button>
        <button class="btn icon sm danger" aria-label="Excluir pelada" onclick="excluirPelada()">${ic('trash')}</button>` : ''}</div></div>
    <div class="eyebrow mt-4">Próxima pelada</div>
    <div class="hero-date mt-2">${esc(fmtDataLonga(p.dataJogo))}</div>
    <div class="row mt-3 wrap gap-2"><span class="chip gray">${ic('clock', 14)} ${esc(fmtHora(p.horaJogo))}</span>
      <span class="chip ${diasAte(p.dataJogo) === 0 ? '' : 'gray'}">${esc(contagem(p.dataJogo))}</span></div>
    <div class="steps mt-6" style="--p:${Math.min(idx, 3) / 3}">
      ${etapas.map(([nome, icone], i) => `<div class="step ${i < idx ? 'done' : i === idx ? 'now' : ''}"><span class="node">${i < idx ? ic('check') : ic(icone)}</span>${nome}</div>`).join('')}
    </div></div>`;
}

/** Cartão compacto: a lista completa só aparece no modal "Mostrar lista" (mantém a página limpa). */
function blocoLista(p) {
  const n = p.lista.length;
  return `<div class="card">
    <div class="row between">
      <div class="row"><span class="avatar-stack">${p.lista.slice(0, 4).map(id => avatar(J(id), 'sm')).join('')}</span>
        <div><h2 class="h3">Lista da semana</h2><div class="small muted">${n} jogador${n === 1 ? '' : 'es'}</div></div></div>
      <button class="btn sm" onclick="abrirLista()">${ic('users', 16)} Mostrar lista</button>
    </div>
  </div>`;
}

function corpoLista() {
  const p = PEL.p, noTime = new Set();
  p.times.forEach(t => t.jogadores.forEach(j => noTime.add(j.jogadorId)));
  const goleiros = p.lista.filter(id => J(id).goleiro).length;
  return `<div class="small muted" style="margin-bottom:8px">${p.lista.length} jogador${p.lista.length === 1 ? '' : 'es'}${goleiros ? ` · ${goleiros} goleiro${goleiros === 1 ? '' : 's'}` : ''}</div>
    ${p.lista.length ? p.lista.map(id => { const j = J(id); return `
      <div class="p-row">${avatar(j, 'sm')}
        <span class="p-name">${esc(j.nome)}${j.goleiro ? ` <span class="faint">${ic('glove', 14)}</span>` : ''}</span>
        ${ovr(j.overall)}
        ${noTime.has(id) ? '' : `<button class="btn icon sm ghost" aria-label="Tirar da lista" onclick="tirarDaLista(${id})">${ic('x', 16)}</button>`}
      </div>`; }).join('')
      : `<div class="empty" style="padding:14px">Ninguém na lista ainda.</div>`}
    <button class="btn primary block mt-4" onclick="abrirPickerLista()">${ic('plus', 16)} Adicionar jogadores</button>`;
}

function abrirLista() { abrirSheet('Lista da semana', corpoLista()); }

function listaModalAberta() {
  return !document.getElementById('sheet-overlay').hidden && document.getElementById('sheet-title').textContent === 'Lista da semana';
}

function abrirPickerLista() {
  PEL.buscaPicker = '';
  abrirSheet('Adicionar à lista', `
    <div style="position:relative;margin-bottom:10px"><span style="position:absolute;left:14px;top:15px;color:var(--text-3)">${ic('search')}</span>
    <input class="input" style="padding-left:44px" placeholder="Buscar jogador" oninput="PEL.buscaPicker=this.value; desenharPicker()"></div>
    <div id="picker-lista"></div>
    <button class="btn block mt-4" onclick="abrirLista()">${ic('check', 16)} Concluído</button>`);
  desenharPicker();
}

function desenharPicker() {
  const el = document.getElementById('picker-lista');
  if (!el) return;
  const termo = PEL.buscaPicker.trim().toLowerCase(), naLista = new Set(PEL.p.lista);
  const js = [...JOGADORES.values()].filter(j => j.ativo && j.nome.toLowerCase().includes(termo))
    .sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR'));
  el.innerHTML = js.length ? js.map(j => `
    <div class="p-row">${avatar(j, 'sm')}<span class="p-name">${esc(j.nome)}${j.goleiro ? ` <span class="faint">${ic('glove', 14)}</span>` : ''}</span>${ovr(j.overall)}
      <button class="btn sm ${naLista.has(j.id) ? 'soft' : 'primary'}" style="min-width:44px" aria-label="Alternar" onclick="alternarNoPicker(${j.id})">
        ${naLista.has(j.id) ? ic('check', 16) : ic('plus', 16)}</button></div>`).join('')
    : '<div class="empty">Nenhum jogador. Cadastre na aba Jogadores.</div>';
}

async function alternarNoPicker(id) {
  try {
    PEL.p = PEL.p.lista.includes(id) ? await DEL(`/peladas/${PEL.p.id}/lista/${id}`) : await PUT(`/peladas/${PEL.p.id}/lista/${id}`);
    vibrar(8);
    desenharPicker();
    desenharPelada();
  } catch (e) { erroToast(e); }
}

async function tirarDaLista(id) {
  try {
    PEL.p = await DEL(`/peladas/${PEL.p.id}/lista/${id}`); desenharPelada();
    if (listaModalAberta()) document.getElementById('sheet-body').innerHTML = corpoLista();
  } catch (e) { erroToast(e); }
}

// ---------- sorteio: caixa morfológica + prévia em pontos (proximidade e similaridade) ----------

function blocoSorteio(p) {
  const refazendo = p.status === 'SORTEADA';
  PEL.tam = PEL.tam || p.jogadoresPorTime || 5;
  return `<div class="card">
    <div class="card-head"><div><h2 class="h3">${refazendo ? 'Sortear novamente' : 'Sortear times'}</h2>
      <div class="small muted mt-2">Aleatório no começo; com histórico, balanceado por overall. Goleiros não entram no sorteio — quem vai para o gol é escolhido na partida.</div></div>
      <span class="chip gray">${ic('dice', 14)}</span></div>
    ${caixa('sorteio', {
      linhas: [{ k: 'tam', rotulo: 'Por time', dica: 'jogadores de linha', v: PEL.tam, custom: { min: 1, max: 20 }, fmt: v => `${v} por time`,
        opcoes: [3, 4, 5, 6, 7, 8].map(v => ({ v, r: v })) }],
      aoMudar: (s) => { PEL.tam = s.tam; atualizarPrevia(); }
    })}
    <div id="previa-sorteio"></div>
    <button class="btn primary block lg mt-4" ${p.sorteioLiberado ? '' : 'disabled'} onclick="sortear()">${ic('dice')} ${refazendo ? 'Sortear de novo' : 'Sortear times'}</button>
    ${p.sorteioLiberado ? '' : `<div class="small muted center mt-3">${ic('clock', 14)} O sorteio é liberado no dia da pelada (${esc(fmtData(p.dataJogo))}).</div>`}
  </div>`;
}

function atualizarPrevia() {
  const el = document.getElementById('previa-sorteio');
  if (!el) return;
  const n = PEL.p.lista.filter(id => !J(id).goleiro).length, tam = PEL.tam;
  if (n < 2) { el.innerHTML = '<div class="dots-cap">Adicione jogadores de linha à lista para ver a divisão.</div>'; return; }
  const times = Math.ceil(n / tam), resto = n % tam;
  if (times < 2) { el.innerHTML = '<div class="dots-cap">Com esse tamanho só sai 1 time. Reduza os jogadores por time.</div>'; return; }
  let k = 0;
  el.innerHTML = `<div class="dots">${Array.from({ length: times }, (_, i) => {
    const tamanho = i === times - 1 && resto ? resto : tam;
    return `<div class="dots-team${tamanho < tam ? ' inc' : ''}" style="--c:${corDoTime(i + 1)};--cols:${Math.min(4, Math.ceil(Math.sqrt(tamanho)))}">
      ${Array.from({ length: tamanho }, () => `<i style="--k:${k++}"></i>`).join('')}</div>`;
  }).join('')}</div>
  <div class="dots-cap"><b>${n}</b> jogadores → <b>${times}</b> times${resto ? ` (o último incompleto, com ${resto})` : ''}</div>`;
}

async function sortear() {
  const executar = async () => {
    try {
      PEL.p = await POST(`/peladas/${PEL.p.id}/sorteio`, { jogadoresPorTime: PEL.tam });
      PEL.timeCampo = null; PEL.par = [];
      toast(PEL.p.modoSorteio === 'OVERALL' ? 'Times balanceados pelo overall!' : 'Times sorteados!');
      vibrar([20, 30, 20]);
      desenharPelada();
      const alvo = document.getElementById('card-times');
      if (alvo) alvo.scrollIntoView({ behavior: 'smooth', block: 'center' });
    } catch (e) { erroToast(e); }
  };
  if (PEL.p.status === 'SORTEADA') confirmar('Sortear novamente?', 'Os times atuais serão substituídos por um novo sorteio.', 'Sortear', executar, false);
  else executar();
}

// ---------- times: cards ou campo em perspectiva ----------

function blocoTimes(p) {
  const balanceado = p.modoSorteio === 'OVERALL';
  return `<div class="row between" style="margin:8px 2px 12px">
      <h2 class="h2">Times</h2>
      <span class="chip ${balanceado ? 'gold' : 'gray'}">${ic(balanceado ? 'scale' : 'dice', 14)} ${balanceado ? 'Balanceado por overall' : 'Aleatório'}</span></div>
    <div class="seg" style="--n:2;--idx:${PEL.vista === 'cards' ? 0 : 1}"><span class="seg-pill"></span>
      <button class="${PEL.vista === 'cards' ? 'on' : ''}" onclick="PEL.vista='cards'; desenharPelada()">${ic('grid', 16)} Cards</button>
      <button class="${PEL.vista === 'campo' ? 'on' : ''}" onclick="PEL.vista='campo'; desenharPelada()">${ic('pitch', 16)} Campo</button></div>
    ${PEL.vista === 'campo' ? blocoCampo(p) : p.times.map(t => cartaoTime(t, p)).join('')}`;
}

function cartaoTime(t, p) {
  const cor = corDoTime(t.numero), incompleto = t.jogadores.length < (p.jogadoresPorTime || 0);
  return `<div class="team-card" style="--team:${cor}">
    <div class="team-head">${escudo(t.numero, 46, true)}
      <div class="grow"><div class="h3">${nomeDoTime(t.numero)}</div>
        <div class="small muted">${nomeDoCorTime(t.numero)} · ${t.jogadores.length} jogador${t.jogadores.length === 1 ? '' : 'es'}${incompleto ? ' · incompleto' : ''}</div></div>
      <div class="center"><div class="eyebrow" style="margin-bottom:4px">média</div>${ovr(Math.round(t.overallMedio))}</div></div>
    <div class="team-body">${t.jogadores.map(i => { const j = J(i.jogadorId); return `
      <div class="p-row">${avatar(j, 'sm')}<span class="p-name">${esc(j.nome)}${i.goleiro ? ` <span class="faint">${ic('glove', 14)}</span>` : ''}</span>${ovr(i.overall)}</div>`; }).join('')}</div>
  </div>`;
}

/** Distribui os jogadores em linhas (goleiro no fundo, melhores à frente) → posições (x,y) em % no campo. */
function formacao(jogadores) {
  const gk = jogadores.find(i => i.goleiro);
  const resto = jogadores.filter(i => i !== gk).sort((a, b) => b.overall - a.overall);
  const n = resto.length, linhas = n <= 2 ? 1 : n <= 5 ? 2 : n <= 9 ? 3 : 4;
  const tamanhos = Array(linhas).fill(Math.floor(n / linhas));
  for (let i = 0; i < n % linhas; i++) tamanhos[(i * 2 + 1) % linhas]++;      // sobras nas linhas do meio
  const pos = [];
  if (gk) pos.push({ i: gk, x: 50, y: 86 });
  let cursor = 0;
  for (let l = 0; l < linhas; l++) {                                          // l = 0 é a linha da frente
    const y = linhas === 1 ? 42 : 20 + l * (52 / (linhas - 1));
    for (let c = 0; c < tamanhos[l]; c++) {
      pos.push({ i: resto[cursor++], x: (c + 1) / (tamanhos[l] + 1) * 100, y });
    }
  }
  return pos;
}

function blocoCampo(p) {
  const t = p.times.find(x => x.id === PEL.timeCampo) || p.times[0];
  PEL.timeCampo = t.id;
  const pos = formacao(t.jogadores);
  // Matrizes: o plano do campo é inclinado e vai para o fundo (perspectiva · rotação X);
  // cada jogador aplica a rotação inversa e fica "em pé" sobre o gramado (billboard).
  const ang = 40;
  const plano = M.css(M.chain(M.perspective(820), M.rotX(ang)));
  const emPe = M.css(M.rotX(-ang));
  return `<div class="tabs mt-3">${p.times.map(x => `<button class="${x.id === t.id ? 'on' : ''}" onclick="PEL.timeCampo=${x.id}; desenharPelada()">${escudo(x.numero, 20)} ${nomeDoTime(x.numero)}</button>`).join('')}</div>
    <div class="pitch-stage" style="--team:${corDoTime(t.numero)}">
      <div class="pitch" style="transform:${plano};--unrot:${emPe}">
        <svg class="lines" viewBox="0 0 68 88" aria-hidden="true">
          <rect x="1" y="1" width="66" height="86"/><path d="M1 44h66"/><circle cx="34" cy="44" r="8"/><circle cx="34" cy="44" r=".6"/>
          <rect x="17" y="70" width="34" height="17"/><rect x="26" y="80" width="16" height="7"/>
          <rect x="17" y="1" width="34" height="17"/><rect x="26" y="1" width="16" height="7"/></svg>
        ${pos.map(q => `<span class="pitch-shadow" style="left:${q.x}%;top:${q.y}%"></span>`).join('')}
        ${pos.map((q, k) => { const j = J(q.i.jogadorId); return `
          <div class="tok" style="left:${q.x}%;top:${q.y}%;--k:${k}">${avatar(j, 'md')}${ovr(q.i.overall)}
            <span class="nm">${esc(j.nome.split(' ')[0])}${q.i.goleiro ? ' ·GK' : ''}</span></div>`; }).join('')}
      </div></div>
    <div class="small muted center mt-3">${nomeDoTime(t.numero)} · média ${Math.round(t.overallMedio)} · goleiro no fundo, melhores à frente</div>`;
}

// ---------- nova partida: matriz de escudos + caixa morfológica ----------

function blocoNovaPartida(p) {
  const sug = p.proximaSugerida;
  if (PEL.par.length !== 2) PEL.par = sug ? [sug.timeAId, sug.timeBId] : [p.times[0].id, p.times[1].id];
  PEL.dur = PEL.dur || p.duracaoMinutos || 8;
  PEL.gols = PEL.gols || p.golsParaEncerrar || 2;
  return `<div class="card">
    <div class="card-head"><div><h2 class="h3">${p.partidas.length ? 'Próxima partida' : 'Primeira partida'}</h2>
      <div class="small muted mt-2">${p.partidas.length ? 'Sugestão: quem ganhou fica e o próximo da fila entra.' : 'Escolha quais times começam — às vezes um está incompleto.'}</div></div>
      <span class="chip gray">${ic('ball', 14)}</span></div>
    <div class="eyebrow" style="margin-bottom:8px">1 · Quem joga <span class="faint">(toque em dois times)</span></div>
    <div class="pairs" id="pairs">${paresHtml(p)}</div>
    <div class="eyebrow" style="margin:18px 0 8px">2 · Regras da partida</div>
    ${caixa('partida', {
      linhas: [
        { k: 'dur', rotulo: 'Tempo', dica: 'minutos', v: PEL.dur, custom: { min: 1, max: 90 }, fmt: v => `${v} min`, opcoes: [5, 6, 8, 10, 12, 15].map(v => ({ v, r: v })) },
        { k: 'gols', rotulo: 'Gols', dica: 'para encerrar', v: PEL.gols, custom: { min: 1, max: 99 }, fmt: v => `${v} gol${v === 1 ? '' : 's'}`, opcoes: [1, 2, 3, 4, 5].map(v => ({ v, r: v })) }
      ],
      aoMudar: (s) => { PEL.dur = s.dur; PEL.gols = s.gols; }
    })}
    <button class="btn primary block lg mt-4" id="btn-abrir" onclick="criarPartida()">${ic('play')} Abrir partida</button>
  </div>`;
}

function paresHtml(p) {
  const [a, b] = PEL.par;
  return p.times.map(t => {
    const slot = t.id === a ? 'A' : t.id === b ? 'B' : '';
    return `<button class="pair ${slot ? 'sel' : ''}" style="--team:${corDoTime(t.numero)}" onclick="escolherPar(${t.id})" aria-pressed="${!!slot}">
      <span class="slot">${slot}</span>${escudo(t.numero, 50, !!slot)}<span class="nm">${nomeDoTime(t.numero)}</span>
      <span class="avg">${t.jogadores.length} jog. · média ${Math.round(t.overallMedio)}</span></button>`;
  }).join('');
}

/** Duas escolhas no máximo: tocar em um terceiro time troca o mais antigo; tocar de novo desmarca. */
function escolherPar(timeId) {
  const i = PEL.par.indexOf(timeId);
  if (i >= 0) PEL.par.splice(i, 1);
  else { PEL.par.push(timeId); if (PEL.par.length > 2) PEL.par.shift(); }
  vibrar(8);
  document.getElementById('pairs').innerHTML = paresHtml(PEL.p);
  const btn = document.getElementById('btn-abrir');
  if (btn) btn.disabled = PEL.par.length !== 2;
}

async function criarPartida() {
  if (PEL.par.length !== 2) return toast('Escolha dois times.', 'error');
  const s = cxEstado('partida');
  try {
    const partida = await POST(`/peladas/${PEL.p.id}/partidas`, { timeAId: PEL.par[0], timeBId: PEL.par[1], duracaoMinutos: s.dur, golsParaEncerrar: s.gols });
    PEL.par = [];
    location.hash = `#/grupos/${GID}/partida/${partida.id}`;
  } catch (e) { erroToast(e); }
}

// ---------- partidas jogadas ----------

function blocoPartidas(p) {
  const rotulo = { AGUARDANDO: 'Aguardando', EM_ANDAMENTO: 'Ao vivo', PAUSADA: 'Pausada', EMPATE_PENDENTE: 'Empate', PENALTIS: 'Pênaltis' };
  return `<div class="card"><div class="card-head"><h2 class="h3">Partidas</h2><span class="chip gray">${p.partidas.length}</span></div>
    ${p.partidas.map(m => {
      const pen = m.status === 'ENCERRADA' && m.vencedorPenaltisId
        ? `<div class="m-note">${nomeDoTime(m.vencedorPenaltisId === m.timeAId ? m.timeANumero : m.timeBNumero)} nos pênaltis</div>` : '';
      return `<a class="match" href="#/grupos/${GID}/partida/${m.id}">
        <div class="m-side">${escudo(m.timeANumero, 36)}<span class="small muted">${nomeDoTime(m.timeANumero)}</span></div>
        <div><div class="m-score">${m.golsA} <span class="faint">×</span> ${m.golsB}</div>${pen}
          ${m.status !== 'ENCERRADA' ? `<div class="m-note" style="color:var(--brand)">${rotulo[m.status]}</div>` : ''}</div>
        <div class="m-side r"><span class="small muted">${nomeDoTime(m.timeBNumero)}</span>${escudo(m.timeBNumero, 36)}</div></a>`; }).join('')}
  </div>`;
}

// ---------- editar / excluir / finalizar ----------

function editarPelada() {
  const p = PEL.p;
  abrirSheet('Data e horário', `
    <div class="field-row">
      <label class="field"><span>Data</span><input type="date" id="ed-data" value="${p.dataJogo}"></label>
      <label class="field"><span>Horário</span><input type="time" id="ed-hora" value="${fmtHora(p.horaJogo)}"></label>
    </div><button class="btn primary block lg" onclick="salvarPelada()">Salvar</button>`);
}

async function salvarPelada() {
  try {
    PEL.p = await PUT(`/peladas/${PEL.p.id}`, { dataJogo: document.getElementById('ed-data').value, horaJogo: document.getElementById('ed-hora').value });
    fecharSheet(); toast('Pelada atualizada!'); desenharPelada();
  } catch (e) { erroToast(e); }
}

function excluirPelada() {
  confirmar('Excluir pelada', 'A lista e os times desta pelada serão apagados.', 'Excluir', async () => {
    try { await DEL(`/peladas/${PEL.p.id}`); toast('Pelada excluída.'); document.getElementById('view').dataset.entrada = '1'; telaPelada(); } catch (e) { erroToast(e); }
  });
}

function finalizarPelada() {
  confirmar('Finalizar pelada?', 'As notas do dia são fechadas e o overall de todos é atualizado. Depois disso não dá para alterar as partidas.',
    'Finalizar', async () => {
      try {
        await POST(`/peladas/${PEL.p.id}/finalizar`);
        await carregarJogadores();
        location.hash = `#/grupos/${GID}/resumo/${PEL.p.id}`;
      } catch (e) { erroToast(e); }
    }, false);
}

// ---------- resumo (tela final): MVP em carta 3D, ranking dos times e evolução do overall ----------

async function telaResumo(view, id) {
  await carregarJogadores();
  const r = await GET(`/peladas/${id}/resumo`);
  const finalizada = r.status === 'FINALIZADA';
  const comNota = r.jogadores.filter(d => d.nota != null);
  const mvp = comNota.slice().sort((a, b) => b.nota - a.nota || b.gols - a.gols)[0];
  const delta = (d) => {
    if (d.overallAntes == null || d.overallDepois == null) return '';
    const v = d.overallDepois - d.overallAntes;
    return `<span class="${v > 0 ? 'delta-up' : v < 0 ? 'delta-down' : 'muted'} small">${v > 0 ? '▲ +' + v : v < 0 ? '▼ ' + v : '='}</span>`;
  };
  const lista = (itens, unidade) => {
    const max = Math.max(1, ...itens.map(i => i.valor));
    return itens.length ? itens.map((i, k) => `
      <div class="rank-row"><span class="rank-pos" style="${k === 0 ? 'color:var(--gold)' : ''}">${k + 1}</span>${avatar(J(i.jogadorId), 'sm')}
        <div class="grow"><div class="p-name">${esc(i.nome)}</div><div class="bar"><i style="--w:${(i.valor / max * 100).toFixed(0)}%;--k:${k}"></i></div></div>
        <span class="rank-val">${i.valor}</span><span class="small muted">${unidade}</span></div>`).join('')
      : '<div class="empty" style="padding:12px">Ninguém pontuou.</div>';
  };

  pintar(`
    <a href="#/grupos/${GID}" class="btn sm ghost" style="margin-bottom:10px">${ic('left', 16)} Pelada</a>
    <div class="card hero">
      <span class="chip ${finalizada ? 'gold' : 'gray'}">${ic('flag', 14)} ${finalizada ? 'Pelada finalizada' : 'Em andamento'}</span>
      <div class="hero-date mt-3">${esc(fmtDataLonga(r.dataJogo))}</div>
      <div class="muted mt-2">${r.partidasDisputadas} partida${r.partidasDisputadas === 1 ? '' : 's'} disputada${r.partidasDisputadas === 1 ? '' : 's'}</div>
      ${mvp ? `<div class="mvp-hero mt-6">
          ${cartaFut(J(mvp.jogadorId), { mvp: true })}
          <div><span class="chip gold">${ic('crown', 14)} Craque da pelada</span>
            <div class="h2 mt-3">${esc(mvp.nome)}</div>
            <div class="muted small mt-2">Nota <b style="color:var(--text)">${nota1(mvp.nota)}</b> · ${mvp.gols} gol${mvp.gols === 1 ? '' : 's'} · ${mvp.assistencias} assist.</div></div></div>` : ''}
    </div>

    <div class="card"><div class="card-head"><h2 class="h3">${ic('trophy')} Times mais vencedores</h2></div>
      ${r.times.length ? `<div class="tbl-wrap"><table class="tbl"><thead><tr><th>Time</th><th>J</th><th>V</th><th>E</th><th>D</th><th>GP</th><th>GC</th><th>SG</th></tr></thead><tbody>
        ${r.times.map((t, k) => `<tr><td><span class="row gap-2">${escudo(t.numero, 28)}${k === 0 ? `<span style="color:var(--gold)">${ic('crown', 16)}</span>` : ''}</span></td><td>${t.jogos}</td><td><b>${t.vitorias}</b></td><td>${t.empates}</td><td>${t.derrotas}</td><td>${t.golsPro}</td><td>${t.golsContra}</td><td>${t.saldo > 0 ? '+' : ''}${t.saldo}</td></tr>`).join('')}
        </tbody></table></div>` : '<div class="empty">Nenhuma partida encerrada.</div>'}</div>

    <div class="card"><div class="card-head"><h2 class="h3">${ic('ball')} Artilheiros</h2></div>${lista(r.artilheiros, 'gols')}</div>
    <div class="card"><div class="card-head"><h2 class="h3">${ic('target')} Assistências</h2></div>${lista(r.assistentes, 'ass.')}</div>

    <div class="card"><div class="card-head"><h2 class="h3">${ic('star')} Notas e overall</h2></div>
      ${r.jogadores.length ? r.jogadores.map(d => { const j = J(d.jogadorId); return `
        <div class="p-row">${avatar(j, 'sm')}
          <span class="p-name">${esc(d.nome)}${j.goleiro ? ` <span class="faint">${ic('glove', 14)}</span>` : ''}
            <span class="p-sub">${d.partidas}j · ${d.vitorias}V ${d.empates}E ${d.derrotas}D · ${d.gols} gols · ${d.assistencias} ass.${j.goleiro ? ` · ${d.semSofrer} sem sofrer` : ''}</span></span>
          <div class="center"><div class="rank-val" style="line-height:1">${nota1(d.nota)}</div><div class="eyebrow" style="margin-top:3px">nota</div></div>
          ${finalizada && d.overallDepois != null ? `<div class="center" style="min-width:58px">${ovr(d.overallDepois)}<div style="margin-top:3px">${delta(d)}</div></div>` : ''}
        </div>`; }).join('') : '<div class="empty">Sem dados.</div>'}</div>
    <a class="btn primary block lg" href="#/grupos/${GID}">Voltar</a>
    <button class="btn danger block mt-3" onclick="abrirExclusaoPelada(${id}, '${esc(fmtDataLonga(r.dataJogo))}')">${ic('trash')} Excluir esta pelada</button>`);

  if (finalizada && mvp && window.__confeteResumo !== id) {
    window.__confeteResumo = id;
    setTimeout(() => confete(innerWidth / 2, innerHeight * .35, null, 140), 500);
  }
}

// ---------- início do grupo: cabeçalho e resumo do ranking ----------

function cabecalhoGrupo() {
  const nome = PEL.grupo ? PEL.grupo.nome : 'Grupo';
  return `<div class="row between" style="margin:2px 2px 14px">
    <div><div class="eyebrow">Grupo</div><h1 class="h1 mt-2">${esc(nome)}</h1></div>
    <a class="btn sm" href="#/grupos">${ic('grid', 16)} Trocar</a></div>`;
}

/** Ranking do grupo (todas as peladas): quem lidera em overall, gols e assistências. Atualiza a cada pelada finalizada. */
function blocoRankingResumo() {
  const rk = PEL.rk;
  if (!rk) return '';
  const secoes = [['Maiores overalls', 'trend', rk.overalls, v => ovr(Math.round(v))],
    ['Artilheiros', 'ball', rk.goleadores, v => `<span class="rank-val">${Math.round(v)}</span>`],
    ['Assistências', 'target', rk.assistentes, v => `<span class="rank-val">${Math.round(v)}</span>`],
    ['Goleiros (sem sofrer)', 'glove', rk.goleiros, v => `<span class="rank-val">${Math.round(v)}</span>`]];
  const vazio = secoes.every(s => !s[2].length);
  return `<div class="card"><div class="card-head"><h2 class="h3">${ic('trophy')} Ranking do grupo</h2>
      <a class="btn sm ghost" href="#/grupos/${GID}/rankings">Ver tudo ${ic('chevR', 14)}</a></div>
    ${vazio ? `<div class="empty" style="padding:12px">O ranking aparece depois da primeira pelada finalizada.</div>`
      : secoes.filter(s => s[2].length).map(([titulo, icone, itens, fmt]) => `
        <div class="eyebrow mt-3">${ic(icone, 14)} ${titulo}</div>
        ${itens.slice(0, 3).map((i, k) => `<div class="rank-row"><span class="rank-pos" style="${k === 0 ? 'color:var(--gold)' : ''}">${k + 1}</span>
          ${avatar(J(i.jogadorId), 'sm')}<div class="grow"><div class="p-name">${esc(i.nome)}</div></div>${fmt(i.valor)}</div>`).join('')}`).join('')}
  </div>`;
}

// ---------- página dos times: resumo na pelada + tela própria com "Iniciar pelada" no fim ----------

/** Cartão compacto na página da pelada: mostra que o sorteio saiu e leva para os times. */
function blocoTimesResumo(p) {
  const jogando = p.times.reduce((s, t) => s + t.jogadores.length, 0);
  return `<a class="card" id="card-times" href="#/grupos/${GID}/times" style="display:block;text-decoration:none;color:inherit">
    <div class="row between"><div><div class="eyebrow">${p.modoSorteio === 'OVERALL' ? 'Balanceado por overall' : 'Sorteio aleatório'}</div>
      <h2 class="h2 mt-2">Times sorteados</h2>
      <div class="small muted mt-2">${p.times.length} times · ${jogando} jogadores de linha</div></div>
      <span class="muted">${ic('chevR')}</span></div>
    <div class="row gap-2 mt-3 wrap">${p.times.map(t => `<span class="chip gray">${escudo(t.numero, 18)} ${nomeDoTime(t.numero)} · ${Math.round(t.overallMedio)}</span>`).join('')}</div>
  </a>`;
}

function desenharPaginaTimes(p) {
  const goleiros = p.lista.filter(id => J(id).goleiro);
  pintar(`
    <a href="#/grupos/${GID}" class="btn sm ghost" style="margin-bottom:10px">${ic('left', 16)} Lista e sorteio</a>
    ${blocoTimes(p)}
    <div class="card mt-4"><div class="card-head"><h2 class="h3">${ic('glove')} Goleiros</h2><span class="chip gray">${goleiros.length}</span></div>
      ${goleiros.length ? goleiros.map(id => `<div class="p-row">${avatar(J(id), 'sm')}<span class="p-name">${esc(J(id).nome)}</span>${ovr(J(id).overall)}</div>`).join('')
        : '<div class="small muted">Ninguém marcado como goleiro na lista.</div>'}
      <div class="small muted mt-3">Goleiros ficam fora dos times: antes de cada partida você escolhe quem vai para o gol de cada lado (qualquer jogador da lista pode agarrar).</div></div>
    <div class="card tight small muted">${ic('info', 14)} Não gostou da divisão? Volte, e toque em <b>Sortear de novo</b>. Depois de iniciar a pelada, os times ficam fixos.</div>
    <button class="btn primary block lg" onclick="iniciarPelada()">${ic('play')} Iniciar pelada</button>`);
}

// ---------- excluir pelada de vez (histórico): exige a senha, para ninguém apagar no celular emprestado ----------

function abrirExclusaoPelada(id, dataTxt) {
  abrirSheet('Excluir pelada', `
    <div class="banner" style="margin-bottom:12px"><h3>${ic('alert', 20)} Isso não pode ser desfeito</h3>
      <div class="muted small">A pelada de <b>${dataTxt}</b> será apagada com todas as partidas, gols e estatísticas. O overall dos jogadores é recalculado sem ela.</div></div>
    <label class="field"><span>Sua senha (para confirmar)</span><input type="password" id="ex-senha" autocomplete="current-password"></label>
    <button class="btn danger block lg" id="ex-btn" onclick="confirmarExclusaoPelada(${id})">${ic('trash')} Excluir definitivamente</button>`);
  setTimeout(() => { const n = document.getElementById('ex-senha'); if (n) n.focus(); }, 60);
}

async function confirmarExclusaoPelada(id) {
  const senha = document.getElementById('ex-senha').value;
  if (!senha) return toast('Digite a senha para confirmar.', 'error');
  const btn = document.getElementById('ex-btn');
  btn.disabled = true;
  try {
    await POST(`/peladas/${id}/exclusao`, { senha });
    fecharSheet();
    toast('Pelada excluída.');
    location.hash = `#/grupos/${GID}`;
  } catch (e) {
    btn.disabled = false;
    erroToast(e);
    const c = document.getElementById('ex-senha'); if (c) { c.value = ''; c.focus(); }
  }
}
