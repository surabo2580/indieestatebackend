package com.indieestate.backend

import com.indieestate.backend.auth.otp.OtpProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan(basePackages = ["com.indieestate.backend"])
@EnableConfigurationProperties(OtpProperties::class)
class IndieestateBackendApplication

fun main(args: Array<String>) {
    runApplication<IndieestateBackendApplication>(*args)
}
