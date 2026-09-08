// Thin wrapper around fetch() that attaches the JWT and handles JSON + errors.
const Api = (() => {
  const BASE = '/api';

  function token() {
    return localStorage.getItem('aic_token');
  }

  async function request(method, path, body, opts = {}) {
    const isForm = body instanceof FormData || opts.formData;
    const headers = {};
    if (!isForm) headers['Content-Type'] = 'application/json';
    const t = token();
    if (t) headers['Authorization'] = 'Bearer ' + t;

    const res = await fetch(BASE + path, {
      method,
      headers,
      body: isForm ? body : body ? JSON.stringify(body) : undefined,
    });

    if (res.status === 401 || res.status === 403) {
      const authFreePaths = ['/auth/login','/auth/register','/auth/forgot-password','/auth/reset-password'];
      if (!authFreePaths.includes(path)) {
        // A server-side JWT failure means the single site session is no
        // longer valid. Clear only the invalid session and use the ONE
        // global login page; never open a second login inside a feature.
        localStorage.removeItem('aic_token');
        localStorage.removeItem('aic_user');
        if (window.location.hash !== '#/login') window.location.hash = '#/login';
        throw new Error('Your login session expired or is invalid. Please log in once again.');
      }
    }

    if (opts.raw) return res;

    let data = null;
    try { data = await res.json(); } catch (e) { /* no body */ }

    if (!res.ok) {
      const message = (data && data.message) ? data.message : ('Request failed (' + res.status + ')');
      throw new Error(message);
    }
    return data;
  }

  return {
    get: (path) => request('GET', path),
    post: (path, body) => request('POST', path, body),
    streamChat: async (body, handlers = {}) => {
      const headers = { 'Content-Type': 'application/json' };
      const t = token();
      if (t) headers['Authorization'] = 'Bearer ' + t;
      let res;
      try {
        res = await fetch(BASE + '/chat/stream', { method: 'POST', headers, body: JSON.stringify(body) });
      } catch (streamNetworkError) {
        // Streaming can be blocked by a proxy/browser transport. Fall back to
        // the same authenticated /chat endpoint so the chatbot never becomes
        // unusable just because SSE is unavailable.
        const fallback = await request('POST', '/chat', body);
        const fallbackReply = fallback?.reply || fallback?.answer || fallback?.response || '';
        if (!fallbackReply) throw streamNetworkError;
        handlers.onToken?.(String(fallbackReply));
        handlers.onDone?.();
        return;
      }
      if (!res.ok || !res.body) {
        if (res.status === 401 || res.status === 403) {
          localStorage.removeItem('aic_token');
          localStorage.removeItem('aic_user');
          if (window.location.hash !== '#/login') window.location.hash = '#/login';
          throw new Error('Your login session expired or is invalid. Please log in once again.');
        }
        try {
          const fallback = await request('POST', '/chat', body);
          const fallbackReply = fallback?.reply || fallback?.answer || fallback?.response || '';
          if (fallbackReply) { handlers.onToken?.(String(fallbackReply)); handlers.onDone?.(); return; }
        } catch (_) {}
        throw new Error('Unable to start AI response (' + res.status + ').');
      }
      const reader = res.body.getReader();
      const decoder = new TextDecoder();
      let buffer = '';
      while (true) {
        const { value, done } = await reader.read();
        if (done) break;
        buffer += decoder.decode(value, { stream: true });
        const events = buffer.split('\n\n');
        buffer = events.pop() || '';
        for (const event of events) {
          const dataLine = event.split('\n').find(line => line.startsWith('data:'));
          if (!dataLine) continue;
          const raw = dataLine.slice(5).trim();
          if (!raw) continue;
          try {
            const data = JSON.parse(raw);
            if (event.includes('event: error')) handlers.onError?.(data);
            else handlers.onToken?.(String(data));
          } catch (_) { handlers.onToken?.(raw); }
        }
      }
      handlers.onDone?.();
    },
    postForm: (path, formData) => request('POST', path, formData, { formData: true }),
    put: (path, body) => request('PUT', path, body),
    del: (path) => request('DELETE', path),
    downloadPdf: async (path, filename) => {
      const res = await request('GET', path, null, { raw: true });
      if (!res.ok) throw new Error('Could not download report');
      const blob = await res.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url; a.download = filename;
      document.body.appendChild(a); a.click(); a.remove();
      window.URL.revokeObjectURL(url);
    }
  };
})();

const Auth = {
  save(data) {
    localStorage.setItem('aic_token', data.token);
    localStorage.setItem('aic_user', JSON.stringify({ id: data.userId, name: data.name, email: data.email }));
  },
  user() {
    const raw = localStorage.getItem('aic_user');
    return raw ? JSON.parse(raw) : null;
  },
  isLoggedIn() {
    const t = localStorage.getItem('aic_token');
    if (!t) return false;
    // Do not locally expire the session. The backend validates the JWT.
    // This prevents the SPA from forcing a second login during normal use.
    return true;
  },
  logout() {
    localStorage.removeItem('aic_token');
    localStorage.removeItem('aic_user');
    window.location.hash = '#/login';
    window.location.reload();
  }
};
