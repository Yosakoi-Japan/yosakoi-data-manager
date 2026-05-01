package jp.yosakoi.sync.application.model

/**
 * 同期処理の実行結果を表す。
 */
data class SyncResult(
    val fetchedCount: Int = 0,
    val approvedCount: Int = 0,
    val newCount: Int = 0,
    val updatedCount: Int = 0,
    val skippedCount: Int = 0,
    val expiredCount: Int = 0,
    val duplicateErrorCount: Int = 0,
    val invalidUpdatedAtCount: Int = 0,
    val outputPath: String = "",
    val changed: Boolean = false,
    val duplicateEventIds: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val trigger: String = "manual",
) {
    /**
     * CLI 契約に合わせて、標準出力へ並べる行一覧を組み立てる。
     */
    fun toStdoutLines(): List<String> = listOf(
        "fetched=$fetchedCount",
        "approved=$approvedCount",
        "new=$newCount",
        "updated=$updatedCount",
        "skipped=$skippedCount",
        "expired=$expiredCount",
        "duplicate_errors=$duplicateErrorCount",
        "invalid_updated_at=$invalidUpdatedAtCount",
        "output=$outputPath",
        "changed=${if (changed) "true" else "false"}",
        "trigger=$trigger",
    )
}
