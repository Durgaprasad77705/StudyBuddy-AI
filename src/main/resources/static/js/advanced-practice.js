document.addEventListener("DOMContentLoaded", () => {

    // The advanced frontend is protected by the same login used by the main app.
    if (typeof Auth !== "undefined" && !Auth.isLoggedIn()) {
        window.location.replace("/");
        return;
    }

    const standardButton = document.getElementById("standardInterviewButton");
    if (standardButton) {
        standardButton.addEventListener("click", () => {
            window.location.href = "/#/start";
        });
    }

    const logoutButton = document.getElementById("advancedLogout");
    if (logoutButton) {
        logoutButton.addEventListener("click", (event) => {
            event.preventDefault();
            if (typeof Auth !== "undefined") {
                Auth.logout();
            } else {
                localStorage.removeItem("aic_token");
                localStorage.removeItem("aic_user");
                window.location.href = "/";
            }
        });
    }

    // Feature cards open the real advanced feature frontends.
    const featureRoutes = {
        Voice: "/pages/VoiceInterview.html",
        Video: "/pages/VideoInterview.html",
        Company: "/pages/CompanyInterview.html",
        Coding: "/pages/CodingInterview.html",
        Group: "/pages/GroupInterview.html",
        Resume: "/pages/ResumeAnalysis.html",
        Prediction: "/pages/CareerPrediction.html",
        Jobs: "/pages/Jobs.html"
    };

    document.querySelectorAll(".practice-button[data-feature]").forEach(button => {
        button.addEventListener("click", () => {
            const feature = button.dataset.feature;
            const route = featureRoutes[feature];
            if (route) window.location.href = route;
        });
    });

    const codingButton = document.getElementById("getCodingPrep");
    if (codingButton) {
        codingButton.addEventListener("click", generateCodingPreparation);
    }
});

/* =========================================
   START INTERVIEW PRACTICE
========================================= */

function startPractice(mode) {

    const result =
        document.getElementById("practiceResult");

    const modeText =
        document.getElementById("practiceModeText");

    const questions =
        document.getElementById("practiceQuestions");


    if (!result) return;


    result.classList.remove("hidden");


    modeText.textContent =
        `AI-generated ${mode} practice session`;


    questions.style.display = "block";


    questions.innerHTML = `

        <h3>
            🎯 ${mode}
        </h3>

        <br>

        <p>
            🤖 AI is preparing your personalized
            interview questions...
        </p>

        <br>

        <p>
            Your interview will focus on:
        </p>

        <ul>

            <li>Communication</li>

            <li>Technical knowledge</li>

            <li>Problem solving</li>

            <li>Confidence</li>

            <li>Interview readiness</li>

        </ul>

    `;


    result.scrollIntoView({
        behavior: "smooth",
        block: "start"
    });

}


/* =========================================
   CODING PREPARATION
========================================= */

async function generateCodingPreparation() {

    const role = "Java Developer";

    const levels = ["Entry", "Mid", "Senior"];
    const level = levels[Math.floor(Math.random() * levels.length)];
    const levelSelect = document.getElementById("experienceLevel");
    if (levelSelect) levelSelect.value = level;

    const topic =
        document.getElementById("problemType").value;

    const language =
        document.getElementById("language")?.value || "Java";

    const button =
        document.getElementById("getCodingPrep");

    const output =
        document.getElementById("codingOutput");


    button.disabled = true;

    button.textContent =
        "🤖 AI is generating...";


    output.style.display = "block";


    output.innerHTML = `
        <div class="typing-indicator">

            <span></span>
            <span></span>
            <span></span>

        </div>

        <br>

        AI is preparing your interview...
    `;


    try {

        const data = await Api.post("/future/coding", {
            language,
            problemType: topic,
            experienceLevel: level
        });

        const tips = (data.tips || [])
            .map(item => `<li>${escapeHTML(item)}</li>`)
            .join("");

        const suggestions = (data.suggestions || [])
            .map(item => `<li>${escapeHTML(item)}</li>`)
            .join("");

        output.innerHTML = `

            <div class="ai-result">

                <h3>
                    🤖 AI Coding Preparation
                </h3>

                <p>
                    <strong>Role:</strong>
                    ${escapeHTML(role)}
                </p>

                <p>
                    <strong>Difficulty:</strong>
                    ${escapeHTML(level)}
                </p>

                <p>
                    <strong>Topic:</strong>
                    ${escapeHTML(topic)}
                </p>

                <hr>

                ${data.headline ? `<p><strong>${escapeHTML(data.headline)}</strong></p>` : ""}

                ${tips ? `<h4>Key Tips</h4><ul>${tips}</ul>` : ""}

                ${suggestions ? `<h4>Suggestions</h4><ul>${suggestions}</ul>` : ""}

            </div>

        `;


    } catch (error) {

        console.error(
            "AI Error:",
            error
        );


        output.innerHTML = `

            <div class="ai-error">

                ❌ <strong>AI Analysis Failed</strong>

                <br><br>

                Unable to connect to the Spring Boot AI backend.

                <br><br>

                Please make sure you are logged in and
                the Spring Boot backend is running on port 8099.

            </div>

        `;

    } finally {

        button.disabled = false;

        button.textContent =
            "🤖 Generate AI Preparation";

    }

}


/* =========================================
   HTML SECURITY
========================================= */

function escapeHTML(value) {

    const div =
        document.createElement("div");

    div.textContent = value;

    return div.innerHTML;

}