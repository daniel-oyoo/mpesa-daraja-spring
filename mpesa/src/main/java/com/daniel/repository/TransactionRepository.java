package com.daniel.repository;

import com.daniel.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByCheckoutRequestId(String checkoutRequestId);

    boolean existsByAccountReferenceAndStatus(String accountReference, Transaction.TransactionStatus status);
}