package jp.yosakoi.sync.contract

import jp.yosakoi.sync.FakeReader
import jp.yosakoi.sync.InMemoryPublishedEventRepository
import jp.yosakoi.sync.SyncEventsCli
import jp.yosakoi.sync.SyncEventsCommand
import jp.yosakoi.sync.makeRow
import jp.yosakoi.sync.application.usecase.SyncEventsUseCase
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Path

class SyncEventsCliTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `cli requires arguments`() {
        val output = ByteArrayOutputStream()

        val exitCode = SyncEventsCli.run(emptyArray(), stdout = PrintStream(output))

        assertEquals(1, exitCode)
        assertContains(output.toString(), "usage:")
    }

    @Test
    fun `cli outputs change detection`() {
        val output = ByteArrayOutputStream()
        val csvPath = tempDir.resolve("yosakoi_festival.csv")
        val repository = InMemoryPublishedEventRepository(
            outputPath = csvPath,
            headers = listOf("event_id", "event_name", "status", "image_url", "official_url", "start_date", "end_date", "location", "team_count", "nearest_station", "parking_info", "description", "youtube_url", "latitude", "longitude", "map_url", "updated_at"),
        )
        val command = SyncEventsCommand(
            stdout = PrintStream(output),
            useCase = SyncEventsUseCase(
                eventSource = FakeReader(rows = listOf(makeRow(eventId = "a", eventName = "Festival A", extra = mapOf("official_url" to "https://example.com/a")))),
                publishedEventRepository = repository,
            ),
        )

        val exitCode = command.run(
            args = arrayOf("--sheet-id", "sheet", "--worksheet", "events", "--dry-run"),
            trigger = "manual",
        )

        assertEquals(0, exitCode)
        assertContains(output.toString(), "changed=true")
    }

    @Test
    fun `cli fails when credentials path is missing`() {
        val output = ByteArrayOutputStream()

        val exitCode = SyncEventsCli.run(
            args = arrayOf("--sheet-id", "sheet", "--worksheet", "events"),
            env = emptyMap(),
            stdout = PrintStream(output),
        )

        assertEquals(1, exitCode)
        assertContains(output.toString(), "GOOGLE_APPLICATION_CREDENTIALS is required")
    }
}
