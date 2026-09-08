document.addEventListener("DOMContentLoaded", () => {
    const post = async (path, body) => {
        if (typeof Api === "undefined") throw new Error("API helper not loaded");
        return Api.post(path, body);
    };

    const show = (id, html) => {
        const el = document.getElementById(id);
        if (el) { el.style.display = "block"; el.innerHTML = html; }
    };
    const esc = (v) => String(v ?? "").replace(/[&<>\"']/g, c => ({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;", "'":"&#039;"}[c]));
    const renderAdvice = (data) => {
        const tips = (data.tips || []).map(x => `<li>${esc(x)}</li>`).join("");
        const suggestions = (data.suggestions || []).map(x => `<li>${esc(x)}</li>`).join("");
        return `<h2>🤖 ${esc(data.headline || data.feature || "AI Advice")}</h2>
                ${tips ? `<h3>Key Tips</h3><ul>${tips}</ul>` : ""}
                ${suggestions ? `<h3>Suggestions</h3><ul>${suggestions}</ul>` : ""}`;
    };

    // Company
    const companyBtn = document.getElementById("startCompany");
    if (companyBtn) companyBtn.addEventListener("click", async () => {
        const companyName = document.getElementById("company")?.value.trim();
        const targetRole = document.getElementById("role")?.value.trim();
        if (!companyName || !targetRole) return show("companyResult", "⚠️ Enter company and role.");
        show("companyResult", "🤖 Connecting to AI backend...");
        try { show("companyResult", renderAdvice(await post("/future/company", {companyName, targetRole}))); }
        catch (e) { show("companyResult", `❌ ${esc(e.message)}`); }
    });

    // Group
    const groupBtn = document.getElementById("startGroup");
    if (groupBtn) groupBtn.addEventListener("click", async () => {
        const topic = document.getElementById("groupTopic")?.value.trim();
        if (!topic) return show("groupChat", "⚠️ Enter a discussion topic.");
        show("groupChat", "🤖 AI is preparing the discussion...");
        try { show("groupChat", renderAdvice(await post("/future/group", {topic, role: "Candidate"}))); }
        catch (e) { show("groupChat", `❌ ${esc(e.message)}`); }
    });
    // Jobs
    const jobsBtn = document.getElementById("analyzeJobs");
    if (jobsBtn) jobsBtn.addEventListener("click", async () => {
        const targetRole = document.getElementById("jobRole")?.value.trim();
        const skill = document.getElementById("jobSkill")?.value.trim();
        if (!targetRole || !skill) return show("jobsResult", "⚠️ Enter target role and primary skill.");
        show("jobsResult", "🤖 AI is finding preparation opportunities...");
        try {
            const data = await post("/future/jobs", {targetRole, skills: [skill]});
            const jobs = (data.recommendedJobs || []).map(x => `<li>${esc(x)}</li>`).join("");
            const reasons = (data.reasons || []).map(x => `<li>${esc(x)}</li>`).join("");
            show("jobsResult", `<h2>💼 AI Job Recommendations</h2>${jobs ? `<h3>Recommended Roles</h3><ul>${jobs}</ul>` : ""}${reasons ? `<h3>Why</h3><ul>${reasons}</ul>` : ""}`);
        } catch (e) { show("jobsResult", `❌ ${esc(e.message)}`); }
    });

    // Video: camera + AI advice
    const startVideo = document.getElementById("startVideo");
    if (startVideo) startVideo.addEventListener("click", async () => {
        const role = "Java Developer";
        const scenario = "Technical video interview";
        show("videoFeedback", "🤖 Camera started. Connecting to AI backend...");
        try { show("videoFeedback", renderAdvice(await post("/future/video", {targetRole: role, scenario}))); }
        catch (e) { show("videoFeedback", `❌ ${esc(e.message)}`); }
    });

    // Voice: submit the spoken answer for AI advice when recording stops.
    const stopBtn = document.getElementById("stopBtn");
    if (stopBtn) stopBtn.addEventListener("click", async () => {
        setTimeout(async () => {
            const answer = document.getElementById("answer")?.innerText?.trim();
            if (!answer || answer.startsWith("Your spoken answer")) return;
            try {
                const data = await post("/future/voice", {targetRole: "Java Developer", sampleAnswer: answer});
                const tips = (data.tips || []).join(" • ");
                const suggestions = (data.suggestions || []).join(" • ");
                const feedback = document.getElementById("feedbackText");
                if (feedback) feedback.innerText = `${data.headline || "AI feedback"}${tips ? " | Tips: " + tips : ""}${suggestions ? " | Suggestions: " + suggestions : ""}`;
            } catch (e) { console.error("Voice backend error", e); }
        }, 500);
    });
});
