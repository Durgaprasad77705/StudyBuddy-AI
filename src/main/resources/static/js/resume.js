document.addEventListener("DOMContentLoaded", () => {
    document.getElementById("analyzeResume")?.addEventListener("click", analyzeResume);
});

async function analyzeResume() {
    const input = document.getElementById("resumeFile");
    const result = document.getElementById("resumeResult");
    if (!input?.files?.length) {
        result.style.display = "block";
        result.innerHTML = "⚠️ Please select a PDF, DOCX or TXT resume first.";
        return;
    }
    const file = input.files[0];
    result.style.display = "block";
    result.innerHTML = `<div style="padding:20px">🤖 Extracting ${escapeHtml(file.name)} and analyzing Skills, Education, Projects, Summary and Experience...</div>`;
    try {
        const form = new FormData();
        form.append("file", file);
        const data = await Api.postForm("/future/resume/upload", form);
        renderResumeResult(data, result);
    } catch (e) {
        result.innerHTML = `<div class="ai-error">❌ ${escapeHtml(e.message)}<br><br>If AI analysis is unavailable, start Ollama with <code>ollama serve</code> and verify <code>ollama list</code> contains <code>llama3.2:3b</code>.</div>`;
    }
}

function renderResumeResult(d, result) {
    const scores = d.sectionScores || {};
    const scoreCard = (name, key) => `<div class="resume-score"><strong>${escapeHtml(scores[key] ?? 0)}/100</strong><span>${escapeHtml(name)}</span></div>`;
    const list = arr => (Array.isArray(arr) && arr.length ? `<ul>${arr.map(x => `<li>${escapeHtml(x)}</li>`).join('')}</ul>` : '<p>Not clearly detected.</p>');
    result.innerHTML = `
      <div class="resume-analysis-result">
        <div style="display:flex;justify-content:space-between;gap:16px;align-items:center;flex-wrap:wrap">
          <div><h2>📊 Resume Analysis Result</h2><p>${escapeHtml(d.analysisNote || '')}</p></div>
          <div class="resume-score" style="min-width:130px"><strong>${escapeHtml(d.overallScore)}/100</strong><span>Overall Score</span></div>
        </div>
        <div class="resume-score-grid">
          ${scoreCard('Skills','SKILLS')}${scoreCard('Education','EDUCATION')}${scoreCard('Projects','PROJECTS')}
          ${scoreCard('Experience','EXPERIENCE')}${scoreCard('Summary','SUMMARY')}${scoreCard('Certifications','CERTIFICATIONS')}
        </div>
        <div class="resume-section"><h3>🎯 Readiness</h3><p><strong>${escapeHtml(d.readiness)}</strong></p></div>
        <div class="resume-section"><h3>🧑‍💼 Candidate Summary</h3><p>${escapeHtml(d.candidateSummary || 'Not detected')}</p></div>
        <div class="resume-section"><h3>🎓 Education</h3><p>${escapeHtml(d.education || 'Not detected')}</p></div>
        <div class="resume-section"><h3>💻 Skills</h3>${list(d.skills)}</div>
        <div class="resume-section"><h3>🚀 Projects</h3>${list(d.projects)}</div>
        <div class="resume-section"><h3>💼 Experience</h3>${list(d.experience)}</div>
        <div class="resume-section"><h3>📜 Certifications</h3><p>${escapeHtml(d.certifications || 'Not detected')}</p></div>
        <div class="resume-section"><h3>💪 Strengths</h3>${list(d.strengths)}</div>
        <div class="resume-section"><h3>⚠️ Skill Gaps</h3>${list(d.gaps)}</div>
        <div class="resume-section"><h3>🛠️ Improvements</h3>${list(d.improvements)}</div>
      </div>`;
}

function escapeHtml(v) { return String(v ?? "").replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#039;"}[c])); }
