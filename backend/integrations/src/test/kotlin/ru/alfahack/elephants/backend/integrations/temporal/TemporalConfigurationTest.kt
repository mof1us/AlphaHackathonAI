package ru.alfahack.elephants.backend.integrations.temporal

import io.temporal.client.WorkflowClient
import io.temporal.serviceclient.WorkflowServiceStubs
import io.temporal.worker.WorkerFactory
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.core.env.MapPropertySource
import ru.alfahack.elephants.backend.integrations.temporal.configuration.TemporalConfiguration
import ru.alfahack.elephants.backend.integrations.temporal.configuration.TemporalProperties
import ru.alfahack.elephants.backend.integrations.temporal.workflows.helloWorld.HelloWorldWorkflowInput
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class TemporalConfigurationTest {
    @Test
    fun `creates temporal infrastructure with configured connection settings`() {
        AnnotationConfigApplicationContext().use { context ->
            context.environment.propertySources.addFirst(
                MapPropertySource(
                    "test-temporal",
                    mapOf(
                        "integrations.temporal.target" to "temporal.internal:7233",
                        "integrations.temporal.namespace" to "elephants",
                    ),
                ),
            )
            context.register(TemporalConfiguration::class.java)
            context.refresh()

            val serviceStubs = context.getBean(WorkflowServiceStubs::class.java)
            val workflowClient = context.getBean(WorkflowClient::class.java)

            assertEquals("temporal.internal:7233", serviceStubs.options.target)
            assertEquals("elephants", workflowClient.options.namespace)
            assertNotNull(context.getBean(WorkerFactory::class.java))

            val input = HelloWorldWorkflowInput(name = "Elephants")
            val payload = workflowClient.options.dataConverter.toPayload(input).orElseThrow()
            assertEquals(
                input,
                workflowClient.options.dataConverter.fromPayload(
                    payload,
                    HelloWorldWorkflowInput::class.java,
                    HelloWorldWorkflowInput::class.java,
                ),
            )
        }
    }

    @Test
    fun `uses local temporal defaults`() {
        assertEquals("127.0.0.1:7233", TemporalProperties().target)
        assertEquals("default", TemporalProperties().namespace)
    }

    @Test
    fun `rejects blank temporal settings`() {
        assertFailsWith<IllegalArgumentException> { TemporalProperties(target = " ") }
        assertFailsWith<IllegalArgumentException> { TemporalProperties(namespace = " ") }
    }
}
