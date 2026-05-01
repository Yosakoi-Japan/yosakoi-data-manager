package jp.yosakoi.sync.unit

import jp.yosakoi.sync.makeRow
import jp.yosakoi.sync.domain.model.ApprovedEvent
import jp.yosakoi.sync.domain.model.PublishedEventRecord
import jp.yosakoi.sync.domain.model.SourceEvent
import jp.yosakoi.sync.domain.model.SyncDecisionType
import jp.yosakoi.sync.domain.service.PublishedEventMergeService
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class PublishedCsvStoreTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `compare new updated and skipped`() {
        val existingColumns = makeRow(eventId = "a", eventName = "A", updatedAt = "2026-05-01T10:00:00+09:00")
        val existing = linkedMapOf(
            "a" to PublishedEventRecord.fromColumns(existingColumns),
        )
        existing["c"] = PublishedEventRecord.fromColumns(
            makeRow(eventId = "c", eventName = "C", updatedAt = "2026-05-01T10:00:00+09:00"),
        )
        val events = listOf(
            ApprovedEvent.fromSource(SourceEvent.fromColumns(makeRow(eventId = "a", eventName = "A", updatedAt = "2026-05-02T10:00:00+09:00"))),
            ApprovedEvent.fromSource(SourceEvent.fromColumns(makeRow(eventId = "b", eventName = "B", updatedAt = "2026-05-03T10:00:00+09:00"))),
            ApprovedEvent.fromSource(SourceEvent.fromColumns(makeRow(eventId = "c", eventName = "C", updatedAt = "invalid"))),
        )

        val result = PublishedEventMergeService().merge(events, existing)

        assertEquals(listOf(SyncDecisionType.UPDATED, SyncDecisionType.NEW, SyncDecisionType.INVALID_UPDATED_AT), result.decisions.map { it.type })
        assertEquals(1, result.invalidUpdatedAtRecords.size)
    }

    @Test
    fun `compare accepts date only updated_at`() {
        val existing = linkedMapOf(
            "a" to PublishedEventRecord.fromColumns(
                makeRow(eventId = "a", eventName = "A", updatedAt = "2026-04-28"),
            ),
        )
        val events = listOf(
            ApprovedEvent.fromSource(
                SourceEvent.fromColumns(makeRow(eventId = "a", eventName = "A", updatedAt = "2026-04-29")),
            ),
        )

        val result = PublishedEventMergeService().merge(events, existing)

        assertEquals(listOf(SyncDecisionType.UPDATED), result.decisions.map { it.type })
        assertEquals(emptyList(), result.invalidUpdatedAtRecords)
    }
}
