package jp.yosakoi.sync

import jp.yosakoi.sync.application.model.PublishedEventsSnapshot
import jp.yosakoi.sync.application.port.EventSource
import jp.yosakoi.sync.application.port.PublishedEventRepository
import jp.yosakoi.sync.domain.model.PublishedEventRecord
import jp.yosakoi.sync.domain.model.SourceEvent
import java.util.LinkedHashMap
import java.nio.file.Path

fun makeRow(
    eventId: String,
    eventName: String,
    status: String = "Approved",
    startDate: String = "2026-06-10",
    endDate: String = "2026-06-14",
    updatedAt: String = "2026-05-01T10:00:00+09:00",
    extra: Map<String, String> = emptyMap(),
): LinkedHashMap<String, String> {
    val row = linkedMapOf(
        "event_id" to eventId,
        "event_name" to eventName,
        "status" to status,
        "image_url" to "",
        "official_url" to "",
        "start_date" to startDate,
        "end_date" to endDate,
        "location" to "",
        "team_count" to "",
        "nearest_station" to "",
        "parking_info" to "",
        "description" to "",
        "youtube_url" to "",
        "latitude" to "",
        "longitude" to "",
        "map_url" to "",
        "updated_at" to updatedAt,
        "note" to "",
    )
    extra.forEach { (key, value) -> row[key] = value }
    return LinkedHashMap(row)
}

class FakeReader(
    private val rows: List<LinkedHashMap<String, String>>? = null,
    private val error: Exception? = null,
) : EventSource {
    override fun fetch(sheetId: String, worksheet: String): List<SourceEvent> {
        error?.let { throw it }
        return rows.orEmpty().map { SourceEvent.fromColumns(it) }
    }
}

class InMemoryPublishedEventRepository(
    override val outputPath: Path,
    headers: List<String>,
    records: Map<String, PublishedEventRecord> = emptyMap(),
) : PublishedEventRepository {
    private var snapshot = PublishedEventsSnapshot(headers = headers, records = records)
    var lastSavedRows: List<Map<String, String>> = emptyList()
        private set
    var changedOnSave: Boolean = true

    override fun loadSnapshot(): PublishedEventsSnapshot = snapshot

    override fun save(headers: List<String>, rows: List<Map<String, String>>, dryRun: Boolean): Boolean {
        lastSavedRows = rows
        if (!dryRun) {
            snapshot = PublishedEventsSnapshot(
                headers = headers,
                records = rows.associate { row ->
                    val ordered = LinkedHashMap(row)
                    val record = PublishedEventRecord.fromColumns(ordered)
                    record.eventId to record
                },
            )
        }
        return changedOnSave
    }
}
