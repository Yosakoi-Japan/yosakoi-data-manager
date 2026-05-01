# yosakoi-data-manager

Google スプレッドシートのイベントデータから、Yosakoi Portal 向けの `yosakoi_festival.csv` を生成・同期する Kotlin CLI です。

## セットアップ

```bash
./gradlew test
```

初回は Gradle Wrapper が依存関係を取得します。`GOOGLE_APPLICATION_CREDENTIALS` にサービスアカウント JSON のパスを設定してください。

## 実行

```bash
./gradlew run --args="--sheet-id <google-sheet-id> --worksheet <worksheet-name>"
```

出力先はリポジトリ直下の `./yosakoi_festival.csv` で固定です。`--dry-run` を付けると、CSV を更新せず判定結果だけ確認できます。

## テスト

```bash
./gradlew test
```
