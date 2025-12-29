package com.emirhankarci.moviebackend.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestTemplate

@Configuration
class RestTemplateConfig {

    @Value("\${gemini.api.timeout:30000}")
    private var timeout: Int = 30000

    @Bean
    fun restTemplate(): RestTemplate {
        val factory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(timeout)
            setReadTimeout(timeout)
        }
        return RestTemplate(factory)
    }
}
