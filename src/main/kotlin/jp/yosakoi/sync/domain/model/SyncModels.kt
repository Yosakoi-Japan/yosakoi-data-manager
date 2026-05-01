package jp.yosakoi.sync.domain.model

/**
 * 公開候補イベントに対する比較結果の種別を表す。
 */
enum class SyncDecisionType {
    NEW,
    UPDATED,
    SKIPPED,
    INVALID_UPDATED_AT,
}

/**
 * 公開候補イベントと既存公開データの比較結果を表す。
 */
data class SyncDecision(
    val event: ApprovedEvent,
    val existing: PublishedEventRecord?,
    val type: SyncDecisionType,
)

/**
 * 公開候補抽出後のイベント一覧と、除外されたイベント一覧をまとめた結果。
 */
data class PublicationResult(
    val publishableEvents: List<ApprovedEvent>,
    val expiredEvents: List<SourceEvent>,
    val duplicateEvents: List<DuplicateEventError>,
)

/**
 * 公開候補と既存公開データを統合した結果を表す。
 */
data class MergeResult(
    val decisions: List<SyncDecision>,
    val invalidUpdatedAtRecords: List<InvalidUpdatedAtRecord>,
    val rowsByEventId: Map<String, LinkedHashMap<String, String>>,
)
