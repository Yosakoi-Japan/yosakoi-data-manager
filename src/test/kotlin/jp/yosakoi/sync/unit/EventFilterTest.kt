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
            SourceEvent.fromColumns(makeRow(eventId = "a", eventName = "A", extra = mapOf("official_url" to "https://example.com/a"))),
            SourceEvent.fromColumns(makeRow(eventId = "b", eventName = "B", status = "Progress")),
            SourceEvent.fromColumns(makeRow(eventId = "c", eventName = "C", endDate = "2026-01-01", extra = mapOf("official_url" to "https://example.com/c"))),
        )

        val filtered = EventPublicationPolicy().filterPublishableEvents(rows, LocalDate.of(2026, 6, 1))

        assertEquals(listOf("a"), filtered.publishableEvents.map { it.eventId })
        assertEquals(listOf("c"), filtered.expiredEvents.map { it.eventId })
        assertEquals(emptyList(), filtered.duplicateEvents)
    }

    @Test
    fun `duplicate event id is excluded`() {
        val rows = listOf(
            SourceEvent.fromColumns(makeRow(eventId = "a", eventName = "A", extra = mapOf("official_url" to "https://example.com/a"))),
            SourceEvent.fromColumns(makeRow(eventId = "a", eventName = "A2", updatedAt = "2026-05-02T10:00:00+09:00", extra = mapOf("official_url" to "https://example.com/a2"))),
        )

        val filtered = EventPublicationPolicy().filterPublishableEvents(rows, LocalDate.of(2026, 6, 1))

        assertEquals(emptyList(), filtered.publishableEvents)
        assertEquals(1, filtered.duplicateEvents.size)
        assertEquals("a", filtered.duplicateEvents.first().eventId)
    }

    @Test
    fun `missing official url is excluded`() {
        val rows = listOf(
            SourceEvent.fromColumns(makeRow(eventId = "a", eventName = "A", extra = mapOf("official_url" to ""))),
            SourceEvent.fromColumns(makeRow(eventId = "b", eventName = "B", extra = mapOf("official_url" to "https://example.com"))),
        )

        val filtered = EventPublicationPolicy().filterPublishableEvents(rows, LocalDate.of(2026, 6, 1))

        assertEquals(listOf("b"), filtered.publishableEvents.map { it.eventId })
    }

    @Test
    fun `invalid official url is excluded`() {
        val rows = listOf(
            SourceEvent.fromColumns(makeRow(eventId = "a", eventName = "A", extra = mapOf("official_url" to "not-a-url"))),
            SourceEvent.fromColumns(makeRow(eventId = "b", eventName = "B", extra = mapOf("official_url" to "https://example.com"))),
        )

        val filtered = EventPublicationPolicy().filterPublishableEvents(rows, LocalDate.of(2026, 6, 1))

        assertEquals(listOf("b"), filtered.publishableEvents.map { it.eventId })
    }
}
