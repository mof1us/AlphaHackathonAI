package ru.alfahack.elephants.backend.integrations.temporal

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TemporalTaskQueueTest {
    @Test
    fun `queue names are unique and not blank`() {
        val names = TemporalTaskQueue.entries.map { it.name }

        assertEquals(names.distinct(), names)
        assertTrue(names.all { it.isNotBlank() })
    }

    @Test
    fun `test queue keeps its client contract`() {
        assertEquals("HELLO_WORLD_QUEUE", TemporalTaskQueue.HELLO_WORLD_QUEUE.name)
    }
}
