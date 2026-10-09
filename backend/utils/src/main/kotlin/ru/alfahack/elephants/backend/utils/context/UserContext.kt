package ru.alfahack.elephants.backend.utils.context

import java.time.Instant
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * Хранит сведения о текущем пользователе и трассировке.
 *
 * В корутине это элемент контекста. Для кода вне корутины то же значение
 * доступно через [current]: так его видит пропагатор Temporal. WebFlux при
 * смене потока опирается на элемент корутины, а не на поток.
 */
data class UserContext(
    val userId: Long? = null,
    val session: AuthenticatedSession? = null,
    val traceId: String? = null,
) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<UserContext> {
        private val thread = ThreadLocal<UserContext?>()

        /** Возвращает контекст, привязанный к текущему потоку. */
        fun current(): UserContext? = thread.get()

        /** Ставит контекст на текущий поток или снимает его. */
        fun bind(context: UserContext?) {
            if (context == null) {
                thread.remove()
            } else {
                thread.set(context)
            }
        }
    }
}

/**
 * Безопасный снимок JWT, которым был аутентифицирован запрос.
 *
 * В контекст не попадают сам access token, cookie или секреты HTTP-сессии.
 */
data class AuthenticatedSession(
    val subject: String,
    val tokenId: String?,
    val issuedAt: Instant?,
    val expiresAt: Instant?,
)
