package com.shiviraj.iot.apigateway.filters

import com.shiviraj.iot.apigateway.config.AppConfig
import com.shiviraj.iot.webClient.WebClientWrapper
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

class AuthenticationFilterTest {

    private val routeValidator = mockk<RouteValidator>()
    private val webClientWrapper = mockk<WebClientWrapper>()
    private val appConfig = AppConfig("http://auth-service", emptyList())
    private val request = mockk<ServerHttpRequest>()
    private val exchange = mockk<ServerWebExchange>()
    private val chain = mockk<GatewayFilterChain>()

    private val authenticationFilter = AuthenticationFilter(
        routeValidator = routeValidator,
        webClientWrapper = webClientWrapper,
        appConfig = appConfig
    )

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `should not apply filter if path is not secured`() {
        every { routeValidator.isSecured(any()) } returns false
        every { chain.filter(any()) } returns Mono.empty()
        every { exchange.request } returns request

        authenticationFilter.apply(AuthenticationFilterConfig()).filter(exchange, chain).subscribe()

        verify(exactly = 1) {
            routeValidator.isSecured(request)
            chain.filter(exchange)
        }
    }

    @Test
    fun `should apply filter if path is not secured`() {
        every { routeValidator.isSecured(any()) } returns true
        every { chain.filter(any()) } returns Mono.empty()
        every { exchange.request } returns request
        every { request.headers } returns HttpHeaders()
        every {
            webClientWrapper.get(
                baseUrl = any(),
                path = any(),
                returnType = any<Class<*>>(),
                headers = any()
            )
        } returns Mono.just("")

        authenticationFilter.apply(AuthenticationFilterConfig()).filter(exchange, chain).subscribe()

        verify(exactly = 1) {
            routeValidator.isSecured(request)
            chain.filter(exchange)
            webClientWrapper.get(
                baseUrl = "http://auth-service",
                path = "/auth/validate",
                returnType = String::class.java,
                headers = mapOf("Authorization" to "")
            )
        }
    }

    @Test
    fun `should give error if authentication failed`() {
        val response = mockk<ServerHttpResponse>()
        val map: MultiValueMap<String, String> = LinkedMultiValueMap();
        map.add("Authorization", "authorization")

        every { routeValidator.isSecured(any()) } returns true
        every { chain.filter(any()) } returns Mono.empty()
        every { exchange.request } returns request
        every { exchange.response } returns response
        every { response.setStatusCode(any()) } returns true
        every { request.headers } returns HttpHeaders(map)
        every {
            webClientWrapper.get(
                baseUrl = any(),
                path = any(),
                returnType = any<Class<*>>(),
                headers = any()
            )
        } returns Mono.error(Exception("Invalid token."))

        authenticationFilter.apply(AuthenticationFilterConfig()).filter(exchange, chain).subscribe()

        verify(exactly = 1) {
            routeValidator.isSecured(request)
            webClientWrapper.get(
                baseUrl = "http://auth-service",
                path = "/auth/validate",
                returnType = String::class.java,
                headers = mapOf("Authorization" to "authorization")
            )
            response.setStatusCode(HttpStatus.UNAUTHORIZED)
        }
        verify(exactly = 0) {
            chain.filter(exchange)
        }
    }
}
