package jp.yosakoi.sync.unit

import jp.yosakoi.sync.makeRow
import jp.yosakoi.sync.domain.model.SourceEvent
import jp.yosakoi.sync.domain.service.EventPublicationPolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import java.time.LocalDate

class EventFilterTest {
    @Test
    fun `status and end date filtering`() {
        val rows = listOf(
            SourceEvent.fromColumns(makeRow(eventId = "a", eventName = "A")),
            SourceEvent.fromColumns(makeRow(eventId = "b", eventName = "B", status = "Progress")),
            SourceEvent.fromColumns(makeRow(eventId = "c", eventName = "C", endDate = "2026-01-01")),
        )

        val filtered = EventPublicationPolicy().filterPublishableEvents(rows, LocalDate.of(2026, 6, 1))

        assertEquals(listOf("a"), filtered.publishableEvents.map { it.eventId })
        assertEquals(listOf("c"), filtered.expiredEvents.map { it.eventId })
        assertEquals(emptyList(), filtered.duplicateEvents)
    }

    @Test
    fun `duplicate event id is excluded`() {
        val rows = listOf(
            SourceEvent.fromColumns(makeRow(eventId = "a", eventName = "A")),
            SourceEvent.fromColumns(makeRow(eventId = "a", eventName = "A2", updatedAt = "2026-05-02T10:00:00+09:00")),
        )

        val filtered = EventPublicationPolicy().filterPublishableEvents(rows, LocalDate.of(2026, 6, 1))

        assertEquals(emptyList(), filtered.publishableEvents)
        assertEquals(1, filtered.duplicateEvents.size)
        assertEquals("a", filtered.duplicateEvents.first().eventId)
    }
}
