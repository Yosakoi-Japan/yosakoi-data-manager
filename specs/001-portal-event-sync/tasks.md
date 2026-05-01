# タスク一覧: Yosakoi Portal イベントデータ連携

**入力**: `/specs/001-portal-event-sync/` の設計ドキュメント  
**前提資料**: `plan.md`（必須）, `spec.md`（必須）, `research.md`, `data-model.md`, `contracts/`

**テスト方針**: 検証タスクは必須。各ユーザーストーリーに必要な契約テスト、統合テスト、単体テストを含める。

**構成方針**: タスクはユーザーストーリー単位でグループ化し、独立実装と独立検証ができるようにする。

## 表記形式: `[ID] [P?] [Story] 説明`

- **[P]**: 並列実行可能（別ファイルで依存なし）
- **[Story]**: 対応するユーザーストーリー（`[US1]`, `[US2]`, `[US3]`）
- 説明には必ず正確なファイルパスを含める

## パス規約

- **単一プロジェクト構成**: リポジトリ直下に `src/`, `tests/` を置く
- この機能では `src/yosakoi_data_manager/` と `tests/` を使う

## フェーズ1: セットアップ（共通セットアップ）

**目的**: プロジェクト初期化と基本構成の準備

- [X] T001 [plan.md](/Users/collie/develop/yosakoi-data-manager/specs/001-portal-event-sync/plan.md:1) に沿って `src/yosakoi_data_manager/` と `tests/` の Python プロジェクト構成を作成する
- [X] T002 `pyproject.toml` にプロジェクトメタデータと実行時依存関係を定義する
- [X] T003 [P] `pytest.ini` に `pytest` の探索設定とテスト設定を追加する
- [X] T004 [P] `.env.example` にローカル実行用の環境変数例を追加する
- [X] T005 [P] `.github/workflows/` ディレクトリと `.github/workflows/sync-events.yml` のひな形を作成する

---

## フェーズ2: 基盤実装（全体の前提実装）

**目的**: すべてのユーザーストーリーに先立って必要な基盤実装

**注意**: このフェーズ完了前にユーザーストーリー実装へ進まない

- [X] T006 `src/yosakoi_data_manager/models/event_record.py` と `src/yosakoi_data_manager/models/sync_result.py` に `SourceEvent`、`ApprovedEvent`、`PublishedCsvRecord`、`SyncResult` モデルを定義する
- [X] T007 [P] `src/yosakoi_data_manager/lib/checksum.py` と `src/yosakoi_data_manager/lib/date_utils.py` に日付処理とチェックサムの補助処理を実装する
- [X] T008 [P] `src/yosakoi_data_manager/services/sheet_reader.py` に Google Sheets 読み取り契約と認証情報読み込みを実装する
- [X] T009 [P] `src/yosakoi_data_manager/services/published_csv_store.py` に `yosakoi_festival.csv` 用の公開 CSV 読み書き抽象化を実装する
- [X] T010 `src/yosakoi_data_manager/services/sync_orchestrator.py` に実行オーケストレーターの骨格と結果集計を実装する
- [X] T011 `src/yosakoi_data_manager/cli/sync_events.py` に CLI の入口と共通オプションを定義する
- [X] T012 [P] `tests/fixtures/sheet_rows/` に `Approved`、`Progress`、重複、異常値を含む fixture を追加する
- [X] T013 `src/yosakoi_data_manager/services/` 配下のコードコメントまたは docstring でモジュール境界と命名規約を定義する

**完了条件**: 基盤実装完了。以降のユーザーストーリーへ進行可能

---

## フェーズ3: ユーザーストーリー1 - 承認済みイベントの公開データ生成（優先度: P1）🎯 MVP

**目的**: 管理元から `Approved` のイベントだけを抽出し、管理元と同一列構成の公開 CSV を生成する

**独立テスト**: 承認済み・未承認・期限切れイベントを含む fixture を使い、出力 CSV に `Approved` かつ有効期間内のイベントだけが列構成そのままで含まれることを確認する

### ユーザーストーリー1の検証

> **注記**: これらのテストは実装前に作成し、先に失敗することを確認する

- [X] T014 [P] [US1] `tests/contract/test_public_csv_schema.py` に CSV スキーマ維持の契約テストを追加する
- [X] T015 [P] [US1] `tests/integration/test_sync_pipeline.py` に `Approved` のみを出力する統合テストを追加する
- [X] T016 [P] [US1] `tests/unit/test_event_filter.py` に `status` と `end_date` の絞り込み単体テストを追加する

### ユーザーストーリー1の実装

- [X] T017 [P] [US1] `src/yosakoi_data_manager/services/sheet_reader.py` に必須列の行正規化と検証を実装する
- [X] T018 [P] [US1] `src/yosakoi_data_manager/services/event_filter.py` に `Approved` 抽出と期限切れ除外を実装する
- [X] T019 [US1] `src/yosakoi_data_manager/services/csv_exporter.py` に元列順を維持した CSV 出力を実装する
- [X] T020 [US1] `src/yosakoi_data_manager/services/sync_orchestrator.py` に読み取り→絞り込み→出力の流れを接続する
- [X] T021 [US1] `src/yosakoi_data_manager/cli/sync_events.py` に手動実行コマンドと出力先処理を公開する
- [X] T022 [US1] `src/yosakoi_data_manager/services/sync_orchestrator.py` に取得件数、承認件数、期限切れ件数の集計出力を追加する
- [X] T023 [US1] `specs/001-portal-event-sync/quickstart.md` に MVP の実行手順と `yosakoi_festival.csv` 出力を追記する

**完了条件**: ユーザーストーリー1単体で、有効な `yosakoi_festival.csv` を生成できる

---

## フェーズ4: ユーザーストーリー2 - 変更がある場合のみ反映する（優先度: P2）

**目的**: 既存公開 CSV と比較し、新規 `event_id` の追加と `updated_at` ベースの更新だけを行う

**独立テスト**: 既存 CSV と新規入力 fixture を比較し、未存在 `event_id` は追加、既存 `event_id` は `updated_at` が新しい時だけ更新、差分なし時はファイル非更新になることを確認する

### ユーザーストーリー2の検証

- [X] T024 [P] [US2] `tests/contract/test_sync_events_cli.py` に変更検知出力の契約テストを追加する
- [X] T025 [P] [US2] `tests/integration/test_sync_pipeline.py` に新規・更新・非更新の統合テストを追加する
- [X] T026 [P] [US2] `tests/unit/test_published_csv_store.py` に公開 CSV 比較ロジックの単体テストを追加する
- [X] T026a [P] [US2] `tests/unit/test_event_filter.py` に重複 `event_id` 検出と除外の単体テストを追加する
- [X] T025a [P] [US2] `tests/integration/test_sync_pipeline.py` に重複 `event_id` が公開 CSV に出力されないことを確認する統合テストを追加する
- [X] T025b [P] [US2] `tests/integration/test_sync_pipeline.py` に管理元取得失敗時に既存 CSV が不変であることを確認する統合テストを追加する
- [X] T025c [P] [US2] `tests/integration/test_sync_pipeline.py` に CSV 書き込み失敗時に既存 CSV が不変であることを確認する統合テストを追加する

### ユーザーストーリー2の実装

- [X] T027 [P] [US2] `src/yosakoi_data_manager/services/published_csv_store.py` に既存公開 CSV の読み込みと `event_id` インデックス化を実装する
- [X] T028 [P] [US2] `src/yosakoi_data_manager/services/published_csv_store.py` に新規・更新・非更新の比較ロジックを実装する
- [X] T028a [US2] `src/yosakoi_data_manager/services/event_filter.py` に重複 `event_id` を検出して除外する処理を実装する
- [X] T029 [US2] `src/yosakoi_data_manager/services/sync_orchestrator.py` に `updated_at` ベースのマージ規則を組み込む
- [X] T030 [US2] `src/yosakoi_data_manager/services/csv_exporter.py` に内容変更時のみ `yosakoi_festival.csv` を安全に置き換える処理を実装する
- [X] T030a [US2] `src/yosakoi_data_manager/services/csv_exporter.py` に一時ファイル書き込み失敗時の安全終了処理を実装する
- [X] T031 [US2] `src/yosakoi_data_manager/services/sync_orchestrator.py` に新規件数、更新件数、スキップ件数、`invalid_updated_at` 件数の出力を追加する
- [X] T031a [US2] `src/yosakoi_data_manager/services/sync_orchestrator.py` に `duplicate_error_count` と対象 `event_id` の出力を追加する
- [X] T032 [US2] `src/yosakoi_data_manager/lib/checksum.py` と `src/yosakoi_data_manager/services/published_csv_store.py` に行単位比較を補助するチェックサム処理を追加する
- [X] T033 [US2] `specs/001-portal-event-sync/quickstart.md` に Git 管理 CSV の比較規則と非更新時の挙動を追記する

**完了条件**: ユーザーストーリー1と2を通じて、破壊的でない増分同期が成立する

---

## フェーズ5: ユーザーストーリー3 - 定期実行と手動実行の両立（優先度: P3）

**目的**: 同一の同期ロジックを GitHub Actions の定期実行と手動実行から利用できるようにする

**独立テスト**: GitHub Actions ワークフローと CLI の両方で同じ引数体系を使い、定期実行・手動実行ともに `yosakoi_festival.csv` 更新とサマリ出力が成立することを確認する

### ユーザーストーリー3の検証

- [X] T034 [P] [US3] `tests/contract/test_sync_events_cli.py` に必須 CLI 引数と終了コードの契約テストを追加する
- [X] T035 [P] [US3] `tests/integration/test_sync_pipeline.py` に `dry-run` と手動実行経路の統合テストを追加する
- [X] T036 [P] [US3] `.github/workflows/sync-events.yml` 向けのワークフロー検証または lint 手順を追加する

### ユーザーストーリー3の実装

- [X] T037 [P] [US3] `src/yosakoi_data_manager/cli/sync_events.py` に任意の `dry-run` 処理と一貫した終了挙動を追加する
- [X] T038 [US3] `.github/workflows/sync-events.yml` に GitHub Actions の定期実行と手動実行ワークフローを実装する
- [X] T039 [US3] `.github/workflows/sync-events.yml` に Secrets から Google 認証情報を書き出して同期コマンドを実行する手順を追加する
- [X] T040 [US3] `.github/workflows/sync-events.yml` に `yosakoi_festival.csv` 変更時のみ commit / push する手順を追加する
- [X] T041 [US3] `src/yosakoi_data_manager/services/sync_orchestrator.py` に定期実行・手動実行の起動文脈を示す運用ログを追加する
- [X] T042 [US3] `specs/001-portal-event-sync/quickstart.md` に GitHub Actions の Secrets、`schedule`、手動実行フローを追記する

**完了条件**: 全ユーザーストーリーが独立して動作する

---

## フェーズN: 仕上げと横断対応

**目的**: 複数ストーリーにまたがる改善と仕上げ

- [X] T043 [P] `README.md` に実行入口とリポジトリ利用方法を追記する
- [X] T044 `src/yosakoi_data_manager/` 配下の変更ファイルについて命名整合、不要コード、モジュール肥大化を見直す
- [X] T045 [P] `tests/fixtures/` に不正 `updated_at`、重複 `event_id`、期限切れイベントの fixture を追加する
- [X] T046 `specs/001-portal-event-sync/quickstart.md` を実際のコマンド名とワークフロー挙動に照らして検証する
- [X] T047 [P] `Makefile` または `README.md` に CI 用コマンド例または開発者向け実行ターゲットを追加する

---

## 依存関係と実行順序

### フェーズ依存関係

- **セットアップ（フェーズ1）**: 依存なし。すぐ着手可能
- **基盤実装（フェーズ2）**: セットアップ完了後。すべてのユーザーストーリーの前提
- **ユーザーストーリー（フェーズ3以降）**: 基盤実装完了後
- **仕上げ（最終フェーズ）**: すべての対象ユーザーストーリー完了後

### ユーザーストーリー依存関係

- **ユーザーストーリー1（P1）**: 基盤実装完了後に着手可能。`Approved` 抽出と CSV 生成の MVP
- **ユーザーストーリー2（P2）**: ユーザーストーリー1の出力モデルと公開 CSV 読み書きに依存
- **ユーザーストーリー3（P3）**: ユーザーストーリー1、2の CLI 挙動が安定した後に着手

### 各ユーザーストーリー内の進め方

- 検証タスクは先に作成し、失敗を確認してから実装する
- モデル / ヘルパーを先に整える
- サービス実装の後に CLI / ワークフロー統合を行う
- 可読性のためのリファクタリングを完了条件に含める

### 並列実行の機会

- セットアップの T003-T005 は並列可能
- 基盤実装の T007-T009, T012 は並列可能
- US1 の T014-T016 と T017-T019 はそれぞれ並列可能
- US2 の T024-T026 と T027-T028 はそれぞれ並列可能
- US3 の T034-T036 は並列可能

---

## 並列実行例: ユーザーストーリー1

```bash
# ユーザーストーリー1の検証タスクを同時に進める:
Task: "tests/contract/test_public_csv_schema.py に CSV スキーマ維持の契約テストを追加する"
Task: "tests/integration/test_sync_pipeline.py に Approved のみを出力する統合テストを追加する"
Task: "tests/unit/test_event_filter.py に status と end_date の絞り込み単体テストを追加する"

# ユーザーストーリー1の実装タスクを同時に進める:
Task: "src/yosakoi_data_manager/services/sheet_reader.py に行正規化と検証を実装する"
Task: "src/yosakoi_data_manager/services/event_filter.py に Approved 抽出と期限切れ除外を実装する"
Task: "src/yosakoi_data_manager/services/csv_exporter.py に元列順を維持した CSV 出力を実装する"
```

## 並列実行例: ユーザーストーリー2

```bash
# ユーザーストーリー2の検証タスクを同時に進める:
Task: "tests/contract/test_sync_events_cli.py に変更検知出力の契約テストを追加する"
Task: "tests/integration/test_sync_pipeline.py に新規・更新・非更新の統合テストを追加する"
Task: "tests/unit/test_published_csv_store.py に公開 CSV 比較ロジックの単体テストを追加する"

# ユーザーストーリー2の実装タスクを同時に進める:
Task: "src/yosakoi_data_manager/services/published_csv_store.py に既存公開 CSV の読み込みとインデックス化を実装する"
Task: "src/yosakoi_data_manager/services/published_csv_store.py に新規・更新・非更新の比較ロジックを実装する"
```

## 並列実行例: ユーザーストーリー3

```bash
# ユーザーストーリー3の検証タスクを同時に進める:
Task: "tests/contract/test_sync_events_cli.py に必須 CLI 引数と終了コードの契約テストを追加する"
Task: "tests/integration/test_sync_pipeline.py に dry-run と手動実行経路の統合テストを追加する"
Task: ".github/workflows/sync-events.yml 向けのワークフロー検証または lint 手順を追加する"
```

## 実装戦略

### MVP 優先（ユーザーストーリー1のみ）

1. フェーズ1を完了する
2. フェーズ2を完了する
3. フェーズ3を完了する
4. **停止して確認**: `Approved` のみの CSV 生成を確認する
5. `yosakoi_festival.csv` の生成結果をレビューする

### 段階的な提供

1. セットアップと基盤実装を完了する
2. ユーザーストーリー1を追加して `Approved` 抽出を確認する
3. ユーザーストーリー2を追加して増分更新を確認する
4. ユーザーストーリー3を追加して GitHub Actions 実行を確認する
5. 最後に仕上げとドキュメント整備を行う

### 並列チーム戦略

1. 全員でセットアップと基盤実装を進める
2. 担当A: ユーザーストーリー1の抽出 / 出力
3. 担当B: ユーザーストーリー2の比較 / 更新
4. 担当C: ユーザーストーリー3のワークフロー / 自動化

## 補足

- すべてのタスクは明示的なファイルパスを含む
- 各ユーザーストーリーはフェーズ完了時点で独立検証可能
- 推奨 MVP スコープは **ユーザーストーリー1のみ**
- すべてのタスクは `- [ ] T### [P?] [US?] 説明 + file path` 形式を満たす
