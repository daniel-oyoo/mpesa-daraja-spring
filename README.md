<p align="center">
 <img src ="mpesa\assets\interaction-flow-diagram.jpg" alt = "Mpesa stk push flow diagram " width=480/>
</p>

# M-Pesa Daraja STK Push API

A Spring Boot 3 integration for Safaricom Daraja STK Push payments. It provides a protected API for starting payments, persists transaction state, and processes Safaricom callbacks.

## Capabilities

- OAuth access-token caching for Daraja
- STK Push initiation with duplicate pending-request protection
- Transaction status polling and callback persistence
- Bean validation and consistent JSON errors
- Stateless HTTP Basic authentication for application endpoints
- Public Safaricom callback route, static demo UI, and OpenAPI documentation
- H2 for local development and PostgreSQL for Docker deployments
- Unit tests, Spring MockMvc integration tests, and GitHub Actions CI

## Requirements

- Java 21
- Maven 3.9+
- Docker Desktop (optional)
- Daraja sandbox or production credentials

## Configuration

Copy `mpesa/.env.example` to `mpesa/.env` and replace every placeholder. Never commit `.env` or credentials.

The API uses HTTP Basic authentication. Send `API_USERNAME` and `API_PASSWORD` credentials for the STK Push and status endpoints. Use a long random password in every shared environment. The callback endpoint is intentionally unauthenticated because Safaricom calls it directly; expose it only through HTTPS and restrict it at the reverse proxy or firewall to Safaricom's documented callback traffic where possible.

Important settings:

| Variable | Purpose |
|---|---|
| `MPESA_BASE_URL` | Daraja base URL, sandbox by default |
| `MPESA_CONSUMER_KEY` / `MPESA_CONSUMER_SECRET` | Daraja OAuth credentials |
| `MPESA_SHORTCODE` / `MPESA_PASSKEY` | STK Push credentials |
| `API_USERNAME` / `API_PASSWORD` | Client credentials for protected endpoints |
| `DDL_AUTO` | Use `update` locally; use `validate` with managed production migrations |
| `API_USERNAME` / `API_PASSWORD` | Client credentials for protected endpoints |
| `DDL_AUTO` | Use `update` locally; use `validate` with managed production migrations |

## Run Locally

From the `mpesa` directory:

```bash
cp .env.example .env
# Load the variables in your shell, then start the application.
mvn spring-boot:run
```

On PowerShell, load the file manually or set variables with `$env:NAME = "value"` before running Maven. The local H2 database is in-memory by default.

The demo UI is available at `http://localhost:8080`. OpenAPI is available at `/swagger-ui/index.html`.

## Run With Docker

```bash
docker compose --env-file .env -f docker/docker-compose.yml up --build
```

The compose deployment uses PostgreSQL and requires the credentials listed in `.env.example`.

## API

### Start an STK Push

```bash
curl -u "$API_USERNAME:$API_PASSWORD" \
  -H "Content-Type: application/json" \
  -X POST http://localhost:8080/api/v1/mpesa/stk-push \
  -d '{"phoneNumber":"254712345678","amount":"100.00","accountReference":"ORDER-12345","description":"Payment for Order 12345"}'
```

### Read Transaction Status

```bash
curl -u "$API_USERNAME:$API_PASSWORD" \
  http://localhost:8080/api/v1/mpesa/stk-push/status/{checkoutRequestId}
```

### Safaricom Callback

`POST /api/v1/mpesa/callback` accepts the Daraja callback payload and returns the acknowledgement expected by Safaricom. The complete request and error contract is documented in [mpesa/src/main/resources/api-specification.md](mpesa/src/main/resources/api-specification.md).

## Test and Build

```bash
mvn clean verify
```

Run that command from `mpesa`. Unit tests cover password generation and callback state transitions. The integration suite starts the full Spring context with H2 and verifies authentication, request IDs, and protected transaction status access.

## CI and GitHub

GitHub Actions runs `mvn -B clean verify` for pushes and pull requests targeting `main`. Before publishing a repository:

1. Create an empty GitHub repository.
2. Review `.env`, build output, and `git diff --check`; do not publish secrets.
3. Set the remote and push:

```bash
git remote add origin https://github.com/<your-account>/<your-repository>.git
git add .
git commit -m "Prepare production-ready M-Pesa integration"
git branch -M main
git push -u origin main
```

## Troubleshooting

Having issues with M-Pesa Daraja authentication or setup? Check the **[Troubleshooting Guide](./mpesa/help/HELP.md)**, or view the specific challenges encountered and their fixes in the **[Problem Log](./mpesa/help/problems.txt)**.

## License

MIT
