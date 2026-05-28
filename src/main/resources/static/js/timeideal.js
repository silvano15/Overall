async function gerarTimeIdeal() {
  const el = document.getElementById('resultado-timeideal');
  el.innerHTML = '<div class="loading"><span class="spinner"></span>Calculando médias...</div>';
  try {
    const data = await req('POST','/time-ideal/gerar');
    let html = '';
    
    // CORREÇÃO DA REGRA SU022 (Mensagem de desequilíbrio exata)
    if (data.alerta) {
      html += `<div class="alerta-box">⚠️ Atenção: A escalação gerada apresenta desequilíbrio tático nas posições. Recomendada a substituição manual.</div>`;
    }
    
    html += `<p style="font-size:12px;color:var(--text3);margin-bottom:16px">
      Semana: ${fmt(data.inicioSemana)} até ${fmt(data.fimSemana)}
    </p>
    <div class="time-ideal-grid">
      <div>
        <div class="card-title" style="margin-bottom:14px">⭐ Titulares (${data.titulares?.length||0})</div>
        ${(data.titulares||[]).map((j,i) => `
          <div class="player-card">
            <div class="player-rank ${i<3?'gold':''}">${i+1}</div>
            <div class="player-info">
              <div class="player-name">${j.nome}</div>
              <div class="player-pos">${fmtPos(j.posicao)}</div>
              <div class="score-mini" style="margin-top:6px">
                <span title="Treino">T: ${j.mediaTreino}</span>
                <span title="Comportamento">C: ${j.mediaComportamento}</span>
                <span title="Desempenho">D: ${j.mediaDesempenho}</span>
              </div>
            </div>
            <div class="player-score">
              <div class="score">${j.mediaFinal}</div>
              <div class="score-label">média final</div>
            </div>
          </div>`).join('')}
      </div>
      <div>
        <div class="card-title" style="margin-bottom:14px">🪑 Reservas (${data.reservas?.length||0})</div>
        ${(data.reservas||[]).slice(0,8).map((j,i) => `
          <div class="player-card" style="padding:10px">
            <div class="player-rank" style="font-size:18px">${i+12}</div>
            <div class="player-info">
              <div class="player-name" style="font-size:13px">${j.nome}</div>
              <div class="player-pos">${fmtPos(j.posicao)}</div>
            </div>
            <div class="player-score">
              <div class="score" style="font-size:22px">${j.mediaFinal}</div>
            </div>
          </div>`).join('')}
      </div>
    </div>`;
    el.innerHTML = html;
  } catch(e) {
    el.innerHTML = `<div class="empty-state"><div class="empty-icon">⚠️</div><p style="color:var(--red)">${e.message}</p></div>`;
  }
}