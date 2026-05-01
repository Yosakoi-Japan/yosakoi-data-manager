package jp.yosakoi.sync.application.port

import jp.yosakoi.sync.domain.model.SourceEvent

/**
 * 管理元イベントを取得するためのアプリケーションポート。
 */
interface EventSource {
    /**
     * 指定したシートとワークシートから行一覧を取得する。
     */
    fun fetch(sheetId: String, worksheet: String): List<SourceEvent>
}
