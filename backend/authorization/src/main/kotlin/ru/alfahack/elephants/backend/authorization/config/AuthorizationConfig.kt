package ru.alfahack.elephants.backend.authorization.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder

/** Общие компоненты авторизации; HTTP-цепочки безопасности принадлежат приложениям. */
@Configuration(proxyBeanMethods = false)
class AuthorizationConfig {
    @Bean
    fun passwordEncoder(): PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()
}
