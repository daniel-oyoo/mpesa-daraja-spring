package com.daniel.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;

class MpesaPasswordGeneratorTest {

    @Test
    void generatesDarajaPasswordUsingBase64EncodedCredentials() {
        String password = MpesaPasswordGenerator.generatePassword("174379", "passkey", "20260920153000");

        assertThat(password).isEqualTo(Base64.getEncoder().encodeToString(
                "174379passkey20260920153000".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void generatesTimestampInDarajaFormat() {
        assertThat(MpesaPasswordGenerator.generateTimestamp())
                .matches("\\d{14}");
    }
}