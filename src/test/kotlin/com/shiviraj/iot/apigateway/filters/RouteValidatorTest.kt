package com.shiviraj.iot.apigateway.filters

import com.shiviraj.iot.apigateway.config.AppConfig
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

import org.junit.jupiter.api.Test
import org.springframework.http.server.reactive.ServerHttpRequest
import java.net.URI

class RouteValidatorTest {

    private val request= mockk<ServerHttpRequest>()
    private val uri= mockk<URI>()
    private val appConfig = AppConfig("", listOf("/auth/login", "/auth/validate"))
    private val routeValidator = RouteValidator(appConfig)

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `should give true if path is not include in open endpoints`() {
        every { request.uri } returns uri
        every { uri.path } returns "/secured"

        val response = routeValidator.isSecured(request)
        response shouldBe true
    }

    @Test
    fun `should give false if path is include in open endpoints`() {
        every { request.uri } returns uri
        every { uri.path } returns "/auth/login"

        val response = routeValidator.isSecured(request)
        response shouldBe false
    }

}
