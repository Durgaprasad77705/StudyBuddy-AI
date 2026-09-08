# Razorpay + Group AI Fixes

## Razorpay

- Added server-side Razorpay order persistence with `PaymentOrder`.
- Monthly plan: ₹120; yearly plan: ₹450.
- Checkout uses the server-created Razorpay `order_id`.
- Server verifies HMAC-SHA256 signatures before activating a plan.
- Payment status is checked when Razorpay is reachable.
- Added subscription status endpoint: `GET /api/payments/status`.
- Added optional signed webhook endpoint: `POST /api/payments/webhook`.
- Razorpay secret values remain server-side environment variables.

## Group AI

The old Group module depended directly on a hard-coded local Ollama URL/model. That caused all three main Group buttons to fail when Ollama was not installed/running.

The Group module now uses the project's existing AI provider abstraction and has an offline fallback:

1. **Generate Related Topic** → `/api/group/topic`
2. **Generate 10 Topics** → `/api/group/topics`
3. **Start Group Discussion** → `/api/group/participant`
4. **Send to AI** continues the discussion.
5. **Evaluate Discussion** → `/api/group/evaluate`

The 10-topic feature is now one backend request instead of ten simultaneous AI requests, which is faster and much less likely to fail.

## Validation performed in this environment

- JavaScript syntax checked with Node.js for `group.js` and `app.js`.
- `pom.xml` parsed successfully as XML.
- Java source brace/syntax-structure checks passed.
- Full Maven build could not be executed in this container because Maven is not installed. Run the project's normal Maven build on Windows with `mvn clean package -DskipTests`.


## Additional reliability fixes in this build

- Razorpay webhook is now reachable without a user JWT; its HMAC signature is still mandatory.
- Removed the unsupported per-order `capture` request field; capture is controlled by Razorpay account configuration.
- Subscription checkout leaves payment-method selection to Razorpay Checkout instead of forcing UPI/card.
- The subscription page now clearly explains UPI/cards/netbanking/wallet/EMI availability.
- Group Discussion preserves the transcript when a live AI response fails, so the user can retry without losing the discussion.
