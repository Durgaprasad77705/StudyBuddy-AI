let currentProblem = "";
let currentLanguage = "";

document.addEventListener("DOMContentLoaded", () => {
    const btn = document.getElementById("generateProblem");
    if (btn) btn.addEventListener("click", generateProblem);
});

async function generateProblem() {
    const language = document.getElementById("language").value;
    const problemType = document.getElementById("problemType").value;
    const experienceLevel = document.getElementById("experienceLevel").value;
    const output = document.getElementById("problemOutput");
    output.style.display = "block";
    output.innerHTML = "🤖 AI is generating a coding problem...";
    try {
        const data = await Api.post("/coding/generate", { language, problemType, experienceLevel });
        currentProblem = data.result || "";
        currentLanguage = language;
        output.innerHTML = `<h2>🧩 Coding Problem</h2><pre>${escapeHtml(currentProblem)}</pre>
            <textarea id="candidateCode" placeholder="Write your ${escapeHtml(language)} solution here..."></textarea>
            <button id="submitCode" class="primary-button">🚀 Submit Solution</button>
            <div id="codeResult" class="output-box"></div>`;
        document.getElementById("submitCode").addEventListener("click", evaluateCode);
    } catch (e) {
        output.innerHTML = `<div class="ai-error">❌ ${escapeHtml(e.message)}<br><br>Make sure the Spring Boot server is running and you are logged in.</div>`;
    }
}

async function evaluateCode() {
    const code = document.getElementById("candidateCode")?.value || "";
    const result = document.getElementById("codeResult");
    if (!code.trim()) { result.innerHTML = "⚠️ Write your solution first."; return; }
    result.innerHTML = "🤖 AI is evaluating your solution...";
    try {
        const data = await Api.post("/coding/evaluate", { problem: currentProblem, code, language: currentLanguage });
        result.innerHTML = `<h3>📊 AI Interview Evaluation</h3><pre>${escapeHtml(data.result || "No evaluation returned")}</pre>`;
    } catch (e) { result.innerHTML = `❌ ${escapeHtml(e.message)}`; }
}

function escapeHtml(value) {
    return String(value ?? "").replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/\"/g, "&quot;").replace(/'/g, "&#039;");
}
