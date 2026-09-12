package com.daniel.controller;

import com.daniel.dto.request.StkPushRequest;
import com.daniel.dto.response.StkPushResponse;
import com.daniel.model.Transaction;
import com.daniel.repository.TransactionRepository;
import com.daniel.service.StkPushService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/mpesa")
@RequiredArgsConstructor
public class StkPushController {

    private final StkPushService stkPushService;
    private final TransactionRepository transactionRepository;

    @PostMapping("/stk-push")
    public ResponseEntity<StkPushResponse> initiate(@Valid @RequestBody StkPushRequest request) {
        return ResponseEntity.ok(stkPushService.initiateStkPush(request));
    }

    @GetMapping("/stk-push/status/{checkoutId}")
    public ResponseEntity<Map<String, Object>> status(@PathVariable String checkoutId) {
        Transaction tx = transactionRepository.findByCheckoutRequestId(checkoutId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + checkoutId));

        return ResponseEntity.ok(Map.of(
                "checkoutRequestId", tx.getCheckoutRequestId(),
                "status", tx.getStatus().name(),
                "resultCode", tx.getResultCode() == null ? -1 : tx.getResultCode(),
                "resultDesc", tx.getResultDesc() == null ? "" : tx.getResultDesc(),
                "receipt", tx.getMpesaReceiptNumber() == null ? "" : tx.getMpesaReceiptNumber()
        ));
    }
}