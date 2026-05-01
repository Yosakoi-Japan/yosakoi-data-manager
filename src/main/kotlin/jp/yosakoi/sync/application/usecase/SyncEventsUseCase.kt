package jp.yosakoi.sync.application.usecase

import jp.yosakoi.sync.application.model.SyncEventsRequest
import jp.yosakoi.sync.application.model.SyncResult
import jp.yosakoi.sync.application.port.EventSource
import jp.yosakoi.sync.application.port.PublishedEventRepository
import jp.yosakoi.sync.domain.model.SyncDecisionType
import jp.yosakoi.sync.domain.service.EventPublicationPolicy
import jp.yosakoi.sync.domain.service.PublishedEventMergeService
import java.time.LocalDate

/**
 * イベント同期のユースケースを表すアプリケーションサービス。
 */
class SyncEventsUseCase(
    private val eventSource: EventSource,
    private val publishedEventRepository: PublishedEventRepository,
    private val publicationPolicy: EventPublicationPolicy = EventPublicationPolicy(),
    private val mergeService: PublishedEventMergeService = PublishedEventMergeService(),
) {
    /**
     * 管理元取得から公開 CSV 更新までの一連の同期処理を実行する。
     */
    fun execute(request: SyncEventsRequest): SyncResult {
        val runDate = request.today ?: LocalDate.now()
        val sourceEvents = eventSource.fetch(request.sheetId, request.worksheet)
        val publicationResult = publicationPolicy.filterPublishableEvents(sourceEvents, runDate)
        val snapshot = publishedEventRepository.loadSnapshot()
        require(snapshot.headers.isNotEmpty()) { "yosakoi_festival.csv header is required" }

        val mergeResult = mergeService.merge(
            publishableEvents = publicationResult.publishableEvents,
            existingRecords = snapshot.records,
        )
        val finalRows = publicationResult.publishableEvents
            .mapNotNull { event -> mergeResult.rowsByEventId[event.eventId] }
        val changed = publishedEventRepository.save(snapshot.headers, finalRows, request.dryRun)

        return SyncResult(
            fetchedCount = sourceEvents.size,
            approvedCount = publicationResult.publishableEvents.size,
            newCount = mergeResult.decisions.count { it.type == SyncDecisionType.NEW },
            updatedCount = mergeResult.decisions.count { it.type == SyncDecisionType.UPDATED },
            skippedCount = mergeResult.decisions.count { it.type == SyncDecisionType.SKIPPED },
            expiredCount = publicationResult.expiredEvents.size,
            duplicateErrorCount = publicationResult.duplicateEvents.size,
            invalidUpdatedAtCount = mergeResult.invalidUpdatedAtRecords.size,
            outputPath = publishedEventRepository.outputPath.normalize().toString(),
            changed = changed,
            duplicateEventIds = publicationResult.duplicateEvents.map { it.eventId },
            warnings = mergeResult.invalidUpdatedAtRecords.map { it.reason },
            trigger = request.trigger,
        )
    }
}
