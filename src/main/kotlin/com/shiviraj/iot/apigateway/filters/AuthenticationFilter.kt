package com.shiviraj.iot.apigateway.filters

import com.shiviraj.iot.apigateway.config.AppConfig
import com.shiviraj.iot.apigateway.exception.UnAuthorizedException
import com.shiviraj.iot.loggingstarter.logOnErrorResponse
import com.shiviraj.iot.loggingstarter.logOnSuccessResponse
import com.shiviraj.iot.loggingstarter.serializer.DefaultSerializer.serialize
import com.shiviraj.iot.webClient.WebClientWrapper
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
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
            authorizeRequest(exchange)
                .flatMap {
                    chain.filter(exchange)
                }
                .logOnSuccessResponse(message = "Successfully authorized")
                .logOnErrorResponse(errorMessage = "Failed to authorize")
                .onErrorResume {
                    val unAuthorizedException = UnAuthorizedException(
                        errorCode = "IOT-4000",
                        errorMessage = "Authorized failure!"
                    )
                    val response = exchange.response

                    response.statusCode = HttpStatus.UNAUTHORIZED
                    response.headers.contentType = MediaType.APPLICATION_JSON
                    response.writeWith(
                        Mono.just(
                            response.bufferFactory().wrap(serialize(unAuthorizedException).toByteArray())
                        )
                    )
                }
                .contextWrite {
                    it.put(ServerWebExchange::class.java, exchange)
                }
        })
    }

    private fun authorizeRequest(exchange: ServerWebExchange): Mono<AuthDetails> {
        return if (routeValidator.isSecured(exchange.request)) {
            webClientWrapper.get(
                baseUrl = appConfig.authServiceBaseUrl,
                path = "/auth/validate",
                returnType = AuthDetails::class.java,
                headers = exchange.request.headers.mapValues { it.value.joinToString(",") }
            )
        } else {
            Mono.just(AuthDetails(userId = "Unauthorized path"))
        }
    }
}
