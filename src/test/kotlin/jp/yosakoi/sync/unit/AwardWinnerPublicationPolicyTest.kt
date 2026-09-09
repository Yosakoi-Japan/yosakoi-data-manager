package jp.yosakoi.sync.unit

import jp.yosakoi.sync.domain.model.SourceAwardWinner
import jp.yosakoi.sync.domain.service.AwardWinnerPublicationPolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.assertThrows

class AwardWinnerPublicationPolicyTest {
    private fun winner(
        eventId: String = "event-a",
        teamName: String = "Team A",
        status: String = "Approved",
        awardName: String = "大賞",
        resultSourceUrl: String = "https://example.com/results",
        videoUrl: String = "https://www.youtube.com/watch?v=abcdefghijk",
        videoSourceType: String = "OrganizerOfficial",
        updatedAt: String = "2026-08-01T10:00:00+09:00",
    ): SourceAwardWinner = SourceAwardWinner.fromColumns(
        linkedMapOf(
            "event_id" to eventId,
            "award_name" to awardName,
            "team_name" to teamName,
            "result_source_url" to resultSourceUrl,
            "video_url" to videoUrl,
            "video_source_type" to videoSourceType,
            "status" to status,
            "updated_at" to updatedAt,
            "note" to "private",
        ),
    )

    @Test
    fun `必須動画が設定された承認済み受賞者は公開される`() {
        val result = AwardWinnerPublicationPolicy().publish(listOf(winner()), setOf("event-a"))

        assertEquals(1, result.winners.size)
        assertEquals("https://www.youtube.com/watch?v=abcdefghijk", result.winners.single().videoUrl.toString())
    }

    @Test
    fun `作業中の行は動画を省略でき公開されない`() {
        val result = AwardWinnerPublicationPolicy().publish(
            listOf(winner(status = "Progress", videoUrl = "", videoSourceType = "", updatedAt = "")),
            setOf("event-a"),
        )

        assertEquals(emptyList(), result.winners)
    }

    @Test
    fun `承認済みの行には単一動画のURLが必要である`() {
        assertThrows<IllegalArgumentException> {
            AwardWinnerPublicationPolicy().publish(
                listOf(winner(videoUrl = "https://www.youtube.com/playlist?list=PL123")),
                setOf("event-a"),
            )
        }
    }

    @Test
    fun `承認済みの行には有効な動画種別とISO形式の更新日時が必要である`() {
        assertThrows<IllegalArgumentException> {
            AwardWinnerPublicationPolicy().publish(
                listOf(winner(videoSourceType = "Unknown")),
                setOf("event-a"),
            )
        }
        assertThrows<IllegalArgumentException> {
            AwardWinnerPublicationPolicy().publish(
                listOf(winner(updatedAt = "2026-08-01")),
                setOf("event-a"),
            )
        }
    }

    @Test
    fun `承認済みの行は公開対象イベントを参照する必要がある`() {
        assertThrows<IllegalArgumentException> {
            AwardWinnerPublicationPolicy().publish(listOf(winner()), setOf("other-event"))
        }
    }

    @Test
    fun `同じイベント内のチーム名は正規化後に一意である必要がある`() {
        assertThrows<IllegalArgumentException> {
            AwardWinnerPublicationPolicy().publish(
                listOf(winner(teamName = "Team A"), winner(teamName = " team  a ")),
                setOf("event-a"),
            )
        }
    }

    @Test
    fun `同じイベントで異なる賞名と結果URLを公開できる`() {
        val result = AwardWinnerPublicationPolicy().publish(
            listOf(
                winner(
                    teamName = "Team A",
                    awardName = "大賞",
                    resultSourceUrl = "https://example.com/result-a",
                ),
                winner(
                    teamName = "Team B",
                    awardName = "準大賞",
                    resultSourceUrl = "https://example.com/result-b",
                ),
            ),
            setOf("event-a"),
        )

        assertEquals(listOf("大賞", "準大賞"), result.winners.map { it.awardName })
        assertEquals(
            listOf("https://example.com/result-a", "https://example.com/result-b"),
            result.winners.map { it.resultSourceUrl.toString() },
        )
    }
}
