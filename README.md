# M-Pesa Daraja Spring Boot Integration

Production-ready M-Pesa STK Push integration using Spring Boot 3.

## Features

-  OAuth token caching
-  STK Push initiation
-  Idempotency check (duplicate prevention)
-  Callback handling with DB persistence
-  Status polling endpoint
-  Global error handling
-  Docker support
-  CI/CD with GitHub Actions

## Quick Start

### 1. Clone

```bash
git clone https://github.com/daniel-oyoo/mpesa-daraja-spring.git
cd mpesa-daraja-spring
```

### 2. Configure

```bash
cp .env.example .env
# Edit .env with your Daraja credentials
```

Get credentials from [https://developer.safaricom.co.ke/](https://developer.safaricom.co.ke/)

### 3. Run

```bash
# Local
./mvnw spring-boot:run

# Docker
docker compose -f docker/docker-compose.yml up --build
```

### 4. Expose callback URL (dev)

```bash
ngrok http 8080
# Copy the HTTPS URL into .env as MPESA_CALLBACK_URL
```

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/mpesa/stk-push` | Initiate STK Push |
| GET | `/api/v1/mpesa/stk-push/status/{checkoutId}` | Query status |
| POST | `/api/v1/mpesa/callback` | Safaricom callback |

## Example Request

```bash
curl -X POST http://localhost:8080/api/v1/mpesa/stk-push \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "254712345678",
    "amount": "100.00",
    "accountReference": "ORDER-12345",
    "description": "Payment for Order #12345"
  }'
```

## License

MIT