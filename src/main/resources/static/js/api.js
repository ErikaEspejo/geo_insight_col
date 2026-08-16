// Cliente REST con credenciales de sesión.
const api = {
  async request(method, url, body) {
    const options = { method, headers: {}, credentials: 'same-origin' };
    if (method === 'GET') options.cache = 'no-store';
    if (body !== undefined) {
      options.headers['Content-Type'] = 'application/json';
      options.body = JSON.stringify(body);
    }
    const res = await fetch(url, options);
    if (res.status === 401 && !url.endsWith('/api/auth/login')) {
      window.location.href = '/login.html';
      throw new Error('Sesión no válida');
    }
    if (!res.ok) {
      let message = 'Ocurrió un error';
      try {
        const data = await res.json();
        if (data.message) message = data.message;
      } catch (e) { /* respuesta sin JSON */ }
      const err = new Error(message);
      err.status = res.status;
      throw err;
    }
    if (res.status === 204) return null;
    return res.json();
  },
  get(url) { return this.request('GET', url); },
  post(url, body) { return this.request('POST', url, body); },
  put(url, body) { return this.request('PUT', url, body); },
  del(url) { return this.request('DELETE', url); }
};
