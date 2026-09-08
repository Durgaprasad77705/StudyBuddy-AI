# Razorpay Setup

StudyBuddy AI now uses Razorpay Standard Checkout for the paid plans. The server creates every Razorpay order, stores the order locally, verifies the checkout signature, checks payment status when possible, and exposes an optional webhook endpoint for production reconciliation.

## Plans

- 1 month free trial: ₹0
- Monthly: ₹120
- Yearly: ₹450

Razorpay amounts are sent in paise: ₹120 = `12000`, ₹450 = `45000`.

## Test mode

Set these environment variables before starting Spring Boot:

```text
RAZORPAY_KEY_ID=rzp_test_...
RAZORPAY_KEY_SECRET=...
RAZORPAY_WEBHOOK_SECRET=...   # optional for local testing
```

Never put `RAZORPAY_KEY_SECRET` in JavaScript, HTML, GitHub, or any public repository. Only the Key ID is sent to the browser.

## Local run

PowerShell example:

```powershell
$env:RAZORPAY_KEY_ID="rzp_test_xxxxxxxxxx"
$env:RAZORPAY_KEY_SECRET="your_test_secret"
mvn spring-boot:run
```

Open `http://localhost:8099/`, sign in, then open **Subscription**.

## Payment flow

1. The user clicks Monthly or Yearly.
2. Spring Boot calls Razorpay Orders API and stores the order in the local `payment_orders` table.
3. Razorpay Checkout opens with that server-created `order_id`.
4. After checkout, the browser sends `razorpay_order_id`, `razorpay_payment_id`, and `razorpay_signature` to Spring Boot.
5. Spring Boot loads the original order from the database, verifies the HMAC-SHA256 signature, checks the payment status when Razorpay is reachable, and only then activates the subscription.
6. For production, configure a Razorpay webhook at `/api/payments/webhook` and use `RAZORPAY_WEBHOOK_SECRET` so payment capture is also reconciled if the browser closes immediately after checkout.

## Go live

Use Razorpay Live Mode only after completing account onboarding/KYC and deploying the application behind HTTPS. Replace the test Key ID/Secret with Live credentials in environment variables; never commit them to source control.


## Payment methods and subscription behavior

This project uses Razorpay Standard Checkout with server-created Orders. Checkout can present the payment methods enabled for your Razorpay account, including UPI, cards, netbanking, wallets and EMI where supported. Do not hard-code `method: "upi"` if you want customers to choose among available methods.

Important: the current Plus/Pro implementation is a **paid access plan with manual renewal**, not an automatically recurring Razorpay Subscription. The browser payment is verified server-side and the plan is activated only after successful verification. If you need automatic monthly/yearly charging, use Razorpay Subscriptions with configured subscription plan IDs and enable the supported recurring methods in the Razorpay Dashboard.

For UPI, use Razorpay's currently supported UPI Intent/QR flows in live mode. Test-mode payment options are simulated by Razorpay.

## Webhook

The production webhook endpoint is:

`POST /api/payments/webhook`

It is intentionally public to Razorpay, but the controller validates `X-Razorpay-Signature` with `RAZORPAY_WEBHOOK_SECRET` before processing the event. Configure the webhook only on a public HTTPS deployment; Razorpay does not deliver production webhooks to localhost.


## Standard Checkout implementation checklist

- Backend order endpoint: `POST /api/payments/order` (authenticated; equivalent to `/api/create-order`).
- Frontend loads Razorpay Checkout from `https://checkout.razorpay.com/v1/checkout.js`.
- The backend returns only `keyId`, `orderId`, `amount`, `currency`, and plan metadata; the Key Secret is never sent to the browser.
- Frontend sends `razorpay_payment_id`, `razorpay_order_id`, and `razorpay_signature` to `POST /api/payments/verify`.
- Backend verifies HMAC-SHA256 over `order_id + "|" + payment_id` with the server-side Key Secret before activating a plan.
- Checkout cancellation and `payment.failed` are handled in the UI.
- Razorpay 401 responses are surfaced as an actionable authentication error instead of a generic gateway error.
- This project uses direct HTTPS calls to Razorpay's REST API via Spring `RestTemplate`; no Razorpay Java SDK is required.
