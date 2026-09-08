# ChatGPT-style subscription for StudyBuddy AI

The subscription screen is redesigned as a modern AI SaaS plan selector inspired by the simple Free/Plus/Pro pattern users expect from AI products. It does not copy OpenAI branding or UI assets.

## Plans
- Free: ₹0
- Plus: ₹120/month through Razorpay
- Pro: ₹450/year through Razorpay

## Current payment behavior
The existing secure Razorpay order flow is preserved: the server creates the order, Checkout opens in the browser, and the server verifies the Razorpay signature before activating the paid plan.

Paid plans currently use manual renewal because this build uses Razorpay Orders + Checkout. Automatic recurring billing requires creating Razorpay Subscription Plans and storing the resulting subscription IDs/webhook events.

## Important
Do not put `RAZORPAY_KEY_SECRET` in frontend JavaScript.
