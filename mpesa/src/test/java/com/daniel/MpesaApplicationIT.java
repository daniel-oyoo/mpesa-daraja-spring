package com.daniel;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daniel.model.Transaction;
import com.daniel.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "API_PASSWORD=test-password",
        "API_USERNAME=test-client",
        "mpesa.api.consumer-key=test-key",
        "mpesa.api.consumer-secret=test-secret",
        "mpesa.api.passkey=test-passkey"
})
class MpesaApplicationIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        Transaction transaction = new Transaction();
        transaction.setCheckoutRequestId("ws_CO_integration");
        transaction.setStatus(Transaction.TransactionStatus.PENDING);
        transactionRepository.save(transaction);
    }

    @Test
    void protectsTransactionStatusEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/mpesa/stk-push/status/ws_CO_integration"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Request-Id"));
    }

    @Test
    void allowsAuthenticatedTransactionStatusRequest() throws Exception {
        mockMvc.perform(get("/api/v1/mpesa/stk-push/status/ws_CO_integration")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Basic dGVzdC1jbGllbnQ6dGVzdC1wYXNzd29yZA=="))
                .andExpect(status().isOk());
    }

    @Test
    void leavesCallbackEndpointPublicForSafaricom() throws Exception {
        mockMvc.perform(get("/api/v1/mpesa/callback"))
                .andExpect(status().isMethodNotAllowed());
    }
}