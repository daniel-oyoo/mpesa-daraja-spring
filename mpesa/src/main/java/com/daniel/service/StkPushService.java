package com.daniel.service;

import com.daniel.config.MpesaConfig;
import com.daniel.dto.request.MpesaStkPushRequest;
import com.daniel.dto.request.StkPushRequest;
import com.daniel.dto.response.StkPushResponse;
import com.daniel.exception.DuplicateRequestException;
import com.daniel.exception.MpesaUnavailableException;
import com.daniel.model.Transaction;
import com.daniel.repository.TransactionRepository;
import com.daniel.util.MpesaPasswordGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.UnknownHostException;

@Slf4j
@Service
@RequiredArgsConstructor
public class StkPushService {

    private final MpesaConfig config;
    private final MpesaAuthService authService;
    private final TransactionRepository transactionRepository;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Transactional
    public StkPushResponse initiateStkPush(StkPushRequest request) {
        // Idempotency check
        if (transactionRepository.existsByAccountReferenceAndStatus(
                request.getAccountReference(), Transaction.TransactionStatus.PENDING)) {
            throw new DuplicateRequestException(
                    "A pending transaction already exists for reference: "
                            + request.getAccountReference());
        }

        String timestamp = MpesaPasswordGenerator.generateTimestamp();
        String password = MpesaPasswordGenerator.generatePassword(
                config.getShortcode(), config.getPasskey(), timestamp);

        MpesaStkPushRequest mpesaRequest = MpesaStkPushRequest.builder()
                .businessShortCode(config.getShortcode())
                .password(password)
                .timestamp(timestamp)
                .transactionType(config.getTransactionType())
                .amount(request.getAmount())
                .partyA(request.getPhoneNumber())
                .partyB(config.getShortcode())
                .phoneNumber(request.getPhoneNumber())
                .callBackUrl(config.getCallbackUrl())
                .accountReference(request.getAccountReference())
                .transactionDesc(request.getDescription())
                .build();

        StkPushResponse response = callMpesa(mpesaRequest);

        // Persist transaction
        Transaction tx = new Transaction();
        tx.setCheckoutRequestId(response.getCheckoutRequestId());
        tx.setMerchantRequestId(response.getMerchantRequestId());
        tx.setAccountReference(request.getAccountReference());
        tx.setPhoneNumber(request.getPhoneNumber());
        tx.setAmount(new BigDecimal(request.getAmount()));
        tx.setDescription(request.getDescription());
        tx.setStatus(Transaction.TransactionStatus.PENDING);
        transactionRepository.save(tx);

        log.info("STK Push initiated: checkoutId={}, ref={}",
                response.getCheckoutRequestId(), request.getAccountReference());

        return response;
    }

    private StkPushResponse callMpesa(MpesaStkPushRequest mpesaRequest) {
        try {
            String json = objectMapper.writeValueAsString(mpesaRequest);
            Request request = new Request.Builder()
                    .url(config.getBaseUrl() + "/mpesa/stkpush/v1/processrequest")
                    .post(RequestBody.create(json, JSON))
                    .addHeader("Authorization", "Bearer " + authService.getAccessToken())
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.body() == null) {
                    throw new RuntimeException("Empty response from M-Pesa");
                }
                String body = response.body().string();
                if (!response.isSuccessful()) {
                    log.error("M-Pesa STK Push failed: {} - {}", response.code(), body);
                    throw new RuntimeException("M-Pesa error: " + body);
                }
                return objectMapper.readValue(body, StkPushResponse.class);
            }
        }catch (UnknownHostException e) {
               throw new MpesaUnavailableException("M-Pesa is unreachable", e);
        }catch (IOException e) {
              throw new RuntimeException("Failed to process json ", e);
        }
    }
}