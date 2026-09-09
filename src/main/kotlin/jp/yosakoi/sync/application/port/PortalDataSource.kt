package jp.yosakoi.sync.application.port

import jp.yosakoi.sync.domain.model.SourceAwardWinner
import jp.yosakoi.sync.domain.model.SourceEvent

/** ポータル公開に必要なイベントと受賞チームを同じ管理元から取得するポート。 */
interface PortalDataSource {
    fun fetchEvents(sheetId: String, worksheet: String): List<SourceEvent>

    fun fetchAwardWinners(sheetId: String, worksheet: String): List<SourceAwardWinner>
}
