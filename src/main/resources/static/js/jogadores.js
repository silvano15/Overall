async function carregarDashboard() {
  try {
    const [j, t, p, d] = await Promise.all([
      req('GET','/jogadores').catch(()=>[]), 
      req('GET','/treinos').catch(()=>[]),
      req('GET','/partidas').catch(()=>[]), 
      req('GET','/desempenhos').catch(()=>[])
    ]);
    document.getElementById('stat-jogadores').textContent = j.length;
    document.getElementById('stat-treinos').textContent = t.length;
    document.getElementById('stat-partidas').textContent = p.length;
    document.getElementById('stat-desempenhos').textContent = d.length;
  } catch(e) {}
}