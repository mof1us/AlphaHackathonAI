package ru.alfahack.elephants.backend.integrations.temporal.configuration

import com.fasterxml.jackson.module.kotlin.kotlinModule
import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowClientOptions
import io.temporal.client.schedules.ScheduleClient
import io.temporal.client.schedules.ScheduleClientOptions
import io.temporal.common.converter.DataConverter
import io.temporal.common.converter.DefaultDataConverter
import io.temporal.common.converter.JacksonJsonPayloadConverter
import io.temporal.serviceclient.WorkflowServiceStubs
import io.temporal.serviceclient.WorkflowServiceStubsOptions
import io.temporal.worker.WorkerFactory
import io.temporal.worker.WorkerFactoryOptions
import ru.alfahack.elephants.backend.integrations.temporal.context.SuspendWorkerInterceptor
import ru.alfahack.elephants.backend.integrations.temporal.context.SuspendWorkflowClientInterceptor
import ru.alfahack.elephants.backend.integrations.temporal.context.TemporalUserContextPropagator
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Создаёт инфраструктурные объекты Temporal без регистрации прикладных workflow.
 *
 * Очереди и workflow подключает [ru.alfahack.elephants.backend.integrations.temporal.TemporalWorkersFactory].
 * Приложение `temporal-runner` вызывает её и только потом запускает общую
 * фабрику workers. API-процесс фабрику не стартует.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TemporalProperties::class)
class TemporalConfiguration {
    /** Открывает ленивое gRPC-подключение и закрывает его вместе со Spring-контекстом. */
    @Bean(destroyMethod = "shutdown")
    fun temporalWorkflowServiceStubs(properties: TemporalProperties): WorkflowServiceStubs =
        WorkflowServiceStubs.newServiceStubs(
            WorkflowServiceStubsOptions.newBuilder()
                .setTarget(properties.target)
                .build(),
        )

    /**
     * Предоставляет клиент, привязанный к настроенному namespace.
     *
     * Его конвертер читает и пишет Kotlin data class через Jackson Temporal
     * с подключённым модулем Kotlin.
     */
    @Bean
    fun temporalWorkflowClient(
        serviceStubs: WorkflowServiceStubs,
        properties: TemporalProperties,
    ): WorkflowClient {
        val options = WorkflowClientOptions.newBuilder()
            .setNamespace(properties.namespace)
            .setDataConverter(temporalDataConverter())
            .setContextPropagators(listOf(userContextPropagator()))
            .setInterceptors(SuspendWorkflowClientInterceptor())
        return WorkflowClient.newInstance(serviceStubs, options.build())
    }

    /**
     * Создаёт общую фабрику workers и останавливает её при завершении приложения.
     *
     * Фабрику стартует только `temporal-runner`.
     */
    @Bean(destroyMethod = "shutdown")
    fun temporalWorkerFactory(
        workflowClient: WorkflowClient,
    ): WorkerFactory {
        val options = WorkerFactoryOptions.newBuilder()
            .setWorkerInterceptors(SuspendWorkerInterceptor())
        return WorkerFactory.newInstance(workflowClient, options.build())
    }

    /**
     * Клиент расписаний в том же namespace и с тем же конвертером, что и workflow.
     *
     * Создание бина не обращается к серверу. Список и запись расписаний
     * выполняет `temporal-runner`.
     */
    @Bean
    fun temporalScheduleClient(
        serviceStubs: WorkflowServiceStubs,
        properties: TemporalProperties,
    ): ScheduleClient = ScheduleClient.newInstance(
        serviceStubs,
        ScheduleClientOptions.newBuilder()
            .setNamespace(properties.namespace)
            .setDataConverter(temporalDataConverter())
            .setContextPropagators(listOf(userContextPropagator()))
            .build(),
    )
}

private fun userContextPropagator(): TemporalUserContextPropagator =
    TemporalUserContextPropagator(temporalDataConverter())

/**
 * Собирает конвертер payload Temporal с поддержкой Kotlin-классов.
 *
 * Workers берут этот же конвертер у клиента, из которого создана фабрика.
 */
internal fun temporalDataConverter(): DataConverter {
    val mapper = JacksonJsonPayloadConverter.newDefaultObjectMapper().apply {
        registerModule(kotlinModule())
    }
    return DefaultDataConverter.newDefaultInstance()
        .withPayloadConverterOverrides(JacksonJsonPayloadConverter(mapper))
}
