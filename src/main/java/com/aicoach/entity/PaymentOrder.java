package com.aicoach.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_orders", indexes = {
        @Index(name = "idx_payment_order_razorpay_id", columnList = "razorpayOrderId", unique = true),
        @Index(name = "idx_payment_order_user_id", columnList = "userId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 20)
    private String plan;

    @Column(nullable = false)
    private long amount;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "CREATED";

    @Column(nullable = false, unique = true, length = 80)
    private String razorpayOrderId;

    @Column(length = 80)
    private String razorpayPaymentId;

    @Column(length = 128)
    private String razorpaySignature;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime paidAt;
}
