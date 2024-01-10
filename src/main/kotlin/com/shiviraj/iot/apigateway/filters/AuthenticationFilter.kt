package com.shiviraj.iot.apigateway.filters

import com.shiviraj.iot.apigateway.config.AppConfig
import com.shiviraj.iot.apigateway.exception.UnAuthorizedException
import com.shiviraj.iot.loggingstarter.logOnError
import com.shiviraj.iot.loggingstarter.logOnSuccess
import com.shiviraj.iot.loggingstarter.serializer.DefaultSerializer.deserialize
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
                .logOnSuccess(message = "Successfully authorized")
                .logOnError(errorMessage = "Failed to authorize")
                .flatMap {
                    chain.filter(exchange)
                }
                .onErrorResume {
                    val unAuthorizedException = UnAuthorizedException(
                        errorCode = "IOT-4000",
                        message = "Authorized failure!"
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
                returnType = String::class.java,
                headers = exchange.request.headers.mapValues { it.value.joinToString(",") }
            )
                .map { deserialize(it, AuthDetails::class.java) }
        } else {
            Mono.just(AuthDetails(userId = "Unauthorized path"))
        }
    }
}
