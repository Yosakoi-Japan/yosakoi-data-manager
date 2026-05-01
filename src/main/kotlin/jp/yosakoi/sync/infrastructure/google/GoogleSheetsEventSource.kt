package jp.yosakoi.sync.infrastructure.google

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import jp.yosakoi.sync.application.port.EventSource
import jp.yosakoi.sync.domain.model.SourceEvent
import java.io.FileInputStream

/**
 * Google Sheets API を使って管理元イベントを取得するインフラ実装。
 */
class GoogleSheetsEventSource(
    private val credentialsPath: String,
) : EventSource {
    /**
     * ワークシートの全行を取得し、[SourceEvent] 一覧へ変換する。
     */
    override fun fetch(sheetId: String, worksheet: String): List<SourceEvent> {
        val credentials = buildCredentials()
        val service = Sheets.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            GsonFactory.getDefaultInstance(),
            HttpCredentialsAdapter(credentials),
        ).setApplicationName("yosakoi-data-manager").build()

        val values = service.spreadsheets().values().get(sheetId, worksheetRange(worksheet)).execute().getValues().orEmpty()
        if (values.isEmpty()) {
            return emptyList()
        }

        val headers = values.first().map { it.toString() }
        return values.drop(1).map { rowValues ->
            val row = LinkedHashMap<String, String>()
            headers.forEachIndexed { index, header ->
                row[header] = rowValues.getOrNull(index)?.toString() ?: ""
            }
            SourceEvent.fromColumns(row)
        }
    }

    /**
     * サービスアカウント JSON から読み取り専用の認証情報を生成する。
     */
    private fun buildCredentials(): GoogleCredentials {
        val scoped = FileInputStream(credentialsPath).use { GoogleCredentials.fromStream(it) }
        return scoped.createScoped(listOf(SheetsScopes.SPREADSHEETS_READONLY))
    }

    /**
     * ワークシート名を Sheets API の range 形式へ変換する。
     */
    private fun worksheetRange(worksheet: String): String {
        val escaped = worksheet.replace("'", "''")
        return "'$escaped'"
    }
}
