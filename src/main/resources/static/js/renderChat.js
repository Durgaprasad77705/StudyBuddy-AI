async function renderChat() {
  const user = Auth.user() || {name:'StudyBuddy User', email:''};
  const storeKey = 'studybuddy_chat_threads_v2';
  let threads = JSON.parse(localStorage.getItem(storeKey) || '[]');
  let activeId = threads[0]?.id || crypto.randomUUID();
  if (!threads.length) threads = [{id: activeId, title:'New chat', messages:[]}];
  let controller = null;

  const save = () => localStorage.setItem(storeKey, JSON.stringify(threads.slice(0,30)));
  const active = () => threads.find(t => t.id === activeId) || threads[0];

  container.innerHTML = `
    <div class="sb-chat-shell">
      <aside class="sb-chat-sidebar">
        <button class="sb-new-chat" id="sbNewChat">＋ New chat</button>
        <div class="sb-chat-history-title">Recent chats</div>
        <div id="sbThreadList" class="sb-thread-list"></div>
        <div class="sb-sidebar-bottom">
          <div class="sb-profile-row">
            <div class="sb-avatar">${escapeHtml((user.name||'U').slice(0,1).toUpperCase())}</div>
            <div class="sb-profile-copy"><b>${escapeHtml(user.name||'User')}</b><small>${escapeHtml(user.email||'')}</small></div>
            <button id="sbProfileMenu" class="sb-icon-btn" title="Profile">•••</button>
          </div>
          <div id="sbProfilePanel" class="sb-profile-panel" hidden>
            <a href="#/profile">Profile & account</a>
            <a href="#/subscription">Subscription</a>
            <button id="sbLogout">Log out</button>
          </div>
        </div>
      </aside>

      <section class="sb-chat-main">
        <header class="sb-chat-topbar">
          <div>
            <div class="sb-model-name">StudyBuddy AI <span>▾</span></div>
            <small>Real-time interview coach</small>
          </div>
          <div class="sb-top-actions">
            <a href="/pages/GroupInterview.html" class="sb-top-link">👥 Group Discussion</a>
            <button id="sbClear" class="sb-icon-btn" title="Clear current chat">⌫</button>
          </div>
        </header>

        <div id="sbMessages" class="sb-messages"></div>

        <div class="sb-composer-wrap">
          <div class="sb-suggestions" id="sbSuggestions">
            <button data-prompt="Explain Java interfaces with a real interview example.">Java interview</button>
            <button data-prompt="Give me a Spring Boot interview question and evaluate my answer.">Spring Boot</button>
            <button data-prompt="Ask me a DSA question one step at a time.">DSA practice</button>
          </div>
          <form id="sbComposer" class="sb-composer">
            <button type="button" id="sbMic" class="sb-composer-btn" title="Voice input">🎙️</button>
            <textarea id="sbInput" rows="1" placeholder="Message StudyBuddy AI..." autocomplete="off"></textarea>
            <button type="submit" id="sbSend" class="sb-send-btn">↑</button>
          </form>
          <div class="sb-disclaimer">StudyBuddy AI can make mistakes. Verify important information.</div>
        </div>
      </section>
    </div>`;

  const list = document.getElementById('sbThreadList');
  const messages = document.getElementById('sbMessages');
  const input = document.getElementById('sbInput');
  const send = document.getElementById('sbSend');

  function renderThreads() {
    list.innerHTML = threads.map(t => `
      <button class="sb-thread ${t.id===activeId?'active':''}" data-id="${t.id}">
        <span>💬</span><span>${escapeHtml(t.title || 'New chat')}</span>
      </button>`).join('');
    list.querySelectorAll('.sb-thread').forEach(b => b.onclick=()=>{activeId=b.dataset.id; renderThreads(); renderMessages();});
  }

  function renderMessages() {
    const t = active();
    if (!t.messages.length) {
      messages.innerHTML = `
        <div class="sb-empty">
          <div class="sb-brand-mark">✦</div>
          <h1>How can I help you today?</h1>
          <p>Practice Java, Spring Boot, SQL, DSA and interview communication with real-time AI responses.</p>
        </div>`;
      document.getElementById('sbSuggestions').hidden=false;
      return;
    }
    document.getElementById('sbSuggestions').hidden=true;
    messages.innerHTML = t.messages.map((m,i)=>messageHtml(m,i)).join('');
    messages.querySelectorAll('[data-copy]').forEach(b=>b.onclick=()=>navigator.clipboard?.writeText(t.messages[Number(b.dataset.copy)].content));
    messages.scrollTop=messages.scrollHeight;
  }

  function messageHtml(m,i) {
    const userMsg=m.role==='user';
    return `<article class="sb-message ${userMsg?'user':'assistant'}">
      <div class="sb-message-avatar">${userMsg?escapeHtml((user.name||'U').slice(0,1).toUpperCase()):'✦'}</div>
      <div class="sb-message-body">
        <div class="sb-message-name">${userMsg?escapeHtml(user.name||'You'):'StudyBuddy AI'}</div>
        <div class="sb-message-text">${formatChatText(m.content)}</div>
        ${!userMsg?`<div class="sb-message-tools"><button data-copy="${i}">Copy</button></div>`:''}
      </div>
    </article>`;
  }

  function formatChatText(text) {
    return escapeHtml(text||'').replace(/```([\s\S]*?)```/g,'<pre><code>$1</code></pre>')
      .replace(/\*\*(.*?)\*\*/g,'<strong>$1</strong>').replace(/\n/g,'<br>');
  }

  function addMessage(role, content) {
    const t=active(); t.messages.push({role,content}); save(); renderMessages();
  }

  async function sendMessage(text) {
    text=text.trim(); if(!text || controller) return;
    const t=active();
    if(!t.messages.length) t.title=text.length>38?text.slice(0,38)+'…':text;
    t.messages.push({role:'user',content:text});
    save(); renderThreads(); renderMessages();
    input.value=''; input.style.height='auto'; send.disabled=true;

    const ai={role:'assistant',content:''}; t.messages.push(ai); save();
    const placeholder=document.createElement('article');
    placeholder.className='sb-message assistant';
    placeholder.innerHTML=`<div class="sb-message-avatar">✦</div><div class="sb-message-body"><div class="sb-message-name">StudyBuddy AI</div><div class="sb-message-text"><span class="sb-cursor"></span></div></div>`;
    messages.appendChild(placeholder); messages.scrollTop=messages.scrollHeight;
    const body=placeholder.querySelector('.sb-message-text');
    controller=new AbortController();

    try {
      await Api.streamChat({message:text,history:t.messages.slice(-12,-1)}, {
        onToken(token){ ai.content += token; body.innerHTML=formatChatText(ai.content)+'<span class="sb-cursor"></span>'; messages.scrollTop=messages.scrollHeight; },
        onError(err){ throw new Error(typeof err==='string'?err:(err?.message||'AI response failed.')); },
        onDone(){ body.innerHTML=formatChatText(ai.content||'I could not generate a response.'); save(); }
      });
      if(!ai.content) ai.content='I could not generate a response.';
      save();
    } catch(e) {
      ai.content = ai.content || `I’m sorry, the AI response failed: ${e.message||'Please try again.'}`;
      body.innerHTML=formatChatText(ai.content); save();
    } finally {
      controller=null; send.disabled=false; input.focus(); renderThreads();
    }
  }

  document.getElementById('sbComposer').onsubmit=e=>{e.preventDefault();sendMessage(input.value);};
  input.oninput=()=>{input.style.height='auto';input.style.height=Math.min(input.scrollHeight,180)+'px';};
  input.onkeydown=e=>{if(e.key==='Enter'&&!e.shiftKey){e.preventDefault();document.getElementById('sbComposer').requestSubmit();}};
  document.getElementById('sbNewChat').onclick=()=>{activeId=crypto.randomUUID();threads.unshift({id:activeId,title:'New chat',messages:[]});save();renderThreads();renderMessages();input.focus();};
  document.getElementById('sbClear').onclick=()=>{active().messages=[];active().title='New chat';save();renderThreads();renderMessages();};
  document.getElementById('sbProfileMenu').onclick=()=>{const p=document.getElementById('sbProfilePanel');p.hidden=!p.hidden;};
  document.getElementById('sbLogout').onclick=()=>Auth.logout();
  document.querySelectorAll('#sbSuggestions button').forEach(b=>b.onclick=()=>{input.value=b.dataset.prompt;input.focus();});
  document.getElementById('sbMic').onclick=()=>{
    const SR=window.SpeechRecognition||window.webkitSpeechRecognition;
    if(!SR){alert('Voice input is not supported. Use Chrome or Edge.');return;}
    const r=new SR(); r.lang='en-US'; r.interimResults=true; r.onresult=e=>input.value=Array.from(e.results).map(x=>x[0].transcript).join(' '); r.start();
  };

  renderThreads(); renderMessages();
}