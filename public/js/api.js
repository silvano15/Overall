// Base da API: a página é servida pelo próprio backend em /api, então usamos caminho relativo
// (funciona em localhost e também quando o ADM abre pelo IP/domínio no celular).
const _base = new URL('.', document.baseURI).pathname.replace(/\/$/, '');
const API = _base.endsWith('/api') ? _base : (_base ? _base + '/api' : '/api');

let token = null;
try { token = localStorage.getItem('overall_token'); } catch (e) { /* armazenamento bloqueado */ }

function salvarToken(t) {
  token = t;
  try { t ? localStorage.setItem('overall_token', t) : localStorage.removeItem('overall_token'); } catch (e) { /* ignora */ }
}

async function req(method, path, body) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = 'Bearer ' + token;
  const res = await fetch(API + path, {
    method, headers, body: body !== undefined ? JSON.stringify(body) : undefined
  });
  if (res.status === 401 || res.status === 403) {
    // Token vencido/inválido: volta para o login.
    if (typeof sair === 'function') sair();
    throw new Error('Sessão expirada. Entre novamente.');
  }
  if (res.status === 204) return null;
  const data = await res.json().catch(() => null);
  if (!res.ok) {
    const campos = data && data.camposInvalidos && data.camposInvalidos.length
      ? data.camposInvalidos.map(c => c.mensagem).join(' · ') : null;
    throw new Error(campos || (data && data.mensagem) || 'Erro ' + res.status);
  }
  return data;
}

const GET = (p) => req('GET', p);
const POST = (p, b) => req('POST', p, b === undefined ? {} : b);
const PUT = (p, b) => req('PUT', p, b === undefined ? {} : b);
const DEL = (p) => req('DELETE', p);
