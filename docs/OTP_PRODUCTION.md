# Mobile OTP login — development vs production

## Development (default)

Your `.env` can stay minimal:

```bash
AUTH_OTP_DEV_MODE=true
# optional development OTP for everyone:
# AUTH_OTP_DEV_OTP=123456
```

- OTP is logged on the server and returned as `debugOtp` in `POST /api/auth/otp/send`.
- No SMS is sent (`sms-provider=logging` is ignored while `dev-mode=true`).
- OTPs are stored **in memory** (single server process).

## Production checklist

1. **Turn off dev mode**
   ```bash
   AUTH_OTP_DEV_MODE=false
   ```

2. **Redis for OTP + rate limits** (required if you run more than one API instance)
   ```bash
   AUTH_OTP_REDIS_ENABLED=true
   REDIS_HOST=...
   REDIS_PORT=6379
   docker compose up -d redis   # local
   ```

3. **SMS provider** (pick one)
   ```bash
   AUTH_OTP_SMS_PROVIDER=msg91   # or twilio
   ```
   **MSG91:** `MSG91_AUTH_KEY`, `MSG91_TEMPLATE_ID`, optional `MSG91_COUNTRY_CODE=91`  
   **Twilio:** `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_FROM_NUMBER`

4. **Security**
   - Strong `JWT_SECRET` (32+ chars)
   - Restrict `CORS_ALLOWED_ORIGINS` to your app origins
   - TLS in front of the API

5. **Optional tuning**
   - `AUTH_OTP_TTL_SECONDS` (default 300)
   - `AUTH_OTP_MAX_VERIFY_ATTEMPTS` (default 5)
   - `AUTH_OTP_SEND_RATE_LIMIT_PHONE` / `_IP` / `_WINDOW`

## API (unchanged for clients)

- `POST /api/auth/otp/send` `{ "phone": "9876543210" }`
- `POST /api/auth/otp/verify` `{ "phone": "...", "otp": "123456" }`
- `GET /api/auth/me` — Bearer JWT (used on app launch to validate stored session)

## Clients (Android / iOS / Web)

Shared `AuthRepository.bootstrapSession()` restores the token from platform storage and validates it with `/api/auth/me` before showing the main app.
