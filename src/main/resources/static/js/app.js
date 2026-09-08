// ---------------------------------------------------------------------------
// StudyBuddy AI - single page frontend (vanilla JS, no build step)
// ---------------------------------------------------------------------------

const container = document.getElementById('container');
const appHeader = document.getElementById('app-header');

document.getElementById('logoutBtn')?.addEventListener('click', Auth.logout);
document.querySelectorAll('.nav-link[data-route]').forEach(link => {
  link.addEventListener('click', (event) => {
    const routeName = link.dataset.route;
    if (routeName) {
      event.preventDefault();
      navigate('#/' + routeName);
    }
  });
});

function navigate(hash) { window.location.hash = hash; }

window.addEventListener('hashchange', render);
window.addEventListener('DOMContentLoaded', () => {
  consumeOAuthRedirect();
  render();
});

function consumeOAuthRedirect() {
  const params = new URLSearchParams(window.location.search);
  const token = params.get('oauthToken');
  const oauthError = params.get('oauthError');
  if (token) {
    Auth.save({
      token,
      userId: null,
      name: params.get('oauthName') || 'OAuth User',
      email: params.get('oauthEmail') || ''
    });
    const pendingPlan = sessionStorage.getItem('aic_pending_plan');
    window.history.replaceState({}, document.title, window.location.pathname + (pendingPlan ? '#/subscription' : '#/dashboard'));
    if (pendingPlan) {
      sessionStorage.removeItem('aic_pending_plan');
      setTimeout(() => window.__continueSubscriptionPlan?.(pendingPlan), 250);
    }
    return;
  }
  if (oauthError) {
    sessionStorage.setItem('aic_oauth_error', oauthError);
    window.history.replaceState({}, document.title, window.location.pathname + '#/login');
  }
}

// In-memory state for the interview currently being taken
let activeSession = null;
let currentQuestionIdx = 0;
let timerInterval = null;
let timerSeconds = 0;
let activeAdvancedTab = 'voice';

let liveMediaStream = null;
let liveRecognition = null;
let liveState = 'READY';
let liveTranscript = '';
let liveTranscriptHistory = [];
let liveQuestionStart = 0;
let liveMicMuted = false;
let liveCameraOff = false;
let liveSpeaking = false;
let liveAiSpeaking = false;
let liveProcessing = false;
let liveCompleted = false;
let liveNetworkStatus = 'Connected';

function route() {
  const hash = window.location.hash || '#/login';
  const parts = hash.replace('#/', '').split('/');
  return { name: parts[0] || 'dashboard', param: parts[1] };
}

async function render() {
  const { name, param } = route();
  document.querySelectorAll('.nav-link[data-route]').forEach(link => {
    link.classList.toggle('active', link.dataset.route === name);
  });
  const loggedIn = Auth.isLoggedIn();
  const isAuthPage = ['login','register','forgot-password','reset-password'].includes(name);

  // Authentication pages are intentionally clean: no application navbar/header.
  // A subscription is never required to log in; it is available only after login.
  if (!loggedIn) {
    if (appHeader) appHeader.hidden = true;
    const logoutButton = document.getElementById('logoutBtn');
    if (logoutButton) logoutButton.style.display = 'none';

    if (name === 'register') return renderRegister();
    if (name === 'forgot-password') return renderForgotPassword();
    if (name === 'reset-password') return renderResetPassword(param);

    // The application uses one login for the entire protected website.
    // Subscription is part of the authenticated application; it is never a
    // second login flow and is never opened as a public pricing page.
    return renderLogin();
  }

  if (appHeader) appHeader.hidden = false;
  const logoutButton = document.getElementById('logoutBtn');
  if (logoutButton) logoutButton.style.display = '';

  // Already authenticated users should never remain on an auth route.
  if (isAuthPage) {
    navigate('#/dashboard');
    return;
  }

  try {
    switch (name) {
          case 'dashboard': return await renderDashboard();
      case 'profile': return await renderProfile();
      case 'subscription': return renderSubscription();
      case 'start': return await renderStart();
      case 'advanced': window.location.href = '/pages/AdvancedPractice.html'; return;
      case 'interview': return await renderInterview(param);
      case 'voice': return await renderVoiceInterview(param);
      case 'chat': return await renderChat();
      case 'report': return await renderReport(param);
      case 'history': return await renderHistory();
      case 'progress': return await renderProgress();
      case 'badges': return await renderBadges();
      default: return await renderDashboard();
    }
  } catch (e) {
    container.innerHTML = `<div class="msg error">${escapeHtml(e.message)}</div>`;
  }
}

// ---------------------------------------------------------------- AUTH VIEWS

function renderLogin() {
  const oauthError = sessionStorage.getItem('aic_oauth_error');
  sessionStorage.removeItem('aic_oauth_error');
  container.innerHTML = `
    <div class="auth-wrap card">
      <h2>Welcome back</h2>
      <p style="color:#64748b;margin-bottom:18px">Sign in with email, Google or GitHub.</p>
      ${oauthError ? `<div class="msg error">${escapeHtml(oauthError)}</div>` : ''}
      <div class="social-login" style="display:grid;gap:10px;margin-bottom:18px">
        <a class="btn" href="/oauth2/authorization/google" style="text-align:center;text-decoration:none;background:#fff;color:#111827;border:1px solid #dbe3f0">🔵 Continue with Google</a>
        <a class="btn" href="/oauth2/authorization/github" style="text-align:center;text-decoration:none;background:#111827;color:#fff">⚫ Continue with GitHub</a>
      </div>
      <div style="text-align:center;color:#94a3b8;margin:12px 0">OR</div>
      <div id="authMsg"></div>
      <form id="loginForm">
        <label>Email</label>
        <input type="email" id="email" required autocomplete="email">
        <label>Password</label>
        <input type="password" id="password" required autocomplete="current-password">
        <button class="btn" style="width:100%" type="submit">Log In</button>
      </form>
      <div style="text-align:right;margin-top:12px"><a onclick="navigate('#/forgot-password')" style="cursor:pointer">Forgot password?</a></div>
      <div class="auth-toggle">No account? <a onclick="navigate('#/register')">Register</a></div>
    </div>`;

  document.getElementById('loginForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;
    try {
      const data = await Api.post('/auth/login', { email, password });
      Auth.save(data);
      const pendingPlan = sessionStorage.getItem('aic_pending_plan');
      sessionStorage.removeItem('aic_return_to');
      if (pendingPlan) {
        sessionStorage.removeItem('aic_pending_plan');
        navigate('#/subscription');
        setTimeout(() => window.__continueSubscriptionPlan?.(pendingPlan), 80);
      } else {
        navigate('#/dashboard');
      }
    } catch (err) {
      document.getElementById('authMsg').innerHTML = `<div class="msg error">${escapeHtml(err.message)}</div>`;
    }
  });
}

function renderSubscription() {
  container.innerHTML = `
    <section class="chatgpt-subscription">
      <div class="subscription-head">
        <div>
          <div class="eyebrow">AI INTERVIEW COACH</div>
          <h2>Choose the plan that fits your interview preparation</h2>
          <p>Simple, transparent access. Start free and upgrade when you need more AI practice.</p>
        </div>
        <div id="subscriptionStatus" class="subscription-status">Checking your plan...</div>
      </div>

      <div class="subscription-grid">
        <article class="subscription-plan free-plan">
          <div class="plan-top"><span class="plan-badge">FREE</span></div>
          <h3>Free</h3>
          <p class="plan-copy">Get started with essential interview practice.</p>
          <div class="plan-price">₹0 <span>/ month</span></div>
          <button class="subscription-btn secondary" id="trialBtn">Start Free Access</button>
          <div class="feature-title">Includes</div>
          <ul>
            <li>✓ Basic AI interview practice</li>
            <li>✓ Limited Group AI sessions</li>
            <li>✓ Basic coding practice</li>
            <li>✓ Resume analysis starter access</li>
          </ul>
        </article>

        <article class="subscription-plan plus-plan featured">
          <div class="popular-label">MOST POPULAR</div>
          <div class="plan-top"><span class="plan-badge">PLUS</span></div>
          <h3>Plus</h3>
          <p class="plan-copy">More AI practice for serious interview preparation.</p>
          <div class="plan-price">₹120 <span>/ month</span></div>
          <button class="subscription-btn primary" id="monthlyBtn">Upgrade to Plus</button>
          <div class="feature-title">Everything in Free, plus</div>
          <ul>
            <li>✓ Advanced AI interview sessions</li>
            <li>✓ Higher Group AI usage</li>
            <li>✓ Advanced resume analysis</li>
            <li>✓ Voice & video interview practice</li>
            <li>✓ Company-specific interview practice</li>
          </ul>
        </article>

        <article class="subscription-plan pro-plan">
          <div class="plan-top"><span class="plan-badge">PRO</span></div>
          <h3>Pro</h3>
          <p class="plan-copy">Best value for continuous interview preparation.</p>
          <div class="plan-price">₹450 <span>/ year</span></div>
          <button class="subscription-btn primary" id="yearlyBtn">Upgrade to Pro</button>
          <div class="feature-title">Everything in Plus, plus</div>
          <ul>
            <li>✓ Long-term access for 12 months</li>
            <li>✓ Maximum AI practice limits</li>
            <li>✓ Full Advanced Practice Center</li>
            <li>✓ LinkedIn & job analysis features</li>
            <li>✓ Priority feature access</li>
          </ul>
        </article>
      </div>

      <div class="subscription-note">
        <strong>Secure Razorpay Checkout</strong>
        <span>Pay securely using the payment methods enabled for your Razorpay account: UPI, cards, netbanking, wallets and EMI where supported.</span>
        <span>For UPI, Razorpay uses the currently supported UPI flow (such as UPI Intent/QR); availability depends on your Razorpay account and Checkout configuration.</span>
        <span>Your Razorpay secret key is never exposed to the browser. The server verifies every successful payment before activating the plan.</span>
      </div>
      <div id="subscriptionMsg" style="margin-top:18px"></div>
    </section>`;

  const statusBox = document.getElementById('subscriptionStatus');
  const message = (text, type = 'success') => {
    document.getElementById('subscriptionMsg').innerHTML = `<div class="msg ${type}">${escapeHtml(text)}</div>`;
  };

  const planLabel = plan => ({TRIAL:'Free', NONE:'Free', MONTHLY:'Plus', YEARLY:'Pro'}[plan] || plan || 'Free');

  // Subscription is part of the authenticated website. There is NEVER a
  // second login form here. If the user reaches this route without the
  // single site session, send them to the one global Login page.
  const requireLogin = () => {
    navigate('#/login');
  };

  const loadStatus = async () => {
    if (!Auth.isLoggedIn()) {
      statusBox.className = 'subscription-status';
      statusBox.innerHTML = '🔐 <strong>Log in to activate a plan</strong>';
      return;
    }
    try {
      const d = await Api.get('/payments/status');
      const label = planLabel(d.plan);
      const ends = d.endsAt ? new Date(d.endsAt).toLocaleDateString() : null;
      statusBox.className = `subscription-status ${d.active ? 'active' : ''}`;
      statusBox.innerHTML = d.active
        ? `✓ <strong>${escapeHtml(label)}</strong> active${ends ? ` until ${escapeHtml(ends)}` : ''}`
        : `Current plan: <strong>Free</strong>`;
      if (d.plan === 'TRIAL' && d.active) document.getElementById('trialBtn').disabled = true;
    } catch (e) {
      statusBox.className = 'subscription-status error';
      statusBox.textContent = e.message;
    }
  };

  document.getElementById('trialBtn').onclick = async () => {
    if (!Auth.isLoggedIn()) { requireLogin(); return; }
    const button = document.getElementById('trialBtn');
    button.disabled = true;
    try {
      const d = await Api.post('/payments/trial', {});
      message(d.message || 'Free access activated.');
      await loadStatus();
    } catch(e) {
      message(e.message, 'error');
    } finally {
      button.disabled = false;
    }
  };

  const pay = async plan => {
    if (!Auth.isLoggedIn()) { requireLogin(); return; }
    const button = document.getElementById(plan === 'YEARLY' ? 'yearlyBtn' : 'monthlyBtn');
    button.disabled = true;
    const original = button.textContent;
    button.textContent = 'Opening secure checkout...';
    try {
      const order = await Api.post('/payments/order', { plan });
      if (!window.Razorpay) throw new Error('Razorpay Checkout did not load. Check your internet connection.');

      const user = Auth.user() || {};
      const rzp = new Razorpay({
        key: order.keyId,
        amount: order.amount,
        currency: order.currency,
        name: 'StudyBuddy AI',
        description: order.description,
        order_id: order.orderId,
        prefill: { name: user.name || '', email: user.email || '' },
        // Do not force a payment method: let Razorpay Checkout present the enabled
        // methods so the customer can choose UPI, card, netbanking, wallet, etc.
        notes: { plan, product: 'StudyBuddy AI' },
        handler: async response => {
          try {
            const verified = await Api.post('/payments/verify', { ...response, plan });
            message(verified.message || 'Payment successful. Your plan is now active.');
            await loadStatus();
          } catch(e) {
            message(e.message || 'Payment verification failed. Please contact support.', 'error');
          }
        },
        modal: { ondismiss: () => message('Checkout closed. No subscription changes were made.', 'error') },
        theme: { color: '#635bff' }
      });
      rzp.on('payment.failed', response => {
        const description = response?.error?.description || 'Razorpay payment failed. Please try again.';
        message(description, 'error');
      });
      rzp.open();
    } catch(e) {
      message(e.message, 'error');
    } finally {
      button.disabled = false;
      button.textContent = original;
    }
  };

  document.getElementById('monthlyBtn').onclick = () => pay('MONTHLY');
  document.getElementById('yearlyBtn').onclick = () => pay('YEARLY');
  window.__continueSubscriptionPlan = async plan => { if (!Auth.isLoggedIn()) { navigate('#/login'); return; } if (plan === 'TRIAL') { try { const d = await Api.post('/payments/trial', {}); message(d.message || 'Free access activated.'); await loadStatus(); } catch(e) { message(e.message, 'error'); } } else { await pay(plan); } };
  loadStatus();
}

function renderForgotPassword() {
  container.innerHTML = `
    <div class="auth-wrap card">
      <h2>Reset your password</h2>
      <p style="color:#64748b">Enter your account email. For this local build, the reset token is displayed so you can test the complete flow without SMTP.</p>
      <div id="forgotMsg"></div>
      <form id="forgotForm">
        <label>Email</label>
        <input type="email" id="forgotEmail" required autocomplete="email">
        <button class="btn" style="width:100%;margin-top:12px" type="submit">Generate Reset Link</button>
      </form>
      <div class="auth-toggle"><a onclick="navigate('#/login')">← Back to login</a></div>
    </div>`;
  document.getElementById('forgotForm').addEventListener('submit', async e => {
    e.preventDefault();
    const box = document.getElementById('forgotMsg');
    box.innerHTML = 'Generating secure reset token...';
    try {
      const data = await Api.post('/auth/forgot-password', { email: document.getElementById('forgotEmail').value });
      if (data.resetToken) {
        box.innerHTML = `<div class="msg success">${escapeHtml(data.message)}<br><br><strong>Development reset link:</strong><br><a href="#/reset-password/${encodeURIComponent(data.resetToken)}">Open password reset page</a></div>`;
      } else box.innerHTML = `<div class="msg">${escapeHtml(data.message)}</div>`;
    } catch (e2) { box.innerHTML = `<div class="msg error">${escapeHtml(e2.message)}</div>`; }
  });
}

function renderResetPassword(token) {
  container.innerHTML = `
    <div class="auth-wrap card">
      <h2>Choose a new password</h2>
      <div id="resetMsg"></div>
      <form id="resetForm">
        <label>New password</label>
        <input type="password" id="newPassword" minlength="6" required autocomplete="new-password">
        <label>Confirm password</label>
        <input type="password" id="confirmPassword" minlength="6" required autocomplete="new-password">
        <button class="btn" style="width:100%;margin-top:12px" type="submit">Reset Password</button>
      </form>
    </div>`;
  document.getElementById('resetForm').addEventListener('submit', async e => {
    e.preventDefault();
    const msg = document.getElementById('resetMsg');
    const a = document.getElementById('newPassword').value;
    const b = document.getElementById('confirmPassword').value;
    if (a !== b) { msg.innerHTML = '<div class="msg error">Passwords do not match.</div>'; return; }
    try {
      const data = await Api.post('/auth/reset-password', { token: decodeURIComponent(token || ''), newPassword: a });
      msg.innerHTML = `<div class="msg success">${escapeHtml(data.message)}</div>`;
      setTimeout(() => navigate('#/login'), 1000);
    } catch (e2) { msg.innerHTML = `<div class="msg error">${escapeHtml(e2.message)}</div>`; }
  });
}

function renderRegister() {
  container.innerHTML = `
    <div class="auth-wrap card">
      <h2>Create your account</h2>
      <div id="authMsg"></div>
      <form id="registerForm">
        <label>Full name</label>
        <input type="text" id="name" required>
        <label>Email</label>
        <input type="email" id="email" required>
        <label>Password (min 6 characters)</label>
        <input type="password" id="password" minlength="6" required>
        <button class="btn" style="width:100%" type="submit">Register</button>
      </form>
      <div class="auth-toggle">Already have an account? <a onclick="navigate('#/login')">Log in</a></div>
    </div>`;

  document.getElementById('registerForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const name = document.getElementById('name').value;
    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;
    try {
      const data = await Api.post('/auth/register', { name, email, password });
      Auth.save(data);
      const pendingPlan = sessionStorage.getItem('aic_pending_plan');
      if (pendingPlan) {
        sessionStorage.removeItem('aic_pending_plan');
        navigate('#/subscription');
        setTimeout(() => window.__continueSubscriptionPlan?.(pendingPlan), 120);
      } else {
        navigate('#/profile');
      }
    } catch (err) {
      document.getElementById('authMsg').innerHTML = `<div class="msg error">${escapeHtml(err.message)}</div>`;
    }
  });
}

// ---------------------------------------------------------------- PHASE 1: DASHBOARD

async function renderDashboard() {
  container.innerHTML = `<div class="card">Loading dashboard...</div>`;
  const d = await Api.get('/dashboard');

  container.innerHTML = `
    <div class="card">
      <h2>Welcome back, ${escapeHtml(d.name)} &#128075;</h2>
      <div class="grid grid-4">
        <div class="stat"><div class="value">${d.totalInterviews}</div><div class="label">Total Interviews</div></div>
        <div class="stat"><div class="value">${d.completedInterviews}</div><div class="label">Completed</div></div>
        <div class="stat"><div class="value">${d.averageScore}</div><div class="label">Avg. Score</div></div>
        <div class="stat"><div class="value">Lv.${d.level}</div><div class="label">${d.xp} XP &middot; ${d.currentStreak} day streak</div></div>
      </div>
    </div>
    <div class="card">
      <h3>Recommended Practice</h3>
      <ul>${d.recommendedPractice.map(r => `<li>${escapeHtml(r)}</li>`).join('')}</ul>
      <button class="btn" onclick="navigate('#/start')">Start a Mock Interview</button>
    </div>
    <div class="grid grid-3">
      <div class="card" style="cursor:pointer" onclick="navigate('#/history')"><h3>&#128340; History</h3><p>Review past interviews and compare performance.</p></div>
      <div class="card" style="cursor:pointer" onclick="navigate('#/progress')"><h3>&#128200; Progress</h3><p>Track score trends and consistency over time.</p></div>
      <div class="card" style="cursor:pointer" onclick="navigate('#/advanced')"><h3>&#127919; Advanced</h3><p>Practice advanced interview flows and future features.</p></div>
    </div>`;
}

// ---------------------------------------------------------------- PHASE 1: PROFILE

async function renderProfile() {
  container.innerHTML = `<div class="card">Loading profile...</div>`;
  const p = await Api.get('/profile');

  container.innerHTML = `
    <div class="card">
      <h2>Complete Your Profile</h2>
      <p style="color:var(--muted);font-size:14px">This helps the AI generate role-relevant questions.</p>
      <div id="profileMsg"></div>
      <form id="profileForm">
        <div class="grid grid-2">
          <div>
            <label>Education</label>
            <input type="text" id="education" value="${attr(p.education)}" placeholder="e.g. B.Tech Computer Science">
          </div>
          <div>
            <label>Target Role</label>
            <input type="text" id="targetRole" value="${attr(p.targetRole)}" placeholder="e.g. Backend Developer">
          </div>
        </div>
        <label>Experience Level</label>
        <select id="experienceLevel">
          <option value="ENTRY" ${p.experienceLevel === 'ENTRY' ? 'selected' : ''}>Entry Level</option>
          <option value="MID" ${p.experienceLevel === 'MID' ? 'selected' : ''}>Mid Level</option>
          <option value="SENIOR" ${p.experienceLevel === 'SENIOR' ? 'selected' : ''}>Senior Level</option>
        </select>
        <label>Bio</label>
        <textarea id="bio" placeholder="A short summary about you">${p.bio || ''}</textarea>
        <label>Skills (comma separated)</label>
        <input type="text" id="skills" value="${attr(p.skills)}" placeholder="Java, Spring Boot, SQL">
        <button class="btn" type="submit">Save Profile</button>
      </form>
    </div>`;

  document.getElementById('profileForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const body = {
      education: document.getElementById('education').value,
      targetRole: document.getElementById('targetRole').value,
      experienceLevel: document.getElementById('experienceLevel').value,
      bio: document.getElementById('bio').value,
      skills: document.getElementById('skills').value,
    };
    try {
      await Api.put('/profile', body);
      document.getElementById('profileMsg').innerHTML = `<div class="msg success">Profile saved!</div>`;
    } catch (err) {
      document.getElementById('profileMsg').innerHTML = `<div class="msg error">${escapeHtml(err.message)}</div>`;
    }
  });
}

async function renderChat() {
  container.innerHTML = `
    <div class="card">
      <h2>🤖 AI Chatbot</h2>
      <p style="color:var(--muted)">Ask the AI interview coach anything about Java, Spring Boot, SQL, DSA, interview preparation, feedback, or career advice.</p>
      <div id="chatStatus" class="msg" style="display:none"></div>
    </div>
    <div class="card chat-card">
      <div class="chat-window" id="chatWindow"></div>
      <form id="chatForm" class="chat-form">
        <input type="text" id="chatMessage" placeholder="Type your question here..." autocomplete="off" maxlength="4000" required>
        <button class="btn" id="chatSend" type="submit">Send</button>
      </form>
    </div>`;

  const chatWindow = document.getElementById('chatWindow');
  const form = document.getElementById('chatForm');
  const input = document.getElementById('chatMessage');
  const send = document.getElementById('chatSend');
  const status = document.getElementById('chatStatus');
  const history = [];

  function appendChat(role, text) {
    const bubble = document.createElement('div');
    bubble.className = `chat-message ${role === 'user' ? 'chat-user' : 'chat-bot'}`;
    const roleEl = document.createElement('div');
    roleEl.className = 'chat-role';
    roleEl.textContent = role === 'user' ? 'You' : 'AI Coach';
    const textEl = document.createElement('div');
    textEl.className = 'chat-text';
    textEl.textContent = text;
    bubble.append(roleEl, textEl);
    chatWindow.appendChild(bubble);
    chatWindow.scrollTop = chatWindow.scrollHeight;
    history.push({ role, content: text });
  }

  appendChat('bot', 'Hello! I am your StudyBuddy AI interview coach. Ask me a Java, Spring Boot, SQL, DSA, or interview question.');

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    if (!Auth.isLoggedIn()) { navigate('#/login'); return; }
    const message = input.value.trim();
    if (!message || send.disabled) return;

    appendChat('user', message);
    input.value = '';
    input.disabled = true;
    send.disabled = true;
    send.textContent = 'AI is typing…';
    status.style.display = 'none';

    const bubble = document.createElement('div');
    bubble.className = 'chat-message chat-bot';
    const roleEl = document.createElement('div');
    roleEl.className = 'chat-role';
    roleEl.textContent = 'AI Coach';
    const textEl = document.createElement('div');
    textEl.className = 'chat-text';
    textEl.textContent = '';
    bubble.append(roleEl, textEl);
    chatWindow.appendChild(bubble);
    chatWindow.scrollTop = chatWindow.scrollHeight;

    let reply = '';
    try {
      const context = history.slice(-9, -1);
      await Api.streamChat({ message, history: context }, {
        onToken: token => {
          reply += String(token || '');
          textEl.textContent = reply;
          chatWindow.scrollTop = chatWindow.scrollHeight;
        },
        onError: err => { throw new Error(String(err || 'AI response failed.')); },
        onDone: () => {}
      });
      if (!reply.trim()) throw new Error('AI returned an empty response.');
      history.push({ role: 'bot', content: reply.trim() });
    } catch (err) {
      bubble.remove();
      const msg = err?.message || 'Chatbot is temporarily unavailable.';
      status.className = 'msg error';
      status.textContent = msg;
      status.style.display = 'block';
      history.pop();
    } finally {
      input.disabled = false;
      send.disabled = false;
      send.textContent = 'Send';
      input.focus();
    }
  });
}

// ---------------------------------------------------------------- PHASE 2: START INTERVIEW

async function renderStart() {
  const profile = await Api.get('/profile');
  let selectedType = 'HR';

  container.innerHTML = `
    <div class="card">
      <h2>Choose Interview Type</h2>
      <div class="grid grid-3" id="typeGrid">
        <div class="type-option selected" data-type="HR"><h3>&#128100; HR Interview</h3><p style="font-size:13px;color:var(--muted)">Culture fit, motivation, communication</p></div>
        <div class="type-option" data-type="TECHNICAL"><h3>&#128187; Technical</h3><p style="font-size:13px;color:var(--muted)">Role-specific technical knowledge</p></div>
        <div class="type-option" data-type="BEHAVIORAL"><h3>&#129504; Behavioral</h3><p style="font-size:13px;color:var(--muted)">STAR-based situational questions</p></div>
      </div>
    </div>
    <div class="card">
      <h2>Interview Settings</h2>
      <div id="startMsg"></div>
      <label>Target Role</label>
      <input type="text" id="targetRole" value="${attr(profile.targetRole)}" placeholder="e.g. Backend Developer">
      <label><input type="checkbox" id="timed" checked style="width:auto;margin-right:8px"> Timed interview</label>
      <div style="display:flex;gap:12px;flex-wrap:wrap">
        <button class="btn" id="startBtn">Generate Questions &amp; Start &rarr;</button>
        <button class="btn secondary" id="startLiveBtn">Start Live Voice Interview</button>
      </div>
    </div>`;

  document.querySelectorAll('.type-option').forEach(el => {
    el.addEventListener('click', () => {
      document.querySelectorAll('.type-option').forEach(o => o.classList.remove('selected'));
      el.classList.add('selected');
      selectedType = el.dataset.type;
    });
  });

  document.getElementById('startBtn').addEventListener('click', async () => {
    const btn = document.getElementById('startBtn');
    btn.disabled = true; btn.textContent = 'Generating questions with AI...';
    try {
      const session = await Api.post('/interviews/start', {
        type: selectedType,
        targetRole: document.getElementById('targetRole').value,
        timed: document.getElementById('timed').checked,
      });
      activeSession = session;
      currentQuestionIdx = 0;
      navigate('#/interview/' + session.id);
    } catch (err) {
      document.getElementById('startMsg').innerHTML = `<div class="msg error">${escapeHtml(err.message)}</div>`;
      btn.disabled = false; btn.textContent = 'Generate Questions & Start →';
    }
  });

  document.getElementById('startLiveBtn').addEventListener('click', async () => {
    const btn = document.getElementById('startLiveBtn');
    btn.disabled = true;
    btn.textContent = 'Preparing live interview...';
    try {
      const session = await Api.post('/interviews/start', {
        type: selectedType,
        targetRole: document.getElementById('targetRole').value,
        timed: document.getElementById('timed').checked,
      });
      activeSession = session;
      currentQuestionIdx = 0;
      navigate('#/voice/' + session.id);
    } catch (err) {
      document.getElementById('startMsg').innerHTML = `<div class="msg error">${escapeHtml(err.message)}</div>`;
      btn.disabled = false;
      btn.textContent = 'Start Live Voice Interview';
    }
  });
}

async function renderAdvanced() {
  container.innerHTML = `
    <div class="card">
      <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap">
        <div>
          <h2>Advanced Practice Center</h2>
          <p style="color:var(--muted);max-width:720px">Explore advanced interview exercises, future AI-assisted features, and personalized preparation tools in one place.</p>
        </div>
        <button class="btn" onclick="navigate('#/start')">Start Standard Interview</button>
      </div>
      <div class="feature-tabs" id="advancedTabs">
        <div class="feature-tab selected" data-tab="voice">Voice</div>
        <div class="feature-tab" data-tab="video">Video</div>
        <div class="feature-tab" data-tab="company">Company</div>
        <div class="feature-tab" data-tab="coding">Coding</div>
        <div class="feature-tab" data-tab="group">Group</div>
        <div class="feature-tab" data-tab="resume">Resume</div>
                <div class="feature-tab" data-tab="jobs">Jobs</div>
      </div>
    </div>
    <div class="card feature-panel" id="advancedPanel"></div>`;

  document.querySelectorAll('.feature-tab').forEach(tab => {
    tab.addEventListener('click', () => {
      document.querySelectorAll('.feature-tab').forEach(t => t.classList.remove('selected'));
      tab.classList.add('selected');
      activeAdvancedTab = tab.dataset.tab;
      renderAdvancedPanel();
    });
  });

  renderAdvancedPanel();
}

function renderAdvancedPanel() {
  const panel = document.getElementById('advancedPanel');
  let html = '';

  switch (activeAdvancedTab) {
    case 'voice':
      html = `
        <h3>Voice Interview Guidance</h3>
        <p style="color:var(--muted)">Practice your spoken responses and get advanced voice preparation advice.</p>
        <div id="advancedResult"></div>
        <form id="advancedForm">
          <label>Target Role</label>
          <input id="afTargetRole" type="text" placeholder="e.g. Product Manager" required>
          <label>Sample Answer</label>
          <textarea id="afSampleAnswer" placeholder="Paste a short answer or pitch here"></textarea>
          <button class="btn" type="submit">Get Voice Advice</button>
        </form>`;
      break;
    case 'video':
      html = `
        <h3>Video Interview Simulation</h3>
        <p style="color:var(--muted)">Learn how to improve your camera presence and answer structure for virtual interviews.</p>
        <div id="advancedResult"></div>
        <form id="advancedForm">
          <label>Target Role</label>
          <input id="afTargetRole" type="text" placeholder="e.g. UX Designer" required>
          <label>Interview Scenario</label>
          <textarea id="afScenario" placeholder="Describe the type of video interview scenario"></textarea>
          <button class="btn" type="submit">Get Video Advice</button>
        </form>`;
      break;
    case 'company':
      html = `
        <h3>Company-Specific Prep</h3>
        <p style="color:var(--muted)">Get tailored advice for company-focused interviews and role preparation.</p>
        <div id="advancedResult"></div>
        <form id="advancedForm">
          <label>Company Name</label>
          <input id="afCompanyName" type="text" placeholder="e.g. Google" required>
          <label>Target Role</label>
          <input id="afTargetRole" type="text" placeholder="e.g. Software Engineer" required>
          <button class="btn" type="submit">Get Company Prep</button>
        </form>`;
      break;
    case 'coding':
      html = `
        <h3>Coding Interview Prep</h3>
        <p style="color:var(--muted)">Practice coding interview strategy and receive advanced setup guidance.</p>
        <div id="advancedResult"></div>
        <form id="advancedForm">
          <label>Language</label>
          <input id="afLanguage" type="text" placeholder="e.g. Java" required>
          <label>Problem Type</label>
          <input id="afProblemType" type="text" placeholder="e.g. algorithms, system design" required>
          <label>Experience Level</label>
          <select id="afExperienceLevel">
            <option value="ENTRY">Entry</option>
            <option value="MID">Mid</option>
            <option value="SENIOR">Senior</option>
          </select>
          <button class="btn" type="submit">Get Coding Prep</button>
        </form>`;
      break;
    case 'group':
      html = `
        <h3>Group Discussion Practice</h3>
        <p style="color:var(--muted)">Develop your group discussion strategy and leadership impact.</p>
        <div id="advancedResult"></div>
        <form id="advancedForm">
          <label>Discussion Topic</label>
          <input id="afTopic" type="text" placeholder="e.g. Remote work effectiveness" required>
          <label>Your Role</label>
          <input id="afRole" type="text" placeholder="e.g. Moderator, Participant" required>
          <button class="btn" type="submit">Get Group Prep</button>
        </form>`;
      break;
    case 'resume':
      html = `
        <h3>Resume Analysis</h3>
        <p style="color:var(--muted)">Improve your resume quality and messaging for advanced positioning.</p>
        <div id="advancedResult"></div>
        <form id="advancedForm">
          <label>Upload Resume (PDF or TXT)</label>
          <input id="afResumeFile" type="file" accept=".pdf,.txt">
          <label>Or paste resume text</label>
          <textarea id="afResumeText" placeholder="Paste your resume summary or bullet points here"></textarea>
          <button class="btn" type="submit">Analyze Resume</button>
        </form>`;
      break;
    case 'prediction':
      html = `
        <h3>🤖 AI Career Prediction</h3>
        <p style="color:var(--muted)">Predict suitable career paths from your target role, skills and experience, then get practical next-step recommendations.</p>
        <div id="advancedResult"></div>
        <form id="advancedForm">
          <label>Target Role</label>
          <input id="afTargetRole" type="text" placeholder="e.g. Java Full Stack Developer" required>
          <label>Skills (comma separated)</label>
          <input id="afSkills" type="text" placeholder="e.g. Java, Spring Boot, SQL, React" required>
          <label>Experience</label>
          <input id="afExperience" type="text" placeholder="e.g. Fresher / 1 year" required>
          <button class="btn" type="submit">Run AI Career Prediction</button>
        </form>`;
      break;
    case 'jobs':
      html = `
        <h3>Job Recommendations</h3>
        <p style="color:var(--muted)">Discover job roles matched to your skills and target role.</p>
        <div id="advancedResult"></div>
        <form id="advancedForm">
          <label>Target Role</label>
          <input id="afTargetRole" type="text" placeholder="e.g. Data Analyst" required>
          <label>Skills (comma separated)</label>
          <input id="afSkills" type="text" placeholder="e.g. SQL, Python, Excel" required>
          <button class="btn" type="submit">Get Job Recommendations</button>
        </form>`;
      break;
  }

  panel.innerHTML = html;
  document.getElementById('advancedForm').addEventListener('submit', handleAdvancedSubmit);
}

async function handleAdvancedSubmit(event) {
  event.preventDefault();
  const resultBox = document.getElementById('advancedResult');
  resultBox.innerHTML = `<div class="msg success">Loading advice...</div>`;

  try {
    let response;
    switch (activeAdvancedTab) {
      case 'voice':
        response = await Api.post('/future/voice', {
          targetRole: document.getElementById('afTargetRole').value,
          sampleAnswer: document.getElementById('afSampleAnswer').value,
        });
        break;
      case 'video':
        response = await Api.post('/future/video', {
          targetRole: document.getElementById('afTargetRole').value,
          scenario: document.getElementById('afScenario').value,
        });
        break;
      case 'company':
        response = await Api.post('/future/company', {
          companyName: document.getElementById('afCompanyName').value,
          targetRole: document.getElementById('afTargetRole').value,
        });
        break;
      case 'coding':
        response = await Api.post('/future/coding', {
          language: document.getElementById('afLanguage').value,
          problemType: document.getElementById('afProblemType').value,
          experienceLevel: document.getElementById('afExperienceLevel').value,
        });
        break;
      case 'group':
        response = await Api.post('/future/group', {
          topic: document.getElementById('afTopic').value,
          role: document.getElementById('afRole').value,
        });
        break;
      case 'resume': {
        const fileInput = document.getElementById('afResumeFile');
        const file = fileInput?.files?.[0];
        if (file) {
          const formData = new FormData();
          formData.append('file', file);
          response = await Api.postForm('/future/resume/upload', formData);
        } else {
          response = await Api.post('/future/resume', {
            resumeText: document.getElementById('afResumeText').value,
          });
        }
        break;
      }
      case 'prediction':
        response = await Api.post('/future/prediction', {
          targetRole: document.getElementById('afTargetRole').value,
          skills: document.getElementById('afSkills').value.split(',').map(s => s.trim()).filter(Boolean),
          experience: document.getElementById('afExperience').value
        });
        break;
      case 'jobs':
        response = await Api.post('/future/jobs', {
          targetRole: document.getElementById('afTargetRole').value,
          skills: document.getElementById('afSkills').value.split(',').map(s => s.trim()).filter(Boolean),
        });
        break;
      default:
        throw new Error('Unknown advanced tab');
    }

    resultBox.innerHTML = renderAdvancedResult(response);
  } catch (err) {
    resultBox.innerHTML = `<div class="msg error">${escapeHtml(err.message)}</div>`;
  }
}

function renderAdvancedResult(response) {
  if (!response) return '<div class="msg error">No response available.</div>';
  if (response.recommendedJobs) {
    return `
      <div class="feature-result">
        <h4>Recommended Jobs</h4>
        <ul>${response.recommendedJobs.map(j => `<li>${escapeHtml(j)}</li>`).join('')}</ul>
        <h4>Why these roles?</h4>
        <ul>${response.reasons.map(r => `<li>${escapeHtml(r)}</li>`).join('')}</ul>
      </div>`;
  }
  return `
    <div class="feature-result">
      <h4>${escapeHtml(response.feature)}</h4>
      <p>${escapeHtml(response.headline)}</p>
      <h4>Tips</h4>
      <ul>${response.tips.map(t => `<li>${escapeHtml(t)}</li>`).join('')}</ul>
      <h4>Suggestions</h4>
      <ul>${response.suggestions.map(s => `<li>${escapeHtml(s)}</li>`).join('')}</ul>
    </div>`;
}

// ---------------------------------------------------------------- PHASE 2/3: MOCK INTERVIEW

async function renderInterview(sessionId) {
  clearInterval(timerInterval);
  if (!activeSession || String(activeSession.id) !== String(sessionId)) {
    activeSession = await Api.get('/interviews/' + sessionId);
    currentQuestionIdx = activeSession.questions.findIndex(q => !q.answered);
    if (currentQuestionIdx === -1) currentQuestionIdx = activeSession.questions.length; // all answered
  }

  if (currentQuestionIdx >= activeSession.questions.length) {
    return renderInterviewComplete();
  }

  const q = activeSession.questions[currentQuestionIdx];
  timerSeconds = 0;

  container.innerHTML = `
    <div class="card">
      <div style="display:flex;justify-content:space-between;align-items:center">
        <h3 style="margin:0">${activeSession.type} Interview &middot; Question ${currentQuestionIdx + 1} of ${activeSession.questions.length}</h3>
        ${activeSession.timed ? '<div class="timer" id="timerDisplay">00:00</div>' : ''}
      </div>
      <div class="progress-bar"><div class="progress-bar-fill" style="width:${(currentQuestionIdx / activeSession.questions.length) * 100}%"></div></div>
    </div>
    <div class="card">
      <div class="question-box"><strong>Q${currentQuestionIdx + 1}.</strong> ${escapeHtml(q.text)}</div>
      <div id="answerArea">
        <label>Your Answer</label>
        <textarea id="answerText" placeholder="Type your answer here..."></textarea>
        <button class="btn" id="submitAnswerBtn">Submit Answer</button>
      </div>
      <div id="feedbackArea"></div>
    </div>`;

  if (activeSession.timed) {
    timerInterval = setInterval(() => {
      timerSeconds++;
      const m = String(Math.floor(timerSeconds / 60)).padStart(2, '0');
      const s = String(timerSeconds % 60).padStart(2, '0');
      const disp = document.getElementById('timerDisplay');
      if (disp) disp.textContent = `${m}:${s}`;
    }, 1000);
  }

  document.getElementById('submitAnswerBtn').addEventListener('click', async () => {
    const text = document.getElementById('answerText').value.trim();
    if (!text) return;
    const btn = document.getElementById('submitAnswerBtn');
    btn.disabled = true; btn.textContent = 'AI is evaluating your answer...';
    clearInterval(timerInterval);

    try {
      const feedback = await Api.post(`/interviews/${activeSession.id}/answers`, {
        questionId: q.id,
        answerText: text,
        timeTakenSeconds: timerSeconds,
      });
      q.answered = true;

      const scoreClass = feedback.score >= 75 ? 'score-high' : feedback.score >= 50 ? 'score-mid' : 'score-low';
      document.getElementById('answerArea').innerHTML = `<p style="color:var(--muted);font-size:13px">Answer submitted.</p>`;
      document.getElementById('feedbackArea').innerHTML = `
        <div class="feedback-box">
          <span class="score-badge ${scoreClass}">Score: ${feedback.score}/100</span>
          <p style="margin-bottom:0">${escapeHtml(feedback.feedback)}</p>
        </div>
        <button class="btn" id="nextBtn" style="margin-top:14px">
          ${currentQuestionIdx + 1 < activeSession.questions.length ? 'Next Question →' : 'Finish Interview →'}
        </button>`;

      document.getElementById('nextBtn').addEventListener('click', () => {
        currentQuestionIdx++;
        render();
      });
    } catch (err) {
      document.getElementById('feedbackArea').innerHTML = `<div class="msg error">${escapeHtml(err.message)}</div>`;
      btn.disabled = false; btn.textContent = 'Submit Answer';
    }
  });
}

async function renderInterviewComplete() {
  container.innerHTML = `
    <div class="card" style="text-align:center">
      <h2>All questions answered &#127881;</h2>
      <p style="color:var(--muted)">Ready to generate your AI evaluation report?</p>
      <button class="btn" id="completeBtn">Complete Interview &amp; Get Report</button>
    </div>`;

  document.getElementById('completeBtn').addEventListener('click', async () => {
    const btn = document.getElementById('completeBtn');
    btn.disabled = true; btn.textContent = 'AI is analyzing your full interview...';
    try {
      const report = await Api.post(`/interviews/${activeSession.id}/complete`);
      navigate('#/report/' + report.sessionId);
    } catch (err) {
      container.innerHTML += `<div class="msg error">${escapeHtml(err.message)}</div>`;
    }
  });
}

async function renderVoiceInterview(sessionId) {
  clearInterval(timerInterval);
  if (!activeSession || String(activeSession.id) !== String(sessionId)) {
    activeSession = await Api.get('/interviews/' + sessionId);
    currentQuestionIdx = activeSession.questions.findIndex(q => !q.answered);
    if (currentQuestionIdx === -1) currentQuestionIdx = activeSession.questions.length;
  }

  liveTranscriptHistory = [];
  liveState = activeSession.status === 'COMPLETED' || currentQuestionIdx >= activeSession.questions.length ? 'COMPLETED' : 'READY';
  liveCompleted = liveState === 'COMPLETED';
  liveTranscript = '';
  liveAiSpeaking = false;
  liveProcessing = false;
  liveMicMuted = false;
  liveCameraOff = false;
  liveNetworkStatus = 'Connected';

  container.innerHTML = `
    <div class="voice-page">
      <div class="voice-grid">
        <section class="voice-panel interviewer-panel">
          <div class="panel-header">
            <div>
              <h2>AI Interviewer</h2>
              <p class="muted">${escapeHtml(activeSession.type)} interview · ${activeSession.targetRole ? escapeHtml(activeSession.targetRole) : 'General role'}</p>
            </div>
            <span class="status-pill" id="voiceState">${liveState}</span>
          </div>
          <div class="avatar-card" id="aiAvatar">
            <div class="avatar-face"></div>
            <div class="avatar-wave"></div>
            <div class="avatar-badge">AI</div>
          </div>
          <div class="voice-summary">
            <div class="summary-row"><strong>Question</strong><span id="voiceQuestionNumber">${currentQuestionIdx + 1}/${activeSession.questions.length}</span></div>
            <p id="voiceQuestionText" class="question-text">${liveCompleted ? 'Interview complete. Review your report.' : escapeHtml(activeSession.questions[currentQuestionIdx].text)}</p>
            <div class="summary-row"><strong>Network</strong><span id="voiceNetworkStatus">${escapeHtml(liveNetworkStatus)}</span></div>
            <div class="summary-row"><strong>Timer</strong><span id="voiceTimer">00:00</span></div>
          </div>
          <div class="transcript-feed" id="voiceTranscriptFeed">
            <div class="transcript-header"><strong>Live transcript</strong></div>
            <div id="voiceTranscriptList" class="transcript-list"></div>
          </div>
        </section>

        <section class="voice-panel candidate-panel">
          <div class="panel-header">
            <div>
              <h2>Candidate</h2>
              <p class="muted">Live webcam preview</p>
            </div>
            <span class="status-pill" id="micStatus">Live</span>
          </div>
          <div class="camera-card">
            <video id="candidateVideo" autoplay playsinline muted></video>
            <div class="camera-overlay hidden" id="cameraOverlay">Camera off</div>
          </div>
          <div class="voice-controls">
            <button class="btn" id="toggleMicBtn">Mute</button>
            <button class="btn" id="toggleCameraBtn">Camera Off</button>
            <button class="btn secondary" id="repeatQuestionBtn">Repeat Question</button>
            <button class="btn danger" id="endInterviewBtn">End Interview</button>
          </div>
          <div class="voice-status-box" id="voiceStatusBox">
            <div><strong>Status</strong></div>
            <p id="voiceStatusText">${liveCompleted ? 'Interview completed. View the report.' : 'Ready to start the live voice interview.'}</p>
          </div>
          <div class="voice-action">
            <button class="btn" id="startVoiceBtn">${liveCompleted ? 'View Report' : 'Start Live Interview'}</button>
          </div>
        </section>
      </div>
      <div class="voice-note card">
        <p><strong>Privacy note:</strong> Camera and microphone access are used only for the live interview session. Raw audio/video is not stored on the server.</p>
      </div>
    </div>`;

  document.getElementById('toggleMicBtn').addEventListener('click', toggleMute);
  document.getElementById('toggleCameraBtn').addEventListener('click', toggleCamera);
  document.getElementById('repeatQuestionBtn').addEventListener('click', repeatVoiceQuestion);
  document.getElementById('endInterviewBtn').addEventListener('click', endVoiceInterview);
  document.getElementById('startVoiceBtn').addEventListener('click', async () => {
    if (liveCompleted) {
      navigate('#/report/' + activeSession.id);
      return;
    }
    await startLiveInterview();
  });
}

function updateVoiceState(state, statusMessage) {
  liveState = state;
  const stateLabel = document.getElementById('voiceState');
  const statusText = document.getElementById('voiceStatusText');
  if (stateLabel) stateLabel.textContent = state.replaceAll('_', ' ');
  if (statusText && statusMessage) statusText.textContent = statusMessage;
  updateAiAvatarState(state);
}

function updateAiAvatarState(state) {
  const avatar = document.getElementById('aiAvatar');
  if (!avatar) return;
  avatar.className = 'avatar-card';
  const normalized = state.toLowerCase();
  if (normalized.includes('speaking')) avatar.classList.add('speaking');
  else if (normalized.includes('listening')) avatar.classList.add('listening');
  else if (normalized.includes('processing') || normalized.includes('processing') || normalized.includes('think')) avatar.classList.add('thinking');
  else if (normalized.includes('error')) avatar.classList.add('error');
  else avatar.classList.add('idle');
}

function updateVoiceHeader(questionText) {
  const textEl = document.getElementById('voiceQuestionText');
  const numEl = document.getElementById('voiceQuestionNumber');
  if (textEl) textEl.textContent = questionText;
  if (numEl) numEl.textContent = `${currentQuestionIdx + 1}/${activeSession.questions.length}`;
}

function appendTranscript(role, text) {
  liveTranscriptHistory.push({ role, text, time: new Date().toLocaleTimeString() });
  const list = document.getElementById('voiceTranscriptList');
  if (!list) return;
  list.innerHTML = liveTranscriptHistory.map(item => `
    <div class="transcript-item ${item.role === 'AI' ? 'ai-speaker' : 'candidate-speaker'}">
      <div class="transcript-meta"><strong>${item.role}</strong> <span>${item.time}</span></div>
      <div class="transcript-text">${escapeHtml(item.text)}</div>
    </div>`).join('');
  list.scrollTop = list.scrollHeight;
}

function renderVoiceTimer() {
  const timerEl = document.getElementById('voiceTimer');
  if (!timerEl) return;
  timerSeconds++;
  const m = String(Math.floor(timerSeconds / 60)).padStart(2, '0');
  const s = String(timerSeconds % 60).padStart(2, '0');
  timerEl.textContent = `${m}:${s}`;
}

async function startLiveInterview() {
  try {
    await requestLiveMedia();
    timerSeconds = 0;
    clearInterval(timerInterval);
    timerInterval = setInterval(renderVoiceTimer, 1000);
    liveQuestionStart = Date.now();
    await askCurrentVoiceQuestion();
  } catch (err) {
    updateVoiceState('ERROR', err.message || 'Unable to start media devices.');
  }
}

async function requestLiveMedia() {
  if (liveMediaStream) return;
  if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
    throw new Error('Camera and microphone access is not supported in this browser.');
  }
  liveMediaStream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true });
  const video = document.getElementById('candidateVideo');
  if (video) video.srcObject = liveMediaStream;
  liveMicMuted = false;
  liveCameraOff = false;
  updateMediaLabels();
}

function updateMediaLabels() {
  const micBtn = document.getElementById('toggleMicBtn');
  const camBtn = document.getElementById('toggleCameraBtn');
  const micStatus = document.getElementById('micStatus');
  const cameraOverlay = document.getElementById('cameraOverlay');
  if (micBtn) micBtn.textContent = liveMicMuted ? 'Unmute' : 'Mute';
  if (camBtn) camBtn.textContent = liveCameraOff ? 'Camera On' : 'Camera Off';
  if (micStatus) micStatus.textContent = liveMicMuted ? 'Muted' : 'Live';
  if (cameraOverlay) cameraOverlay.classList.toggle('hidden', !liveCameraOff);
  if (liveMediaStream) {
    liveMediaStream.getAudioTracks().forEach(track => track.enabled = !liveMicMuted);
    liveMediaStream.getVideoTracks().forEach(track => track.enabled = !liveCameraOff);
  }
}

function toggleMute() {
  liveMicMuted = !liveMicMuted;
  updateMediaLabels();
}

function toggleCamera() {
  liveCameraOff = !liveCameraOff;
  updateMediaLabels();
}

async function repeatVoiceQuestion() {
  if (currentQuestionIdx >= activeSession.questions.length) return;
  const question = activeSession.questions[currentQuestionIdx];
  await speakInterviewer(question.text, 'Repeating the question.');
}

async function endVoiceInterview() {
  if (confirm('End the live interview early and view the report?')) {
    clearInterval(timerInterval);
    stopLiveRecognition();
    if (liveMediaStream) {
      liveMediaStream.getTracks().forEach(track => track.stop());
      liveMediaStream = null;
    }
    navigate('#/report/' + activeSession.id);
  }
}

async function askCurrentVoiceQuestion() {
  if (currentQuestionIdx >= activeSession.questions.length) {
    navigate('#/report/' + activeSession.id);
    return;
  }
  const question = activeSession.questions[currentQuestionIdx];
  updateVoiceState('AI_SPEAKING', 'AI interviewer is speaking...');
  appendTranscript('AI', question.text);
  await speakInterviewer(question.text);
  if (liveMicMuted) {
    updateVoiceState('ERROR', 'Unmute your microphone to answer.');
    return;
  }
  liveQuestionStart = Date.now();
  await listenToCandidate();
}

async function speakInterviewer(text, preface) {
  if (preface) appendTranscript('AI', preface);
  if (!window.speechSynthesis) {
    updateVoiceState('ERROR', 'Text-to-speech is unsupported in this browser.');
    return;
  }
  const utterance = new SpeechSynthesisUtterance(text);
  utterance.rate = 1.0;
  utterance.pitch = 1.0;
  utterance.volume = 1.0;
  utterance.lang = 'en-US';
  const voices = window.speechSynthesis.getVoices();
  if (voices.length) {
    const voice = voices.find(v => /female|alloy|katherine|samantha/i.test(v.name)) || voices[0];
    utterance.voice = voice;
  }
  return new Promise((resolve, reject) => {
    utterance.onend = () => {
      liveAiSpeaking = false;
      resolve();
    };
    utterance.onerror = () => {
      updateVoiceState('ERROR', 'AI voice playback failed.');
      reject(new Error('Speech synthesis failed'));
    };
    liveAiSpeaking = true;
    window.speechSynthesis.speak(utterance);
  });
}
// ---------------------------------------------------------------------------
// ADVANCED NAVIGATION CONTROLLER
// ---------------------------------------------------------------------------

let liveNavigationBusy = false;

document.querySelectorAll('.nav-link').forEach(btn => {

  btn.addEventListener('click', async (event) => {

    event.preventDefault();

    const targetRoute = btn.dataset.route;

    // ---------------------------------------------------------
    // User is not logged in
    // ---------------------------------------------------------

    if (!Auth.isLoggedIn()) {
      navigate('#/login');
      return;
    }

    // ---------------------------------------------------------
    // LIVE INTERVIEW
    // ---------------------------------------------------------

    if (targetRoute === 'voice') {

      if (liveNavigationBusy) {
        return;
      }

      liveNavigationBusy = true;

      const oldText = btn.textContent;

      try {

        btn.disabled = true;
        btn.textContent = 'Preparing Live Interview...';

        // Get candidate profile
        const profile = await Api.get('/profile');

        const targetRole =
          profile?.targetRole?.trim() ||
          'Software Developer';

        // Create a NEW backend interview session
        const session = await Api.post('/interviews/start', {

          type: 'TECHNICAL',

          targetRole: targetRole,

          timed: true

        });

        // -----------------------------------------------------
        // IMPORTANT: validate session ID
        // -----------------------------------------------------

        if (
          !session ||
          session.id === undefined ||
          session.id === null ||
          session.id === '' ||
          String(session.id) === 'undefined' ||
          String(session.id) === 'null'
        ) {

          console.error(
            'Invalid session returned by backend:',
            session
          );

          throw new Error(
            'Interview session was created but no valid session ID was returned.'
          );
        }

        // Make sure the ID is numeric because Spring Boot expects Long
        const sessionId = Number(session.id);

        if (
          !Number.isSafeInteger(sessionId) ||
          sessionId <= 0
        ) {

          console.error(
            'Invalid numeric session ID:',
            session.id
          );

          throw new Error(
            'Invalid interview session ID.'
          );
        }

        // Store session globally
        activeSession = session;

        currentQuestionIdx = 0;

        // -----------------------------------------------------
        // Navigate WITH session ID
        // -----------------------------------------------------

        navigate(
          '#/voice/' + encodeURIComponent(String(sessionId))
        );

      } catch (err) {

        console.error(
          'Live Interview start error:',
          err
        );

        container.innerHTML = `
          <div class="card">

            <div class="msg error">

              ${escapeHtml(
                err?.message ||
                'Unable to start Live Interview.'
              )}

            </div>

            <div style="margin-top:15px">

              <button
                class="btn"
                onclick="navigate('#/start')">

                Start Interview

              </button>

            </div>

          </div>
        `;

      } finally {

        liveNavigationBusy = false;

        btn.disabled = false;
        btn.textContent = oldText;
      }

      return;
    }

    // ---------------------------------------------------------
    // ALL OTHER NAVIGATION
    // ---------------------------------------------------------

    navigate('#/' + targetRoute);

  });

});

function getSpeechRecognition() {
  const Constructor = window.SpeechRecognition || window.webkitSpeechRecognition;
  if (!Constructor) return null;
  const recognition = new Constructor();
  recognition.lang = 'en-US';
  recognition.interimResults = true;
  recognition.maxAlternatives = 1;
  recognition.continuous = false;
  return recognition;
}

async function listenToCandidate() {
  const recognitionConstructor = getSpeechRecognition();
  if (!recognitionConstructor) {
    updateVoiceState('ERROR', 'Speech recognition unavailable. Please type your response below.');
    renderManualTranscriptInput();
    return;
  }

  liveState = 'CANDIDATE_SPEAKING';
  updateVoiceState('CANDIDATE_SPEAKING', 'Listening to your answer...');
  liveTranscript = '';
  liveRecognition = recognitionConstructor;

  liveRecognition.onresult = (event) => {
    let interim = '';
    for (let i = event.resultIndex; i < event.results.length; i++) {
      const transcript = event.results[i][0].transcript;
      if (event.results[i].isFinal) {
        liveTranscript += transcript + ' ';
      } else {
        interim += transcript;
      }
    }
    const display = (liveTranscript + interim).trim();
    const statusText = document.getElementById('voiceStatusText');
    if (statusText) statusText.textContent = display ? `Heard: ${display}` : 'Listening...';
  };

  liveRecognition.onerror = (event) => {
    updateVoiceState('ERROR', `Speech recognition error: ${event.error}`);
    renderManualTranscriptInput();
  };

  liveRecognition.onend = async () => {
    if (!liveTranscript.trim()) {
      updateVoiceState('ERROR', 'No voice input detected. Please try again.');
      return;
    }
    await submitVoiceTranscript(liveTranscript.trim());
  };

  liveRecognition.start();
}

function stopLiveRecognition() {
  if (liveRecognition) {
    try { liveRecognition.stop(); } catch (_) {}
    liveRecognition = null;
  }
}

function renderManualTranscriptInput() {
  const feed = document.getElementById('voiceTranscriptFeed');
  if (!feed) return;
  const existing = document.getElementById('manualTranscriptBox');
  if (existing) return;
  const box = document.createElement('div');
  box.id = 'manualTranscriptBox';
  box.className = 'manual-transcript-box';
  box.innerHTML = `
    <label>Type your answer</label>
    <textarea id="manualTranscriptInput" placeholder="Unable to transcribe, type your answer here..."></textarea>
    <button class="btn" id="submitManualTranscriptBtn">Submit Answer</button>`;
  feed.appendChild(box);
  document.getElementById('submitManualTranscriptBtn').addEventListener('click', async () => {
    const value = document.getElementById('manualTranscriptInput').value.trim();
    if (!value) return;
    await submitVoiceTranscript(value);
  });
}

async function submitVoiceTranscript(transcript) {
  stopLiveRecognition();
  liveProcessing = true;
  updateVoiceState('PROCESSING', 'Analyzing your response...');
  appendTranscript('Candidate', transcript);
  const question = activeSession.questions[currentQuestionIdx];
  const timeTakenSeconds = Math.max(1, Math.round((Date.now() - liveQuestionStart) / 1000));
  try {
    const response = await Api.post(`/interviews/${activeSession.id}/voice-answer`, {
      questionId: question.id,
      transcript,
      timeTakenSeconds,
      latencySeconds: 0,
    });
    liveProcessing = false;
    updateVoiceState('PROCESSING', `Score: ${response.aiScore}/100 — ${response.analysis}`);
    question.answered = true;
    if (response.nextQuestionText) {
      if (activeSession.questions[currentQuestionIdx + 1]) {
        activeSession.questions[currentQuestionIdx + 1].text = response.nextQuestionText;
      }
    }
    if (response.completed) {
      await Api.post(`/interviews/${activeSession.id}/complete`);
      updateVoiceState('COMPLETED', 'Interview complete. Redirecting to report...');
      setTimeout(() => navigate('#/report/' + activeSession.id), 1200);
      return;
    }
    currentQuestionIdx++;
    liveQuestionStart = Date.now();
    updateVoiceHeader(activeSession.questions[currentQuestionIdx].text);
    await askCurrentVoiceQuestion();
  } catch (err) {
    updateVoiceState('ERROR', err.message || 'Failed to process response.');
  }
}

// ---------------------------------------------------------------- PHASE 3: REPORT

async function renderReport(sessionId) {
  container.innerHTML = `<div class="card">Loading report...</div>`;
  const r = await Api.get('/interviews/' + sessionId + '/report');

  const scoreClass = (s) => s >= 75 ? 'score-high' : s >= 50 ? 'score-mid' : 'score-low';

  container.innerHTML = `
    <div class="card">
      <div style="display:flex;justify-content:space-between;align-items:flex-start">
        <div>
          <h2>Interview Report</h2>
          <p style="color:var(--muted)">${r.type} interview ${r.targetRole ? '&middot; ' + escapeHtml(r.targetRole) : ''}</p>
        </div>
        <button class="btn secondary" id="downloadBtn">&#11015; Download PDF</button>
      </div>
      <div class="grid grid-4">
        <div class="stat"><div class="value">${r.overallScore}</div><div class="label">Overall</div></div>
        <div class="stat"><div class="value">${r.communicationScore}</div><div class="label">Communication</div></div>
        <div class="stat"><div class="value">${r.confidenceScore}</div><div class="label">Confidence</div></div>
        <div class="stat"><div class="value">${r.knowledgeScore}</div><div class="label">Knowledge</div></div>
      </div>
    </div>

    ${r.summary ? `<div class="card"><h3>Summary</h3><p>${escapeHtml(r.summary)}</p></div>` : ''}

    <div class="grid grid-2">
      <div class="card">
        <h3>Strengths</h3>
        <ul>${r.strengths.map(s => `<li>${escapeHtml(s)}</li>`).join('') || '<li>-</li>'}</ul>
      </div>
      <div class="card">
        <h3>Weaknesses</h3>
        <ul>${r.weaknesses.map(s => `<li>${escapeHtml(s)}</li>`).join('') || '<li>-</li>'}</ul>
      </div>
    </div>

    <div class="card">
      <h3>Improvement Plan</h3>
      <ul>${r.improvementPlan.map(s => `<li>${escapeHtml(s)}</li>`).join('') || '<li>-</li>'}</ul>
    </div>

    <div class="card">
      <h3>Question-wise Feedback</h3>
      ${r.questionFeedback.map((qf, i) => `
        <div class="question-box">
          <strong>Q${i + 1}.</strong> ${escapeHtml(qf.questionText)}
          <p style="color:var(--muted);margin:8px 0"><em>${escapeHtml(qf.answerText)}</em></p>
          <span class="score-badge ${scoreClass(qf.score)}">${qf.score}/100</span>
          <p style="margin-bottom:0">${escapeHtml(qf.feedback)}</p>
        </div>`).join('')}
    </div>

    <button class="btn secondary" onclick="navigate('#/dashboard')">Back to Dashboard</button>`;

  document.getElementById('downloadBtn').addEventListener('click', () => {
    Api.downloadPdf(`/interviews/${sessionId}/report/pdf`, `interview-report-${sessionId}.pdf`);
  });
}

// ---------------------------------------------------------------- PHASE 4: HISTORY

async function renderHistory() {
  container.innerHTML = `<div class="card">Loading history...</div>`;
  const items = await Api.get('/interviews/history');

  const scoreClass = (s) => s === null || s === undefined ? '' : (s >= 75 ? 'score-high' : s >= 50 ? 'score-mid' : 'score-low');

  container.innerHTML = `
    <div class="card">
      <h2>Interview History</h2>
      ${items.length === 0 ? '<p style="color:var(--muted)">No interviews yet. Start your first one!</p>' : ''}
      ${items.map(it => `
        <div class="history-row">
          <div>
            <strong>${it.type}</strong> ${it.targetRole ? '&middot; ' + escapeHtml(it.targetRole) : ''}
            <div style="font-size:12px;color:var(--muted)">${new Date(it.startedAt).toLocaleString()} &middot; ${it.status}</div>
          </div>
          <div style="display:flex;align-items:center;gap:12px">
            ${it.overallScore != null ? `<span class="score-badge ${scoreClass(it.overallScore)}">${it.overallScore}/100</span>` : ''}
            ${it.status === 'COMPLETED'
              ? `<button class="btn secondary" onclick="navigate('#/report/${it.id}')">View Report</button>`
              : `<button class="btn secondary" onclick="continueSession(${it.id})">Continue</button>`}
          </div>
        </div>`).join('')}
    </div>`;
}

window.continueSession = async function (id) {
  activeSession = null;
  navigate('#/interview/' + id);
};

// ---------------------------------------------------------------- PHASE 4: PROGRESS

async function renderProgress() {
  container.innerHTML = `<div class="card">Loading progress...</div>`;
  const p = await Api.get('/interviews/progress');

  container.innerHTML = `
    <div class="card">
      <h2>Progress Tracking</h2>
      <div class="grid grid-4">
        <div class="stat"><div class="value">${p.averageScore}</div><div class="label">Average Score</div></div>
        <div class="stat"><div class="value">${p.trendDelta >= 0 ? '+' : ''}${p.trendDelta}</div><div class="label">Trend</div></div>
        <div class="stat"><div class="value">${p.currentStreak}</div><div class="label">Current Streak</div></div>
        <div class="stat"><div class="value">${p.longestStreak}</div><div class="label">Longest Streak</div></div>
      </div>
    </div>
    <div class="card">
      <h3>Performance Over Time</h3>
      ${p.points.length === 0 ? '<p style="color:var(--muted)">Complete interviews to see your trend line here.</p>' : '<canvas id="progressChart" height="220"></canvas>'}
    </div>`;

  if (p.points.length > 0) drawLineChart('progressChart', p.points);
}

function drawLineChart(canvasId, points) {
  const canvas = document.getElementById(canvasId);
  const ctx = canvas.getContext('2d');
  const w = canvas.width = canvas.clientWidth;
  const h = canvas.height = 220;
  const pad = 32;

  ctx.clearRect(0, 0, w, h);
  ctx.strokeStyle = '#e2e8f0';
  ctx.beginPath();
  ctx.moveTo(pad, h - pad); ctx.lineTo(w - 10, h - pad); ctx.stroke(); // x axis
  ctx.moveTo(pad, 10); ctx.lineTo(pad, h - pad); ctx.stroke(); // y axis

  const maxScore = 100;
  const stepX = (w - pad - 20) / Math.max(1, points.length - 1);

  ctx.strokeStyle = '#4f46e5';
  ctx.lineWidth = 2;
  ctx.beginPath();
  points.forEach((pt, i) => {
    const x = pad + i * stepX;
    const y = h - pad - (pt.overallScore / maxScore) * (h - pad - 20);
    if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
  });
  ctx.stroke();

  ctx.fillStyle = '#4f46e5';
  points.forEach((pt, i) => {
    const x = pad + i * stepX;
    const y = h - pad - (pt.overallScore / maxScore) * (h - pad - 20);
    ctx.beginPath(); ctx.arc(x, y, 4, 0, Math.PI * 2); ctx.fill();
  });

  ctx.fillStyle = '#64748b';
  ctx.font = '11px sans-serif';
  ctx.fillText('0', pad - 16, h - pad + 4);
  ctx.fillText('100', pad - 24, 14);
}

// ---------------------------------------------------------------- PHASE 5: BADGES / GAMIFICATION

async function renderBadges() {
  container.innerHTML = `<div class="card">Loading badges...</div>`;
  const g = await Api.get('/gamification/status');

  container.innerHTML = `
    <div class="card">
      <h2>Gamification &amp; Rewards</h2>
      <div class="grid grid-4">
        <div class="stat"><div class="value">${g.xp}</div><div class="label">Total XP</div></div>
        <div class="stat"><div class="value">Lv.${g.level}</div><div class="label">Level</div></div>
        <div class="stat"><div class="value">${g.currentStreak}</div><div class="label">Current Streak</div></div>
        <div class="stat"><div class="value">${g.longestStreak}</div><div class="label">Longest Streak</div></div>
      </div>
      <p style="font-size:13px;color:var(--muted)">${g.xpForNextLevel} XP to next level</p>
      <div class="progress-bar"><div class="progress-bar-fill" style="width:${100 - (g.xpForNextLevel / 500 * 100)}%"></div></div>
    </div>
    <div class="card">
      <h3>Badges Earned</h3>
      ${g.badges.length === 0 ? '<p style="color:var(--muted)">Complete interviews and build streaks to unlock badges!</p>' : ''}
      <div class="grid grid-4">
        ${g.badges.map(b => `
          <div class="stat">
            <div style="font-size:28px">${b.icon || '🏅'}</div>
            <div style="font-weight:700;font-size:13px;margin-top:6px">${escapeHtml(b.name)}</div>
            <div style="font-size:11px;color:var(--muted)">${escapeHtml(b.description)}</div>
          </div>`).join('')}
      </div>
    </div>`;
}

// ---------------------------------------------------------------- helpers

function escapeHtml(str) {
  if (str === null || str === undefined) return '';
  return String(str)
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;').replace(/'/g, '&#039;');
}
function attr(str) { return escapeHtml(str || ''); }
