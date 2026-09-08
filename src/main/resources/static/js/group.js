// StudyBuddy AI - live multi-participant Group Discussion simulator.
(() => {
  const api = (path, body) => Api.post(path, body);
  const esc = (v) => String(v ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#039;'}[c]));
  const formatAI = text => esc(text).replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>').replace(/^\s*[-*]\s+(.+)$/gm, '• $1').replace(/\n/g, '<br>');
  const setBusy = (button, busy, label) => { if (!button) return; button.disabled = busy; button.dataset.originalLabel ??= button.textContent; button.textContent = busy ? label : button.dataset.originalLabel; };

  document.addEventListener('DOMContentLoaded', () => {
    if (!Auth.isLoggedIn()) { window.location.href = '/#/login'; return; }

    const topic = document.getElementById('groupTopic');
    const role = document.getElementById('groupRole');
    const topicButton = document.getElementById('generateGroupTopic');
    const topicsButton = document.getElementById('generateTenTopics');
    const startButton = document.getElementById('startGroup');
    const topicList = document.getElementById('topicList');
    const groupChat = document.getElementById('groupChat');

    const personas = [
      { name:'Priya', role:'Product Manager', icon:'👩‍💼', style:'a practical product manager who focuses on business impact, users and trade-offs' },
      { name:'Arjun', role:'Software Engineer', icon:'👨‍💻', style:'a technical software engineer who focuses on evidence, implementation and risks' },
      { name:'Meera', role:'HR & People Lead', icon:'👩‍💼', style:'an HR and people leader who focuses on teamwork, ethics, communication and people impact' }
    ];

    let currentTopic = '';
    let transcript = [];
    let liveVoice = false;
    let recognition = null;
    let recognitionRunning = false;
    let voiceSending = false;
    let round = 0;
    let timer = null;
    let seconds = 0;
    let currentPersonaIndex = 0;
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;

    const show = (el, html) => { if (el) el.innerHTML = html; };
    const error = (el, e) => show(el, `<div class="ai-error">❌ ${esc(e?.message || 'Request failed. Please try again.')}</div>`);
    const speakAI = text => { if (!liveVoice || !speechSynthesis || !text) return; speechSynthesis.cancel(); const u = new SpeechSynthesisUtterance(text); u.rate=1.03; u.lang='en-US'; speechSynthesis.speak(u); };
    const formatTime = s => `${String(Math.floor(s/60)).padStart(2,'0')}:${String(s%60).padStart(2,'0')}`;

    function startTimer() { clearInterval(timer); seconds=0; timer=setInterval(()=>{ seconds++; document.getElementById('gdTimer')?.replaceChildren(document.createTextNode(formatTime(seconds))); },1000); }
    function stopTimer() { clearInterval(timer); timer=null; }

    topicButton?.addEventListener('click', async () => {
      const r = role?.value.trim() || 'Any Job Role';
      setBusy(topicButton,true,'🤖 Generating...');
      show(groupChat,"<div class='typing-indicator'><span></span><span></span><span></span></div><p>AI is generating a role-relevant discussion topic...</p>");
      try {
        const d=await api('/group/topic',{role:r,experience:'Fresher'}); const response=d.response||d.message||'';
        currentTopic=response.replace(/^Topic:\s*/i,'').split('\n')[0].trim(); if(topic) topic.value=currentTopic;
        show(groupChat,`<h3>🎯 AI Topic for ${esc(r)}</h3><div>${formatAI(response)}</div><p style="margin-top:15px;color:#64748b">Choose <b>Start Group Discussion</b> to enter the live multi-participant room.</p>`);
      } catch(e){ error(groupChat,e); } finally { setBusy(topicButton,false,''); }
    });

    topicsButton?.addEventListener('click', async () => {
      const r=role?.value.trim()||'Any Job Role'; setBusy(topicsButton,true,'🔟 Generating...');
      show(topicList,"<div class='typing-indicator'><span></span><span></span><span></span></div><p>Generating 10 unique topics...</p>");
      try { const d=await api('/group/topics',{role:r,experience:'Fresher'}); const results=Array.isArray(d.topics)?d.topics:[]; if(!results.length) throw new Error('No topics were returned.'); show(topicList,`<h3>🔟 AI Group Discussion Topics — ${esc(r)}</h3><ol>${results.map(x=>`<li style="margin:9px 0">${esc(x)}</li>`).join('')}</ol>`); }
      catch(e){error(topicList,e);} finally{setBusy(topicsButton,false,'');}
    });

    document.getElementById('liveVoiceBtn')?.addEventListener('click',()=>{
      if(!SpeechRecognition){alert('Live voice is not supported in this browser. Use Chrome or Edge.');return;}
      liveVoice=!liveVoice; const b=document.getElementById('liveVoiceBtn'); if(b){b.textContent=liveVoice?'🎙️ Live Voice: On':'🎙️ Live Voice: Off';b.style.background=liveVoice?'#dc2626':'#0f766e';}
      if(liveVoice && transcript.length) startRecognition(); else if(!liveVoice) stopRecognition();
    });

    function startRecognition(){
      if(!liveVoice||!SpeechRecognition||recognitionRunning||voiceSending)return;
      recognition=new SpeechRecognition(); recognition.lang='en-US'; recognition.interimResults=true; recognition.continuous=false; recognitionRunning=true; let finalText='';
      recognition.onresult=e=>{finalText=Array.from(e.results).map(r=>r[0].transcript).join(' ').trim();const input=document.getElementById('candidateStatement');if(input)input.value=finalText;};
      recognition.onerror=()=>{recognitionRunning=false;};
      recognition.onend=async()=>{recognitionRunning=false;if(liveVoice&&finalText&&!voiceSending)await sendCandidateStatement(finalText,true);};
      try{recognition.start();}catch(_){recognitionRunning=false;}
    }
    function stopRecognition(){try{recognition?.stop();}catch(_){} recognition=null;recognitionRunning=false;speechSynthesis?.cancel();}

    async function getAIResponse(persona, statement){
      const d=await api('/group/participant',{topic:currentTopic,candidateStatement:statement,persona:persona.style});
      return d.response||d.message||'I would like to add another perspective to this discussion.';
    }

    async function sendCandidateStatement(statement){
      if(!statement||voiceSending)return; voiceSending=true; const send=document.getElementById('sendGroupResponse'); if(send)setBusy(send,true,'🤖 AI is responding...');
      transcript.push({speaker:'You',text:statement,icon:'🧑‍🎓'}); round++; renderLive(true);
      try{
        const persona=personas[currentPersonaIndex%personas.length]; currentPersonaIndex++;
        const ai=await getAIResponse(persona,statement); transcript.push({speaker:persona.name,text:ai,icon:persona.icon,role:persona.role}); renderLive(); speakAI(ai);
        if(liveVoice)setTimeout(startRecognition,700);
      }catch(e){renderLive();groupChat?.insertAdjacentHTML('afterbegin',`<div class="ai-error">❌ ${esc(e?.message||'AI participant request failed.')}</div>`);}
      finally{voiceSending=false;}
    }

    startButton?.addEventListener('click', async()=>{
      const t=topic?.value.trim(); if(!t){show(groupChat,"<div class='msg error'>⚠️ Enter a topic or click Generate Related Topic first.</div>");topic?.focus();return;}
      currentTopic=t; transcript=[]; round=1; currentPersonaIndex=0; setBusy(startButton,true,'👥 Starting live room...');
      show(groupChat,"<div class='typing-indicator'><span></span><span></span><span></span></div><p>Priya, Arjun and Meera are joining the discussion...</p>");
      try{
        const opening=`The group discussion topic is: ${currentTopic}. Open the discussion with a short, natural point that invites other participants to contribute.`;
        const replies=await Promise.all(personas.map(p=>getAIResponse(p,opening)));
        replies.forEach((text,i)=>transcript.push({speaker:personas[i].name,text,icon:personas[i].icon,role:personas[i].role}));
        renderLive(); replies.forEach(speakAI); startTimer();
        if(liveVoice)setTimeout(startRecognition,900);
      }catch(e){error(groupChat,e);}finally{setBusy(startButton,false,'');}
    });

    function renderLive(typing=false){
      const lastAI=[...transcript].reverse().find(x=>x.speaker!=='You');
      show(groupChat,`<div style="display:flex;justify-content:space-between;align-items:center;gap:12px;flex-wrap:wrap"><h3 style="margin:0">🎤 Live Group Discussion</h3><div id="gdTimer" style="font-weight:800;background:#eef2ff;padding:8px 12px;border-radius:999px">${formatTime(seconds)}</div></div>
        <div class="result" style="background:#eef2ff;margin-top:12px"><b>Topic:</b> ${esc(currentTopic)}<br><span style="color:#64748b">Round ${round} · 3 AI participants · live coaching mode</span></div>
        <div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(190px,1fr));gap:10px;margin:14px 0">${personas.map(p=>`<div class="result" style="margin:0"><b>${p.icon} ${p.name}</b><br><small>${esc(p.role)}</small></div>`).join('')}</div>
        ${typing?`<div class="result"><b>🤖 AI participants</b><br><span class="typing-indicator"><span></span><span></span><span></span></span> Thinking...</div>`:`<div class="result"><b>${lastAI?.icon||'🤖'} ${esc(lastAI?.speaker||'AI Participant')}</b>${lastAI?.role?` <small>(${esc(lastAI.role)})</small>`:''}<br>${formatAI(lastAI?.text||'Ready.')}</div>`}
        <div style="margin-top:14px"><label><b>🗣️ Your response</b></label><textarea id="candidateStatement" rows="4" placeholder="Speak or type your point, agree/disagree, give an example, or challenge another participant..."></textarea>
          <div style="display:flex;gap:10px;flex-wrap:wrap;margin-top:10px"><button id="sendGroupResponse" class="primary-button" type="button">🎤 Send to Group</button><button id="liveVoiceBtn" class="primary-button" style="background:${liveVoice?'#dc2626':'#0f766e'}" type="button">🎙️ Live Voice: ${liveVoice?'On':'Off'}</button><button id="evaluateGroup" class="primary-button" type="button">📊 End & Evaluate</button></div></div>
        <div id="groupTranscript" class="output-box" style="margin-top:18px"><h4>Live Transcript</h4>${transcript.map(x=>`<p><b>${x.icon||'🤖'} ${esc(x.speaker)}${x.role?` (${esc(x.role)})`:''}:</b> ${formatAI(x.text)}</p>`).join('')}</div>`);

      document.getElementById('sendGroupResponse')?.addEventListener('click',async()=>{const input=document.getElementById('candidateStatement');const statement=input?.value.trim();if(!statement){alert('Enter your response first.');input?.focus();return;}await sendCandidateStatement(statement);});
      document.getElementById('liveVoiceBtn')?.addEventListener('click',()=>document.querySelector('[data-route="advanced"]')?toggleVoice():toggleVoice());
      document.getElementById('evaluateGroup')?.addEventListener('click',evaluate);
      function toggleVoice(){ if(!SpeechRecognition){alert('Live voice is not supported in this browser. Use Chrome or Edge.');return;} liveVoice=!liveVoice; if(liveVoice)startRecognition();else stopRecognition(); renderLive(); }
      async function evaluate(){ stopRecognition();stopTimer();const b=document.getElementById('evaluateGroup');setBusy(b,true,'📊 Evaluating...');const text=transcript.map(x=>`${x.speaker}: ${x.text}`).join('\n');show(groupChat,"<div class='typing-indicator'><span></span><span></span><span></span></div><p>AI is scoring communication, leadership, clarity and teamwork...</p>");try{const d=await api('/group/evaluate',{topic:currentTopic,transcript:text});show(groupChat,`<h3>📊 AI Group Discussion Evaluation</h3><div>${formatAI(d.response||d.message||'Evaluation unavailable.')}</div><button id="backToGroup" class="primary-button" type="button" style="margin-top:18px">↩ Continue Discussion</button>`);document.getElementById('backToGroup')?.addEventListener('click',()=>{startTimer();renderLive();});}catch(e){error(groupChat,e);}}
    }
  });
})();
