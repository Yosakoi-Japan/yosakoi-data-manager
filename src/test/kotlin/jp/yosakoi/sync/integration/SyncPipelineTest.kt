package jp.yosakoi.sync.integration

import jp.yosakoi.sync.FakePortalDataSource
import jp.yosakoi.sync.makeAwardRow
import jp.yosakoi.sync.makeRow
import jp.yosakoi.sync.application.model.SyncEventsRequest
import jp.yosakoi.sync.application.usecase.SyncEventsUseCase
import jp.yosakoi.sync.infrastructure.csv.FilePublishedEventRepository
import jp.yosakoi.sync.application.port.PublicationSaveResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class SyncPipelineTest {
    @TempDir
    lateinit var tempDir: Path

    private val output: Path
        get() = tempDir.resolve("yosakoi_festival.csv")

    private val headerLine =
        "event_id,event_name,status,image_url,official_url,start_date,end_date,location,team_count,nearest_station,parking_info,description,youtube_url,latitude,longitude,map_url,updated_at\n"

    @Test
    fun `approved only export flow`() {
        Files.writeString(output, headerLine)
        val reader = FakePortalDataSource(
            rows = listOf(
                makeRow(eventId = "a", eventName = "Festival A", status = "Approved", extra = mapOf("official_url" to "https://example.com/a")),
                makeRow(eventId = "b", eventName = "Festival B", status = "Progress"),
                makeRow(eventId = "c", eventName = "Festival C", status = "Approved", endDate = "2026-01-01", extra = mapOf("official_url" to "https://example.com/c")),
            ),
        )

        val result = SyncEventsUseCase(reader, FilePublishedEventRepository(output))
            .execute(SyncEventsRequest("sheet", "events", dryRun = false, trigger = "manual"))

        assertEquals(3, result.fetchedCount)
        assertEquals(2, result.approvedCount)
        val lines = Files.readAllLines(output)
        assertEquals(3, lines.size)
        assertTrue(lines[1].contains("Festival A"))
        assertTrue(lines[2].contains("Festival C"))
    }

    @Test
    fun `new update no change flow`() {
        Files.writeString(
            output,
            headerLine +
                "a,Festival A,Approved,,https://example.com/a,2026-06-10,2026-06-14,,,,,,,,,,2026-05-01T10:00:00+09:00\n",
        )
        val reader = FakePortalDataSource(
            rows = listOf(
                makeRow(eventId = "a", eventName = "Festival A", updatedAt = "2026-05-02T10:00:00+09:00", extra = mapOf("official_url" to "https://example.com/a")),
                makeRow(eventId = "b", eventName = "Festival B", updatedAt = "2026-05-03T10:00:00+09:00", extra = mapOf("official_url" to "https://example.com/b")),
            ),
        )

        val result = SyncEventsUseCase(reader, FilePublishedEventRepository(output))
            .execute(SyncEventsRequest("sheet", "events", dryRun = false, trigger = "manual"))

        assertEquals(1, result.newCount)
        assertEquals(1, result.updatedCount)
        assertTrue(result.changed)
    }

    @Test
    fun `duplicate event id is not exported`() {
        Files.writeString(output, headerLine)
        val reader = FakePortalDataSource(
            rows = listOf(
                makeRow(eventId = "a", eventName = "Festival A", extra = mapOf("official_url" to "https://example.com/a")),
                makeRow(eventId = "a", eventName = "Festival A duplicate", updatedAt = "2026-05-02T10:00:00+09:00", extra = mapOf("official_url" to "https://example.com/a2")),
            ),
        )

        val result = SyncEventsUseCase(reader, FilePublishedEventRepository(output))
            .execute(SyncEventsRequest("sheet", "events", dryRun = false, trigger = "manual"))

        assertEquals(1, result.duplicateErrorCount)
        val lines = Files.readAllLines(output)
        assertEquals(1, lines.size)
    }

    @Test
    fun `source read failure keeps existing csv`() {
        val original = headerLine
        Files.writeString(output, original)
        val useCase = SyncEventsUseCase(FakePortalDataSource(error = RuntimeException("read failed")), FilePublishedEventRepository(output))

        assertThrows<RuntimeException> {
            useCase.execute(SyncEventsRequest("sheet", "events", dryRun = false, trigger = "manual"))
        }

        assertEquals(original, Files.readString(output))
    }

    @Test
    fun `award source failure keeps both existing csv files`() {
        val awardOutput = tempDir.resolve("award_winners.csv")
        val originalEvents = headerLine
        val originalAwards = "event_id,award_name,team_name,result_source_url,video_url,video_source_type,status,updated_at\n"
        Files.writeString(output, originalEvents)
        Files.writeString(awardOutput, originalAwards)
        val useCase = SyncEventsUseCase(
            source = FakePortalDataSource(
                rows = emptyList(),
                awardError = RuntimeException("award read failed"),
            ),
            publishedEventRepository = FilePublishedEventRepository(output, awardOutput),
        )

        assertThrows<RuntimeException> {
            useCase.execute(
                SyncEventsRequest("sheet", "events", dryRun = false, trigger = "manual"),
            )
        }

        assertEquals(originalEvents, Files.readString(output))
        assertEquals(originalAwards, Files.readString(awardOutput))
    }

    @Test
    fun `write failure is propagated`() {
        Files.writeString(output, headerLine)
        val failingRepository = object : FilePublishedEventRepository(output) {
            override fun save(
                eventHeaders: List<String>,
                eventRows: List<Map<String, String>>,
                awardHeaders: List<String>,
                awardRows: List<Map<String, String>>,
                dryRun: Boolean,
            ): PublicationSaveResult {
                throw java.io.IOException("write failed")
            }
        }
        val useCase = SyncEventsUseCase(
            FakePortalDataSource(rows = listOf(makeRow(eventId = "a", eventName = "Festival A", extra = mapOf("official_url" to "https://example.com/a")))),
            failingRepository,
        )

        assertThrows<java.io.IOException> {
            useCase.execute(SyncEventsRequest("sheet", "events", dryRun = false, trigger = "manual"))
        }
    }

    @Test
    fun `approved award winners are exported with mandatory videos`() {
        Files.writeString(output, headerLine)
        val awardOutput = tempDir.resolve("award_winners.csv")
        val useCase = SyncEventsUseCase(
            source = FakePortalDataSource(
                rows = listOf(
                    makeRow(
                        eventId = "a",
                        eventName = "Festival A",
                        endDate = "2025-01-01",
                        extra = mapOf("official_url" to "https://example.com/a"),
                    ),
                ),
                awardRows = listOf(
                    makeAwardRow(eventId = "a", teamName = "Team A"),
                    makeAwardRow(eventId = "a", teamName = "Team B"),
                    makeAwardRow(eventId = "a", teamName = "Draft", status = "Progress", videoUrl = "", videoSourceType = "", updatedAt = ""),
                ),
            ),
            publishedEventRepository = FilePublishedEventRepository(output, awardOutput),
        )

        val result = useCase.execute(
            SyncEventsRequest("sheet", "events", dryRun = false, trigger = "manual"),
        )

        assertEquals(3, result.awardFetchedCount)
        assertEquals(2, result.awardPublishedCount)
        val lines = Files.readAllLines(awardOutput)
        assertEquals("event_id,award_name,team_name,result_source_url,video_url,video_source_type,status,updated_at", lines.first())
        assertEquals(3, lines.size)
        assertTrue(lines.none { it.contains("private") || it.contains("Draft") })
    }

    @Test
    fun `invalid approved award preserves both public files`() {
        val originalEvents = headerLine
        val awardOutput = tempDir.resolve("award_winners.csv")
        val originalAwards = "event_id,award_name,team_name,result_source_url,video_url,video_source_type,status,updated_at\n"
        Files.writeString(output, originalEvents)
        Files.writeString(awardOutput, originalAwards)
        val useCase = SyncEventsUseCase(
            source = FakePortalDataSource(
                rows = listOf(
                    makeRow(eventId = "a", eventName = "Festival A", extra = mapOf("official_url" to "https://example.com/a")),
                ),
                awardRows = listOf(makeAwardRow(eventId = "a", teamName = "Team A", videoUrl = "")),
            ),
            publishedEventRepository = FilePublishedEventRepository(output, awardOutput),
        )

        assertThrows<IllegalArgumentException> {
            useCase.execute(
                SyncEventsRequest("sheet", "events", dryRun = false, trigger = "manual"),
            )
        }

        assertEquals(originalEvents, Files.readString(output))
        assertEquals(originalAwards, Files.readString(awardOutput))
    }

    @Test
    fun `deapproved award is removed on next sync`() {
        Files.writeString(output, headerLine)
        val awardOutput = tempDir.resolve("award_winners.csv")
        Files.writeString(
            awardOutput,
            "event_id,award_name,team_name,result_source_url,video_url,video_source_type,status,updated_at\n" +
                "a,大賞,Team A,https://example.com/results/a,https://youtu.be/abcdefghijk,General,Approved,2026-08-01T10:00:00+09:00\n",
        )
        val useCase = SyncEventsUseCase(
            source = FakePortalDataSource(
                rows = listOf(
                    makeRow(eventId = "a", eventName = "Festival A", extra = mapOf("official_url" to "https://example.com/a")),
                ),
                awardRows = listOf(makeAwardRow(eventId = "a", teamName = "Team A", status = "Progress")),
            ),
            publishedEventRepository = FilePublishedEventRepository(output, awardOutput),
        )

        useCase.execute(
            SyncEventsRequest("sheet", "events", dryRun = false, trigger = "manual"),
        )

        assertEquals(1, Files.readAllLines(awardOutput).size)
    }

    @Test
    fun `dry run manual invocation path`() {
        Files.writeString(output, headerLine)
        val useCase = SyncEventsUseCase(
            FakePortalDataSource(rows = listOf(makeRow(eventId = "a", eventName = "Festival A", extra = mapOf("official_url" to "https://example.com/a")))),
            FilePublishedEventRepository(output),
        )

        val result = useCase.execute(SyncEventsRequest("sheet", "events", dryRun = true, trigger = "manual"))

        assertTrue(result.changed)
        assertTrue(Files.exists(output))
    }

    @Test
    fun `fails when existing csv header is missing`() {
        val useCase = SyncEventsUseCase(
            FakePortalDataSource(rows = listOf(makeRow(eventId = "a", eventName = "Festival A", extra = mapOf("official_url" to "https://example.com/a")))),
            FilePublishedEventRepository(output),
        )

        val error = assertThrows<IllegalArgumentException> {
            useCase.execute(SyncEventsRequest("sheet", "events", dryRun = false, trigger = "manual"))
        }

        assertEquals("yosakoi_festival.csv header is required", error.message)
    }

    @Test
    fun `event without official url is excluded from export`() {
        Files.writeString(output, headerLine)
        val reader = FakePortalDataSource(
            rows = listOf(
                makeRow(eventId = "a", eventName = "Festival A", extra = mapOf("official_url" to "")),
                makeRow(eventId = "b", eventName = "Festival B", extra = mapOf("official_url" to "https://example.com/b")),
            ),
        )

        val result = SyncEventsUseCase(reader, FilePublishedEventRepository(output))
            .execute(SyncEventsRequest("sheet", "events", dryRun = false, trigger = "manual"))

        assertEquals(1, result.approvedCount)
        val lines = Files.readAllLines(output)
        assertEquals(2, lines.size)
        assertTrue(lines[1].contains("Festival B"))
    }

    @Test
    fun `event with invalid official url is excluded from export`() {
        Files.writeString(output, headerLine)
        val reader = FakePortalDataSource(
            rows = listOf(
                makeRow(eventId = "a", eventName = "Festival A", extra = mapOf("official_url" to "not-a-url")),
                makeRow(eventId = "b", eventName = "Festival B", extra = mapOf("official_url" to "https://example.com/b")),
            ),
        )

        val result = SyncEventsUseCase(reader, FilePublishedEventRepository(output))
            .execute(SyncEventsRequest("sheet", "events", dryRun = false, trigger = "manual"))

        assertEquals(1, result.approvedCount)
        val lines = Files.readAllLines(output)
        assertEquals(2, lines.size)
        assertTrue(lines[1].contains("Festival B"))
    }

    @Test
    fun `private columns are removed from managed csv`() {
        Files.writeString(
            output,
            "event_id,event_name,status,image_url,official_url,start_date,end_date,location,team_count,nearest_station,parking_info,description,youtube_url,latitude,longitude,map_url,updated_at,note,review\n",
        )
        val reader = FakePortalDataSource(
            rows = listOf(
                makeRow(
                    eventId = "a",
                    eventName = "Festival A",
                    extra = mapOf(
                        "official_url" to "https://example.com/a",
                        "note" to "internal memo",
                        "review" to "needs review",
                    ),
                ),
            ),
        )

        SyncEventsUseCase(reader, FilePublishedEventRepository(output))
            .execute(SyncEventsRequest("sheet", "events", dryRun = false, trigger = "manual"))

        val lines = Files.readAllLines(output)
        assertEquals("event_id,event_name,status,image_url,official_url,start_date,end_date,location,team_count,nearest_station,parking_info,description,youtube_url,latitude,longitude,map_url,updated_at", lines.first())
        assertTrue(lines[1].contains("Festival A"))
        assertTrue(!lines.first().contains("note"))
        assertTrue(!lines.first().contains("review"))
        assertTrue(!lines[1].contains("internal memo"))
        assertTrue(!lines[1].contains("needs review"))
    }
}
