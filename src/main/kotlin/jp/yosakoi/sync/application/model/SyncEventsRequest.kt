package jp.yosakoi.sync.application.model

import java.time.LocalDate

/**
 * 同期処理の入力値をまとめたアプリケーション要求モデル。
 */
data class SyncEventsRequest(
    val sheetId: String,
    val worksheet: String,
    val dryRun: Boolean,
    val trigger: String,
    val today: LocalDate? = null,
)
