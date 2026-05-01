# 実装計画: Yosakoi Portal イベントデータ連携

**ブランチ**: `001-portal-event-sync` | **日付**: 2026-04-29 | **仕様書**: [spec.md](./spec.md)
**入力**: `/specs/001-portal-event-sync/spec.md` の機能仕様書

## 概要

Google スプレッドシートを管理元として読み取り、`Approved` のイベントのみを抽出し、
管理元と同一列構成の CSV を生成する CLI バッチを実装する。反映は、`end_date` が
過去の日付であるイベントを除外したうえで、未保存の `event_id` を新規追加し、
既存データは `updated_at` が新しい場合のみ更新する。重複 `event_id` や不正な
`updated_at` は上書き対象外として記録し、定期実行と手動実行で同一ロジックを使う。

## 技術コンテキスト

**言語 / バージョン**: Kotlin 2.1 / Java 21  
**主要依存関係**: Google Sheets API Client, `google-auth-library-oauth2-http`, Apache Commons CSV, JUnit 5  
**ストレージ**: ローカルファイル（Git 管理の生成 CSV、ログ）  
**テスト**: Gradle + JUnit 5 による unit / integration / contract テスト  
**対象プラットフォーム**: GitHub Actions から実行される CLI 環境  
**プロジェクト種別**: CLI バッチアプリケーション  
**制約**: データベース不使用、UTF-8 CSV、Google Sheets 管理元と同一列構成、差分がない場合は非更新  
**規模 / スコープ**: 単一スプレッドシート起点のイベント同期、運営担当者向け手動実行 + GitHub Actions 定期実行  
**実データ観察**: `specs/001-portal-event-sync/events - events.csv` の列は `event_id`,
`event_name`, `status`, `start_date`, `end_date`, `updated_at` を含む。`updated_at`
は必須項目として運用する前提に更新された。

## 憲章チェック

*ゲート: Phase 0 の調査に入る前に通過必須。Phase 1 の設計後に再確認する。*

- **Source Data Integrity**: 管理元は Google スプレッドシートを正本データとし、
  補正が必要な場合は正規化処理として明示し、元データは書き換えない。公開結果は
  Git 管理された生成 CSV で追跡できるようにする。
- **Scripted and Reproducible Operations**: `sync-events` CLI を単一の実行入口にし、
  手動実行と GitHub Actions 定期実行の両方で同一引数体系を利用する。入力、出力、
  Git 管理 CSV、認証情報の場所を quickstart に明記する。
- **Verification Before Merge**: Approved 抽出、`updated_at` 比較、`end_date`
  による過去開催除外、`event_id` 重複除外、CSV 列構成維持を自動テストで検証する。
- **Safe and Idempotent File Handling**: 生成 CSV は一時ファイル作成後に置換する。
  反映前に Git 管理された既存 CSV との差分なし判定を行い、上書き対象外レコードは
  ログに残して既存公開データを維持する。
- **Clean Code and Maintainability**: Google Sheets 取得、正規化、判定、CSV 書き出し、
  公開 CSV 読み込みを責務ごとに分離する。CLI は実行の統括のみを担当させる。
- **Observable Outputs and Decision Records**: 実行ごとに取得件数、Approved 件数、
  新規追加件数、更新件数、スキップ件数、重複件数、期限切れ除外件数を出力する。

**ゲート結果（設計前）**: 通過

## プロジェクト構成

### ドキュメント（この機能）

```text
specs/001-portal-event-sync/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── sync-events-cli.md
│   └── public-csv-schema.md
└── tasks.md
```

### ソースコード（リポジトリ直下）

```text
yosakoi_festival.csv

src/
    └── kotlin/
        └── jp/yosakoi/sync/
            ├── Main.kt
            ├── SyncEventsCli.kt
            ├── model/
            ├── service/
            └── util/

src/test/kotlin/jp/yosakoi/sync/
├── contract/
├── integration/
└── unit/
```

**構成判断**: 単一プロジェクト構成を採用する。公開用 CSV である
`yosakoi_festival.csv` はリポジトリ直下に置き、Git 管理の正本として扱う。
現在の実データ列に合わせて、識別子は `event_id`、開催判定は `end_date` を使う。

## データフローと成果物

- **入力**: Google スプレッドシートのイベント一覧、ローカル認証情報、リポジトリ直下の `yosakoi_festival.csv`
- **変換**: Sheets 読み取り → 行正規化 → `Approved` 抽出 → `end_date` による過去開催除外 →
  `event_id` 重複除外 → 新規 / 更新判定 → CSV 生成 → 差分あり時のみ反映
- **出力**: リポジトリ直下の `yosakoi_festival.csv`、実行ログ
- **運用手順**: GitHub Actions と手動実行の両方で `./gradlew run --args="--sheet-id ... --worksheet ..."` を使う。出力先はリポジトリ直下の `yosakoi_festival.csv` に固定する

## 設計後の憲章チェック

- **Source Data Integrity**: `data-model.md` で管理元イベントと公開用レコードの対応を定義済み。
- **Scripted and Reproducible Operations**: `quickstart.md` と `contracts/sync-events-cli.md`
  で実行コマンド、入力、出力を定義済み。
- **Verification Before Merge**: `contracts/` と `data-model.md` に対して必要なテスト境界を定義済み。
- **Safe and Idempotent File Handling**: Git 管理 CSV と出力 CSV の更新条件を `research.md`
  と `quickstart.md` に記録済み。
- **Clean Code and Maintainability**: モジュール境界と責務分離をプロジェクト構成に反映済み。
- **Observable Outputs and Decision Records**: `contracts/sync-events-cli.md` と `quickstart.md`
  に観測項目を明記済み。

**ゲート結果（設計後）**: 通過

## 複雑性の記録

| 違反項目 | 必要理由 | より単純な代替案を採用しなかった理由 |
|-----------|----------|------------------------------------|
| なし | 該当なし | 該当なし |
