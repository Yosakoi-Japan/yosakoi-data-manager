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

## GitHub Actions

GitHub Actions では、生成した `yosakoi_festival.csv` を別リポジトリ
`Yosakoi-Japan/yosakoiPortal` の
`frontend/app/src/assets/data/yosakoi_event.csv` にコピーして commit / push します。

必要な Secrets は次です。

- `GOOGLE_SERVICE_ACCOUNT_JSON`
- `GOOGLE_SHEET_ID`
- `GOOGLE_WORKSHEET`
- `YOSAKOI_PORTAL_PAT`

## テスト

```bash
./gradlew test
```
