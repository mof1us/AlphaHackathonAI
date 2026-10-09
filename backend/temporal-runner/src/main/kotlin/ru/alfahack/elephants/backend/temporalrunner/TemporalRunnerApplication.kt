package ru.alfahack.elephants.backend.temporalrunner

import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder

/** Самостоятельный процесс Temporal без HTTP-сервера и цепочек авторизации. */
@SpringBootApplication(scanBasePackages = [
    "ru.alfahack.elephants.backend.temporalrunner",
    "ru.alfahack.elephants.backend.domain",
    "ru.alfahack.elephants.backend.data",
    "ru.alfahack.elephants.backend.integrations",
])
class TemporalRunnerApplication

fun main(args: Array<String>) {
    SpringApplicationBuilder(TemporalRunnerApplication::class.java)
        .web(WebApplicationType.NONE)
        .run(*args)
}
