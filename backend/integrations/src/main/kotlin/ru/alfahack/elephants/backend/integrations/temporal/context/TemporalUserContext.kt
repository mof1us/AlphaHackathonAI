package ru.alfahack.elephants.backend.integrations.temporal.context

import io.temporal.api.common.v1.Payload
import io.temporal.common.context.ContextPropagator
import io.temporal.common.converter.DataConverter
import kotlinx.coroutines.ThreadContextElement
import ru.alfahack.elephants.backend.utils.context.AuthenticatedSession
import ru.alfahack.elephants.backend.utils.context.UserContext
import java.time.Instant
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * Переносит [UserContext] через заголовки Temporal от старта workflow к activity.
 *
 * Идентификатор трассировки сюда не кладётся. В заголовке только пользователь
 * и безопасные метаданные JWT.
 */
class TemporalUserContextPropagator(
    private val dataConverter: DataConverter,
) : ContextPropagator {
    override fun getName(): String = NAME

    override fun getCurrentContext(): Any? = UserContext.current()

    override fun setCurrentContext(context: Any?) {
        UserContext.bind(context as? UserContext)
    }

    override fun serializeContext(context: Any?): Map<String, Payload> {
        val user = context as? UserContext ?: return emptyMap()
        return buildMap {
            putPayload(USER_ID, user.userId?.toString())
            putPayload(SUBJECT, user.session?.subject)
            putPayload(TOKEN_ID, user.session?.tokenId)
            putPayload(ISSUED_AT, user.session?.issuedAt?.toEpochMilli()?.toString())
            putPayload(EXPIRES_AT, user.session?.expiresAt?.toEpochMilli()?.toString())
        }
    }

    override fun deserializeContext(context: Map<String, Payload>): Any {
        val subject = read(context, SUBJECT)
        return UserContext(
            userId = read(context, USER_ID)?.toLongOrNull(),
            session = subject?.let { value ->
                AuthenticatedSession(
                    subject = value,
                    tokenId = read(context, TOKEN_ID),
                    issuedAt = read(context, ISSUED_AT)?.toLongOrNull()?.let(Instant::ofEpochMilli),
                    expiresAt = read(context, EXPIRES_AT)?.toLongOrNull()?.let(Instant::ofEpochMilli),
                )
            },
        )
    }

    private fun MutableMap<String, Payload>.putPayload(key: String, value: String?) {
        if (value == null) {
            return
        }
        dataConverter.toPayload(value).ifPresent { payload -> put(key, payload) }
    }

    private fun read(context: Map<String, Payload>, key: String): String? {
        val payload = context[key] ?: return null
        return dataConverter.fromPayload(payload, String::class.java, String::class.java)
    }

    private companion object {
        const val NAME = "elephants-user-context"
        const val USER_ID = "userId"
        const val SUBJECT = "subject"
        const val TOKEN_ID = "tokenId"
        const val ISSUED_AT = "issuedAt"
        const val EXPIRES_AT = "expiresAt"
    }
}

/**
 * Пока корутина выполняется на потоке, кладёт [UserContext] в [UserContext.current].
 *
 * Старт workflow на том же потоке забирает пользователя сам.
 */
class TemporalUserContextElement(
    private val userContext: UserContext,
) : AbstractCoroutineContextElement(Key), ThreadContextElement<UserContext?> {
    override fun updateThreadContext(context: CoroutineContext): UserContext? {
        val previous = UserContext.current()
        UserContext.bind(userContext)
        return previous
    }

    override fun restoreThreadContext(context: CoroutineContext, oldState: UserContext?) {
        UserContext.bind(oldState)
    }

    companion object Key : CoroutineContext.Key<TemporalUserContextElement>
}

/** Контекст корутины, из которого Temporal видит пользователя без ручной обёртки. */
fun UserContext.propagated(): CoroutineContext = this + TemporalUserContextElement(this)

/**
 * Кладёт [UserContext] в слот пропагатора для кода вне корутины.
 *
 * В запросе и в activity это уже делает [UserContext.propagated].
 */
fun <T> withTemporalUserContext(context: UserContext, action: () -> T): T {
    val previous = UserContext.current()
    UserContext.bind(context)
    try {
        return action()
    } finally {
        UserContext.bind(previous)
    }
}
