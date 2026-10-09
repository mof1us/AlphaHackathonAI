package ru.alfahack.elephants.backend.integrations.temporal.context

import io.temporal.common.converter.DataConverter
import org.junit.jupiter.api.Test
import ru.alfahack.elephants.backend.integrations.temporal.configuration.temporalDataConverter
import ru.alfahack.elephants.backend.utils.context.AuthenticatedSession
import ru.alfahack.elephants.backend.utils.context.UserContext
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TemporalUserContextTest {
    private val dataConverter: DataConverter = temporalDataConverter()
    private val propagator = TemporalUserContextPropagator(dataConverter)

    @Test
    fun `propagates user identity without a trace id or token`() {
        val issuedAt = Instant.parse("2026-10-03T12:00:00Z")
        val original = UserContext(
            userId = 42L,
            session = AuthenticatedSession(
                subject = "user-42",
                tokenId = "token-1",
                issuedAt = issuedAt,
                expiresAt = issuedAt.plusSeconds(60),
            ),
            traceId = "must-not-travel",
        )

        val restored = propagator.deserializeContext(propagator.serializeContext(original)) as UserContext

        assertEquals(42L, restored.userId)
        assertEquals("user-42", restored.session?.subject)
        assertEquals("token-1", restored.session?.tokenId)
        assertEquals(issuedAt, restored.session?.issuedAt)
        assertNull(restored.traceId)
    }

    @Test
    fun `binds user context to the current thread`() {
        val propagated = UserContext(userId = 7L)

        assertEquals(7L, withTemporalUserContext(propagated) { UserContext.current()?.userId })
        assertNull(UserContext.current())
    }
}
