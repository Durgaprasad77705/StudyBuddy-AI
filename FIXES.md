# StudyBuddy AI - Final Login / Subscription / Chatbot Fixes

## Fixed
- Login and registration never require a subscription.
- The first URL now renders the Login screen only; the old static home page is removed from the initial HTML to prevent a flash of the dashboard/home content.
- Navbar/header is hidden on Login, Register, Forgot Password, and Reset Password.
- After successful login, the app goes to `#/dashboard` and the authenticated navbar appears.
- Direct access to `#/subscription` while logged out shows the Login page.
- Subscription page and Razorpay upgrade flow were preserved; upgrade buttons still create a server-side Razorpay order and verify the checkout signature before activating the plan.
- Chatbot UI now sends conversation history and uses the authenticated `/api/chat` endpoint.
- Chatbot backend now requires the logged-in JWT, supports Anthropic first and Ollama fallback, uses the configured AI URL where appropriate, and has a deterministic Java interview fallback if both AI providers are unavailable.
- AI Group Interview files were not changed.
- Advanced Practice files were not changed.

## Maven run
From the folder containing `pom.xml`:

```powershell
mvn clean package -DskipTests
mvn spring-boot:run
```

Open:

`http://localhost:8099/`

## Environment
For Razorpay payments, configure matching `RAZORPAY_KEY_ID` and `RAZORPAY_KEY_SECRET` for the same Razorpay mode. For cloud AI chat, configure `AI_PROVIDER=anthropic`, `ANTHROPIC_API_KEY`, `AI_MODEL`, `AI_FAST_MODEL`, and `AI_BASE_URL`. For local Ollama chat, run Ollama with `llama3.2:3b` and leave `AI_PROVIDER=ollama`.


## 2026-09-08 final stability fixes
- Logged-out Subscription always redirects to `#/login`; authenticated users can open Subscription normally.
- Removed the browser-side artificial chatbot timeout that could display a misleading 45/65-second timeout.
- Backend AI timeout defaults to 20 seconds and falls back to a useful local answer instead of leaving the chatbot hanging.
- Advanced Practice coding difficulty is randomized between Entry/Mid/Senior for each generation; the existing `problemType` request bug is fixed.
- Added an app.js cache-busting query so browsers load the latest authentication/chatbot code.

## Subscription one-time login fix
- JWT default lifetime is 30 days so a normal logged-in user does not have to sign in again when opening Razorpay from Subscription.
- Payment API 401/403 responses no longer trigger an automatic `#/login` redirect, preventing an unexpected login-page jump during checkout.
- Razorpay order and verification endpoints remain authenticated; payment security is unchanged.
