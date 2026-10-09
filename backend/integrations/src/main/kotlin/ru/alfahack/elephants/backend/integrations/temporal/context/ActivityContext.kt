package ru.alfahack.elephants.backend.integrations.temporal.context

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import ru.alfahack.elephants.backend.utils.context.UserContext

/** Выполняет фоновую activity без наследования HTTP-пользователя и его сессии. */
suspend fun <T> withActivityContext(block: suspend CoroutineScope.() -> T): T =
    withContext(UserContext().propagated(), block)
