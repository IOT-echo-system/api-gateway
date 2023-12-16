package com.shiviraj.iot.apigateway

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [ApiGatewayApplication::class])
class ApiGatewayApplicationTests {

    @Autowired
    private lateinit var apiGatewayApplication: ApiGatewayApplication

    @Test
    fun contextLoads() {
        assertNotNull(apiGatewayApplication)
    }

}

