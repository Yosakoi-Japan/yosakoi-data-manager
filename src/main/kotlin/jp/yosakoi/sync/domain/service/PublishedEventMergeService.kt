package jp.yosakoi.sync.domain.service

import jp.yosakoi.sync.domain.model.ApprovedEvent
import jp.yosakoi.sync.domain.model.InvalidUpdatedAtRecord
import jp.yosakoi.sync.domain.model.MergeResult
import jp.yosakoi.sync.domain.model.PublishedEventRecord
import jp.yosakoi.sync.domain.model.SyncDecision
import jp.yosakoi.sync.domain.model.SyncDecisionType
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * 公開候補と既存公開データを比較し、最終的な公開内容を組み立てるドメインサービス。
 */
class PublishedEventMergeService {
    /**
     * 公開候補を既存公開データへ適用し、新規・更新・非更新の判定結果を返す。
     */
    fun merge(
        publishableEvents: List<ApprovedEvent>,
        existingRecords: Map<String, PublishedEventRecord>,
    ): MergeResult {
        val decisions = mutableListOf<SyncDecision>()
        val invalidUpdatedAtRecords = mutableListOf<InvalidUpdatedAtRecord>()
        val rowsByEventId = linkedMapOf<String, LinkedHashMap<String, String>>()

        existingRecords.forEach { (eventId, record) ->
            rowsByEventId[eventId] = LinkedHashMap(record.columns)
        }

        publishableEvents.forEach { event ->
            val existing = existingRecords[event.eventId]
            if (existing == null) {
                decisions += SyncDecision(event, null, SyncDecisionType.NEW)
                rowsByEventId[event.eventId] = LinkedHashMap(event.columns)
                return@forEach
            }

            try {
                val decisionType = if (parseUpdatedAt(event.updatedAt).isAfter(parseUpdatedAt(existing.updatedAt))) {
                    rowsByEventId[event.eventId] = LinkedHashMap(event.columns)
                    SyncDecisionType.UPDATED
                } else {
                    SyncDecisionType.SKIPPED
                }
                decisions += SyncDecision(event, existing, decisionType)
            } catch (_: Exception) {
                invalidUpdatedAtRecords += InvalidUpdatedAtRecord(
                    eventId = event.eventId,
                    eventName = event.eventName,
                    updatedAt = event.updatedAt,
                    reason = "invalid updated_at",
                )
                decisions += SyncDecision(event, existing, SyncDecisionType.INVALID_UPDATED_AT)
            }
        }

        return MergeResult(
            decisions = decisions,
            invalidUpdatedAtRecords = invalidUpdatedAtRecords,
            rowsByEventId = rowsByEventId,
        )
    }

    /**
     * `updated_at` を比較可能な日時へ変換する。日付のみの場合は日本時間の 00:00 として扱う。
     */
    private fun parseUpdatedAt(value: String): OffsetDateTime {
        return runCatching { OffsetDateTime.parse(value) }
            .getOrElse {
                LocalDate.parse(value).atStartOfDay().atOffset(ZoneOffset.ofHours(9))
            }
    }
}
