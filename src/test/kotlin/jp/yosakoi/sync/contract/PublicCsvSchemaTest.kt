package jp.yosakoi.sync.contract

import jp.yosakoi.sync.makeRow
import jp.yosakoi.sync.infrastructure.csv.FilePublishedEventRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class PublicCsvSchemaTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `csv schema is preserved`() {
        val output = tempDir.resolve("yosakoi_festival.csv")
        val repository = FilePublishedEventRepository(output)
        val row = makeRow(eventId = "a", eventName = "Festival A")

        val changed = repository.save(row.keys.toList(), listOf(row))

        assertTrue(changed)
        val lines = output.toFile().readLines()
        assertEquals(row.keys.toList(), lines.first().split(","))
        assertEquals("a", lines[1].split(",").first())
    }
}
