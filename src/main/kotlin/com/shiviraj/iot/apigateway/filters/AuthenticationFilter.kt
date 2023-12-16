package com.shiviraj.iot.apigateway.filters

import com.shiviraj.iot.apigateway.config.AppConfig
import com.shiviraj.iot.webClient.WebClientWrapper
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class AuthenticationFilter(
    private val routeValidator: RouteValidator,
    private val webClientWrapper: WebClientWrapper,
    private val appConfig: AppConfig
) : AbstractGatewayFilterFactory<AuthenticationFilterConfig>(AuthenticationFilterConfig::class.java) {
    override fun apply(config: AuthenticationFilterConfig): GatewayFilter {
        return (GatewayFilter { exchange: ServerWebExchange, chain: GatewayFilterChain ->
            if (routeValidator.isSecured(exchange.request)) {
                val authorizationHeader: String = exchange.request.headers["Authorization"]?.get(0) ?: ""
                webClientWrapper.get(
                    baseUrl = appConfig.authServiceBaseUrl,
                    path = "/auth/validate",
                    returnType = String::class.java,
                    headers = mapOf("Authorization" to authorizationHeader)
                )
                    .flatMap {
                        chain.filter(exchange)
                    }
                    .onErrorResume {
                        val response = exchange.response
                        response.statusCode = HttpStatus.UNAUTHORIZED
                        Mono.empty()
                    }
            } else {
                chain.filter(exchange)
            }
        })
    }
}
