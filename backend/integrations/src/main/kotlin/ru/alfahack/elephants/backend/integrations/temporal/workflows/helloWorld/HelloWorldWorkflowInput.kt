package ru.alfahack.elephants.backend.integrations.temporal.workflows.helloWorld

/** Вход проверочного workflow. Поле `name` приходит из JSON-объекта. */
data class HelloWorldWorkflowInput(
    val name: String?,
)
