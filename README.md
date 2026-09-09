# yosakoi-data-manager

Google スプレッドシートのイベント・受賞チームデータから、Yosakoi Portal 向けの
`yosakoi_festival.csv` と `award_winners.csv` を生成・同期する Kotlin CLI です。

## セットアップ

```bash
./gradlew test
```

初回は Gradle Wrapper が依存関係を取得します。`GOOGLE_APPLICATION_CREDENTIALS` にサービスアカウント JSON のパスを設定してください。

## 実行

```bash
./gradlew run --args="--sheet-id <google-sheet-id> --worksheet events"
```

受賞シートは常に `award_winners` を読みます。出力先はリポジトリ直下の2つの CSV で
固定です。`--dry-run` を付けると、どちらも更新せず判定結果だけ確認できます。

イベント CSV は `Approved` のイベントを開催終了後も保持します。受賞シートの
`Approved` 行は `event_id`、賞名、チーム名、公式結果 URL、単一動画 URL、動画投稿元種別、
`updated_at` が必須です。動画が未確認の行は `Progress` のまま管理してください。

## GitHub Actions

GitHub Actions では、生成した2つの CSV を別リポジトリ
`Yosakoi-Japan/yosakoiPortal` の
`frontend/app/src/assets/data/yosakoi_event.csv` と
`frontend/app/src/assets/data/award_winners.csv` にコピーして commit / push します。
その前に、このリポジトリ自身の2つの CSV も同じ commit で push します。

必要な Secrets は次です。

- `GOOGLE_SERVICE_ACCOUNT_JSON`
- `GOOGLE_SHEET_ID`
- `GOOGLE_WORKSHEET`
- `YOSAKOI_PORTAL_PAT`

## テスト

```bash
./gradlew test
```
