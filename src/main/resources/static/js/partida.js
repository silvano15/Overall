// ---------- Partida ao vivo: placar 3D, anel do cronômetro, gols, substituições, empate e pênaltis ----------

const PT = { p: null, recebidoEm: 0, timer: null, sincronizando: false, pelada: null, gol: null };

async function telaPartida(view, id) {
  await carregarJogadores();
  const p = await GET(`/partidas/${id}`);
  PT.pelada = null; PT.p = null;
  aplicarPartida(p);

  // O relógio da tela só desenha; a verdade do tempo está no servidor.
  PT.timer = setInterval(tickRelogio, 250);
  const aoVoltar = () => { if (!document.hidden) recarregarPartida(); };
  document.addEventListener('visibilitychange', aoVoltar);
  aoSair = () => {
    clearInterval(PT.timer); PT.timer = null;
    document.removeEventListener('visibilitychange', aoVoltar);
  };
}

function emJogo(p) { return p.status === 'EM_ANDAMENTO' || p.status === 'PAUSADA'; }

function aplicarPartida(p) {
  const antes = PT.p;
  PT.p = p;
  PT.recebidoEm = Date.now();
  desenharPartida(antes);
  if (antes) {
    if (emJogo(antes) && (p.status === 'ENCERRADA' || p.status === 'EMPATE_PENDENTE')) vibrar([250, 100, 250]);
    if (p.timeA.gols > antes.timeA.gols) celebrar(p.timeA, p);
    else if (p.timeB.gols > antes.timeB.gols) celebrar(p.timeB, p);
  }
}

async function recarregarPartida() {
  try { aplicarPartida(await GET(`/partidas/${PT.p.id}`)); } catch (e) { erroToast(e); }
}

/** Executa uma ação (POST/DELETE) que devolve o novo estado da partida. */
async function acaoPartida(fn, mensagem) {
  try {
    const p = await fn();
    fecharSheet();
    aplicarPartida(p);
    if (mensagem) toast(mensagem);
  } catch (e) { erroToast(e); }
}

/** Gol: confete na cor do time saindo do lado que marcou, faixa "GOL" e vibração. */
function celebrar(lado, p) {
  const ultimo = [...p.gols].reverse().find(g => g.timeId === lado.timeId);
  const cor = corDoTime(lado.numero);
  const flash = document.createElement('div');
  flash.className = 'goal-flash';
  flash.style.setProperty('--team', cor);
  flash.innerHTML = `<b>GOOOL!</b>${ultimo ? `<span>${esc(J(ultimo.autorId).nome)}</span>` : ''}`;
  document.body.appendChild(flash);
  setTimeout(() => flash.remove(), 1700);
  const x = lado.timeId === p.timeA.timeId ? innerWidth * .22 : innerWidth * .78;
  confete(x, innerHeight * .3, [cor, '#ffffff', clarear(cor, .5), '#D9FF3F'], 120);
  vibrar([60, 40, 140]);
}

// ---------- relógio (anel de progresso) ----------

function tempoDecorrido() {
  const p = PT.p;
  return p.decorridoMs + (p.relogioCorrendo ? Date.now() - PT.recebidoEm : 0);
}

function tickRelogio() {
  const p = PT.p, box = document.getElementById('ring-box'), el = document.getElementById('timer');
  if (!p || !box || !el) return;
  const total = p.duracaoSegundos * 1000;
  const decorrido = Math.min(tempoDecorrido(), total), restante = total - decorrido;
  el.textContent = fmtMs(restante);
  anelProgresso(box, restante / total);
  box.classList.toggle('paused', p.status === 'PAUSADA');
  box.classList.toggle('warn', p.status === 'EM_ANDAMENTO' && restante / total <= .25 && restante > 10000);
  box.classList.toggle('last', p.status === 'EM_ANDAMENTO' && restante <= 10000);

  // Acabou o tempo: pergunta ao servidor (que é quem encerra e decide vitória/empate).
  if (p.status === 'EM_ANDAMENTO' && restante <= 0 && !PT.sincronizando) {
    PT.sincronizando = true;
    recarregarPartida().finally(() => setTimeout(() => { PT.sincronizando = false; }, 1500));
  }
}

// ---------- desenho ----------

const STATUS_PARTIDA = { AGUARDANDO: ['Aguardando', 'gray'], EM_ANDAMENTO: ['Ao vivo', ''], PAUSADA: ['Pausada', 'gold'],
  EMPATE_PENDENTE: ['Empate', 'gold'], PENALTIS: ['Pênaltis', 'gold'], ENCERRADA: ['Encerrada', 'gray'] };

function desenharPartida(antes) {
  const p = PT.p, a = p.timeA, b = p.timeB;
  const jogando = emJogo(p);
  const [rotulo, cls] = STATUS_PARTIDA[p.status];
  const mudouA = antes && antes.timeA.gols !== a.gols, mudouB = antes && antes.timeB.gols !== b.gols;

  pintar(`
    <div class="card board" style="padding-bottom:16px">
      <div class="board-top">
        <a href="#/pelada" class="btn sm ghost">${ic('left', 16)} Pelada</a>
        <span class="chip gray">Partida ${p.ordem}</span>
        <span class="chip ${cls}">${p.status === 'EM_ANDAMENTO' ? '<span class="dot"></span>' : ''}${rotulo}</span>
      </div>
      <div class="stage"><div class="scoreline">
        ${ladoDoPlacar(a, 'a', jogando, p)}
        <div class="vs"><div class="score"><span class="digit${mudouA ? ' flip' : ''}">${a.gols}</span><span class="x">×</span><span class="digit${mudouB ? ' flip' : ''}">${b.gols}</span></div></div>
        ${ladoDoPlacar(b, 'b', jogando, p)}
      </div></div>
      <div class="ring-box" id="ring-box">${anel()}<div class="t"><div class="timer" id="timer">${fmtMs(p.duracaoSegundos * 1000)}</div>
        <small>${p.duracaoSegundos / 60} min ou ${p.golsParaEncerrar} gol${p.golsParaEncerrar === 1 ? '' : 's'}</small></div></div>
      ${controles(p)}
    </div>
    ${blocoDecisao(p)}
    ${p.status === 'PENALTIS' || (p.penaltis && p.status === 'ENCERRADA') ? blocoPenaltis(p) : ''}
    ${blocoEventos(p)}`);
  tickRelogio();
}

function ladoDoPlacar(lado, lr, jogando, p) {
  const pode = p.status === 'AGUARDANDO' || jogando;
  return `<div class="side ${lr}" style="--team:${corDoTime(lado.numero)}">
    <div class="side-3d">${escudo(lado.numero, 92, true)}</div><div class="floor"></div>
    <div class="t-name">${nomeDoTime(lado.numero)}</div>
    <div class="side-actions">
      <button class="btn goal" ${jogando ? '' : 'disabled'} onclick="abrirGol(${lado.timeId})">${ic('ball', 18)} Gols</button>
      <button class="btn" ${pode ? '' : 'disabled'} onclick="abrirEscalacao(${lado.timeId})">${ic('swap', 18)} Troca</button>
    </div></div>`;
}

function controles(p) {
  switch (p.status) {
    case 'AGUARDANDO':
      return `<button class="btn primary block lg mt-3" onclick="acaoPartida(() => POST('/partidas/${p.id}/iniciar'))">${ic('play')} Iniciar partida</button>
        <div class="center mt-3"><button class="btn sm ghost" onclick="cancelarPartida()">Cancelar esta partida</button></div>`;
    case 'EM_ANDAMENTO':
    case 'PAUSADA':
      return `<div class="dock">
        ${p.status === 'EM_ANDAMENTO'
          ? `<button class="btn" onclick="acaoPartida(() => POST('/partidas/${p.id}/pausar'))">${ic('pause')}Pausar</button>`
          : `<button class="btn primary" onclick="acaoPartida(() => POST('/partidas/${p.id}/retomar'))">${ic('play')}Retomar</button>`}
        <button class="btn" onclick="reiniciarPartida()">${ic('restart')}Reiniciar</button>
        <button class="btn danger" onclick="encerrarPartida()">${ic('stop')}Encerrar</button></div>`;
    default:
      return '';
  }
}

/** Faixas que explicam o que fazer em cada situação: empate pendente ou resultado final. */
function blocoDecisao(p) {
  if (p.status === 'EMPATE_PENDENTE') {
    return `<div class="banner warn">
      <div class="row"><span class="chip gold">${ic('handshake', 16)}</span><div><h3>Empatou em ${p.timeA.gols} × ${p.timeB.gols}</h3>
      <div class="muted small">Encerre a partida (os dois times saem) ou decida nos pênaltis.</div></div></div>
      <div class="btn-row mt-4">
        <button class="btn lg" onclick="acaoPartida(() => POST('/partidas/${p.id}/empate/encerrar'))">Encerrar</button>
        <button class="btn primary lg" onclick="abrirModoPenaltis()">${ic('goal')} Pênaltis</button></div>
      <div class="center mt-3"><button class="btn sm ghost" onclick="reiniciarPartida()">${ic('restart', 16)} Reiniciar partida</button></div>
    </div>`;
  }
  if (p.status === 'ENCERRADA') {
    const a = p.timeA, b = p.timeB;
    let titulo, sub = '';
    if (p.vencedorId) titulo = `${nomeDoTime(p.vencedorId === a.timeId ? a.numero : b.numero)} venceu`;
    else if (p.vencedorPenaltisId) {
      titulo = `Empate em ${a.gols} × ${b.gols}`;
      sub = `${nomeDoTime(p.vencedorPenaltisId === a.timeId ? a.numero : b.numero)} venceu nos pênaltis (${p.penaltis.golsA} × ${p.penaltis.golsB}) e fica na quadra.`;
    } else { titulo = `Empate em ${a.gols} × ${b.gols}`; sub = 'Partida encerrada: os dois times saem.'; }
    return `<div class="banner"><div class="row"><span class="chip">${ic('flag', 16)}</span><div><h3>${titulo}</h3>${sub ? `<div class="muted small">${esc(sub)}</div>` : ''}</div></div>
      <a class="btn primary block lg mt-4" href="#/pelada">Próxima partida ${ic('chevR')}</a></div>`;
  }
  return '';
}

// ---------- lances: linha do tempo (continuidade) ----------

function blocoEventos(p) {
  const nomeDe = (id) => esc(J(id).nome);
  const numeroDe = (timeId) => (timeId === p.timeA.timeId ? p.timeA.numero : p.timeB.numero);
  const eventos = [...p.gols.map(g => ({ ...g, tipo: 'gol' })), ...p.substituicoes.map(s => ({ ...s, tipo: 'sub' }))]
    .sort((x, y) => x.tempoMs - y.tempoMs);
  if (!eventos.length) return '';
  return `<div class="card"><div class="card-head"><h2 class="h3">Lances</h2><span class="chip gray">${eventos.length}</span></div>
    <div class="tl">${eventos.map(e => `
      <div class="tl-item" style="--dot:${corDoTime(numeroDe(e.timeId))}"><div class="tl-min">${fmtMinuto(e.tempoMs)}</div>
        <div class="row">${escudo(numeroDe(e.timeId), 28)}
          ${e.tipo === 'gol'
            ? `<div class="grow"><b>${ic('ball', 15)} ${nomeDe(e.autorId)}</b><div class="small muted">${e.assistenteId ? 'Assistência: ' + nomeDe(e.assistenteId) : 'Sem assistência'}</div></div>
               ${emJogo(p) ? `<button class="btn icon sm ghost" aria-label="Desfazer gol" onclick="desfazerGol(${e.id})">${ic('trash', 16)}</button>` : ''}`
            : `<div class="grow"><b>${ic('swap', 15)} Entra ${nomeDe(e.entrouId)}</b><div class="small muted">${e.saiuId ? 'Sai ' + nomeDe(e.saiuId) : 'Reforço (ninguém saiu)'}</div></div>`}
        </div></div>`).join('')}</div></div>`;
}

function desfazerGol(golId) {
  confirmar('Desfazer gol', 'Remover este gol do placar?', 'Desfazer', () => acaoPartida(() => DEL(`/partidas/${PT.p.id}/gols/${golId}`), 'Gol desfeito.'));
}

// ---------- controles com confirmação ----------

function reiniciarPartida() {
  confirmar('Reiniciar partida?', 'O cronômetro e o placar voltam a zero (os gols lançados serão apagados). Você poderá iniciar de novo.',
    'Reiniciar', () => acaoPartida(() => POST(`/partidas/${PT.p.id}/reiniciar`), 'Partida reiniciada.'));
}
function encerrarPartida() {
  const p = PT.p;
  confirmar('Encerrar partida agora?', `Vale o placar atual: Time ${p.timeA.numero} ${p.timeA.gols} × ${p.timeB.gols} Time ${p.timeB.numero}.`,
    'Encerrar', () => acaoPartida(() => POST(`/partidas/${p.id}/encerrar`)));
}
function cancelarPartida() {
  confirmar('Cancelar partida?', 'Ela ainda não começou e será descartada.', 'Cancelar partida', async () => {
    try { await DEL(`/partidas/${PT.p.id}`); location.hash = '#/pelada'; } catch (e) { erroToast(e); }
  });
}

// ---------- gol: autor → assistente ----------

function ladoPorTime(timeId) { return PT.p.timeA.timeId === timeId ? PT.p.timeA : PT.p.timeB; }
function emCampoDo(timeId) { return ladoPorTime(timeId).jogadores.filter(a => a.emCampo); }

function abrirGol(timeId) {
  PT.gol = { timeId, autorId: null };
  const lado = ladoPorTime(timeId);
  abrirSheet(`Gol · ${nomeDoTime(lado.numero)}`, `<div class="step"><b>1 de 2</b> · Quem fez o gol?</div>
    <div class="pick-grid">${emCampoDo(timeId).map(a => cartaoPick(a.jogadorId, `escolherAutor(${a.jogadorId})`, a.goleiro)).join('')}</div>`);
}

function cartaoPick(jogadorId, acao, goleiro, extra) {
  const j = J(jogadorId);
  return `<button class="pick" onclick="${acao}">${avatar(j, 'lg')}
    <span class="p-name">${esc(j.nome)}${goleiro ? ` ${ic('glove', 13)}` : ''}</span>${extra ? `<span class="badge">${extra}</span>` : ''}</button>`;
}

function escolherAutor(autorId) {
  PT.gol.autorId = autorId;
  const outros = emCampoDo(PT.gol.timeId).filter(a => a.jogadorId !== autorId);
  document.getElementById('sheet-body').innerHTML = `
    <div class="step"><b>2 de 2</b> · Gol de <b>${esc(J(autorId).nome)}</b>. Quem deu a assistência?</div>
    <div class="pick-grid">
      <button class="pick none" onclick="registrarGol(null)"><span style="font-size:26px">${ic('x', 26)}</span><span class="p-name">Sem assistência</span></button>
      ${outros.map(a => cartaoPick(a.jogadorId, `registrarGol(${a.jogadorId})`, a.goleiro)).join('')}
    </div>`;
}

function registrarGol(assistenteId) {
  const g = PT.gol;
  acaoPartida(() => POST(`/partidas/${PT.p.id}/gols`, { timeId: g.timeId, autorId: g.autorId, assistenteId }));
}

// ---------- escalação: substituição, reforço e goleiro ----------

async function abrirEscalacao(timeId) {
  try { if (!PT.pelada || PT.pelada.id !== PT.p.peladaId) PT.pelada = await GET(`/peladas/${PT.p.peladaId}`); } catch (e) { return erroToast(e); }
  desenharEscalacao(timeId);
}

function desenharEscalacao(timeId) {
  const lado = ladoPorTime(timeId), jogadores = emCampoDo(timeId);
  abrirSheet(`Escalação · ${nomeDoTime(lado.numero)}`, `
    <div class="small muted" style="margin-bottom:8px">Toque na luva para definir quem está no gol. Use <b>Trocar</b> quando alguém se machucar ou os times mudarem.</div>
    ${jogadores.length ? jogadores.map(a => { const j = J(a.jogadorId); return `
      <div class="p-row">${avatar(j, 'sm')}<span class="p-name">${esc(j.nome)}</span>
        <button class="btn sm icon ${a.goleiro ? 'primary' : ''}" aria-label="Goleiro" onclick="definirGoleiro(${timeId}, ${a.goleiro ? 'null' : a.jogadorId})">${ic('glove', 18)}</button>
        <button class="btn sm" onclick="escolherEntrada(${timeId}, ${a.jogadorId})">${ic('swap', 16)} Trocar</button></div>`; }).join('')
      : '<div class="empty" style="padding:12px">Ninguém em campo neste time.</div>'}
    <button class="btn block mt-4" onclick="escolherEntrada(${timeId}, null)">${ic('plus')} Reforço (entra sem ninguém sair)</button>`);
}

function definirGoleiro(timeId, jogadorId) {
  acaoPartidaSemFechar(() => PUT(`/partidas/${PT.p.id}/goleiro`, { timeId, jogadorId }), () => desenharEscalacao(timeId),
    jogadorId ? `${J(jogadorId).nome} está no gol.` : 'Time sem goleiro fixo.');
}
async function acaoPartidaSemFechar(fn, depois, mensagem) {
  try { const p = await fn(); aplicarPartida(p); depois(); if (mensagem) toast(mensagem); } catch (e) { erroToast(e); }
}

function escolherEntrada(timeId, saiuId) {
  const p = PT.p;
  const emCampoAgora = new Set([...p.timeA.jogadores, ...p.timeB.jogadores].filter(a => a.emCampo).map(a => a.jogadorId));
  const timeOrigem = new Map();
  PT.pelada.times.forEach(t => t.jogadores.forEach(i => timeOrigem.set(i.jogadorId, t.numero)));
  const candidatos = PT.pelada.lista.filter(id => !emCampoAgora.has(id)).sort((x, y) => J(x).nome.localeCompare(J(y).nome, 'pt-BR'));
  document.getElementById('sheet-body').innerHTML = `
    <div class="step">${saiuId ? `Sai <b>${esc(J(saiuId).nome)}</b>.` : 'Reforço.'} <b>Quem entra?</b></div>
    ${candidatos.length ? `<div class="pick-grid">${candidatos.map(id =>
      cartaoPick(id, `substituir(${timeId}, ${saiuId}, ${id})`, J(id).goleiro, timeOrigem.has(id) ? nomeDoTime(timeOrigem.get(id)) : 'Sem time')).join('')}</div>`
      : '<div class="empty">Não há mais ninguém na lista fora de campo.<br>Adicione o jogador na lista da pelada.</div>'}
    <button class="btn block mt-4" onclick="desenharEscalacao(${timeId})">${ic('left', 16)} Voltar</button>`;
}

function substituir(timeId, saiuId, entrouId) {
  acaoPartida(() => POST(`/partidas/${PT.p.id}/substituicoes`, { timeId, saiuId, entrouId }),
    `Entra ${J(entrouId).nome}${saiuId ? ', sai ' + J(saiuId).nome : ''}.`);
}

// ---------- pênaltis: caixa morfológica (tipo × cobranças) ----------

function abrirModoPenaltis() {
  const p = PT.p;
  const normal = Math.max(1, emCampoDo(p.timeA.timeId).length, emCampoDo(p.timeB.timeId).length);
  abrirSheet('Disputa de pênaltis', `
    <div class="step">Combine <b>tipo</b> e <b>quantidade</b>. Empatou nas cobranças previstas? Seguem as <b>alternadas</b>.</div>
    ${caixa('penaltis', {
      linhas: [
        { k: 'tipo', rotulo: 'Tipo', v: 'NORMAL', fmt: v => ({ NORMAL: 'Normal', PREDETERMINADO: 'Pré-determinada', ALTERNADO: 'Alternadas' })[v],
          opcoes: [{ v: 'NORMAL', r: 'Normal', sub: `todos · ${normal}/time` }, { v: 'PREDETERMINADO', r: 'Pré-determ.', sub: 'você escolhe' }, { v: 'ALTERNADO', r: 'Alternadas', sub: '1 por vez' }] },
        { k: 'qtd', rotulo: 'Cobranças', dica: 'por time', v: 3, custom: { min: 1, max: 20 }, fmt: v => `${v} por time`,
          ativa: s => s.tipo === 'PREDETERMINADO', opcoes: [1, 2, 3, 4, 5].map(v => ({ v, r: v })) }
      ],
      resumo: s => s.tipo === 'NORMAL' ? `<span class="chip">Normal · ${normal} por time</span>`
        : s.tipo === 'ALTERNADO' ? '<span class="chip">Alternadas</span>' : `<span class="chip">Pré-determinada · ${s.qtd} por time</span>`
    })}
    <div class="small muted mt-3">${ic('info', 14)} Termina antes se um time não puder mais ser alcançado. Ninguém repete antes de o time inteiro bater.</div>
    <button class="btn primary block lg mt-4" onclick="iniciarPenaltis()">${ic('goal')} Começar disputa</button>`);
}

function iniciarPenaltis() {
  const s = cxEstado('penaltis'), corpo = { modo: s.tipo };
  if (s.tipo === 'PREDETERMINADO') corpo.quantidade = s.qtd;
  acaoPartida(() => POST(`/partidas/${PT.p.id}/penaltis/iniciar`, corpo));
}

function rotuloModoPenaltis(pen) {
  if (pen.modo === 'NORMAL') return `Normal · ${pen.rodadas} por time`;
  if (pen.modo === 'PREDETERMINADO') return `Pré-determinada · ${pen.rodadas} por time`;
  return 'Alternadas';
}

function blocoPenaltis(p) {
  const pen = p.penaltis, a = p.timeA, b = p.timeB;
  const ativo = p.status === 'PENALTIS', decidido = pen.decididoParaTimeId;
  const nomeTime = (timeId) => nomeDoTime(timeId === a.timeId ? a.numero : b.numero);

  // Pontinhos com o resultado de cada cobrança (o total continua contando mesmo quando o ciclo zera).
  const pontos = (timeId) => {
    const cobr = pen.cobrancas.filter(c => c.timeId === timeId);
    const slots = pen.alternadas ? cobr.length : Math.max(pen.rodadas, cobr.length);
    return Array.from({ length: slots }, (_, i) => `<i class="${cobr[i] ? (cobr[i].convertido ? 'ok' : 'no') : ''}"></i>`).join('');
  };

  const coluna = (lado, batidos, gols, ciclo) => {
    const vez = ativo && !decidido && pen.proximoTimeId === lado.timeId;
    const doCiclo = new Set(ciclo), jogadores = lado.jogadores.filter(x => x.emCampo);
    return `<div class="pen-col ${vez ? 'turn' : ''}" style="--team:${corDoTime(lado.numero)}">
      <h4>${escudo(lado.numero, 28)} ${nomeDoTime(lado.numero)}</h4>
      <div class="pen-count"><b>${batidos}</b> pênalti${batidos === 1 ? '' : 's'} batido${batidos === 1 ? '' : 's'} · ${gols} gol${gols === 1 ? '' : 's'}</div>
      <div class="pen-dots">${pontos(lado.timeId)}</div>
      ${jogadores.map(x => {
        const jaBateu = doCiclo.has(x.jogadorId), ult = [...pen.cobrancas].reverse().find(c => c.jogadorId === x.jogadorId);
        const acoes = vez && !jaBateu
          ? `<button class="btn sm primary" aria-label="Fez o gol" onclick="cobrar(${x.jogadorId}, true)">${ic('ball', 16)}</button>
             <button class="btn sm" aria-label="Perdeu" onclick="cobrar(${x.jogadorId}, false)">${ic('x', 16)}</button>`
          : `<span class="pen-mark">${jaBateu && ult ? (ult.convertido ? ic('check', 16) : ic('x', 16)) : ''}</span>`;
        return `<div class="pen-p ${jaBateu ? 'done' : ''}">${avatar(J(x.jogadorId), 'xs')}<span class="p-name">${esc(J(x.jogadorId).nome)}</span>${acoes}</div>`;
      }).join('')}
      ${ativo && !decidido && jogadores.length && ciclo.length === 0 && batidos > 0 ? '<div class="small muted mt-2">Todos já bateram: novo ciclo.</div>' : ''}
    </div>`;
  };

  return `<div class="card">
    <div class="card-head"><h2 class="h3">${ic('goal')} Disputa de pênaltis</h2><div class="rank-val">${pen.golsA} × ${pen.golsB}</div></div>
    <div class="row wrap gap-2" style="margin-bottom:12px"><span class="chip gray">${esc(rotuloModoPenaltis(pen))}</span>
      ${pen.alternadas && pen.modo !== 'ALTERNADO' ? '<span class="chip gold">Alternadas (desempate)</span>' : ''}</div>
    ${ativo ? (decidido
      ? `<div class="banner"><h3>${ic('trophy', 20)} ${nomeTime(decidido)} venceu nos pênaltis</h3>
          <div class="muted small" style="margin-bottom:12px">Confirme para fechar a partida (o vencedor fica na quadra). Lançou errado? Desfaça a última cobrança.</div>
          <button class="btn primary block lg" onclick="acaoPartida(() => POST('/partidas/${p.id}/penaltis/encerrar'))">Confirmar e encerrar</button></div>`
      : `<div class="step">Cobrança <b>${pen.cobrancas.length + 1}</b> · vez do <b>${nomeTime(pen.proximoTimeId)}</b>${pen.alternadas ? ' (alternadas)' : ''}. Marque quem bateu.</div>`) : ''}
    <div class="pen-cols">${coluna(a, pen.cobrancasA, pen.golsA, pen.cicloA)}${coluna(b, pen.cobrancasB, pen.golsB, pen.cicloB)}</div>
    ${ativo && pen.cobrancas.length ? `<div class="center mt-4"><button class="btn sm ghost" onclick="acaoPartida(() => DEL('/partidas/${p.id}/penaltis/ultimo'), 'Última cobrança desfeita.')">${ic('undo', 16)} Desfazer última cobrança</button></div>` : ''}
  </div>`;
}

function cobrar(jogadorId, convertido) {
  acaoPartida(() => POST(`/partidas/${PT.p.id}/penaltis`, { jogadorId, convertido }), convertido ? 'Gol!' : 'Perdeu…');
}
