Here is the cleaned-up version following your exact structure, with the format template placed right at the top:

---

### Format Template

```text
1. Problem
2. Cause / Reasons
3. Why (Root Mechanism)
4. Fixes
5. Why Fixes Work
6. How to Avoid

```

---

## 1. Failed to Obtain M-Pesa Daraja OAuth Access Token

### 1. Problem

Failing to obtain an M-Pesa Daraja OAuth access token (`GET /oauth/v1/generate?grant_type=client_credentials`), resulting in authentication errors (`400`/`401`), gateway blocks, or null/invalid credentials inside the application.

---

### 2. Cause / Reasons

#### A. Immediate / Most Likely Causes (Check These First)

* **Missing `User-Agent` and `Content-Type` headers:** Safaricom's Web Application Firewall (WAF) flags requests without standard identifying headers as automated bot traffic and blocks them regardless of whether your keys are valid.
* **Application not loading `.env` values:** In Spring Boot, `application.yml` falls back to default or empty values if `.env` is not explicitly imported, sending blank/placeholder credentials instead of your actual keys.

#### B. Credential & Environment Mismatches

* **Mixing Sandbox and Production credentials:** Using sandbox keys against `[https://api.safaricom.co.ke](https://api.safaricom.co.ke)`—or live keys against `[https://sandbox.safaricom.co.ke](https://sandbox.safaricom.co.ke)`.
* **Swapped Key and Secret:** Encoding `ConsumerSecret:ConsumerKey` instead of `ConsumerKey:ConsumerSecret`.
* **Trailing whitespace or hidden newlines:** Copying keys with extra spaces or encoding via CLI (`echo "key:secret" | base64` without `-w 0`), which appends a `\n` newline character to the Base64 string.
* **Regenerated or revoked credentials:** Clicking **Regenerate** on the Daraja portal immediately invalidates old keys.

#### C. HTTP Request & Header Formatting Errors

* **Using `POST` instead of `GET`:** The OAuth endpoint strictly requires HTTP `GET`. Sending `POST` returns `405 Method Not Allowed` or `404.001.04`.
* **Missing or misspelled query parameter:** Omitting `?grant_type=client_credentials` or typing `client_credential` (singular) triggers error `400.008.02`.
* **Malformed `Authorization` header:** Using `Bearer` instead of `Basic` (`400.008.01`), missing the space after `Basic`, double-encoding the Base64 string, or wrapping credentials in quotes.
* **Proxy or redirect stripping headers:** Reverse proxies, corporate firewalls, or `http://` to `https://` redirects stripping the `Authorization` header in transit.

#### D. Daraja Portal & Go-Live Provisioning Issues

* **Unapproved Live App or API Product:** Newly created Go-Live apps generate keys before Safaricom's API team finishes backend activation or API product assignment.
* **App not subscribed to any API product:** Creating an app without checking at least one API product box (e.g., *Lipa Na M-Pesa Sandbox* or *M-Pesa Express*).
* **Suspended or inactive Daraja account/shortcode:** Administrative holds on the parent shortcode or developer account.

#### E. Rate Limiting, Network & Server-Side Failures

* **Rate limiting (`429 Too Many Requests`):** Requesting a fresh token on every API call instead of reusing it during its `3600`-second (1-hour) validity window.
* **Outdated TLS/SSL cipher suites:** Legacy runtimes (e.g., older Java 8 or OpenSSL builds) failing Safaricom's TLS 1.2+ requirement during the SSL handshake.
* **DNS, firewall, or cloud IP blocks:** Outbound port `443` blocked on your host, or Safaricom's CDN/WAF blocking specific cloud IP ranges.
* **Safaricom Daraja downtime:** Transient `500`, `502`, `503`, or `504` gateway errors on Safaricom's side.

---

### 3. Why (Root Mechanism)

Safaricom's API gateway validates incoming token requests in strict layers before issuing a token:

1. **Perimeter WAF inspection:** Rejects bare HTTP requests lacking `User-Agent`/`Content-Type` headers before reaching the auth service.
2. **Environment & credential resolution:** Decodes the `Basic` Base64 string (`ConsumerKey:ConsumerSecret`) byte-for-byte against the specific environment's database (Sandbox vs. Production). Any hidden newline, uninjected `.env` default, or swapped pair produces a completely different Base64 hash, causing immediate rejection.

---

### 4. Fixes

#### Step 1: Isolate the Issue with `curl`

Run this directly from the server or machine hosting your app (swap `sandbox.safaricom.co.ke` for `api.safaricom.co.ke` for live keys):

```bash
curl -i -X GET "https://sandbox.safaricom.co.ke/oauth/v1/generate?grant_type=client_credentials" \
  -u "YOUR_CONSUMER_KEY:YOUR_CONSUMER_SECRET" \
  -H "User-Agent: Mozilla/5.0" \
  -H "Content-Type: application/json"

```

* **If `200 OK` returns an `access_token`:** Your keys, portal setup, and network are valid—the bug is in your application code or `.env` loading.
* **If `400` / `401` returns:** Recheck environment URLs, whitespace, or contact `apisupport@safaricom.co.ke` with your App Name and Consumer Key if your Go-Live app is still pending backend activation.

#### Step 2: Explicitly Import `.env` in Spring Boot (`application.yml`)

Add the `spring.config.import` directive so Spring Boot loads `.env` instead of falling back to defaults:

```yaml
spring:
  config:
    import: optional:file:.env[.properties]

```

#### Step 3: Add Required Headers & Verify Loaded Config in Code

* Explicitly attach `User-Agent` and `Content-Type: application/json` alongside `Authorization: Basic <base64(key:secret)>` using an HTTP `GET` request.
* Log/print the resolved configuration properties on startup (masking the secret) or write a unit test asserting that the injected key/secret are non-null and do not equal fallback defaults.

---

### 5. Why Fixes Work

* **Adding `User-Agent` and `Content-Type` headers:** Satisfies Safaricom's WAF rules so the request is treated as a legitimate client rather than dropped as bot traffic.
* **`optional:file:.env[.properties]`:** Instructs Spring Boot's config loader to parse your project-root `.env` file into the environment property sources before resolving `${...}` placeholders in `application.yml`, while `optional:` ensures production deployments using real OS environment variables won't crash if `.env` is absent.
* **Isolating with `curl`:** Eliminates application-layer variables (YAML parsing, Base64 encoding bugs, missing headers) in seconds so you know immediately whether to fix code or portal credentials.

---

### 6. How to Avoid

* **Write startup validation / config tests:** Add an integration or startup check that fails fast if `consumerKey` or `consumerSecret` is blank, contains whitespace, or matches placeholder defaults.
* **Cache the OAuth token:** Store the token in memory or Redis for ~50–55 minutes (just under its 3,600-second expiry) instead of calling `/oauth/v1/generate` per transaction, preventing `429` rate-limit blocks.
* **Separate environment profiles:** Use `application-sandbox.yml` and `application-prod.yml` so sandbox keys and live URLs can never be mixed.
* **Trim secrets programmatically:** Call `.trim()` on loaded keys before Base64 encoding to neutralize accidental trailing spaces or newlines from copy-pasting.