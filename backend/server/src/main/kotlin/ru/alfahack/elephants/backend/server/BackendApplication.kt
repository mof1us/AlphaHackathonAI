package ru.alfahack.elephants.backend.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = [
    "ru.alfahack.elephants.backend.server",
    "ru.alfahack.elephants.backend.domain",
    "ru.alfahack.elephants.backend.data",
    "ru.alfahack.elephants.backend.authorization",
    "ru.alfahack.elephants.backend.integrations",
])
class BackendApplication

fun main(args: Array<String>) {
	runApplication<BackendApplication>(*args)
}
