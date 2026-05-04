package jp.yosakoi.sync.contract

import jp.yosakoi.sync.makeRow
import jp.yosakoi.sync.infrastructure.csv.FilePublishedEventRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.LinkedHashMap

class PublicCsvSchemaTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `csv schema is preserved`() {
        val output = tempDir.resolve("yosakoi_festival.csv")
        val repository = FilePublishedEventRepository(output)
        val row = makeRow(eventId = "a", eventName = "Festival A")
        val headers = row.keys.filterNot { it == "note" }
        val managedRow = LinkedHashMap<String, String>().apply {
            headers.forEach { header -> this[header] = row[header].orEmpty() }
        }

        val changed = repository.save(headers, listOf(managedRow))

        assertTrue(changed)
        val lines = output.toFile().readLines()
        assertEquals(headers, lines.first().split(","))
        assertEquals("a", lines[1].split(",").first())
    }
}
