document.addEventListener("DOMContentLoaded",()=>{const $=id=>document.getElementById(id);const esc=s=>String(s??"").replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;","\"":"&quot;","'":"&#039;"}[c]));
$("searchJobs").onclick=async()=>{
 const q=$("jobRole").value.trim(); if(!q){alert("Enter any job, role, or skill such as Java, Python, Tester, Data Analyst");return;}
 $("jobsResult").innerHTML="🔎 Searching live jobs and generating AI job precision...";
 try{
  const d=await Api.post("/jobs/search",{query:q,location:$("jobLocation").value.trim(),experience:$("jobExperience").value});
  const jobs=(d.jobs||[]).slice(0,10).map(j=>{const lines=(j.descriptionLines||[]).slice(0,10);const desc=lines.length?lines.map((x,i)=>`<div><b>${i+1}.</b> ${esc(x)}</div>`).join(""):`<div>${esc(j.description||"Job description not available.")}</div>`;return `<div class="job"><a href="${esc(j.url)}" target="_blank" rel="noopener">${esc(j.title)}</a><div><b>${esc(j.company)}</b> · ${esc(j.location)} · ${esc(j.type)}</div><h4>Job Description</h4><div class="job-description">${desc}</div></div>`}).join("");
  const resources=(d.resources||[]).map(r=>`<a class="resource" href="${esc(r.url)}" target="_blank" rel="noopener">🔗 ${esc(r.title)}</a>`).join("");
  const linkedIn=`https://www.linkedin.com/jobs/search/?keywords=${encodeURIComponent(q)}${$("jobLocation").value.trim()?"&location="+encodeURIComponent($("jobLocation").value.trim()):""}`;
  $("jobsResult").innerHTML=`<h2>💼 Live Job Results (${d.jobs?.length||0})</h2>
    <p><b>Search:</b> ${esc(q)} ${$("jobLocation").value.trim()?`· <b>Location:</b> ${esc($("jobLocation").value.trim())}`:""}</p>
    <p><a class="btnx linkedin" href="${linkedIn}" target="_blank" rel="noopener">🔵 Open LinkedIn Live Job Search</a></p>
    ${jobs||"<p>No live listing found. Try another role or skill.</p>"}
    <section class="result"><h3>📚 Job Preparation Resources</h3>${resources||"<p>No resources returned.</p>"}</section>
    <div id="aiPrecision" class="result">🤖 AI Job Description Precision...</div>`;
  try{
   const p=await Api.post("/jobs/precision",{query:q,location:$("jobLocation").value.trim(),experience:$("jobExperience").value});
   $("aiPrecision").innerHTML=`<h2>🎯 AI Job Description Precision</h2><div>${esc(p.analysis||p.message||"AI analysis unavailable.").replace(/\n/g,"<br>")}</div>`;
  }catch(e){$("aiPrecision").innerHTML=`<h3>🎯 AI Job Precision</h3><p>${esc(e.message)}</p>`;}
 }catch(e){$("jobsResult").innerHTML="❌ "+esc(e.message);}
};});