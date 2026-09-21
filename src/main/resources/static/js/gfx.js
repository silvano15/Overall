// =====================================================================
//  Overall — motor gráfico
//  · matrizes 4×4 (perspectiva, rotação, translação, escala) → matrix3d do CSS
//  · tilt 3D das cartas · radar (matriz de rotação 2D) · anel do cronômetro · confete · grafo de forças
// =====================================================================

const REDUZIDO = !!(window.matchMedia && matchMedia('(prefers-reduced-motion: reduce)').matches);

// ---------- matrizes 4×4 (linha-maior; o CSS espera coluna-maior, ver M.css) ----------
const M = {
  I: () => [1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1],
  mul(a, b) {
    const r = new Array(16).fill(0);
    for (let i = 0; i < 4; i++) for (let j = 0; j < 4; j++) {
      let s = 0;
      for (let k = 0; k < 4; k++) s += a[i * 4 + k] * b[k * 4 + j];
      r[i * 4 + j] = s;
    }
    return r;
  },
  /** Projeção em perspectiva com ponto de fuga a distância d (igual a `perspective(d)` do CSS). */
  perspective(d) { const m = M.I(); m[14] = -1 / d; return m; },
  rotX(g) { const t = g * Math.PI / 180, c = Math.cos(t), s = Math.sin(t); return [1, 0, 0, 0, 0, c, -s, 0, 0, s, c, 0, 0, 0, 0, 1]; },
  rotY(g) { const t = g * Math.PI / 180, c = Math.cos(t), s = Math.sin(t); return [c, 0, s, 0, 0, 1, 0, 0, -s, 0, c, 0, 0, 0, 0, 1]; },
  rotZ(g) { const t = g * Math.PI / 180, c = Math.cos(t), s = Math.sin(t); return [c, -s, 0, 0, s, c, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1]; },
  trans(x, y, z = 0) { return [1, 0, 0, x, 0, 1, 0, y, 0, 0, 1, z, 0, 0, 0, 1]; },
  scale(sx, sy = sx, sz = 1) { return [sx, 0, 0, 0, 0, sy, 0, 0, 0, 0, sz, 0, 0, 0, 0, 1]; },
  /** Compõe várias matrizes na ordem em que são aplicadas ao objeto: M.chain(A, B, C) = A·B·C. */
  chain(...ms) { return ms.reduce((acc, m) => M.mul(acc, m)); },
  css(m) {
    const c = [];
    for (let col = 0; col < 4; col++) for (let row = 0; row < 4; row++) c.push(+m[row * 4 + col].toFixed(5));
    return `matrix3d(${c.join(',')})`;
  }
};

// ---------- tilt 3D: a carta gira em direção ao ponteiro, com reflexo ----------
function tilt(el, opcoes = {}) {
  if (REDUZIDO || el._tilt) return;
  el._tilt = true;
  const max = opcoes.max ?? 13, escala = opcoes.escala ?? 1.035;
  const mover = (e) => {
    const r = el.getBoundingClientRect();
    const px = Math.min(1, Math.max(0, (e.clientX - r.left) / r.width));
    const py = Math.min(1, Math.max(0, (e.clientY - r.top) / r.height));
    const ry = (px - .5) * 2 * max, rx = -(py - .5) * 2 * max;
    el.style.transition = 'transform .08s linear, box-shadow .3s';
    el.style.transform = M.css(M.chain(M.perspective(820), M.rotX(rx), M.rotY(ry), M.scale(escala)));
    el.style.setProperty('--mx', (px * 100).toFixed(1) + '%');
    el.style.setProperty('--my', (py * 100).toFixed(1) + '%');
    el.style.setProperty('--glare', '1');
  };
  const soltar = () => {
    el.style.transition = '';
    el.style.transform = '';
    el.style.setProperty('--glare', '0');
  };
  el.addEventListener('pointermove', mover);
  el.addEventListener('pointerleave', soltar);
  el.addEventListener('pointercancel', soltar);
  el.addEventListener('pointerup', () => setTimeout(soltar, 120));
}
function montarFx(raiz) { (raiz || document).querySelectorAll('[data-tilt]').forEach(el => tilt(el)); }

// ---------- contador animado ----------
function contar(el, ate, ms = 800, de = 0) {
  if (!el) return;
  if (REDUZIDO) { el.textContent = ate; return; }
  const t0 = performance.now();
  const passo = (t) => {
    const k = Math.min(1, (t - t0) / ms), e = 1 - Math.pow(1 - k, 3);
    el.textContent = Math.round(de + (ate - de) * e);
    if (k < 1) requestAnimationFrame(passo);
  };
  requestAnimationFrame(passo);
}

// ---------- radar: cada vértice é o vetor (0, −r) girado pela matriz de rotação 2D R(θ) ----------
function radar(valores, rotulos, opcoes = {}) {
  const n = valores.length, S = opcoes.tam || 260, c = S / 2, R = S / 2 - 40;
  const girar = (i, r) => {                       // R(θ)·(0,−r) = (r·sen θ, −r·cos θ)
    const t = i * 2 * Math.PI / n;
    return [c + r * Math.sin(t), c - r * Math.cos(t)];
  };
  const poly = (f) => valores.map((_, i) => girar(i, R * f).map(v => v.toFixed(1)).join(',')).join(' ');
  const dados = valores.map((v, i) => girar(i, R * Math.max(.04, Math.min(1, v / 99))));
  const rot = rotulos.map((r, i) => {
    const [x, y] = girar(i, R + 18);
    const anc = Math.abs(x - c) < 6 ? 'middle' : x > c ? 'start' : 'end';
    return `<text x="${x.toFixed(1)}" y="${(y + 3).toFixed(1)}" text-anchor="${anc}">${r}</text>
            <text class="v" x="${x.toFixed(1)}" y="${(y + 15).toFixed(1)}" text-anchor="${anc}">${Math.round(valores[i])}</text>`;
  }).join('');
  return `<svg class="radar" viewBox="0 0 ${S} ${S}" role="img" aria-label="Atributos do jogador">
    ${[.25, .5, .75, 1].map(f => `<polygon class="grid" points="${poly(f)}"/>`).join('')}
    ${valores.map((_, i) => { const [x, y] = girar(i, R); return `<line class="axis" x1="${c}" y1="${c}" x2="${x.toFixed(1)}" y2="${y.toFixed(1)}"/>`; }).join('')}
    <g style="transform-origin:${c}px ${c}px" class="shape-g">
      <polygon class="shape" style="transform-origin:${c}px ${c}px" points="${dados.map(p => p.map(v => v.toFixed(1)).join(',')).join(' ')}"/>
      ${dados.map(p => `<circle class="pt" cx="${p[0].toFixed(1)}" cy="${p[1].toFixed(1)}" r="3.4"/>`).join('')}
    </g>${rot}</svg>`;
}

// ---------- anel de progresso (cronômetro) ----------
function anel(tam = 220, traco = 11) {
  const r = (tam - traco) / 2, circ = 2 * Math.PI * r;
  return `<svg class="ring" viewBox="0 0 ${tam} ${tam}" aria-hidden="true" data-circ="${circ}">
    <defs><linearGradient id="rg" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="var(--brand)"/><stop offset="1" stop-color="var(--brand-2)"/></linearGradient></defs>
    <circle class="ring-track" cx="${tam / 2}" cy="${tam / 2}" r="${r}"/>
    <circle class="ring-bar" cx="${tam / 2}" cy="${tam / 2}" r="${r}" stroke-dasharray="${circ}" stroke-dashoffset="0" transform="rotate(-90 ${tam / 2} ${tam / 2})"/>
  </svg>`;
}
function anelProgresso(raiz, fracao) {
  const svg = raiz.querySelector('.ring'), bar = raiz.querySelector('.ring-bar');
  if (!svg || !bar) return;
  const circ = parseFloat(svg.dataset.circ);
  bar.style.strokeDashoffset = (circ * (1 - Math.max(0, Math.min(1, fracao)))).toFixed(2);
}

// ---------- confete ----------
function confete(x, y, cores, quantidade = 110) {
  if (REDUZIDO) return;
  let cv = document.getElementById('fx');
  if (!cv) { cv = document.createElement('canvas'); cv.id = 'fx'; document.body.appendChild(cv); }
  const dpr = window.devicePixelRatio || 1;
  cv.width = innerWidth * dpr; cv.height = innerHeight * dpr;
  const ctx = cv.getContext('2d'); ctx.scale(dpr, dpr);
  const paleta = cores && cores.length ? cores : ['#2CE58D', '#D9FF3F', '#F3CD70', '#5CB2FF', '#FF5E6E'];
  const ps = Array.from({ length: quantidade }, () => {
    const a = -Math.PI / 2 + (Math.random() - .5) * 2.4, v = 6 + Math.random() * 11;
    return { x, y, vx: Math.cos(a) * v, vy: Math.sin(a) * v, w: 5 + Math.random() * 7, h: 3 + Math.random() * 5,
      r: Math.random() * 6, vr: (Math.random() - .5) * .4, c: paleta[(Math.random() * paleta.length) | 0], vida: 1 };
  });
  const passo = () => {
    ctx.clearRect(0, 0, innerWidth, innerHeight);
    let vivos = 0;
    for (const p of ps) {
      p.vy += .32; p.vx *= .985; p.vy *= .992; p.x += p.vx; p.y += p.vy; p.r += p.vr; p.vida -= .009;
      if (p.vida <= 0 || p.y > innerHeight + 30) continue;
      vivos++;
      ctx.save(); ctx.globalAlpha = Math.min(1, p.vida * 1.6); ctx.translate(p.x, p.y); ctx.rotate(p.r);
      ctx.fillStyle = p.c; ctx.fillRect(-p.w / 2, -p.h / 2, p.w, p.h); ctx.restore();
    }
    if (vivos) requestAnimationFrame(passo); else cv.remove();
  };
  requestAnimationFrame(passo);
}

// ---------- rede de interação: layout por forças (repulsão de Coulomb + molas nas arestas) ----------
class Rede {
  /**
   * @param svg     elemento <svg viewBox="0 0 W H">
   * @param nos     [{id, nome, gols, assistencias, overall, foto}]
   * @param arestas [{de, para, q}]  (de = quem serviu, para = quem marcou)
   */
  constructor(svg, nos, arestas, opcoes = {}) {
    this.svg = svg; this.W = opcoes.W || 360; this.H = opcoes.H || 380; this.onSelecionar = opcoes.onSelecionar || (() => {});
    this.nos = nos.map((n, i) => {
      const ang = i * 2 * Math.PI / Math.max(1, nos.length);
      return { ...n, x: this.W / 2 + Math.cos(ang) * 90, y: this.H / 2 + Math.sin(ang) * 90, vx: 0, vy: 0,
        r: 15 + Math.min(n.gols, 10) * 1.3, fixo: false };
    });
    this.mapa = new Map(this.nos.map(n => [n.id, n]));
    this.arestas = arestas.filter(a => this.mapa.has(a.de) && this.mapa.has(a.para)).map(a => ({ ...a, a: this.mapa.get(a.de), b: this.mapa.get(a.para) }));
    this.sel = null;
    this.montar();
    for (let i = 0; i < 260; i++) this.passo(); // pré-estabiliza: já abre em equilíbrio
    this.desenhar();
    this.anima(REDUZIDO ? 0 : 90);
  }

  passo() {
    const N = this.nos, cx = this.W / 2, cy = this.H / 2;
    for (let i = 0; i < N.length; i++) {
      for (let j = i + 1; j < N.length; j++) {
        const a = N[i], b = N[j];
        let dx = a.x - b.x, dy = a.y - b.y, d2 = dx * dx + dy * dy || .01, d = Math.sqrt(d2);
        const f = 9200 / d2, fx = dx / d * f, fy = dy / d * f;        // repulsão: k / d²
        a.vx += fx; a.vy += fy; b.vx -= fx; b.vy -= fy;
      }
    }
    for (const e of this.arestas) {                                    // mola: quanto mais assistências, mais curta
      const dx = e.b.x - e.a.x, dy = e.b.y - e.a.y, d = Math.sqrt(dx * dx + dy * dy) || .01;
      const alvo = Math.max(78, 150 - e.q * 16), f = (d - alvo) * .03;
      e.a.vx += dx / d * f; e.a.vy += dy / d * f; e.b.vx -= dx / d * f; e.b.vy -= dy / d * f;
    }
    for (const n of N) {
      n.vx += (cx - n.x) * .007; n.vy += (cy - n.y) * .007;            // gravidade ao centro
      if (n.fixo) { n.vx = n.vy = 0; continue; }
      n.vx *= .82; n.vy *= .82;
      n.x = Math.max(n.r + 12, Math.min(this.W - n.r - 12, n.x + n.vx));
      n.y = Math.max(n.r + 12, Math.min(this.H - n.r - 22, n.y + n.vy));
    }
  }

  anima(frames) {
    cancelAnimationFrame(this._raf);
    let k = frames;
    const loop = () => { this.passo(); this.atualizar(); if (--k > 0 || this.arrastando) this._raf = requestAnimationFrame(loop); };
    if (frames > 0) this._raf = requestAnimationFrame(loop);
  }

  montar() {
    const defs = `<defs>
      <marker id="seta" viewBox="0 0 10 10" refX="8" refY="5" markerUnits="userSpaceOnUse" markerWidth="9" markerHeight="9" orient="auto-start-reverse"><path d="M0 0L10 5L0 10z" fill="var(--brand)"/></marker>
      ${this.nos.map(n => `<clipPath id="cp-${n.id}"><circle r="${n.r}"/></clipPath>`).join('')}</defs>`;
    const arestas = this.arestas.map((e, i) => `<path class="g-edge" data-i="${i}" stroke="var(--brand)" stroke-width="${1.2 + e.q * 1.1}" marker-end="url(#seta)"/>`).join('');
    const nos = this.nos.map(n => `<g class="g-node" data-id="${n.id}">
      <circle r="${n.r + 3}" fill="none" stroke="${tierCor(n.overall)}" stroke-width="2.5"/>
      <circle r="${n.r}" fill="var(--solid)"/>
      ${n.foto ? `<image href="${esc(n.foto)}" x="${-n.r}" y="${-n.r}" width="${n.r * 2}" height="${n.r * 2}" preserveAspectRatio="xMidYMid slice" clip-path="url(#cp-${n.id})"/>`
               : `<text y="4" style="font-size:${Math.max(10, n.r * .62)}px;stroke:none;fill:var(--text)">${esc(iniciais(n.nome))}</text>`}
      <text y="${n.r + 14}">${esc(n.nome.split(' ')[0])}</text></g>`).join('');
    this.svg.innerHTML = defs + `<g class="g-arestas">${arestas}</g><g class="g-nos">${nos}</g>`;
    this.svg.querySelectorAll('.g-node').forEach(g => {
      g.addEventListener('pointerdown', (ev) => this.aoPressionar(ev, this.mapa.get(+g.dataset.id)));
    });
  }

  ponto(ev) {
    const pt = this.svg.createSVGPoint(); pt.x = ev.clientX; pt.y = ev.clientY;
    return pt.matrixTransform(this.svg.getScreenCTM().inverse());
  }

  aoPressionar(ev, n) {
    ev.preventDefault();
    const inicio = this.ponto(ev); let moveu = false;
    n.fixo = true; this.arrastando = true; this.anima(9999);
    const mover = (e) => {
      const p = this.ponto(e);
      if (Math.abs(p.x - inicio.x) + Math.abs(p.y - inicio.y) > 4) moveu = true;
      n.x = Math.max(n.r + 8, Math.min(this.W - n.r - 8, p.x)); n.y = Math.max(n.r + 8, Math.min(this.H - n.r - 8, p.y));
    };
    const soltar = () => {
      removeEventListener('pointermove', mover); removeEventListener('pointerup', soltar); removeEventListener('pointercancel', soltar);
      n.fixo = false; this.arrastando = false; this.anima(70);
      if (!moveu) this.selecionar(this.sel === n.id ? null : n.id);
    };
    addEventListener('pointermove', mover); addEventListener('pointerup', soltar); addEventListener('pointercancel', soltar);
  }

  /** Figura-fundo: o nó escolhido e seus vizinhos ficam em destaque; o resto recua. */
  selecionar(id) {
    this.sel = id;
    const viz = new Set(id == null ? [] : [id]);
    this.arestas.forEach(e => { if (e.a.id === id) viz.add(e.b.id); if (e.b.id === id) viz.add(e.a.id); });
    this.svg.querySelectorAll('.g-node').forEach(g => g.classList.toggle('g-dim', id != null && !viz.has(+g.dataset.id)));
    this.svg.querySelectorAll('.g-edge').forEach(p => {
      const e = this.arestas[+p.dataset.i];
      p.classList.toggle('g-dim', id != null && e.a.id !== id && e.b.id !== id);
    });
    this.onSelecionar(id == null ? null : this.mapa.get(id), this.arestas.filter(e => e.a.id === id || e.b.id === id));
  }

  atualizar() {
    this.svg.querySelectorAll('.g-node').forEach(g => {
      const n = this.mapa.get(+g.dataset.id);
      g.setAttribute('transform', `translate(${n.x.toFixed(1)},${n.y.toFixed(1)})`);
    });
    this.svg.querySelectorAll('.g-edge').forEach(p => {
      const e = this.arestas[+p.dataset.i];
      const dx = e.b.x - e.a.x, dy = e.b.y - e.a.y, d = Math.sqrt(dx * dx + dy * dy) || 1;
      const ux = dx / d, uy = dy / d;
      const x1 = e.a.x + ux * (e.a.r + 4), y1 = e.a.y + uy * (e.a.r + 4);
      const x2 = e.b.x - ux * (e.b.r + 7), y2 = e.b.y - uy * (e.b.r + 7);
      const mx = (x1 + x2) / 2 - uy * 14, my = (y1 + y2) / 2 + ux * 14;    // leve curva: separa A→B de B→A
      p.setAttribute('d', `M${x1.toFixed(1)},${y1.toFixed(1)} Q${mx.toFixed(1)},${my.toFixed(1)} ${x2.toFixed(1)},${y2.toFixed(1)}`);
    });
  }
  desenhar() { this.atualizar(); }
  destruir() { cancelAnimationFrame(this._raf); }
}
