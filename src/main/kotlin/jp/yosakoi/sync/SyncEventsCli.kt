package jp.yosakoi.sync

import jp.yosakoi.sync.application.model.SyncEventsRequest
import jp.yosakoi.sync.application.usecase.SyncEventsUseCase
import jp.yosakoi.sync.infrastructure.csv.FilePublishedEventRepository
import jp.yosakoi.sync.infrastructure.google.GoogleSheetsEventSource
import java.io.PrintStream
import java.time.LocalDate

object SyncEventsCli {
    const val DEFAULT_OUTPUT_PATH: String = "./yosakoi_festival.csv"

    /**
     * 本番用の依存関係を組み立てて同期コマンドを実行する。
     */
    fun run(
        args: Array<String>,
        env: Map<String, String> = System.getenv(),
        stdout: PrintStream = System.out,
        today: LocalDate? = null,
    ): Int {
        val parsed = parseArgs(args.toList()) ?: run {
            printUsage(stdout)
            return 1
        }

        return try {
            val credentialsPath = env["GOOGLE_APPLICATION_CREDENTIALS"]
                ?: throw IllegalStateException("GOOGLE_APPLICATION_CREDENTIALS is required")
            val trigger = env["GITHUB_EVENT_NAME"] ?: "manual"
            val useCase = SyncEventsUseCase(
                eventSource = GoogleSheetsEventSource(credentialsPath),
                publishedEventRepository = FilePublishedEventRepository(DEFAULT_OUTPUT_PATH),
            )
            val command = SyncEventsCommand(stdout = stdout, useCase = useCase)
            command.run(parsed = parsed, trigger = trigger, today = today)
        } catch (error: Exception) {
            stdout.println("error=${error.message ?: error::class.simpleName}")
            1
        }
    }

    /**
     * CLI の使い方を標準出力へ表示する。
     */
    fun printUsage(stdout: PrintStream) {
        stdout.println(
            "usage: sync-events --sheet-id <sheet-id> --worksheet <worksheet-name> [--dry-run]",
        )
    }

    /**
     * CLI 引数を解析し、必須引数がそろっている場合だけ構造化して返す。
     */
    fun parseArgs(args: List<String>): ParsedArgs? {
        var sheetId: String? = null
        var worksheet: String? = null
        var dryRun = false
        var index = 0
        while (index < args.size) {
            when (args[index]) {
                "--sheet-id" -> sheetId = args.getOrNull(++index)
                "--worksheet" -> worksheet = args.getOrNull(++index)
                "--dry-run" -> dryRun = true
                else -> return null
            }
            index += 1
        }
        if (sheetId.isNullOrBlank() || worksheet.isNullOrBlank()) {
            return null
        }
        return ParsedArgs(sheetId, worksheet, dryRun)
    }
}

class SyncEventsCommand(
    private val stdout: PrintStream,
    private val useCase: SyncEventsUseCase,
) {
    /**
     * 解析済みの引数をユースケースへ渡し、CLI 用の終了コードを返す。
     */
    fun run(parsed: ParsedArgs, trigger: String, today: LocalDate? = null): Int {
        return try {
            val result = useCase.execute(
                SyncEventsRequest(
                    sheetId = parsed.sheetId,
                    worksheet = parsed.worksheet,
                    dryRun = parsed.dryRun,
                    trigger = trigger,
                    today = today,
                ),
            )
            result.toStdoutLines().forEach(stdout::println)
            if (result.duplicateEventIds.isNotEmpty()) {
                stdout.println("duplicate_event_ids=${result.duplicateEventIds.joinToString(",")}")
            }
            if (result.warnings.isNotEmpty()) {
                stdout.println("warnings=${result.warnings.joinToString(";")}")
            }
            0
        } catch (error: Exception) {
            stdout.println("error=${error.message ?: error::class.simpleName}")
            1
        }
    }

    /**
     * 引数配列を解析してからユースケースを実行する。
     */
    fun run(args: Array<String>, trigger: String, today: LocalDate? = null): Int {
        val parsed = SyncEventsCli.parseArgs(args.toList()) ?: run {
            SyncEventsCli.printUsage(stdout)
            return 1
        }
        return run(parsed, trigger, today)
    }
}

data class ParsedArgs(
    val sheetId: String,
    val worksheet: String,
    val dryRun: Boolean,
)
