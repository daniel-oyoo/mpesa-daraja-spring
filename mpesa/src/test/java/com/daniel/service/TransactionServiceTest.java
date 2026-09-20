package com.daniel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.daniel.dto.response.CallbackResponse;
import com.daniel.model.Transaction;
import com.daniel.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository repository;

    @InjectMocks
    private TransactionService transactionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void marksSuccessfulCallbackAndStoresReceipt() throws Exception {
        Transaction transaction = pendingTransaction();
        when(repository.findByCheckoutRequestId("ws_CO_123")).thenReturn(Optional.of(transaction));

        transactionService.processCallback(callback("""
                {"Body":{"stkCallback":{"CheckoutRequestID":"ws_CO_123","ResultCode":0,
                "ResultDesc":"Success","CallbackMetadata":{"Item":[
                {"Name":"MpesaReceiptNumber","Value":"RCP123"}]}}}}
                """));

        assertThat(transaction.getStatus()).isEqualTo(Transaction.TransactionStatus.SUCCESS);
        assertThat(transaction.getResultCode()).isZero();
        assertThat(transaction.getMpesaReceiptNumber()).isEqualTo("RCP123");
        verify(repository).save(transaction);
    }

    @Test
    void mapsCancelledCallbackToCancelledStatus() throws Exception {
        Transaction transaction = pendingTransaction();
        when(repository.findByCheckoutRequestId("ws_CO_123")).thenReturn(Optional.of(transaction));

        transactionService.processCallback(callback("""
                {"Body":{"stkCallback":{"CheckoutRequestID":"ws_CO_123","ResultCode":1037,
                "ResultDesc":"Cancelled by user"}}}
                """));

        assertThat(transaction.getStatus()).isEqualTo(Transaction.TransactionStatus.CANCELLED);
        verify(repository).save(any(Transaction.class));
    }

    private Transaction pendingTransaction() {
        Transaction transaction = new Transaction();
        transaction.setCheckoutRequestId("ws_CO_123");
        transaction.setStatus(Transaction.TransactionStatus.PENDING);
        return transaction;
    }

    private CallbackResponse callback(String json) throws Exception {
        return objectMapper.readValue(json, CallbackResponse.class);
    }
}