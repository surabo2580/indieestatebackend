package com.indieestate.backend.config

import com.indieestate.backend.chat.ChatWebSocketHandler
import com.indieestate.backend.security.JwtService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import org.springframework.web.socket.server.HandshakeInterceptor

@Configuration
@EnableWebSocket
class ChatWebSocketConfig(
    private val chatWebSocketHandler: ChatWebSocketHandler,
    private val jwtService: JwtService,
    @param:Value("\${app.cors.allowed-origins:*}") private val allowedOrigins: String,
) : WebSocketConfigurer {

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        val origins = allowedOrigins.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
            .addInterceptors(ChatHandshakeInterceptor(jwtService))
            .setAllowedOriginPatterns(*origins.toTypedArray())
    }
}

private class ChatHandshakeInterceptor(
    private val jwtService: JwtService,
) : HandshakeInterceptor {
    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>,
    ): Boolean {
        val token = bearerToken(request) ?: queryToken(request)
        val principal = token?.let { jwtService.parsePrincipal(it) } ?: return false
        attributes[ChatWebSocketHandler.USER_ID] = principal.userId
        return true
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?,
    ) = Unit

    private fun bearerToken(request: ServerHttpRequest): String? {
        val header = request.headers.getFirst(HttpHeaders.AUTHORIZATION) ?: return null
        return header.takeIf { it.startsWith("Bearer ") }?.removePrefix("Bearer ")?.trim()
    }

    private fun queryToken(request: ServerHttpRequest): String? {
        val servletRequest = (request as? ServletServerHttpRequest)?.servletRequest ?: return null
        return servletRequest.getParameter("access_token")?.trim()?.takeIf { it.isNotEmpty() }
    }
}
