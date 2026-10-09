package ru.alfahack.elephants.backend.integrations.temporal

import ru.alfahack.elephants.backend.integrations.temporal.workflows.helloWorld.HelloWorldWorkflow

/** Интерфейсы приложения для восстановления типов результата suspend-workflow. */
object TemporalWorkflowTypes {
    val interfaces: List<Class<*>> = listOf(HelloWorldWorkflow::class.java)
}
