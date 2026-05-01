# Quickstart: Yosakoi Portal イベントデータ連携

## 前提

- Java 21 以上が利用できること
- Google Sheets 読み取り用のサービスアカウント認証情報をローカルに配置すること
- 管理元シートに `event_id`、`event_name`、`status`、`start_date`、`end_date`、`updated_at` の列が存在すること
- `updated_at` は必須入力であること
- `specs/001-portal-event-sync/events - events.csv` の現状では `status=Progress` が中心なので、
  公開同期には `Approved` 運用が必要であること

## セットアップ

1. Gradle Wrapper で依存関係を取得する
2. 認証情報ファイルを環境変数で参照できるようにする

```bash
./gradlew test
export GOOGLE_APPLICATION_CREDENTIALS=/absolute/path/to/service-account.json
```

## 手動実行

```bash
./gradlew run --args="--sheet-id <google-sheet-id> --worksheet <worksheet-name>"
```

## 実行ルール

- `Approved` のイベントだけを公開候補にする
- 出力先はリポジトリ直下の `yosakoi_festival.csv` に固定する
- `end_date` が今日より前のイベントは対象外にする
- `event_id` が未保存なら新規追加する
- 既存の `event_id` は `updated_at` が新しい場合のみ上書きする
- `updated_at` が不正なら上書きしない
- 同じ `event_id` の `Approved` イベントが複数ある場合は重複エラーとして除外する
- 差分がない場合は既存公開 CSV を更新しない
- 公開用 CSV は Git 上で管理し、次回同期時の比較元として使う

## `dry-run` 相当の確認

手動確認専用モードを実装する場合は、出力前に判定結果だけを表示する。
現時点の基本フローでは通常実行時ログで以下を必ず確認できるようにする。

- 取得件数
- `Approved` 件数
- 新規追加件数
- 更新件数
- 非更新件数
- 期限切れ除外件数
- 重複エラー件数
- `updated_at` 不正件数

## 生成物

- `yosakoi_festival.csv`: リポジトリ直下で Git 管理する公開用 CSV
- `artifacts/logs/`: 実行ログ保存先

## GitHub Actions 運用

- 定期実行は GitHub Actions の `schedule` を使う
- 手動実行は GitHub Actions の `workflow_dispatch` を使う
- Actions 実行時はリポジトリを checkout し、`yosakoi_festival.csv` を比較元として使う
- 更新後の `yosakoi_festival.csv` を commit / push する
- Google Sheets 認証情報とシート ID は GitHub Secrets で管理する

想定するワークフローの役割は次のとおりです。

- `schedule`: 定期実行
- `workflow_dispatch`: 手動実行
- `actions/setup-java`: Java 実行環境の準備
- `gradle/actions/setup-gradle`: Gradle 実行準備
- 認証情報ファイル生成: Secrets から一時ファイルを作成
- 同期コマンド実行: `./gradlew run --args="..."` を実行
- CSV 変更時のみ commit / push: Git 管理の公開 CSV を更新

## 検証

```bash
./gradlew test
```

最低限、以下のケースを fixture で確認する。

- `Approved` 以外が出力されない
- 同じ `event_id` の既存イベントは `updated_at` が新しい時だけ更新される
- `end_date` 終了済みイベントが対象外になる
- 重複 `event_id` が除外される
