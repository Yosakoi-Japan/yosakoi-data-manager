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
    val duplicateErrorCount: Int = 0,
    val invalidUpdatedAtCount: Int = 0,
    val outputPath: String = "",
    val awardOutputPath: String = "",
    val awardFetchedCount: Int = 0,
    val awardApprovedCount: Int = 0,
    val awardPublishedCount: Int = 0,
    val organizerOfficialVideoCount: Int = 0,
    val teamOfficialVideoCount: Int = 0,
    val generalVideoCount: Int = 0,
    val eventChanged: Boolean = false,
    val awardChanged: Boolean = false,
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
        "duplicate_errors=$duplicateErrorCount",
        "invalid_updated_at=$invalidUpdatedAtCount",
        "output=$outputPath",
        "award_output=$awardOutputPath",
        "award_fetched=$awardFetchedCount",
        "award_approved=$awardApprovedCount",
        "award_published=$awardPublishedCount",
        "videos_organizer_official=$organizerOfficialVideoCount",
        "videos_team_official=$teamOfficialVideoCount",
        "videos_general=$generalVideoCount",
        "event_changed=${if (eventChanged) "true" else "false"}",
        "award_changed=${if (awardChanged) "true" else "false"}",
        "changed=${if (changed) "true" else "false"}",
        "trigger=$trigger",
    )
}
