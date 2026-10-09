package ru.alfahack.elephants.backend.domain.context

import org.springframework.stereotype.Component
import ru.alfahack.elephants.backend.utils.context.UserContext
import kotlin.coroutines.coroutineContext

/**
 * Предоставляет доменному и прикладному коду сведения о текущем пользователе.
 *
 * Контекст запроса может отсутствовать, например при запуске фоновой задачи. В
 * таком случае идентификатор пользователя возвращает `null`.
 */
@Component
class UserContextProvider {
    /** Возвращает идентификатор аутентифицированного пользователя. */
    suspend fun getUserID(): Long? = coroutineContext[UserContext]?.userId

    /** Возвращает безопасный снимок JWT-сессии аутентифицированного пользователя. */
    suspend fun getSession() = coroutineContext[UserContext]?.session

    /** Возвращает идентификатор трассировки текущего запроса. */
    suspend fun getTraceId(): String? = coroutineContext[UserContext]?.traceId

}
