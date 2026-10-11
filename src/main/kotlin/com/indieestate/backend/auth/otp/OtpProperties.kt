package com.indieestate.backend.auth.otp

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "auth.otp")
data class OtpProperties(
    val ttlSeconds: Long = 300,
    val length: Int = 6,
    /** When true: log OTP, return debugOtp in API, skip real SMS. Set false in production. */
    val devMode: Boolean = true,
    /** Optional OTP used in development instead of a random code. */
    val devOtp: String = "",
    /** Store OTP in Redis (required for multi-instance production). */
    val redisEnabled: Boolean = false,
    /** logging | msg91 | twilio | none */
    val smsProvider: String = "logging",
    val maxVerifyAttempts: Int = 5,
    val sendRateLimitPerPhone: Int = 5,
    val sendRateLimitPerIp: Int = 30,
    val sendRateWindowSeconds: Long = 900,
    val msg91: Msg91Properties = Msg91Properties(),
    val twilio: TwilioProperties = TwilioProperties(),
) {
    data class Msg91Properties(
        val authKey: String = "",
        val templateId: String = "",
        val senderId: String = "",
        /** E.164 country prefix without +, e.g. 91 for India */
        val countryCode: String = "91",
    )

    data class TwilioProperties(
        val accountSid: String = "",
        val authToken: String = "",
        val fromNumber: String = "",
    )
}
