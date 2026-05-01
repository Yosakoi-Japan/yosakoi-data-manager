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
     * 既存の公開 CSV を読み込み、ヘッダと保存済みイベント一覧を返す。
     */
    fun loadSnapshot(): PublishedEventsSnapshot

    /**
     * 指定ヘッダと行一覧で公開 CSV を保存し、差分があったかを返す。
     */
    fun save(headers: List<String>, rows: List<Map<String, String>>, dryRun: Boolean = false): Boolean
}
