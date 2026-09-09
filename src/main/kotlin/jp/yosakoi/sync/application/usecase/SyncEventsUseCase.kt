package jp.yosakoi.sync.application.usecase

import jp.yosakoi.sync.application.model.SyncEventsRequest
import jp.yosakoi.sync.application.model.SyncResult
import jp.yosakoi.sync.application.port.PortalDataSource
import jp.yosakoi.sync.application.port.PublishedEventRepository
import jp.yosakoi.sync.domain.model.PublishedAwardWinner
import jp.yosakoi.sync.domain.model.SyncDecisionType
import jp.yosakoi.sync.domain.model.VideoSourceType
import jp.yosakoi.sync.domain.service.AwardWinnerPublicationPolicy
import jp.yosakoi.sync.domain.service.EventPublicationPolicy
import jp.yosakoi.sync.domain.service.PublishedEventMergeService

/**
 * イベント同期のユースケースを表すアプリケーションサービス。
 */
class SyncEventsUseCase(
    private val source: PortalDataSource,
    private val publishedEventRepository: PublishedEventRepository,
    private val publicationPolicy: EventPublicationPolicy = EventPublicationPolicy(),
    private val awardPublicationPolicy: AwardWinnerPublicationPolicy = AwardWinnerPublicationPolicy(),
    private val mergeService: PublishedEventMergeService = PublishedEventMergeService(),
) {
    companion object {
        const val AWARD_WINNERS_WORKSHEET = "award_winners"
        private val excludedOutputColumns = setOf("note", "review")
    }

    /**
     * 管理元取得から公開 CSV 更新までの一連の同期処理を実行する。
     */
    fun execute(request: SyncEventsRequest): SyncResult {
        val sourceEvents = source.fetchEvents(request.sheetId, request.worksheet)
        val sourceAwardWinners = source.fetchAwardWinners(request.sheetId, AWARD_WINNERS_WORKSHEET)
        val publicationResult = publicationPolicy.filterPublishableEvents(sourceEvents)
        val awardPublicationResult = awardPublicationPolicy.publish(
            sourceAwardWinners,
            publicationResult.publishableEvents.map { it.eventId }.toSet(),
        )
        val snapshot = publishedEventRepository.loadSnapshot()
        require(snapshot.headers.isNotEmpty()) { "yosakoi_festival.csv header is required" }
        val managedHeaders = snapshot.headers.filterNot { it in excludedOutputColumns }

        val mergeResult = mergeService.merge(
            publishableEvents = publicationResult.publishableEvents,
            existingRecords = snapshot.records,
        )
        val finalRows = publicationResult.publishableEvents
            .mapNotNull { event -> mergeResult.rowsByEventId[event.eventId] }
            .map { row -> selectManagedColumns(row, managedHeaders) }
        val awardRows = awardPublicationResult.winners.map(PublishedAwardWinner::toCsvRow)
        val saveResult = publishedEventRepository.save(
            eventHeaders = managedHeaders,
            eventRows = finalRows,
            awardHeaders = PublishedAwardWinner.HEADERS,
            awardRows = awardRows,
            dryRun = request.dryRun,
        )

        return SyncResult(
            fetchedCount = sourceEvents.size,
            approvedCount = publicationResult.publishableEvents.size,
            newCount = mergeResult.decisions.count { it.type == SyncDecisionType.NEW },
            updatedCount = mergeResult.decisions.count { it.type == SyncDecisionType.UPDATED },
            skippedCount = mergeResult.decisions.count { it.type == SyncDecisionType.SKIPPED },
            duplicateErrorCount = publicationResult.duplicateEvents.size,
            invalidUpdatedAtCount = mergeResult.invalidUpdatedAtRecords.size,
            outputPath = publishedEventRepository.outputPath.normalize().toString(),
            awardOutputPath = publishedEventRepository.awardOutputPath.normalize().toString(),
            awardFetchedCount = sourceAwardWinners.size,
            awardApprovedCount = sourceAwardWinners.count { it.status == "Approved" },
            awardPublishedCount = awardPublicationResult.winners.size,
            organizerOfficialVideoCount = awardPublicationResult.winners.count { it.videoSourceType == VideoSourceType.ORGANIZER_OFFICIAL },
            teamOfficialVideoCount = awardPublicationResult.winners.count { it.videoSourceType == VideoSourceType.TEAM_OFFICIAL },
            generalVideoCount = awardPublicationResult.winners.count { it.videoSourceType == VideoSourceType.GENERAL },
            eventChanged = saveResult.eventChanged,
            awardChanged = saveResult.awardChanged,
            changed = saveResult.changed,
            duplicateEventIds = publicationResult.duplicateEvents.map { it.eventId },
            warnings = mergeResult.invalidUpdatedAtRecords.map { it.reason },
            trigger = request.trigger,
        )
    }

    /**
     * 公開用 CSV に含める列だけを、指定ヘッダ順で取り出す。
     */
    private fun selectManagedColumns(row: Map<String, String>, headers: List<String>): LinkedHashMap<String, String> {
        val selected = LinkedHashMap<String, String>()
        headers.forEach { header -> selected[header] = row[header].orEmpty() }
        return selected
    }
}
