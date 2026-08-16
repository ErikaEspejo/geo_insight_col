// Login y registro. Todo acceso exige autenticación (FR-021).
(function () {
  const errorBox = document.getElementById('auth-error');
  const loginForm = document.getElementById('login-form');
  const registerForm = document.getElementById('register-form');
  const tabLogin = document.getElementById('tab-login');
  const tabRegister = document.getElementById('tab-register');

  function showError(message) {
    errorBox.textContent = message;
  }

  function switchTab(showRegister) {
    loginForm.classList.toggle('hidden', showRegister);
    registerForm.classList.toggle('hidden', !showRegister);
    tabLogin.classList.toggle('active', !showRegister);
    tabRegister.classList.toggle('active', showRegister);
    tabLogin.setAttribute('aria-selected', String(!showRegister));
    tabRegister.setAttribute('aria-selected', String(showRegister));
    document.getElementById('auth-title').textContent = showRegister ? 'Crear cuenta' : 'Iniciar sesión';
    document.getElementById('auth-description').textContent = showRegister
      ? 'Regístrate para consultar la información geocientífica disponible.'
      : 'Ingresa tus credenciales para continuar';
    errorBox.textContent = '';
    setTimeout(() => document.getElementById(showRegister ? 'register-username' : 'login-username').focus(), 0);
  }

  function setLoading(button, loading, label) {
    button.disabled = loading;
    button.classList.toggle('loading', loading);
    button.innerHTML = loading ? `${label} <span>↻</span>` : `${label} <span>→</span>`;
  }

  tabLogin.addEventListener('click', () => switchTab(false));
  tabRegister.addEventListener('click', () => switchTab(true));

  loginForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    const username = document.getElementById('login-username').value.trim();
    const password = document.getElementById('login-password').value;
    const button = document.getElementById('login-button');
    if (!username || !password) {
      showError('Ingresa tu usuario y contraseña.');
      return;
    }
    try {
      setLoading(button, true, 'Ingresando');
      await api.post('/api/auth/login', { username, password });
      window.location.href = '/index.html';
    } catch (e) {
      showError(e.message);
      setLoading(button, false, 'Ingresar a GeoInsight');
    }
  });

  registerForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    const username = document.getElementById('register-username').value.trim();
    const password = document.getElementById('register-password').value;
    const button = document.getElementById('register-button');
    if (!username || !password) {
      showError('Completa el usuario y la contraseña.');
      return;
    }
    try {
      setLoading(button, true, 'Creando cuenta');
      await api.post('/api/auth/register', { username, password });
      await api.post('/api/auth/login', { username, password });
      window.location.href = '/index.html';
    } catch (e) {
      showError(e.message);
      setLoading(button, false, 'Crear cuenta');
    }
  });

  document.querySelectorAll('.password-toggle').forEach((toggle) => {
    toggle.addEventListener('click', () => {
      const input = document.getElementById(toggle.dataset.target);
      const visible = input.type === 'text';
      input.type = visible ? 'password' : 'text';
      toggle.setAttribute('aria-label', visible ? 'Mostrar contraseña' : 'Ocultar contraseña');
      toggle.classList.toggle('password-visible', !visible);
    });
  });

  if (document.getElementById('register-username')) {
    document.getElementById('login-username').focus();
  }
})();
