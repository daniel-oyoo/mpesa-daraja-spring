package com.daniel.service;

import com.daniel.config.MpesaConfig;
import com.daniel.dto.response.AuthResponse;
import com.daniel.exception.MpesaUnavailableException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.rmi.UnknownHostException;
import java.time.Instant;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class MpesaAuthService {

    private final MpesaConfig config;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String cachedToken;
    private Instant tokenExpiry = Instant.MIN;

    public synchronized String getAccessToken() {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiry.minusSeconds(60))) {
            return cachedToken;
        }
        return refreshToken();
    }

    private String refreshToken() {
        String credentials = config.getConsumerKey() + ":" + config.getConsumerSecret();
        String auth = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        Request request = new Request.Builder()
                .url(config.getBaseUrl() + "/oauth/v1/generate?grant_type=client_credentials")
                .get()
                .addHeader("Authorization", "Basic " + auth)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new RuntimeException("Failed to obtain M-Pesa access token: " + response.code());
            }
            AuthResponse authResponse = objectMapper.readValue(
                    response.body().string(), AuthResponse.class);
            cachedToken = authResponse.getAccessToken();
            tokenExpiry = Instant.now().plusSeconds(Long.parseLong(authResponse.getExpiresIn()));
            log.debug("M-Pesa access token refreshed, expires in {}s", authResponse.getExpiresIn());
            return cachedToken;
        }catch(UnknownHostException e){
            throw new MpesaUnavailableException("Mpesa service unavailable ",e);
        }catch (IOException e) {
            throw new RuntimeException("M-Pesa auth request failed", e);
        }
    }
}