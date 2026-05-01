package jp.yosakoi.sync.domain.model

import java.net.URI

/**
 * 管理元の Google スプレッドシートから取得した 1 行分のイベントを表す。
 */
data class SourceEvent(
    val eventId: String,
    val eventName: String,
    val status: String,
    val officialUrl: URI?,
    val startDate: String,
    val endDate: String,
    val updatedAt: String,
    val columns: LinkedHashMap<String, String>,
) {
    companion object {
        private val requiredColumns = listOf("event_id", "event_name", "status", "start_date", "end_date", "updated_at")

        /**
         * 生の列データを正規化し、必須列を検証したうえで [SourceEvent] を生成する。
         */
        fun fromColumns(columns: LinkedHashMap<String, String>): SourceEvent {
            val normalized = LinkedHashMap(columns.mapValues { (_, value) -> value.trim() })
            val missing = requiredColumns.filter { normalized[it].isNullOrBlank() }
            require(missing.isEmpty()) { "required field is empty: ${missing.joinToString(",")}" }
            return SourceEvent(
                eventId = normalized.getValue("event_id"),
                eventName = normalized.getValue("event_name"),
                status = normalized.getValue("status"),
                officialUrl = parseOfficialUrl(normalized["official_url"].orEmpty()),
                startDate = normalized.getValue("start_date"),
                endDate = normalized.getValue("end_date"),
                updatedAt = normalized.getValue("updated_at"),
                columns = normalized,
            )
        }

        /**
         * `official_url` を検証済み URI として解釈する。無効な場合は `null` を返す。
         */
        private fun parseOfficialUrl(value: String): URI? {
            if (value.isBlank()) {
                return null
            }
            return runCatching { URI(value) }
                .getOrNull()
                ?.takeIf { it.isAbsolute && !it.host.isNullOrBlank() && it.scheme in setOf("http", "https") }
        }
    }
}

/**
 * 公開対象判定の途中で扱う、公開候補イベントを表す。
 */
data class ApprovedEvent(
    val eventId: String,
    val eventName: String,
    val updatedAt: String,
    val startDate: String,
    val endDate: String,
    val columns: LinkedHashMap<String, String>,
) {
    companion object {
        /**
         * 管理元イベントから公開候補イベントを生成する。
         */
        fun fromSource(source: SourceEvent): ApprovedEvent = ApprovedEvent(
            eventId = source.eventId,
            eventName = source.eventName,
            updatedAt = source.updatedAt,
            startDate = source.startDate,
            endDate = source.endDate,
            columns = LinkedHashMap(source.columns),
        )
    }
}

/**
 * 既存の公開 CSV に保存済みの 1 行分のイベントを表す。
 */
data class PublishedEventRecord(
    val eventId: String,
    val eventName: String,
    val updatedAt: String,
    val columns: LinkedHashMap<String, String>,
) {
    companion object {
        /**
         * 既存 CSV の列データから [PublishedEventRecord] を生成する。
         */
        fun fromColumns(columns: LinkedHashMap<String, String>): PublishedEventRecord {
            require(!columns["event_id"].isNullOrBlank()) { "event_id is required" }
            require(!columns["event_name"].isNullOrBlank()) { "event_name is required" }
            require(!columns["updated_at"].isNullOrBlank()) { "updated_at is required" }
            return PublishedEventRecord(
                eventId = columns.getValue("event_id"),
                eventName = columns.getValue("event_name"),
                updatedAt = columns.getValue("updated_at"),
                columns = LinkedHashMap(columns),
            )
        }
    }
}

/**
 * 重複 event_id により公開対象外になったイベント群を表す。
 */
data class DuplicateEventError(
    val eventId: String,
    val eventNames: List<String>,
)

/**
 * updated_at が不正だったため既存行を更新できなかったイベントを表す。
 */
data class InvalidUpdatedAtRecord(
    val eventId: String,
    val eventName: String,
    val updatedAt: String,
    val reason: String,
)
