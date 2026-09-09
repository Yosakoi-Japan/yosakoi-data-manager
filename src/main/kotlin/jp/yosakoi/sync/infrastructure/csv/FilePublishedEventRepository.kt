package jp.yosakoi.sync.infrastructure.csv

import jp.yosakoi.sync.application.model.PublishedEventsSnapshot
import jp.yosakoi.sync.application.port.PublishedEventRepository
import jp.yosakoi.sync.application.port.PublicationSaveResult
import jp.yosakoi.sync.domain.model.PublishedEventRecord
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVPrinter
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

/**
 * ローカルファイル上の `yosakoi_festival.csv` を読み書きするインフラ実装。
 */
open class FilePublishedEventRepository(
    override val outputPath: Path,
    override val awardOutputPath: Path = outputPath.resolveSibling("award_winners.csv"),
) : PublishedEventRepository {
    /**
     * 既存の公開 CSV を読み込み、ヘッダと event_id ごとの既存レコードを返す。
     */
    override fun loadSnapshot(): PublishedEventsSnapshot {
        if (!Files.exists(outputPath)) {
            return PublishedEventsSnapshot(headers = emptyList(), records = emptyMap())
        }

        Files.newBufferedReader(outputPath, StandardCharsets.UTF_8).use { reader ->
            val parser = CSVParser(
                reader,
                CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build(),
            )
            val headers = parser.headerNames.toList()
            val records = linkedMapOf<String, PublishedEventRecord>()
            parser.records.forEach { record ->
                val row = LinkedHashMap<String, String>()
                headers.forEach { header -> row[header] = record.get(header) ?: "" }
                val publishedRecord = PublishedEventRecord.fromColumns(row)
                records[publishedRecord.eventId] = publishedRecord
            }
            return PublishedEventsSnapshot(headers = headers, records = records)
        }
    }

    /** 差分がある CSV だけを書き込み、書き込み失敗は呼び出し元へ伝える。 */
    override fun save(
        eventHeaders: List<String>,
        eventRows: List<Map<String, String>>,
        awardHeaders: List<String>,
        awardRows: List<Map<String, String>>,
        dryRun: Boolean,
    ): PublicationSaveResult {
        val documents = listOf(
            CsvDocument(outputPath, buildCsv(eventHeaders, eventRows)),
            CsvDocument(awardOutputPath, buildCsv(awardHeaders, awardRows)),
        )
        val changed = documents.filter { document -> contentDiffers(document.path, document.content) }
        val result = PublicationSaveResult(
            eventChanged = changed.any { it.path == outputPath },
            awardChanged = changed.any { it.path == awardOutputPath },
        )
        if (dryRun || changed.isEmpty()) {
            return result
        }

        changed.forEach { document ->
            val target = document.path.toAbsolutePath()
            Files.createDirectories(target.parent)
            Files.writeString(target, document.content, StandardCharsets.UTF_8)
        }
        return result
    }

    private fun contentDiffers(path: Path, content: String): Boolean {
        if (!Files.exists(path)) return true
        return contentChecksum(Files.readString(path, StandardCharsets.UTF_8)) != contentChecksum(content)
    }

    /**
     * 指定ヘッダ順で CSV 文字列を組み立てる。
     */
    private fun buildCsv(headers: List<String>, rows: List<Map<String, String>>): String {
        val writer = StringWriter()
        CSVPrinter(
            writer,
            CSVFormat.DEFAULT.builder().setHeader(*headers.toTypedArray()).setRecordSeparator('\n').build(),
        ).use { printer ->
            rows.forEach { row ->
                printer.printRecord(headers.map { header -> row[header] ?: "" })
            }
        }
        return writer.toString()
    }

    /**
     * CSV 全文の差分判定に使うハッシュ値を生成する。
     */
    private fun contentChecksum(content: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(content.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private data class CsvDocument(val path: Path, val content: String)
}
