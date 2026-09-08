# Production Payment + GitHub Checklist

## Razorpay
1. Set `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, and `RAZORPAY_WEBHOOK_SECRET` on the server.
2. Use live keys (`rzp_live_...`) only after completing Razorpay account/payment-method activation.
3. Keep the Standard Checkout script in `index.html`.
4. Test the checkout with the methods enabled on your Razorpay account.
5. Configure Razorpay webhook URL as `https://YOUR-DOMAIN/api/payments/webhook`.
6. Enable relevant payment events such as `payment.captured`, `payment.failed`, and `order.paid`.
7. Keep the webhook secret separate from the Razorpay API key secret.

## GitHub Login
1. Create a GitHub OAuth App.
2. Local callback: `http://localhost:8099/login/oauth2/code/github`.
3. Production callback: `https://YOUR-DOMAIN/login/oauth2/code/github`.
4. Set `GITHUB_CLIENT_ID` and `GITHUB_CLIENT_SECRET` on the server.
5. Keep `read:user,user:email` scope.
6. Never commit client secrets.

## AI Group Discussion
1. Set `ANTHROPIC_API_KEY` for real AI responses.
2. The Group module still has a server fallback if Anthropic is unavailable.
3. The three main buttons call `/api/group/topic`, `/api/group/topics`, and `/api/group/participant`.
4. Evaluation calls `/api/group/evaluate`.
