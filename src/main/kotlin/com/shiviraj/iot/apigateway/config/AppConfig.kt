package com.shiviraj.iot.apigateway.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.env")
data class AppConfig(val authServiceBaseUrl: String, val openApiEndpoints: List<String>)
