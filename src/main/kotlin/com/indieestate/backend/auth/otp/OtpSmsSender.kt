package com.indieestate.backend.auth.otp

fun interface OtpSmsSender {
    /** @param phoneNormalized 10-digit local number (India) */
    fun sendLoginOtp(phoneNormalized: String, otp: String, ttlSeconds: Long)
}

@org.springframework.stereotype.Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = ["auth.otp.sms-provider"],
    havingValue = "logging",
    matchIfMissing = true,
)
class LoggingOtpSmsSender : OtpSmsSender {
    private val log = org.slf4j.LoggerFactory.getLogger(javaClass)

    override fun sendLoginOtp(phoneNormalized: String, otp: String, ttlSeconds: Long) {
        log.info(
            "SMS (logging provider): OTP to {} valid {}s — configure auth.otp.sms-provider for real delivery",
            OtpPhone.mask(phoneNormalized),
            ttlSeconds,
        )
    }
}

@org.springframework.stereotype.Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = ["auth.otp.sms-provider"],
    havingValue = "none",
)
class NoOpOtpSmsSender : OtpSmsSender {
    override fun sendLoginOtp(phoneNormalized: String, otp: String, ttlSeconds: Long) {
        throw com.indieestate.backend.controller.ApiException(
            org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
            "SMS provider is not configured. Set AUTH_OTP_SMS_PROVIDER or enable AUTH_OTP_DEV_MODE for development.",
        )
    }
}

@org.springframework.stereotype.Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = ["auth.otp.sms-provider"],
    havingValue = "msg91",
)
class Msg91OtpSmsSender(
    private val properties: OtpProperties,
) : OtpSmsSender {
    private val log = org.slf4j.LoggerFactory.getLogger(javaClass)

    override fun sendLoginOtp(phoneNormalized: String, otp: String, ttlSeconds: Long) {
        val cfg = properties.msg91
        require(cfg.authKey.isNotBlank() && cfg.templateId.isNotBlank()) {
            "MSG91 requires auth.otp.msg91.auth-key and template-id (env MSG91_AUTH_KEY, MSG91_TEMPLATE_ID)"
        }
        val mobile = cfg.countryCode + phoneNormalized
        val url = "https://control.msg91.com/api/v5/otp?" +
            "template_id=${cfg.templateId}&mobile=$mobile&authkey=${cfg.authKey}&otp=$otp"
        val connection = java.net.URI(url).toURL().openConnection() as java.net.HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        connection.outputStream.use { it.write("{}".toByteArray()) }
        val status = connection.responseCode
        if (status !in 200..299) {
            val err = connection.errorStream?.bufferedReader()?.readText().orEmpty()
            log.error("MSG91 OTP failed status={} body={}", status, err)
            throw com.indieestate.backend.controller.ApiException(
                org.springframework.http.HttpStatus.BAD_GATEWAY,
                "Failed to send OTP via SMS provider",
            )
        }
        log.info("MSG91 OTP dispatched to {}", OtpPhone.mask(phoneNormalized))
    }
}

@org.springframework.stereotype.Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = ["auth.otp.sms-provider"],
    havingValue = "twilio",
)
class TwilioOtpSmsSender(
    private val properties: OtpProperties,
) : OtpSmsSender {
    private val log = org.slf4j.LoggerFactory.getLogger(javaClass)

    override fun sendLoginOtp(phoneNormalized: String, otp: String, ttlSeconds: Long) {
        val cfg = properties.twilio
        require(cfg.accountSid.isNotBlank() && cfg.authToken.isNotBlank() && cfg.fromNumber.isNotBlank()) {
            "Twilio requires TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_FROM_NUMBER"
        }
        val to = "+91$phoneNormalized"
        val body = "Your Indieestate login code is $otp. It expires in ${ttlSeconds / 60} minutes."
        val url = "https://api.twilio.com/2010-04-01/Accounts/${cfg.accountSid}/Messages.json"
        val credentials = "${cfg.accountSid}:${cfg.authToken}"
        val encoded = java.util.Base64.getEncoder().encodeToString(credentials.toByteArray())
        val form = "To=${java.net.URLEncoder.encode(to, Charsets.UTF_8)}" +
            "&From=${java.net.URLEncoder.encode(cfg.fromNumber, Charsets.UTF_8)}" +
            "&Body=${java.net.URLEncoder.encode(body, Charsets.UTF_8)}"
        val connection = java.net.URI(url).toURL().openConnection() as java.net.HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Authorization", "Basic $encoded")
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        connection.doOutput = true
        connection.outputStream.use { it.write(form.toByteArray()) }
        val status = connection.responseCode
        if (status !in 200..299) {
            val err = connection.errorStream?.bufferedReader()?.readText().orEmpty()
            log.error("Twilio SMS failed status={} body={}", status, err)
            throw com.indieestate.backend.controller.ApiException(
                org.springframework.http.HttpStatus.BAD_GATEWAY,
                "Failed to send OTP via SMS provider",
            )
        }
        log.info("Twilio OTP SMS sent to {}", OtpPhone.mask(phoneNormalized))
    }
}

object OtpPhone {
    fun normalize(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return when {
            digits.length == 10 -> digits
            digits.length == 12 && digits.startsWith("91") -> digits.substring(2)
            digits.length in 10..15 -> digits.takeLast(10)
            else -> throw com.indieestate.backend.controller.ApiException(
                org.springframework.http.HttpStatus.BAD_REQUEST,
                "Enter a valid 10-digit mobile number",
            )
        }
    }

    fun mask(phone: String): String =
        if (phone.length <= 4) phone else "******${phone.takeLast(4)}"
}
