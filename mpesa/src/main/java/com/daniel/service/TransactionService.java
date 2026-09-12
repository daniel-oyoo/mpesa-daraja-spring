package com.daniel.service;

import com.daniel.dto.response.CallbackResponse;
import com.daniel.exception.TransactionNotFoundException;
import com.daniel.model.Transaction;
import com.daniel.repository.TransactionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository repository;

    @Transactional
    public void processCallback(CallbackResponse callback) {
        JsonNode stkCallback = callback.getBody().getStkCallback();
        String checkoutId = stkCallback.get("CheckoutRequestID").asText();
        int resultCode = stkCallback.get("ResultCode").asInt();
        String resultDesc = stkCallback.get("ResultDesc").asText();

        Transaction tx = repository.findByCheckoutRequestId(checkoutId)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found: " + checkoutId));

        tx.setResultCode(resultCode);
        tx.setResultDesc(resultDesc);

        if (resultCode == 0 && stkCallback.has("CallbackMetadata")) {
            JsonNode items = stkCallback.get("CallbackMetadata").get("Item");
            for (JsonNode item : items) {
                String name = item.get("Name").asText();
                JsonNode value = item.get("Value");
                if ("MpesaReceiptNumber".equals(name)) {
                    tx.setMpesaReceiptNumber(value.asText());
                }
            }
            tx.setStatus(Transaction.TransactionStatus.SUCCESS);
        } else {
            tx.setStatus(mapStatus(resultCode));
        }

        tx.setUpdatedAt(LocalDateTime.now());
        repository.save(tx);

        log.info("Callback processed: checkoutId={}, resultCode={}, status={}",
                checkoutId, resultCode, tx.getStatus());
    }

    private Transaction.TransactionStatus mapStatus(int resultCode) {
        return switch (resultCode) {
            case 1037 -> Transaction.TransactionStatus.CANCELLED;
            case 1032 -> Transaction.TransactionStatus.FAILED;
            case 2001 -> Transaction.TransactionStatus.TIMEOUT;
            default -> Transaction.TransactionStatus.FAILED;
        };
    }
}