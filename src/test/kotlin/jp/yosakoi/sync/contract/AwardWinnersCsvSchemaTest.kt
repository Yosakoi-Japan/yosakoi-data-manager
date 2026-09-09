package jp.yosakoi.sync.contract

import jp.yosakoi.sync.domain.model.PublishedAwardWinner
import kotlin.test.Test
import kotlin.test.assertEquals
import java.nio.file.Files
import java.nio.file.Path

class AwardWinnersCsvSchemaTest {
    @Test
    fun `award winner csv has fixed public header`() {
        assertEquals(
            listOf(
                "event_id",
                "award_name",
                "team_name",
                "result_source_url",
                "video_url",
                "video_source_type",
                "status",
                "updated_at",
            ),
            PublishedAwardWinner.HEADERS,
        )
    }

    @Test
    fun `tracked award csv starts with contracted header`() {
        assertEquals(
            PublishedAwardWinner.HEADERS.joinToString(","),
            Files.readAllLines(Path.of("award_winners.csv")).first(),
        )
    }
}
