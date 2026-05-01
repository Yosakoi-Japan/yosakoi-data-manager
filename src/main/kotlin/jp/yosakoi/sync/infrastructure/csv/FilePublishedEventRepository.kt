package jp.yosakoi.sync.infrastructure.csv

import jp.yosakoi.sync.application.model.PublishedEventsSnapshot
import jp.yosakoi.sync.application.port.PublishedEventRepository
import jp.yosakoi.sync.domain.model.PublishedEventRecord
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVPrinter
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

/**
 * ローカルファイル上の `yosakoi_festival.csv` を読み書きするインフラ実装。
 */
open class FilePublishedEventRepository(
    override val outputPath: Path,
) : PublishedEventRepository {
    constructor(outputPath: String) : this(Path.of(outputPath))

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

    /**
     * 生成内容に差分がある場合だけ、公開 CSV を安全に置き換える。
     */
    override fun save(headers: List<String>, rows: List<Map<String, String>>, dryRun: Boolean): Boolean {
        val content = buildCsv(headers, rows)
        if (Files.exists(outputPath)) {
            val existing = Files.readString(outputPath, StandardCharsets.UTF_8)
            if (contentChecksum(existing) == contentChecksum(content)) {
                return false
            }
        }
        if (dryRun) {
            return true
        }

        outputPath.parent?.let(Files::createDirectories)
        val tempPath = outputPath.resolveSibling("${outputPath.fileName}.tmp")
        try {
            Files.writeString(tempPath, content, StandardCharsets.UTF_8)
            Files.move(tempPath, outputPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } finally {
            Files.deleteIfExists(tempPath)
        }
        return true
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
}
