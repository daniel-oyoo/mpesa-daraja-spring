package com.daniel.fuzz;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class MpesaFuzzRunner {

    private static final String BASE_URL = "http://localhost:8080";
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("Starting M-Pesa Live Fuzz Runner (10,000+ cases)...");
        System.out.println("==================================================");

        long startTime = System.currentTimeMillis();

        runStkPushFuzz();
        runCallbackFuzz();

        long duration = (System.currentTimeMillis() - startTime) / 1000;
        System.out.println("==================================================");
        System.out.println("Fuzz testing complete in " + duration + " seconds.");
        System.out.println("==================================================");
    }

    private static void runStkPushFuzz() {
        String[] phones = {
            "254712345678", "0712345678", "+254712345678", "25470000000", 
            "invalid_phone", "", null, "2547abc", "12345", "999999999999999"
        };

        String[] amounts = {
            "100.00", "1.00", "0.00", "0", "-100.00", "-0.01", "abc", "", null, 
            "100.00.00", "999999999.99", "1e2", "NaN", "Infinity", " ", "100,", 
            "null", "true", "100.9999", "0.0001"
        };

        String[] references = {
            "ORDER-123", "", null, "<script>alert(1)</script>", "A".repeat(300),
            "DROP TABLE users;", "' OR '1'='1", "Order #998!", "Spaces inside", "Üñîçøđë"
        };

        String[] tokens = {
            "Bearer valid_token", "Bearer expired_token", "InvalidHeader", "Bearer ", null
        };

        int count = 0;
        int failures = 0;

        for (String phone : phones) {
            for (String amount : amounts) {
                for (String ref : references) {
                    for (String token : tokens) {
                        count++;
                        try {
                            String jsonBody = String.format("""
                                {
                                  "phoneNumber": %s,
                                  "amount": %s,
                                  "accountReference": %s,
                                  "description": "Manual fuzz test payload"
                                }
                                """,
                                phone == null ? "null" : "\"" + phone + "\"",
                                amount == null ? "null" : "\"" + amount + "\"",
                                ref == null ? "null" : "\"" + ref + "\""
                            );

                            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                                    .uri(URI.create(BASE_URL + "/api/v1/mpesa/stk-push"))
                                    .header("Content-Type", "application/json")
                                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

                            if (token != null) {
                                reqBuilder.header("Authorization", token);
                            }

                            HttpResponse<String> response = client.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
                            int status = response.statusCode();

                            // Validate invariant: Status must be a handled API response code
                            if (status != 200 && status != 400 && status != 401 && status != 403 && status != 409 && status != 500) {
                                failures++;
                                System.err.printf("[FAIL] Case #%d -> Unexpected status %d | Phone: %s, Amount: %s%n", 
                                        count, status, phone, amount);
                            } else if (count % 500 == 0) {
                                System.out.printf("[INFO] Progress: Processed %d STK Push fuzz permutations...%n", count);
                            }

                        } catch (Exception e) {
                            failures++;
                            System.err.printf("[ERROR] Case #%d threw exception: %s%n", count, e.getMessage());
                        }
                    }
                }
            }
        }
        System.out.printf("STK Push Fuzz Complete. Total: %d, Invariant Violations/Errors: %d%n", count, failures);
    }

    private static void runCallbackFuzz() {
        System.out.println("Starting Callback Robustness Fuzz...");
        int[] resultCodes = {0, 1037, 1032, 2001, 1021, -1, 9999, 400, 500};
        String[] descriptions = {"Success", "Cancelled", "Timeout", "", null, "A".repeat(500), "<script>xss</script>"};

        int count = 0;
        int failures = 0;

        for (int code : resultCodes) {
            for (String desc : descriptions) {
                count++;
                String safeDesc = desc == null ? "null" : "\"" + desc + "\"";
                String payload = String.format("""
                    {
                      "Body": {
                        "stkCallback": {
                          "MerchantRequestID": "req-%d",
                          "CheckoutRequestID": "ws_CO_%d",
                          "ResultCode": %d,
                          "ResultDesc": %s
                        }
                      }
                    }
                    """, code, code, code, safeDesc);

                executeCallbackTest(payload, 200, count);
            }
        }

        // Structural corruption variations
        String[] corruptions = {"{}", "{\"Body\": {}}", "null", ""};
        for (String corruption : corruptions) {
            count++;
            executeCallbackTest(corruption, 400, count);
        }

        System.out.printf("Callback Fuzz Complete. Total: %d cases tested.%n", count);
    }

    private static void executeCallbackTest(String payload, int expectedStatus, int caseNum) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/v1/mpesa/callback"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();

            if (status != expectedStatus) {
                System.err.printf("[CALLBACK FAIL] Case #%d -> Expected %d but got %s%n", caseNum, expectedStatus, status);
            }
        } catch (Exception e) {
            System.err.printf("[CALLBACK ERROR] Case #%d threw exception: %s%n", caseNum, e.getMessage());
        }
    }
}