package ru.alfahack.elephants.backend.integrations.temporal.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

/** Параметры подключения общего клиента Temporal. */
@ConfigurationProperties("integrations.temporal")
data class TemporalProperties(
    /** Адрес frontend-сервиса Temporal в формате `host:port`. */
    val target: String = DEFAULT_TARGET,
    /** Пространство имён, в котором запускаются и читаются workflow. */
    val namespace: String = DEFAULT_NAMESPACE,
) {
    init {
        require(target.isNotBlank()) { "Temporal target must not be blank" }
        require(namespace.isNotBlank()) { "Temporal namespace must not be blank" }
    }

    private companion object {
        const val DEFAULT_TARGET = "127.0.0.1:7233"
        const val DEFAULT_NAMESPACE = "default"
    }
}
