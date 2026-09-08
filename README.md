# StudyBuddy AI — Full Stack (Java / Spring Boot)

A complete, runnable implementation of the "StudyBuddy AI" flowchart —
authentication, profile, AI-generated mock interviews, AI evaluation & feedback,
downloadable reports, history, progress tracking, and gamification — built as a
single deployable Spring Boot app with a lightweight built-in web frontend.

## Tech stack

- **Backend:** Java 17, Spring Boot 3.3 (Web, Security, Data JPA, Validation)
- **Auth:** JWT (stateless) with BCrypt password hashing
- **Database:** H2 file-based database (zero setup) — swap for PostgreSQL/MySQL trivially
- **AI:** Anthropic Claude API for question generation & answer evaluation, with an
  **offline fallback** (static question bank + heuristic scorer) so the app runs
  end-to-end even with no API key configured
- **PDF reports:** OpenPDF
- **Frontend:** Plain HTML/CSS/JS single-page app served from
  `src/main/resources/static` (no Node/npm build step required)

## Project structure

```
src/main/java/com/aicoach/
  config/       Security, CORS, AI properties, RestTemplate config
  security/     JWT service/filter, UserDetails, UserPrincipal
  entity/       JPA entities (User, Profile, InterviewSession, Question, Answer,
                 Feedback, Badge, UserBadge, UserGamification)
  repository/   Spring Data JPA repositories
  dto/          Request/response records
  service/      Business logic (Auth, Profile, Interview, Dashboard,
                 Gamification, AI abstraction + Anthropic impl, PDF report)
  controller/   REST controllers
  exception/    Global exception handling
src/main/resources/
  application.yml
  static/       Frontend (index.html, css/, js/)
```

## How the flowchart phases map to the code

| Phase | Flowchart step | Implementation |
|---|---|---|
| 1 | Auth, Profile, Dashboard | `AuthController`, `ProfileController`, `DashboardController` |
| 2 | Choose type → AI generates questions → Mock interview | `InterviewController#start`, `AiCoachService#generateQuestions` |
| 3 | AI evaluation → Feedback → Report | `InterviewController#submitAnswer` (per-question), `#complete` (holistic), `#getReport`, PDF via `ReportPdfService` |
| 4 | History & Progress Tracking | `InterviewController#history`, `#progress` |
| 5 | Gamification & Continuous Improvement | `GamificationService` (XP, levels, streaks, badges), `DashboardService` recommendations |
| 6 | Future Features Expansion | Not built — see **Roadmap** below (kept as extension points) |
| 7 | Success & Impact | Emergent from the above — dashboard + report surface outcomes to the user |

## Running it locally

**Prerequisites:** JDK 17+, Maven 3.9+ (or use the included `mvnw` if you add one).

```bash
cd studybuddy-ai

# optional but recommended — enables real AI question generation & evaluation
export ANTHROPIC_API_KEY=sk-ant-...

mvn spring-boot:run
```

Then open **http://localhost:8080** — register an account, complete your
profile, and start a mock interview.

> **No API key?** The app still works. `AnthropicAiCoachService` automatically
> falls back to a curated static question bank and a heuristic answer scorer
> (`StaticQuestionBank` / `HeuristicEvaluator`) so you can demo the entire
> flow offline. Set `ANTHROPIC_API_KEY` (and optionally `AI_MODEL`,
> `AI_BASE_URL`) whenever you want real AI-generated questions and evaluation.

### Configuration (environment variables)

| Variable | Default | Purpose |
|---|---|---|
| `JWT_SECRET` | dev default (change in prod!) | HMAC signing key for JWTs |
| `JWT_EXPIRATION_MS` | `86400000` (24h) | Token lifetime |
| `ANTHROPIC_API_KEY` | *(empty → offline fallback)* | Enables real AI calls |
| `AI_MODEL` | `claude-sonnet-4-6` | Model used for generation/evaluation |
| `AI_PROVIDER` | `anthropic` | Set to anything else to force offline mode |
| `CORS_ORIGINS` | `http://localhost:8080,http://localhost:3000` | Allowed origins if you split the frontend out |

### Database

H2 file DB is created automatically at `./data/aicoach.mv.db`. Browse it at
`http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:file:./data/aicoach`,
user `sa`, empty password). To use Postgres/MySQL instead, swap the
`spring.datasource.*` properties and add the relevant driver dependency.

## Core API endpoints

```
POST   /api/auth/register
POST   /api/auth/login

GET    /api/profile
PUT    /api/profile

GET    /api/dashboard

POST   /api/interviews/start                     { type, targetRole, timed }
GET    /api/interviews/{id}
POST   /api/interviews/{id}/answers               { questionId, answerText, timeTakenSeconds }
POST   /api/interviews/{id}/complete
GET    /api/interviews/{id}/report
GET    /api/interviews/{id}/report/pdf
GET    /api/interviews/history
GET    /api/interviews/progress

GET    /api/gamification/status
```

All `/api/**` routes except `/api/auth/**` require `Authorization: Bearer <jwt>`.

## Roadmap (Phase 6 — Future Features Expansion)

These were intentionally left as extension points rather than built, matching
how the source flowchart itself marks Phase 6 as future work:

- **Voice interviews** — capture audio, transcribe (e.g. via a speech-to-text
  API), reuse the existing `AiCoachService.evaluateAnswer` for scoring, add
  tone/filler-word analysis as extra evaluation dimensions.
- **Video interview simulation** — add a `VideoAnalysisService` interface
  (facial expression / eye contact / posture) that plugs into the same
  `SessionEvaluation` model.
- **Company-specific packs & coding interviews** — new `InterviewType` values
  (`COMPANY_SPECIFIC`, `CODING`) plus a `CodeExecutionService` for running and
  grading submitted code.
- **Group discussion practice** — multi-participant AI simulation; would
  reuse `InterviewSession` with multiple `Question` "prompts" per round.
- **AI career mentor / resume analysis / LinkedIn review / job recommendations**
  — each maps cleanly to a new `AiCoachService`-style interface + controller;
  the existing fallback pattern (AI call → offline heuristic) should be reused
  for resilience.

## Security notes for production

- Replace the default `JWT_SECRET` with a strong, secret value via environment variable.
- Put the app behind HTTPS.
- Move from H2 to a managed Postgres/MySQL instance.
- Add rate limiting around `/api/auth/**` and the AI-backed endpoints.
- Never commit a real `ANTHROPIC_API_KEY` — always inject via environment/secret manager.

## Real-Time Advanced Features

See `REALTIME_FEATURES_SETUP.md` for LinkedIn OIDC, interactive Company AI Interview, and Smart Job Preparation setup.

## Requested Product Updates

- Login page remains the first protected entry point with email/password plus Google/GitHub sign-in; LinkedIn sign-in and LinkedIn analysis UI have been removed.
- Added Subscription page with **1 Month Free**, **₹120/month**, and **₹499/year** plans.
- Added Razorpay Standard Checkout integration. The backend creates Razorpay orders and verifies the payment signature before activating the subscription.
- Added AI Career Prediction to replace the LinkedIn feature.
- Group Discussion now includes practical speaking structure: opening, point, example, agree/disagree, connect, leadership and conclusion.
- Job search results now display a structured **10-line job description** for each returned job.
