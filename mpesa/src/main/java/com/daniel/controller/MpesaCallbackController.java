package com.daniel.controller;

import com.daniel.dto.response.CallbackResponse;
import com.daniel.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/mpesa")
@RequiredArgsConstructor
public class MpesaCallbackController {

    private final TransactionService transactionService;

    @PostMapping("/callback")
    public ResponseEntity<Map<String, String>> callback(@RequestBody CallbackResponse callback) {
        log.info("Received M-Pesa callback: {}", callback);
        try {
            transactionService.processCallback(callback);
        } catch (Exception e) {
            log.error("Failed to process callback", e);
            // We STILL return 200 so Safaricom doesn't retry
        }
        return ResponseEntity.ok(Map.of(
                "ResultCode", "0",
                "ResultDesc", "Callback processed successfully"
        ));
    }
}