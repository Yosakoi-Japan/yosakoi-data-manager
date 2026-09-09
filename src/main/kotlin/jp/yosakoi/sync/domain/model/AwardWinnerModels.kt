package jp.yosakoi.sync.domain.model

import java.net.URI
import java.text.Normalizer
import java.util.Locale

enum class VideoSourceType(val csvValue: String) {
    ORGANIZER_OFFICIAL("OrganizerOfficial"),
    TEAM_OFFICIAL("TeamOfficial"),
    GENERAL("General"),
    ;

    companion object {
        fun fromCsvValue(value: String): VideoSourceType = entries.firstOrNull { it.csvValue == value }
            ?: throw IllegalArgumentException("invalid video_source_type: $value")
    }
}

/** Google Sheets の award_winners シートにある一行を表す。 */
data class SourceAwardWinner(
    val eventId: String,
    val awardName: String,
    val teamName: String,
    val resultSourceUrl: String,
    val videoUrl: String,
    val videoSourceType: String,
    val status: String,
    val updatedAt: String,
    val columns: LinkedHashMap<String, String>,
) {
    companion object {
        private val requiredSourceColumns = listOf(
            "event_id",
            "award_name",
            "team_name",
            "result_source_url",
            "video_url",
            "video_source_type",
            "status",
            "updated_at",
        )

        fun fromColumns(columns: LinkedHashMap<String, String>): SourceAwardWinner {
            val missingColumns = requiredSourceColumns.filterNot(columns::containsKey)
            require(missingColumns.isEmpty()) { "award_winners columns are missing: ${missingColumns.joinToString(",")}" }
            val normalized = LinkedHashMap(columns.mapValues { (_, value) -> value.trim() })
            require(normalized.getValue("event_id").isNotBlank()) { "award_winners event_id is required" }
            require(normalized.getValue("status").isNotBlank()) { "award_winners status is required" }
            return SourceAwardWinner(
                eventId = normalized.getValue("event_id"),
                awardName = normalized.getValue("award_name"),
                teamName = normalized.getValue("team_name"),
                resultSourceUrl = normalized.getValue("result_source_url"),
                videoUrl = normalized.getValue("video_url"),
                videoSourceType = normalized.getValue("video_source_type"),
                status = normalized.getValue("status"),
                updatedAt = normalized.getValue("updated_at"),
                columns = normalized,
            )
        }
    }
}

/** 検証済みで公開可能な受賞チーム行を表す。 */
data class PublishedAwardWinner(
    val eventId: String,
    val awardName: String,
    val teamName: String,
    val resultSourceUrl: URI,
    val videoUrl: URI,
    val videoSourceType: VideoSourceType,
    val updatedAt: String,
) {
    companion object {
        val HEADERS = listOf(
            "event_id",
            "award_name",
            "team_name",
            "result_source_url",
            "video_url",
            "video_source_type",
            "status",
            "updated_at",
        )

        fun normalizedTeamName(value: String): String = Normalizer
            .normalize(value.trim(), Normalizer.Form.NFKC)
            .replace(Regex("\\s+"), " ")
            .lowercase(Locale.ROOT)
    }

    fun toCsvRow(): LinkedHashMap<String, String> = linkedMapOf(
        "event_id" to eventId,
        "award_name" to awardName,
        "team_name" to teamName,
        "result_source_url" to resultSourceUrl.toString(),
        "video_url" to videoUrl.toString(),
        "video_source_type" to videoSourceType.csvValue,
        "status" to "Approved",
        "updated_at" to updatedAt,
    )
}

data class AwardPublicationResult(
    val winners: List<PublishedAwardWinner>,
)
