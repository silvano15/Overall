// Formação 4-3-3: 11 vagas fixas. Cada vaga tem a posição que aceita e a coordenada no campo.
// Campo vertical: goleiro embaixo, ataque em cima.
const FORMACAO = [
  { pos: 'GOLEIRO',          top: 88, left: 50 },
  { pos: 'LATERAL_DIREITO',  top: 70, left: 84 },
  { pos: 'ZAGUEIRO',         top: 73, left: 38 },
  { pos: 'ZAGUEIRO',         top: 73, left: 62 },
  { pos: 'LATERAL_ESQUERDO', top: 70, left: 16 },
  { pos: 'VOLANTE',          top: 52, left: 50 },
  { pos: 'MEIO_CAMPO',       top: 45, left: 30 },
  { pos: 'MEIA_ATACANTE',    top: 45, left: 70 },
  { pos: 'PONTA_DIREITA',    top: 20, left: 82 },
  { pos: 'ATACANTE',         top: 14, left: 50 },
  { pos: 'PONTA_ESQUERDA',   top: 20, left: 18 },
];

async function gerarTimeIdeal() {
  const el = document.getElementById('resultado-timeideal');
  el.innerHTML = '<div class="loading"><span class="spinner"></span>Calculando médias...</div>';
  try {
    const data = await req('POST', '/time-ideal/gerar');

    // Junta titulares + reservas num único grupo de candidatos
    const pool = [...(data.titulares || []), ...(data.reservas || [])];

    // Para cada posição, ordena os candidatos pela média final (maior primeiro)
    const porPosicao = {};
    pool.forEach(j => {
      if (!porPosicao[j.posicao]) porPosicao[j.posicao] = [];
      porPosicao[j.posicao].push(j);
    });
    Object.values(porPosicao).forEach(arr => arr.sort((a, b) => b.mediaFinal - a.mediaFinal));

    // Preenche as 11 vagas pegando o melhor de cada posição (sem repetir jogador)
    const escalados = [];
    const usadosId = new Set();
    const cartas = FORMACAO.map(vaga => {
      const candidatos = porPosicao[vaga.pos] || [];
      const escolhido = candidatos.find(j => !usadosId.has(j.jogadorId));
      if (escolhido) {
        usadosId.add(escolhido.jogadorId);
        escalados.push(escolhido);
        return { ...escolhido, top: vaga.top, left: vaga.left, vazia: false };
      }
      // Sem jogador para essa posição → vaga vazia
      return { top: vaga.top, left: vaga.left, vazia: true, posVaga: vaga.pos };
    });

    // Reservas = todos do pool que não foram escalados
    const reservas = pool.filter(j => !usadosId.has(j.jogadorId))
                         .sort((a, b) => b.mediaFinal - a.mediaFinal);

    let html = '';
    if (data.alerta) {
      html += `<div class="alerta-box">⚠️ Atenção: A escalação gerada apresenta desequilíbrio tático nas posições. Recomendada a substituição manual.</div>`;
    }
    html += `<p style="font-size:12px;color:var(--text3);margin-bottom:16px">Semana: ${fmt(data.inicioSemana)} até ${fmt(data.fimSemana)}</p>`;

    html += `<div style="display:flex;gap:24px;flex-wrap:wrap;align-items:flex-start">`;

    // ─── CAMPO ───
    html += `
      <div style="flex:1;min-width:320px;max-width:560px">
        <div class="ti-campo">
          ${cartas.map(j => {
            if (j.vazia) {
              return `
                <div class="ti-carta" style="top:${j.top}%;left:${j.left}%">
                  <div class="ti-bola" style="border-color:#555;border-style:dashed">
                    <span style="color:#888;font-size:11px">?</span>
                  </div>
                  <div class="ti-pos">${fmtPos(j.posVaga)}</div>
                </div>`;
            }
            const cor = j.mediaFinal >= 8 ? '#00e676' : j.mediaFinal >= 6 ? '#ffd93d' : '#ff5252';
            return `
              <div class="ti-carta" style="top:${j.top}%;left:${j.left}%">
                <div class="ti-bola" style="border-color:${cor}">
                  <span style="color:${cor}">${j.mediaFinal}</span>
                </div>
                <div class="ti-nome">${j.nome.split(' ')[0]}</div>
                <div class="ti-pos">${fmtPos(j.posicao)}</div>
              </div>`;
          }).join('')}
        </div>
      </div>`;

    // ─── RESERVAS ───
    html += `
      <div style="min-width:220px">
        <div class="card-title" style="margin-bottom:14px">🪑 Reservas (${reservas.length})</div>
        ${reservas.slice(0, 8).map((j, i) => `
          <div class="player-card" style="padding:10px;margin-bottom:8px">
            <div class="player-rank" style="font-size:18px">${i + 12}</div>
            <div class="player-info">
              <div class="player-name" style="font-size:13px">${j.nome}</div>
              <div class="player-pos">${fmtPos(j.posicao)}</div>
            </div>
            <div class="player-score"><div class="score" style="font-size:22px">${j.mediaFinal}</div></div>
          </div>`).join('')}
      </div>`;

    html += `</div>`;

    // ─── CSS embutido ───
    html += `<style>
      .ti-campo {
        position: relative; width: 100%; aspect-ratio: 2 / 3; border-radius: 14px;
        background: repeating-linear-gradient(0deg, #1f8a4c 0 9%, #1c7e45 9% 18%);
        border: 3px solid rgba(255,255,255,0.25); overflow: hidden;
        box-shadow: inset 0 0 60px rgba(0,0,0,0.35);
      }
      .ti-campo::before {
        content:''; position:absolute; top:50%; left:0; right:0; height:2px; background:rgba(255,255,255,0.4);
      }
      .ti-campo::after {
        content:''; position:absolute; top:50%; left:50%; width:110px; height:110px;
        transform:translate(-50%,-50%); border:2px solid rgba(255,255,255,0.4); border-radius:50%;
      }
      .ti-carta {
        position:absolute; transform:translate(-50%,-50%);
        display:flex; flex-direction:column; align-items:center; width:84px; text-align:center;
      }
      .ti-bola {
        width:46px; height:46px; border-radius:50%; background:rgba(15,20,18,0.92);
        border:3px solid #00e676; display:flex; align-items:center; justify-content:center;
        font-weight:700; font-size:16px; box-shadow:0 4px 10px rgba(0,0,0,0.5);
      }
      .ti-nome {
        margin-top:5px; font-size:12px; font-weight:600; color:#fff;
        text-shadow:0 1px 3px rgba(0,0,0,0.9); line-height:1.1;
      }
      .ti-pos {
        font-size:10px; color:rgba(255,255,255,0.85); text-shadow:0 1px 2px rgba(0,0,0,0.9);
      }
    </style>`;

    el.innerHTML = html;

  } catch(e) {
    el.innerHTML = `<div class="empty-state"><div class="empty-icon">⚠️</div><p style="color:var(--red)">${e.message}</p></div>`;
  }
}