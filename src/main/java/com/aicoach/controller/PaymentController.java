package com.aicoach.controller;

import com.aicoach.entity.PaymentOrder;
import com.aicoach.entity.User;
import com.aicoach.repository.PaymentOrderRepository;
import com.aicoach.repository.UserRepository;
import com.aicoach.security.UserPrincipal;
import com.aicoach.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PaymentOrderRepository paymentOrderRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${razorpay.key-id:}")
    private String keyId;

    @Value("${razorpay.key-secret:}")
    private String keySecret;

    @Value("${razorpay.webhook-secret:}")
    private String webhookSecret;

    private record Plan(String code, long amountPaise, String label, int months) {}

    private Plan plan(String code) {
        return switch (code) {
            case "MONTHLY" -> new Plan("MONTHLY", 12_000L, "Monthly Subscription", 1);
            case "YEARLY" -> new Plan("YEARLY", 45_000L, "Yearly Subscription", 12);
            default -> null;
        };
    }

    @PostMapping("/order")
    public ResponseEntity<?> createOrder(@AuthenticationPrincipal UserPrincipal principal,
                                         @RequestHeader(value = "Authorization", required = false) String authorization,
                                         @RequestBody Map<String, String> request) {
        String planCode = Optional.ofNullable(request.get("plan")).orElse("").toUpperCase(Locale.ROOT);
        Plan plan = plan(planCode);

        if (plan == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Choose MONTHLY or YEARLY."));
        }
        if (keyId.isBlank() || keySecret.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "message", "Razorpay is not configured. Add RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET to the environment."
            ));
        }
        Long userId = resolveUserId(principal, authorization);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Authentication required."));
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBasicAuth(keyId, keySecret);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("amount", plan.amountPaise());
            body.put("currency", "INR");
            body.put("receipt", "aic_" + userId + "_" + System.currentTimeMillis());
            body.put("notes", Map.of(
                    "plan", plan.code(),
                    "userId", String.valueOf(userId),
                    "product", "StudyBuddy AI"
            ));

            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://api.razorpay.com/v1/orders",
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            Map<?, ?> data = response.getBody();
            if (data == null || data.get("id") == null) {
                throw new IllegalStateException("Razorpay returned an empty order response.");
            }

            String razorpayOrderId = String.valueOf(data.get("id"));
            paymentOrderRepository.save(PaymentOrder.builder()
                    .userId(userId)
                    .plan(plan.code())
                    .amount(plan.amountPaise())
                    .razorpayOrderId(razorpayOrderId)
                    .status("CREATED")
                    .build());

            return ResponseEntity.ok(Map.of(
                    "keyId", keyId,
                    "orderId", razorpayOrderId,
                    "amount", plan.amountPaise(),
                    "currency", "INR",
                    "plan", plan.code(),
                    "description", plan.label()
            ));
        } catch (RestClientResponseException e) {
            int razorpayStatus = e.getStatusCode().value();
            String detail = e.getResponseBodyAsString();
            if (razorpayStatus == 401) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "success", false,
                        "message", "Razorpay authentication failed (401). Check that RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET are a matching pair from the same Razorpay mode/account."
                ));
            }
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                    "success", false,
                    "message", "Razorpay order creation failed (HTTP " + razorpayStatus + ").",
                    "details", detail == null ? "" : detail
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Unable to create Razorpay order. " + safeMessage(e)
            ));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@AuthenticationPrincipal UserPrincipal principal,
                                    @RequestHeader(value = "Authorization", required = false) String authorization,
                                    @RequestBody Map<String, String> request) {
        String orderId = request.get("razorpay_order_id");
        String paymentId = request.get("razorpay_payment_id");
        String signature = request.get("razorpay_signature");
        String planCode = Optional.ofNullable(request.get("plan")).orElse("").toUpperCase(Locale.ROOT);

        Long userId = resolveUserId(principal, authorization);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Authentication required."));
        }
        if (isBlank(orderId) || isBlank(paymentId) || isBlank(signature)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Incomplete payment verification data."));
        }
        if (keySecret.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("message", "Razorpay secret is not configured."));
        }

        try {
            PaymentOrder paymentOrder = paymentOrderRepository.findByRazorpayOrderId(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Payment order was not created by this application."));

            if (!paymentOrder.getUserId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "This payment order does not belong to the logged-in user."));
            }
            if (!paymentOrder.getPlan().equals(planCode)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Payment plan does not match the server order."));
            }
            if ("PAID".equals(paymentOrder.getStatus())) {
                return subscriptionResponse(userId, "Payment already verified.");
            }

            String expected = hmacSha256(orderId + "|" + paymentId, keySecret);
            if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8))) {
                paymentOrder.setStatus("SIGNATURE_FAILED");
                paymentOrderRepository.save(paymentOrder);
                return ResponseEntity.badRequest().body(Map.of("message", "Payment signature verification failed."));
            }

            String paymentStatus = fetchPaymentStatus(paymentId);
            if (paymentStatus != null && !"captured".equalsIgnoreCase(paymentStatus)) {
                paymentOrder.setRazorpayPaymentId(paymentId);
                paymentOrder.setRazorpaySignature(signature);
                paymentOrder.setStatus(paymentStatus.toUpperCase(Locale.ROOT));
                paymentOrderRepository.save(paymentOrder);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                        "message", "Payment is verified but not captured yet. Current status: " + paymentStatus
                ));
            }

            activateSubscription(userId, paymentOrder, paymentId, signature);
            return subscriptionResponse(userId, "Payment verified and subscription activated.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "message", "Payment verification failed. " + safeMessage(e)
            ));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> status(@AuthenticationPrincipal UserPrincipal principal,
                                      @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = resolveUserId(principal, authorization);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Authentication required."));
        return subscriptionResponse(userId, "");
    }

    @PostMapping("/trial")
    public ResponseEntity<?> startTrial(@AuthenticationPrincipal UserPrincipal principal,
                                          @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = resolveUserId(principal, authorization);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Authentication required."));
        User user = userRepository.findById(userId).orElseThrow();
        if (user.isTrialUsed()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Your free one-month trial has already been used."));
        }
        user.setTrialUsed(true);
        user.setSubscriptionPlan("TRIAL");
        user.setSubscriptionEndsAt(LocalDateTime.now().plusMonths(1));
        userRepository.save(user);
        return subscriptionResponse(userId, "Your 1-month free trial is active.");
    }

    /**
     * Optional production webhook endpoint. Configure RAZORPAY_WEBHOOK_SECRET in the server
     * and point the Razorpay Dashboard webhook URL at /api/payments/webhook.
     */
    @PostMapping("/webhook")
    public ResponseEntity<?> webhook(@RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
                                     @RequestBody String rawBody) {
        if (webhookSecret.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("message", "Webhook secret is not configured."));
        }
        if (isBlank(signature)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Missing webhook signature."));
        }
        try {
            String expected = hmacSha256(rawBody, webhookSecret);
            if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8))) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid webhook signature."));
            }

            JsonNode root = objectMapper.readTree(rawBody);
            String event = root.path("event").asText("");
            JsonNode paymentEntity = root.path("payload").path("payment").path("entity");
            JsonNode orderEntity = root.path("payload").path("order").path("entity");
            String orderId = firstNonBlank(
                    paymentEntity.path("order_id").asText(null),
                    orderEntity.path("id").asText(null)
            );
            String paymentId = paymentEntity.path("id").asText(null);

            if (orderId != null) {
                paymentOrderRepository.findByRazorpayOrderId(orderId).ifPresent(order -> {
                    if (("payment.captured".equals(event) || "order.paid".equals(event))
                            && !"PAID".equals(order.getStatus())) {
                        order.setStatus("PAID");
                        if (paymentId != null) order.setRazorpayPaymentId(paymentId);
                        order.setPaidAt(LocalDateTime.now());
                        paymentOrderRepository.save(order);
                        userRepository.findById(order.getUserId()).ifPresent(user -> {
                            Plan p = plan(order.getPlan());
                            if (p != null) {
                                user.setSubscriptionPlan(p.code());
                                user.setSubscriptionEndsAt(LocalDateTime.now().plusMonths(p.months()));
                                userRepository.save(user);
                            }
                        });
                    } else if ("payment.failed".equals(event) && !"PAID".equals(order.getStatus())) {
                        order.setStatus("FAILED");
                        paymentOrderRepository.save(order);
                    }
                });
            }

            return ResponseEntity.ok(Map.of("received", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid webhook payload."));
        }
    }

    private void activateSubscription(Long userId, PaymentOrder paymentOrder, String paymentId, String signature) {
        User user = userRepository.findById(userId).orElseThrow();
        Plan plan = plan(paymentOrder.getPlan());
        if (plan == null) throw new IllegalArgumentException("Unknown subscription plan.");

        paymentOrder.setRazorpayPaymentId(paymentId);
        paymentOrder.setRazorpaySignature(signature);
        paymentOrder.setStatus("PAID");
        paymentOrder.setPaidAt(LocalDateTime.now());
        paymentOrderRepository.save(paymentOrder);

        user.setSubscriptionPlan(plan.code());
        user.setSubscriptionEndsAt(LocalDateTime.now().plusMonths(plan.months()));
        userRepository.save(user);
    }

    private ResponseEntity<Map<String, Object>> subscriptionResponse(Long userId, String message) {
        User user = userRepository.findById(userId).orElseThrow();
        boolean active = user.getSubscriptionEndsAt() != null && user.getSubscriptionEndsAt().isAfter(LocalDateTime.now());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", message);
        String plan = user.getSubscriptionPlan();
        String displayPlan = switch (plan) {
            case "MONTHLY" -> "Plus";
            case "YEARLY" -> "Pro";
            case "TRIAL" -> "Free";
            default -> "Free";
        };
        result.put("plan", plan);
        result.put("displayPlan", displayPlan);
        result.put("active", active);
        result.put("endsAt", user.getSubscriptionEndsAt() == null ? null : user.getSubscriptionEndsAt().toString());
        result.put("billing", "Razorpay");
        result.put("renewal", "manual");
        result.put("note", "Paid plans are activated after successful Razorpay payment verification.");
        return ResponseEntity.ok(result);
    }

    private String fetchPaymentStatus(String paymentId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(keyId, keySecret);
            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://api.razorpay.com/v1/payments/" + paymentId,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );
            Map<?, ?> body = response.getBody();
            return body == null || body.get("status") == null ? null : String.valueOf(body.get("status"));
        } catch (Exception ignored) {
            // Signature verification remains authoritative for the checkout callback. Webhook/status
            // reconciliation can confirm capture later when Razorpay is reachable again.
            return null;
        }
    }

    private String hmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String firstNonBlank(String first, String second) {
        return !isBlank(first) ? first : (!isBlank(second) ? second : null);
    }

    /**
     * Resolves the authenticated user from Spring Security first, then from the
     * same signed JWT supplied by the browser. This makes payment endpoints
     * resilient to a SecurityContext/session mismatch without weakening JWT
     * validation. No payment endpoint trusts a user id supplied by the browser.
     */
    private Long resolveUserId(UserPrincipal principal, String authorization) {
        if (principal != null && principal.getUserId() != null) return principal.getUserId();
        if (authorization == null || !authorization.startsWith("Bearer ")) return null;
        try {
            String token = authorization.substring(7).trim();
            String email = jwtService.extractEmail(token);
            Long userId = jwtService.extractUserId(token);
            if (email == null || userId == null) return null;
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null || !user.getId().equals(userId)) return null;
            if (!jwtService.isTokenValid(token, user.getEmail())) return null;
            return userId;
        } catch (Exception e) {
            return null;
        }
    }

    private String safeMessage(Exception e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }
}
