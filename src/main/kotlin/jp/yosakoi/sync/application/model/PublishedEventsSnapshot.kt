package jp.yosakoi.sync.application.model

import jp.yosakoi.sync.domain.model.PublishedEventRecord

/**
 * 既存公開 CSV のヘッダと、保存済みイベント一覧を表す。
 */
data class PublishedEventsSnapshot(
    val headers: List<String>,
    val records: Map<String, PublishedEventRecord>,
)
