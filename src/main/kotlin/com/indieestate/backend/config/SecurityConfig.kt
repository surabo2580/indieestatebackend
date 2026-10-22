package com.indieestate.backend.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.indieestate.backend.security.JwtAuthenticationFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.util.matcher.RequestMatcher
import jakarta.servlet.DispatcherType
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    @param:Value("\${app.cors.allowed-origins:*}") private val allowedOrigins: String,
) {
    private val objectMapper = ObjectMapper().findAndRegisterModules()

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = allowedOrigins.split(",").map { it.trim() }
        configuration.allowedMethods = listOf("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        configuration.exposedHeaders = listOf("Authorization", "Accept-Ranges", "Content-Range", "Content-Length")
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatcher(RequestMatcher { request ->
                val path = request.servletPath.ifBlank { request.requestURI }
                !path.startsWith("/api/reels/media")
            })
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .cors { }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .exceptionHandling {
                it.authenticationEntryPoint { _, response, _ ->
                    writeProblem(response, HttpStatus.UNAUTHORIZED, "Authentication required")
                }
                it.accessDeniedHandler { _, response, _ ->
                    writeProblem(response, HttpStatus.FORBIDDEN, "Access denied")
                }
            }
            .authorizeHttpRequests { auth ->
                auth.dispatcherTypeMatchers(
                    DispatcherType.ASYNC,
                    DispatcherType.ERROR,
                    DispatcherType.FORWARD,
                ).permitAll()
                auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                auth.requestMatchers("/api/reels/media/**").permitAll()
                auth.requestMatchers("/ws/**").permitAll()
                auth.requestMatchers(
                    HttpMethod.GET,
                    "/api/categories",
                    "/api/categories/**",
                    "/api/services",
                    "/api/services/**",
                    "/api/forms",
                    "/api/forms/**",
                    "/api/ads",
                    "/api/community",
                    "/api/community/**",
                    "/api/reels",
                    "/api/reels/**",
                    "/api/health",
                ).permitAll()
                auth.requestMatchers(
                    HttpMethod.HEAD,
                    "/api/reels",
                    "/api/reels/**",
                    "/api/health",
                ).permitAll()
                auth.requestMatchers(
                    "/api/auth/register",
                    "/api/auth/login",
                    "/api/auth/otp/send",
                    "/api/auth/otp/verify",
                    "/actuator/health",
                    "/actuator/health/**",
                    "/actuator/info",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                ).permitAll()
                auth.requestMatchers(HttpMethod.POST, "/api/categories", "/api/services").permitAll()
                auth.anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }

    private fun writeProblem(
        response: jakarta.servlet.http.HttpServletResponse,
        status: HttpStatus,
        detail: String,
    ) {
        val problem = ProblemDetail.forStatusAndDetail(status, detail)
        problem.title = status.reasonPhrase
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
        objectMapper.writeValue(response.outputStream, problem)
    }
}
