package jp.yosakoi.sync.application.port

import jp.yosakoi.sync.application.model.PublishedEventsSnapshot
import java.nio.file.Path

/**
 * 公開 CSV の読み書きを行うためのアプリケーションポート。
 */
interface PublishedEventRepository {
    /**
     * 公開 CSV の出力先を返す。
     */
    val outputPath: Path

    /**
     * 受賞チーム CSV の出力先を返す。
     */
    val awardOutputPath: Path

    /**
     * 既存の公開 CSV を読み込み、ヘッダと保存済みイベント一覧を返す。
     */
    fun loadSnapshot(): PublishedEventsSnapshot

    /** イベント CSV と受賞チーム CSV を一つの公開単位として保存する。 */
    fun save(
        eventHeaders: List<String>,
        eventRows: List<Map<String, String>>,
        awardHeaders: List<String>,
        awardRows: List<Map<String, String>>,
        dryRun: Boolean = false,
    ): PublicationSaveResult
}

data class PublicationSaveResult(
    val eventChanged: Boolean,
    val awardChanged: Boolean,
) {
    val changed: Boolean = eventChanged || awardChanged
}
