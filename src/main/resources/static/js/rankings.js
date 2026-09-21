// ---------- Aba Rankings: pódio em perspectiva, barras e rede de assistências ----------

const RK = { periodo: 'MES', ano: new Date().getFullYear(), mes: new Date().getMonth() + 1, cat: 'vencedores', dados: null, rede: null, grafo: null };

const CATEGORIAS = [
  { id: 'vencedores', nome: 'Vencedores', icone: 'trophy', unidade: 'vitórias' },
  { id: 'goleadores', nome: 'Artilheiros', icone: 'ball', unidade: 'gols' },
  { id: 'assistentes', nome: 'Assistências', icone: 'target', unidade: 'ass.' },
  { id: 'goleiros', nome: 'Goleiros', icone: 'glove', unidade: 'sem sofrer' },
  { id: 'notas', nome: 'Notas', icone: 'star', unidade: 'nota' },
  { id: 'overalls', nome: 'Overall', icone: 'trend', unidade: 'overall' },
  { id: 'conexoes', nome: 'Conexões', icone: 'network', unidade: '' }
];

async function telaRankings() {
  await carregarJogadores();
  await carregarRanking();
}

function urlPeriodo(base) {
  let url = `${base}?periodo=${RK.periodo}`;
  if (RK.periodo === 'MES') url += `&ano=${RK.ano}&mes=${RK.mes}`;
  if (RK.periodo === 'ANO') url += `&ano=${RK.ano}`;
  return url;
}

async function carregarRanking() {
  const [dados, rede] = await Promise.all([GET(urlPeriodo('/rankings')), GET(urlPeriodo('/rankings/conexoes'))]);
  RK.dados = dados; RK.rede = rede;
  desenharRanking();
}

function rotuloPeriodo() {
  if (RK.periodo === 'MES') return `${capital(MESES[RK.mes - 1])} de ${RK.ano}`;
  if (RK.periodo === 'ANO') return String(RK.ano);
  return 'Todas as peladas';
}
function mudarPeriodo(p) { RK.periodo = p; carregarRanking().catch(erroToast); }
function moverPeriodo(delta) {
  if (RK.periodo === 'MES') {
    RK.mes += delta;
    if (RK.mes < 1) { RK.mes = 12; RK.ano--; } else if (RK.mes > 12) { RK.mes = 1; RK.ano++; }
  } else RK.ano += delta;
  vibrar(8);
  carregarRanking().catch(erroToast);
}
function mudarCategoria(c) { RK.cat = c; vibrar(8); desenharRanking(); }

function desenharRanking() {
  if (RK.grafo) { RK.grafo.destruir(); RK.grafo = null; }
  const d = RK.dados, cat = CATEGORIAS.find(c => c.id === RK.cat);
  const periodos = [['MES', 'Mês'], ['ANO', 'Ano'], ['GERAL', 'Geral']];

  pintar(`
    <div style="margin:2px 2px 14px"><div class="eyebrow">Rankings</div><h1 class="h1 mt-2">${esc(rotuloPeriodo())}</h1>
      <div class="small muted mt-2">${d.peladas} pelada${d.peladas === 1 ? '' : 's'} no período</div></div>
    <div class="seg" style="--n:3;--idx:${periodos.findIndex(x => x[0] === RK.periodo)}"><span class="seg-pill"></span>
      ${periodos.map(([id, nome]) => `<button class="${RK.periodo === id ? 'on' : ''}" onclick="mudarPeriodo('${id}')">${nome}</button>`).join('')}</div>
    ${RK.periodo === 'GERAL' ? '' : `<div class="row between" style="margin:-4px 0 10px">
      <button class="btn icon sm" aria-label="Anterior" onclick="moverPeriodo(-1)">${ic('chevL')}</button>
      <span class="small muted">Navegar entre ${RK.periodo === 'MES' ? 'meses' : 'anos'}</span>
      <button class="btn icon sm" aria-label="Próximo" onclick="moverPeriodo(1)">${ic('chevR')}</button></div>`}
    <div class="tabs">${CATEGORIAS.map(c => `<button class="${RK.cat === c.id ? 'on' : ''}" onclick="mudarCategoria('${c.id}')">${ic(c.icone, 16)} ${c.nome}</button>`).join('')}</div>
    ${RK.cat === 'conexoes' ? blocoRede() : blocoRanking(d[RK.cat], cat)}`);
  if (RK.cat === 'conexoes') montarRede();
}

// ---------- pódio (perspectiva) + lista com barras ----------

function blocoRanking(itens, cat) {
  if (!itens.length) return `<div class="card"><div class="empty">${arteVazia()}Sem dados neste período.<br>Os rankings aparecem depois que uma pelada for finalizada.</div></div>`;
  const top = itens.slice(0, 3), resto = itens.slice(3);
  const max = cat.id === 'overalls' ? 99 : cat.id === 'notas' ? 10 : Math.max(1, ...itens.map(i => i.valor));
  const min = cat.id === 'overalls' ? 40 : 0;
  const valorTxt = (i) => cat.id === 'notas' ? nota1(i.valor) : Math.round(i.valor);
  const medalha = ['var(--gold)', '#C4D0DB', '#D8956A'];
  // ordem visual do pódio: 2º · 1º · 3º
  const ordem = [1, 0, 2].filter(k => top[k]);
  const alturas = [150, 112, 84];
  return `<div class="card hero">
      <div class="podium">${ordem.map(k => { const i = top[k], j = J(i.jogadorId); return `
        <div class="pod" style="--medal:${medalha[k]}">
          ${k === 0 ? `<span class="crown">${ic('crown', 22)}</span>` : ''}
          ${avatar(j, k === 0 ? 'lg' : '')}
          <div class="nm ellipsis">${esc(i.nome.split(' ')[0])}</div>
          <div class="val">${cat.id === 'overalls' ? ovr(Math.round(i.valor), true) : valorTxt(i)}</div>
          <div class="pod-step" style="--h:${alturas[k]}px;--k:${k}">${k + 1}</div></div>`; }).join('')}</div>
      ${resto.map((i, k) => `
        <div class="rank-row"><span class="rank-pos">${k + 4}</span>${avatar(J(i.jogadorId), 'sm')}
          <div class="grow"><div class="p-name">${esc(i.nome)}${J(i.jogadorId).goleiro ? ` <span class="faint">${ic('glove', 14)}</span>` : ''}
            <span class="p-sub">${cat.id === 'overalls' ? `<span class="${i.detalhe.startsWith('▲') ? 'delta-up' : i.detalhe.startsWith('▼') ? 'delta-down' : ''}">${esc(i.detalhe)}</span> · ${i.partidas} jogos` : esc(i.detalhe)}</span></div>
            <div class="bar"><i style="--w:${Math.max(4, (i.valor - min) / (max - min) * 100).toFixed(0)}%;--k:${k}"></i></div></div>
          ${cat.id === 'overalls' ? ovr(Math.round(i.valor)) : `<span class="rank-val">${valorTxt(i)}</span>`}</div>`).join('')}
    </div>`;
}

// ---------- rede de interação: quem serve quem ----------

function blocoRede() {
  const r = RK.rede;
  if (!r.arestas.length) return `<div class="card"><div class="empty">${arteVazia()}Sem assistências neste período.<br>A rede aparece quando houver gols com assistência.</div></div>`;
  const total = r.arestas.reduce((s, a) => s + a.quantidade, 0);
  const melhor = r.arestas[0];
  return `<div class="card" style="padding:0;overflow:hidden">
      <div style="padding:14px 16px 10px"><h2 class="h3">${ic('network')} Rede de assistências</h2>
        <div class="small muted mt-2">${total} gol${total === 1 ? '' : 's'} com assistência · ${r.arestas.length} dupla${r.arestas.length === 1 ? '' : 's'}. Arraste os jogadores; toque para destacar as conexões.</div></div>
      <div class="graph-wrap" style="border:0;border-radius:0;border-top:1px solid var(--line)"><svg id="grafo" viewBox="0 0 360 440" role="img" aria-label="Rede de assistências"></svg></div>
      <div class="graph-info" id="grafo-info"><b>Melhor dupla:</b> ${esc(J(melhor.assistenteId).nome)} <span class="faint">→</span> ${esc(J(melhor.autorId).nome)} · ${melhor.quantidade} gol${melhor.quantidade === 1 ? '' : 's'}</div>
      <div class="graph-legend"><span><i style="width:18px;height:3px;background:var(--brand);border-radius:3px"></i>espessura = gols da dupla</span>
        <span>→ assistente para artilheiro</span><span>tamanho = gols</span><span>anel = nível</span></div>
    </div>`;
}

function montarRede() {
  const svg = document.getElementById('grafo');
  if (!svg) return;
  const r = RK.rede;
  const nos = r.nos.map(n => ({ id: n.jogadorId, nome: n.nome, gols: n.gols, assistencias: n.assistencias, overall: n.overall, foto: J(n.jogadorId).foto }));
  const arestas = r.arestas.map(a => ({ de: a.assistenteId, para: a.autorId, q: a.quantidade }));
  RK.grafo = new Rede(svg, nos, arestas, {
    W: 360, H: 440,
    onSelecionar: (n, ligacoes) => {
      const info = document.getElementById('grafo-info');
      if (!info) return;
      if (!n) { const m = r.arestas[0]; info.innerHTML = `<b>Melhor dupla:</b> ${esc(J(m.assistenteId).nome)} <span class="faint">→</span> ${esc(J(m.autorId).nome)} · ${m.quantidade} gol${m.quantidade === 1 ? '' : 's'}`; return; }
      const recebe = ligacoes.filter(l => l.para === n.id).map(l => `${esc(J(l.de).nome.split(' ')[0])} (${l.q})`);
      const serve = ligacoes.filter(l => l.de === n.id).map(l => `${esc(J(l.para).nome.split(' ')[0])} (${l.q})`);
      info.innerHTML = `<b>${esc(n.nome)}</b> · ${n.gols} gol${n.gols === 1 ? '' : 's'} · ${n.assistencias} assist.<br>
        ${recebe.length ? `<span class="muted">Recebe de:</span> ${recebe.join(', ')}. ` : ''}${serve.length ? `<span class="muted">Serve:</span> ${serve.join(', ')}.` : ''}
        ${!recebe.length && !serve.length ? '<span class="muted">Sem conexões neste período.</span>' : ''}`;
    }
  });
}
