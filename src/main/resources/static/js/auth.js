async function doLogin() {
  const username = document.getElementById('login-username').value.trim();
  const senha = document.getElementById('login-password').value;
  const err = document.getElementById('login-error');
  err.style.display = 'none';
  try {
    const data = await fetch(`${API}/auth/login`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, senha })
    }).then(r => { if (!r.ok) throw new Error(); return r.json(); });
    
    token = data.token;
    perfil = data.perfil;
    document.getElementById('sidebar-username').textContent = data.username;
    document.getElementById('sidebar-perfil').textContent = data.perfil;
    if (perfil !== 'TREINADOR') document.getElementById('nav-timeideal').style.display = 'none';
    
    document.getElementById('login-screen').style.display = 'none';
    document.getElementById('app').style.display = 'flex';
    navigate('dashboard');
    carregarDashboard();
    document.getElementById('login-password').value = '';
  } catch(e) { 
      err.style.display = 'block'; 
  }
}

function doLogout() {
  token = null; perfil = null;
  document.getElementById('app').style.display = 'none';
  document.getElementById('login-screen').style.display = 'flex';
}

document.getElementById('login-password').addEventListener('keydown', e => { if(e.key==='Enter') doLogin(); });
document.getElementById('login-username').addEventListener('keydown', e => { if(e.key==='Enter') doLogin(); });