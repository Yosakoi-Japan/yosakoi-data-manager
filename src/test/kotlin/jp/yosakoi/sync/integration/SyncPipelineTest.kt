package jp.yosakoi.sync.integration

import jp.yosakoi.sync.FakeReader
import jp.yosakoi.sync.makeRow
import jp.yosakoi.sync.application.model.SyncEventsRequest
import jp.yosakoi.sync.application.usecase.SyncEventsUseCase
import jp.yosakoi.sync.infrastructure.csv.FilePublishedEventRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate

class SyncPipelineTest {
    @TempDir
    lateinit var tempDir: Path

    private val output: Path
        get() = tempDir.resolve("yosakoi_festival.csv")

    private val headerLine =
        "event_id,event_name,status,image_url,official_url,start_date,end_date,location,team_count,nearest_station,parking_info,description,youtube_url,latitude,longitude,map_url,updated_at,note\n"

    @Test
    fun `approved only export flow`() {
        Files.writeString(output, headerLine)
        val reader = FakeReader(
            rows = listOf(
                makeRow(eventId = "a", eventName = "Festival A", status = "Approved"),
                makeRow(eventId = "b", eventName = "Festival B", status = "Progress"),
                makeRow(eventId = "c", eventName = "Festival C", status = "Approved", endDate = "2026-01-01"),
            ),
        )

        val result = SyncEventsUseCase(reader, FilePublishedEventRepository(output))
            .execute(SyncEventsRequest("sheet", "events", dryRun = false, trigger = "manual", today = LocalDate.of(2026, 6, 1)))

        assertEquals(3, result.fetchedCount)
        assertEquals(1, result.approvedCount)
        assertEquals(1, result.expiredCount)
        val lines = Files.readAllLines(output)
        assertEquals(2, lines.size)
        assertTrue(lines[1].contains("Festival A"))
    }

    @Test
    fun `new update no change flow`() {
        Files.writeString(
            output,
            headerLine +
                "a,Festival A,Approved,,,2026-06-10,2026-06-14,,,,,,,,,,2026-05-01T10:00:00+09:00,\n",
        )
        val reader = FakeReader(
            rows = listOf(
                makeRow(eventId = "a", eventName = "Festival A", updatedAt = "2026-05-02T10:00:00+09:00"),
                makeRow(eventId = "b", eventName = "Festival B", updatedAt = "2026-05-03T10:00:00+09:00"),
            ),
        )

        val result = SyncEventsUseCase(reader, FilePublishedEventRepository(output))
            .execute(SyncEventsRequest("sheet", "events", dryRun = false, trigger = "manual", today = LocalDate.of(2026, 6, 1)))

        assertEquals(1, result.newCount)
        assertEquals(1, result.updatedCount)
        assertTrue(result.changed)
    }

    @Test
    fun `duplicate event id is not exported`() {
        Files.writeString(output, headerLine)
        val reader = FakeReader(
            rows = listOf(
                makeRow(eventId = "a", eventName = "Festival A"),
                makeRow(eventId = "a", eventName = "Festival A duplicate", updatedAt = "2026-05-02T10:00:00+09:00"),
            ),
        )

        val result = SyncEventsUseCase(reader, FilePublishedEventRepository(output))
            .execute(SyncEventsRequest("sheet", "events", dryRun = false, trigger = "manual", today = LocalDate.of(2026, 6, 1)))

        assertEquals(1, result.duplicateErrorCount)
        val lines = Files.readAllLines(output)
        assertEquals(1, lines.size)
    }

    @Test
    fun `source read failure keeps existing csv`() {
        val original = headerLine
        Files.writeString(output, original)
        val useCase = SyncEventsUseCase(FakeReader(error = RuntimeException("read failed")), FilePublishedEventRepository(output))

        assertThrows<RuntimeException> {
            useCase.execute(SyncEventsRequest("sheet", "events", dryRun = false, trigger = "manual", today = LocalDate.of(2026, 6, 1)))
        }

        assertEquals(original, Files.readString(output))
    }

    @Test
    fun `write failure keeps existing csv`() {
        val original = headerLine
        Files.writeString(output, original)
        val failingRepository = object : FilePublishedEventRepository(output) {
            override fun save(headers: List<String>, rows: List<Map<String, String>>, dryRun: Boolean): Boolean {
                throw java.io.IOException("write failed")
            }
        }
        val useCase = SyncEventsUseCase(
            FakeReader(rows = listOf(makeRow(eventId = "a", eventName = "Festival A"))),
            failingRepository,
        )

        assertThrows<java.io.IOException> {
            useCase.execute(SyncEventsRequest("sheet", "events", dryRun = false, trigger = "manual", today = LocalDate.of(2026, 6, 1)))
        }

        assertEquals(original, Files.readString(output))
    }

    @Test
    fun `dry run manual invocation path`() {
        Files.writeString(output, headerLine)
        val useCase = SyncEventsUseCase(
            FakeReader(rows = listOf(makeRow(eventId = "a", eventName = "Festival A"))),
            FilePublishedEventRepository(output),
        )

        val result = useCase.execute(SyncEventsRequest("sheet", "events", dryRun = true, trigger = "manual", today = LocalDate.of(2026, 6, 1)))

        assertTrue(result.changed)
        assertTrue(Files.exists(output))
    }

    @Test
    fun `fails when existing csv header is missing`() {
        val useCase = SyncEventsUseCase(
            FakeReader(rows = listOf(makeRow(eventId = "a", eventName = "Festival A"))),
            FilePublishedEventRepository(output),
        )

        val error = assertThrows<IllegalArgumentException> {
            useCase.execute(SyncEventsRequest("sheet", "events", dryRun = false, trigger = "manual", today = LocalDate.of(2026, 6, 1)))
        }

        assertEquals("yosakoi_festival.csv header is required", error.message)
    }
}
