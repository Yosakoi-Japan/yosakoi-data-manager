package jp.yosakoi.sync

import jp.yosakoi.sync.application.model.PublishedEventsSnapshot
import jp.yosakoi.sync.application.port.PortalDataSource
import jp.yosakoi.sync.application.port.PublishedEventRepository
import jp.yosakoi.sync.application.port.PublicationSaveResult
import jp.yosakoi.sync.domain.model.PublishedEventRecord
import jp.yosakoi.sync.domain.model.SourceAwardWinner
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
        "review" to "",
    )
    extra.forEach { (key, value) -> row[key] = value }
    return LinkedHashMap(row)
}

fun makeAwardRow(
    eventId: String,
    teamName: String,
    status: String = "Approved",
    awardName: String = "大賞",
    resultSourceUrl: String = "https://example.com/results/$eventId",
    videoUrl: String = "https://www.youtube.com/watch?v=abcdefghijk",
    videoSourceType: String = "OrganizerOfficial",
    updatedAt: String = "2026-08-01T10:00:00+09:00",
): LinkedHashMap<String, String> = linkedMapOf(
    "event_id" to eventId,
    "award_name" to awardName,
    "team_name" to teamName,
    "result_source_url" to resultSourceUrl,
    "video_url" to videoUrl,
    "video_source_type" to videoSourceType,
    "status" to status,
    "updated_at" to updatedAt,
    "note" to "",
)

class FakePortalDataSource(
    private val rows: List<LinkedHashMap<String, String>>? = null,
    private val error: Exception? = null,
    private val awardRows: List<LinkedHashMap<String, String>> = emptyList(),
    private val awardError: Exception? = null,
) : PortalDataSource {
    override fun fetchEvents(sheetId: String, worksheet: String): List<SourceEvent> {
        error?.let { throw it }
        return rows.orEmpty().map { SourceEvent.fromColumns(it) }
    }

    override fun fetchAwardWinners(sheetId: String, worksheet: String): List<SourceAwardWinner> {
        awardError?.let { throw it }
        return awardRows.map(SourceAwardWinner::fromColumns)
    }
}

class InMemoryPublishedEventRepository(
    override val outputPath: Path,
    override val awardOutputPath: Path = outputPath.resolveSibling("award_winners.csv"),
    headers: List<String>,
    records: Map<String, PublishedEventRecord> = emptyMap(),
) : PublishedEventRepository {
    private var snapshot = PublishedEventsSnapshot(headers = headers, records = records)
    var lastSavedRows: List<Map<String, String>> = emptyList()
        private set
    var changedOnSave: Boolean = true
    var lastSavedAwardRows: List<Map<String, String>> = emptyList()

    override fun loadSnapshot(): PublishedEventsSnapshot = snapshot

    override fun save(
        eventHeaders: List<String>,
        eventRows: List<Map<String, String>>,
        awardHeaders: List<String>,
        awardRows: List<Map<String, String>>,
        dryRun: Boolean,
    ): PublicationSaveResult {
        lastSavedRows = eventRows
        lastSavedAwardRows = awardRows
        if (!dryRun) {
            snapshot = PublishedEventsSnapshot(
                headers = eventHeaders,
                records = eventRows.associate { row ->
                    val ordered = LinkedHashMap(row)
                    val record = PublishedEventRecord.fromColumns(ordered)
                    record.eventId to record
                },
            )
        }
        return PublicationSaveResult(eventChanged = changedOnSave, awardChanged = awardRows.isNotEmpty())
    }
}
