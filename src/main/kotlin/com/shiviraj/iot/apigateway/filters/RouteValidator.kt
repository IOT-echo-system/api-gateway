package com.shiviraj.iot.apigateway.filters

import com.shiviraj.iot.apigateway.config.AppConfig
import org.springframework.stereotype.Component

@Component
class RouteValidator(private val appConfig: AppConfig) {

    fun isSecured(request: org.springframework.http.server.reactive.ServerHttpRequest): Boolean {
        return !appConfig.openApiEndpoints.any { request.uri.path.startsWith(it) }
    }
}
