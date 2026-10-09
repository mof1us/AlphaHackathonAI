package ru.alfahack.elephants.backend.authserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/** Отдельная servlet-точка входа сервера авторизации. */
@SpringBootApplication(scanBasePackages = [
    "ru.alfahack.elephants.backend.authserver",
    "ru.alfahack.elephants.backend.authorization",
    "ru.alfahack.elephants.backend.domain",
    "ru.alfahack.elephants.backend.data",
])
class AuthorizationServerApplication

fun main(args: Array<String>) {
    runApplication<AuthorizationServerApplication>(*args)
}
