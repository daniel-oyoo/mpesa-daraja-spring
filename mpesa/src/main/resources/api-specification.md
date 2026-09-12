# M-Pesa STK Push Integration — API Specification

## Global Error Format

All API errors follow a consistent structure:

```json
{
  "timestamp": "2026-01-03T12:00:00Z",
  "status": 400,
  "message": "Descriptive error message",
  "path": "/api/v1/mpesa/stk-push"
}
```

| Field       | Type      | Description                                      |
|-------------|-----------|--------------------------------------------------|
| `timestamp` | `string`  | ISO-8601 timestamp of when the error occurred    |
| `status`    | `integer` | HTTP status code                                 |
| `message`   | `string`  | Human-readable explanation of the error          |
| `path`      | `string`  | The API endpoint that triggered the error        |

---

## 1. Initiate STK Push (Create Payment)

### Overview
Initiates an M-Pesa STK Push (Lipa Na M-Pesa Online) request. The customer receives a prompt on their phone to enter their M-Pesa PIN.

### Endpoint

```
POST http://localhost:8080/api/v1/mpesa/stk-push
```

### Authentication
| Header          | Value                    |
|-----------------|--------------------------|
| `Authorization` | `Bearer {accessToken}`   |

Access token is obtained using:
- `MPESA_CONSUMER_KEY`
- `MPESA_CONSUMER_SECRET`

### Configuration

| Property                    | Value                          |
|-----------------------------|--------------------------------|
| `mpesa.api.shortcode`       | `174379`                       |
| `MPESA_PASSKEY`             | `your_passkey`                 |
| `Callback URL`              | `https://...` (must be HTTPS)  |

### Request Body (Client → Backend)

```json
{
  "phoneNumber": "254712345678",
  "amount": "100.00",
  "accountReference": "ORDER-12345",
  "description": "Payment for Order #12345"
}
```

| Field              | Type     | Required | Description                                  |
|--------------------|----------|----------|----------------------------------------------|
| `phoneNumber`      | `string` | Yes      | Customer phone number in MSISDN format       |
| `amount`           | `string` | Yes      | Amount to charge (as string, e.g., "100.00") |
| `accountReference` | `string` | Yes      | Unique identifier (e.g., order ID)           |
| `description`      | `string` | Yes      | Transaction description                      |

### Upstream Request (Backend → M-Pesa)

```json
{
  "BusinessShortCode": "174379",
  "Password": "base64(shortcode + passkey + timestamp)",
  "Timestamp": "20260103120000",
  "TransactionType": "CustomerPayBillOnline",
  "Amount": "100.00",
  "PartyA": "254712345678",
  "PartyB": "174379",
  "PhoneNumber": "254712345678",
  "CallBackURL": "https://your-domain.com/api/v1/mpesa/callback/stk-push",
  "AccountReference": "ORDER-12345",
  "TransactionDesc": "Payment for Order #12345"
}
```

> **Note:** `Password` = Base64 encode of `Shortcode + Passkey + Timestamp`.

### Response (M-Pesa → Backend → Client)

```json
{
  "MerchantRequestID": "12345-67890-abcde",
  "CheckoutRequestID": "ws_CO_123456789",
  "ResponseCode": "0",
  "ResponseDescription": "Success. Request accepted for processing",
  "CustomerMessage": "Please check your phone and enter PIN"
}
```

### Error Responses

#### 400 — Client Errors

| Scenario                    | Description                                  |
|-----------------------------|----------------------------------------------|
| Bad Request                 | Malformed request body / wrong format        |
| Unauthorized                | Missing or invalid access token              |
| Duplicate Request           | Idempotency key violation (same order twice) |
| Too Many Requests           | Rate limit exceeded                          |
| Forbidden                   | User not permitted to perform this action    |
| Not Found                   | Wrong path or resource missing               |

#### 500 — Server Errors

| Status | Scenario                                       |
|--------|------------------------------------------------|
| `500`  | Internal server error / unhandled exception    |
| `503`  | M-Pesa service unavailable (host unknown, etc.)|

#### 200 — Success
`ResponseCode: 0` indicates the request was accepted for processing.

---

## 2. Query STK Push Status

### Overview
Polls the status of an STK Push transaction. Intended for **internal/backend use** (e.g., reading from DB) or when the callback has not yet been received.

> This endpoint does **not** call M-Pesa directly; it reads the transaction status from the local database.

### Endpoint

```
GET http://localhost:8080/api/v1/mpesa/stk-push/status/{checkoutId}
```

**Path Variable:** `checkoutId` — e.g., `ws_CO_123456789`

### Request Body (Backend → M-Pesa Query)

```json
{
  "BusinessShortCode": "174379",
  "Password": "base64(shortcode + passkey + timestamp)",
  "Timestamp": "20260103120000",
  "TransactionType": "CustomerPayBillOnline",
  "Amount": "100.00",
  "PartyA": "254712345678",
  "PartyB": "174379",
  "PhoneNumber": "254712345678",
  "CallBackURL": "https://your-domain.com/api/v1/mpesa/callback/stk-push",
  "AccountReference": "ORDER-12345",
  "TransactionDesc": "Payment for Order #12345"
}
```

### Response — Status Codes

| Code   | Status                     |
|--------|----------------------------|
| `0`    | Success — Payment completed|
| `1037` | Cancelled by user          |
| `1032` | Insufficient balance       |
| `2001` | Timeout                    |
| `1021` | Invalid transaction        |

### Error Responses

| Status | Scenario                              |
|--------|---------------------------------------|
| `400`  | Bad request / invalid checkoutId      |
| `401`  | Unauthorized                          |
| `403`  | Forbidden                             |
| `404`  | Transaction not found                 |
| `500`  | Internal server error / service offline|
| `200`  | Success (with status in body)         |

---

## 3. Callback URL (M-Pesa → Backend)

### Overview
Safaricom calls this endpoint to deliver the result of the STK Push. The backend then updates the transaction status in the database, which the frontend polls.

> **Must be HTTPS** — M-Pesa requires a secure callback URL.

### Endpoint

```
POST /api/v1/mpesa/callback
```

### Request Body (Safaricom → Backend)

#### Success Callback

```json
{
  "Body": {
    "stkCallback": {
      "MerchantRequestID": "12345-67890-abcde",
      "CheckoutRequestID": "ws_CO_123456789",
      "ResultCode": 0,
      "ResultDesc": "The service request was processed successfully.",
      "CallbackMetadata": {
        "Item": [
          { "Name": "MpesaReceiptNumber", "Value": "RZXXXXXX" },
          { "Name": "TransactionDate", "Value": "20260103120000" },
          { "Name": "Amount", "Value": 100.00 }
        ]
      }
    }
  }
}
```

#### Failure Callback

```json
{
  "Body": {
    "stkCallback": {
      "MerchantRequestID": "12345-67890-abcde",
      "CheckoutRequestID": "ws_CO_123456789",
      "ResultCode": 1037,
      "ResultDesc": "The transaction was cancelled by the user"
    }
  }
}
```

### Response (Backend → Safaricom)

The backend **always** returns `200 OK` to acknowledge receipt:

```json
{
  "ResultCode": "0",
  "ResultDesc": "Callback processed successfully"
}
```

### Error Responses

| Status | Scenario                             |
|--------|--------------------------------------|
| `400`  | Bad request / malformed payload      |
| `500`  | Internal server error                |
| `200`  | Success — callback acknowledged      |

---

## Summary Table

| # | Action            | Method | Endpoint                                          | Auth       |
|---|-------------------|--------|---------------------------------------------------|------------|
| 1 | Initiate STK Push | POST   | `/api/v1/mpesa/stk-push`                          | Bearer     |
| 2 | Query Status      | GET    | `/api/v1/mpesa/stk-push/status/{checkoutId}`      | Bearer     |
| 3 | Callback (Safaricom) | POST | `/api/v1/mpesa/callback`                        | None (HTTPS)|

---

## Flow Diagram

```
┌──────────┐         ┌──────────────┐         ┌──────────────┐         ┌──────────┐
│  Client  │──(1)───▶│   Backend    │──(2)───▶│  M-Pesa API  │──(3)───▶│ Customer │
│ (Frontend)│        │  (Spring Boot)│        │  (Safaricom) │        │  Phone   │
└──────────┘         └──────────────┘         └──────────────┘         └──────────┘
      │                     │                        │                      │
      │                     │                        │◀────(4) PIN entered ─┘
      │                     │◀───────(5) Callback ───┘
      │◀──(6) Poll status ──│
      │                     │
      ▼                     ▼
   UI updated         DB updated
```

1. Client sends STK Push request
2. Backend forwards to M-Pesa
3. M-Pesa prompts customer's phone
4. Customer enters PIN
5. M-Pesa calls back with result
6. Client polls backend for status

---

## Notes

- All monetary amounts are represented as **strings** (e.g., `"100.00"`) to preserve precision.
- `Password` is generated per-request: `base64(Shortcode + Passkey + Timestamp)`.
- Timestamps use format `YYYYMMDDHHmmss`.
- Callback URLs **must** be HTTPS — Safaricom rejects HTTP.
- The callback endpoint must **always** return `200 OK`, even on internal errors, to prevent Safaricom retries.