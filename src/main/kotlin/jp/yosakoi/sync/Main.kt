package jp.yosakoi.sync

/**
 * CLI のプロセス入口として引数を受け取り、終了コードを OS に返す。
 */
fun main(args: Array<String>) {
    val exitCode = SyncEventsCli.run(args)
    if (exitCode != 0) {
        kotlin.system.exitProcess(exitCode)
    }
}
