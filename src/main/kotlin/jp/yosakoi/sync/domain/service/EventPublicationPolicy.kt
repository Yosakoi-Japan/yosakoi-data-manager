package jp.yosakoi.sync.domain.service

import jp.yosakoi.sync.domain.model.ApprovedEvent
import jp.yosakoi.sync.domain.model.DuplicateEventError
import jp.yosakoi.sync.domain.model.PublicationResult
import jp.yosakoi.sync.domain.model.SourceEvent
import java.time.LocalDate

/**
 * 管理元イベントから公開可能なイベントだけを抽出するドメインルールを表す。
 */
class EventPublicationPolicy {
    /**
     * `Approved`、期限内、重複なしのイベントだけを公開候補として返す。
     */
    fun filterPublishableEvents(sourceEvents: List<SourceEvent>, today: LocalDate): PublicationResult {
        val approvedEvents = sourceEvents.filter { it.status == "Approved" && it.officialUrl != null }
        val activeEvents = mutableListOf<SourceEvent>()
        val expiredEvents = mutableListOf<SourceEvent>()

        approvedEvents.forEach { event ->
            if (LocalDate.parse(event.endDate).isBefore(today)) {
                expiredEvents += event
            } else {
                activeEvents += event
            }
        }

        val duplicateEvents = activeEvents
            .groupBy { it.eventId }
            .filterValues { it.size > 1 }
            .map { (eventId, items) -> DuplicateEventError(eventId, items.map { it.eventName }) }
        val duplicateIds = duplicateEvents.map { it.eventId }.toSet()
        val publishableEvents = activeEvents
            .filterNot { it.eventId in duplicateIds }
            .map(ApprovedEvent::fromSource)

        return PublicationResult(
            publishableEvents = publishableEvents,
            expiredEvents = expiredEvents,
            duplicateEvents = duplicateEvents,
        )
    }
}
