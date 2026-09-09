package jp.yosakoi.sync.domain.service

import jp.yosakoi.sync.domain.model.ApprovedEvent
import jp.yosakoi.sync.domain.model.DuplicateEventError
import jp.yosakoi.sync.domain.model.PublicationResult
import jp.yosakoi.sync.domain.model.SourceEvent

/**
 * 管理元イベントから公開可能なイベントだけを抽出するドメインルールを表す。
 */
class EventPublicationPolicy {
    /**
     * `Approved` かつ重複なしのイベントを、開催終了後も含めて公開候補として返す。
     */
    fun filterPublishableEvents(sourceEvents: List<SourceEvent>): PublicationResult {
        val approvedEvents = sourceEvents.filter { it.status == "Approved" && it.officialUrl != null }
        val duplicateEvents = approvedEvents
            .groupBy { it.eventId }
            .filterValues { it.size > 1 }
            .map { (eventId, items) -> DuplicateEventError(eventId, items.map { it.eventName }) }
        val duplicateIds = duplicateEvents.map { it.eventId }.toSet()
        val publishableEvents = approvedEvents
            .filterNot { it.eventId in duplicateIds }
            .map(ApprovedEvent::fromSource)

        return PublicationResult(
            publishableEvents = publishableEvents,
            duplicateEvents = duplicateEvents,
        )
    }
}
