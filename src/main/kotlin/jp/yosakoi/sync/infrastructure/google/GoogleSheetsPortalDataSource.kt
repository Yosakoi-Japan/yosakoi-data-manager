package jp.yosakoi.sync.infrastructure.google

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import jp.yosakoi.sync.application.port.PortalDataSource
import jp.yosakoi.sync.domain.model.SourceAwardWinner
import jp.yosakoi.sync.domain.model.SourceEvent
import java.io.FileInputStream

/** Google Sheets APIからイベントと受賞チームを取得する。 */
class GoogleSheetsPortalDataSource(
    private val credentialsPath: String,
) : PortalDataSource {
    private val service: Sheets by lazy(::buildService)

    override fun fetchEvents(sheetId: String, worksheet: String): List<SourceEvent> {
        return fetchRows(sheetId, worksheet).map(SourceEvent::fromColumns)
    }

    override fun fetchAwardWinners(sheetId: String, worksheet: String): List<SourceAwardWinner> {
        return fetchRows(sheetId, worksheet).map(SourceAwardWinner::fromColumns)
    }

    private fun fetchRows(sheetId: String, worksheet: String): List<LinkedHashMap<String, String>> {
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
            row
        }
    }

    private fun buildService(): Sheets {
        val credentials = buildCredentials()
        return Sheets.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            GsonFactory.getDefaultInstance(),
            HttpCredentialsAdapter(credentials),
        ).setApplicationName("yosakoi-data-manager").build()
    }

    private fun buildCredentials(): GoogleCredentials {
        val scoped = FileInputStream(credentialsPath).use { GoogleCredentials.fromStream(it) }
        return scoped.createScoped(listOf(SheetsScopes.SPREADSHEETS_READONLY))
    }

    private fun worksheetRange(worksheet: String): String {
        val escaped = worksheet.replace("'", "''")
        return "'$escaped'"
    }
}
