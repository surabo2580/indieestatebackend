package com.indieestate.backend

import com.indieestate.backend.service.OtpAuthService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.annotation.Transactional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull

@SpringBootTest
@ActiveProfiles("h2")
@TestPropertySource(
    properties = [
        "auth.otp.dev-mode=true",
        "auth.otp.dev-fixed-code=424242",
        "auth.otp.redis-enabled=false",
        "auth.otp.sms-provider=logging",
    ],
)
@Transactional
class OtpAuthServiceTest {

    @Autowired
    lateinit var otpAuthService: OtpAuthService

    @Test
    fun sendAndVerifyOtpInDevMode() {
        val send = otpAuthService.sendOtp("9876543210", "127.0.0.1")
        assertEquals("424242", send.debugOtp)

        val auth = otpAuthService.verifyOtp("9876543210", "424242")
        assertNotNull(auth.accessToken)
        assertEquals("9876543210", auth.user.phone)
    }
}
