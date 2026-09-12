package com.daniel.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_checkout_id", columnList = "checkoutRequestId", unique = true),
        @Index(name = "idx_account_ref", columnList = "accountReference")
})
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String checkoutRequestId;

    private String merchantRequestId;
    private String accountReference;
    private String phoneNumber;
    private BigDecimal amount;
    private String description;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status = TransactionStatus.PENDING;

    private String mpesaReceiptNumber;
    private Integer resultCode;
    private String resultDesc;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum TransactionStatus {
        PENDING, SUCCESS, FAILED, CANCELLED, TIMEOUT
    }
}