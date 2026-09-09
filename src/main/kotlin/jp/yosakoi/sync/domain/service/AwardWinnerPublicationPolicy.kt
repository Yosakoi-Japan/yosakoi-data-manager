package jp.yosakoi.sync.domain.service

import jp.yosakoi.sync.domain.model.AwardPublicationResult
import jp.yosakoi.sync.domain.model.PublishedAwardWinner
import jp.yosakoi.sync.domain.model.SourceAwardWinner
import jp.yosakoi.sync.domain.model.VideoSourceType
import java.net.URI
import java.time.OffsetDateTime

/** Approved の受賞行を公開可能な形式へ検証・変換する。 */
class AwardWinnerPublicationPolicy {
    fun publish(sourceRows: List<SourceAwardWinner>, publishedEventIds: Set<String>): AwardPublicationResult {
        val winners = sourceRows
            .filter { it.status == "Approved" }
            .map { validateApproved(it, publishedEventIds) }

        val duplicateKeys = winners
            .groupBy { it.eventId to PublishedAwardWinner.normalizedTeamName(it.teamName) }
            .filterValues { it.size > 1 }
            .keys
        require(duplicateKeys.isEmpty()) {
            "duplicate award winner: ${duplicateKeys.joinToString { "${it.first}/${it.second}" }}"
        }

        return AwardPublicationResult(
            winners = winners.sortedWith(
                compareBy<PublishedAwardWinner> { it.eventId }
                    .thenBy { PublishedAwardWinner.normalizedTeamName(it.teamName) },
            ),
        )
    }

    private fun validateApproved(row: SourceAwardWinner, publishedEventIds: Set<String>): PublishedAwardWinner {
        require(row.eventId in publishedEventIds) { "award winner references unpublished event_id=${row.eventId}" }
        require(row.awardName.isNotBlank()) { "award_name is required for event_id=${row.eventId}" }
        require(row.teamName.isNotBlank()) { "team_name is required for event_id=${row.eventId}" }
        val resultSourceUrl = parseHttpsUrl(row.resultSourceUrl, "result_source_url", row.eventId)
        val videoUrl = parseDirectVideoUrl(row.videoUrl, row.eventId)
        val sourceType = VideoSourceType.fromCsvValue(row.videoSourceType)
        require(row.updatedAt.isNotBlank()) { "updated_at is required for event_id=${row.eventId}" }
        runCatching { OffsetDateTime.parse(row.updatedAt) }
            .getOrElse { throw IllegalArgumentException("invalid award updated_at for event_id=${row.eventId}") }

        return PublishedAwardWinner(
            eventId = row.eventId,
            awardName = row.awardName,
            teamName = row.teamName,
            resultSourceUrl = resultSourceUrl,
            videoUrl = videoUrl,
            videoSourceType = sourceType,
            updatedAt = row.updatedAt,
        )
    }

    private fun parseHttpsUrl(value: String, fieldName: String, eventId: String): URI {
        require(value.isNotBlank()) { "$fieldName is required for event_id=$eventId" }
        val uri = runCatching { URI(value) }.getOrNull()
        require(uri != null && uri.isAbsolute && uri.scheme == "https" && !uri.host.isNullOrBlank()) {
            "$fieldName must be an absolute HTTPS URL for event_id=$eventId"
        }
        return uri
    }

    private fun parseDirectVideoUrl(value: String, eventId: String): URI {
        val uri = parseHttpsUrl(value, "video_url", eventId)
        val path = uri.path.orEmpty().lowercase()
        val query = uri.query.orEmpty().lowercase()
        val prohibitedPath = listOf("/channel/", "/playlist", "/user/", "/@", "/c/")
        require(prohibitedPath.none(path::contains) && "list=" !in query) {
            "video_url must point to a single video for event_id=$eventId"
        }
        if (uri.host.lowercase().removePrefix("www.") in setOf("youtube.com", "m.youtube.com")) {
            val directYouTubePath = path == "/watch" && query.split('&').any { it.startsWith("v=") && it.length > 2 } ||
                path.startsWith("/shorts/") || path.startsWith("/live/")
            require(directYouTubePath) { "video_url must point to a single YouTube video for event_id=$eventId" }
        }
        return uri
    }
}
