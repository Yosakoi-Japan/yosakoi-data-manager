# Research: Yosakoi Portal イベントデータ連携

## Decision 1: 実装形態は Kotlin CLI バッチとする

- **Decision**: 単一の Kotlin / Java 21 製 CLI バッチとして実装する。
- **Rationale**: Kotlin に寄せて実装理解と保守を統一しつつ、Google Sheets 読み取り、
  CSV 変換、差分判定、定期実行のすべてを単一バイナリ的に扱える。GitHub Actions 上でも
  Gradle Wrapper で再現しやすい。
- **Alternatives considered**:
  - Python スクリプト: 実装は可能だが、チームが Kotlin に寄せて理解したい要求と合わない。
  - Google Apps Script: Sheets への近さはあるが、ローカル再現性とテスト容易性が下がる。
  - 手動 CSV 運用継続: 目的である手動更新削減を満たさない。

## Decision 2: Google Sheets 読み取りは API クライアント経由で行う

- **Decision**: Google Sheets API Client と `google-auth-library-oauth2-http` を使い、
  Google Sheets API 経由で読み取る。
- **Rationale**: Kotlin から公式 API クライアントを直接扱え、サービスアカウント認証、
  シート選択、行取得を GitHub Actions とローカルで同じ実装にできる。
- **Alternatives considered**:
  - CSV エクスポート URL 直接取得: 実装は軽いが、認証や安定運用で制約が大きい。
  - Google Apps Script 中継: 運用対象が増え、責務分離が弱くなる。

## Decision 3: 同期判定は Git 管理された公開 CSV を基準に管理する

- **Decision**: 公開対象イベントは Git 管理された既存の公開 CSV を比較元とし、
  `event_id` と `updated_at` 比較で新規 / 更新 / 非更新を判定する。
- **Rationale**: 生成 CSV を Git で管理する運用方針と一致し、履歴確認と差分追跡が
  そのまま可能になる。状態ファイルを別管理する必要もない。
- **Alternatives considered**:
  - 状態ファイルを別管理する: 比較元が二重化し、Git 管理したい要件に合わない。
  - 出力 CSV 全体ハッシュのみ比較: 新規 / 更新 / スキップ理由が見えにくい。
  - 毎回全件上書き: 不要更新を防げず、運用ログも粗くなる。

## Decision 4: 過去開催イベントは `end_date` ベースで同期対象外とする

- **Decision**: 管理元の `end_date` を直接使い、終了日が実行日より前なら公開判定と
  更新判定から除外する。
- **Rationale**: 実データは `start_date` / `end_date` 列を持っており、期間文字列を
  解析するより堅牢で簡潔に判定できる。
- **Alternatives considered**:
  - `start_date` で除外判定: 開催中イベントを誤除外する。
  - 期間文字列をパースする: 現在の実データ構造では不要。
  - 期間判定なし: 期限切れイベントが残り続ける。

## Decision 5: `updated_at` 不正値と `event_id` 重複はエラーとしてスキップする

- **Decision**: `updated_at` が不正な既存イベントは上書きしない。同じ
  `event_id` の `Approved` イベントが複数ある場合は重複エラーとして対象外にする。
- **Rationale**: 自動同期で誤更新を起こさないことが優先。除外理由を運営が追跡できれば
  データ修正は管理元で行える。
- **Alternatives considered**:
  - 内容差分で強制更新: 仕様の `updated_at` 優先ルールと衝突する。
  - 最新 `updated_at` の 1 件を採用: 重複データ混入を隠してしまう。

## Decision 6: 出力 CSV は管理元シートと同一列構成を維持する

- **Decision**: 公開用 CSV は Google スプレッドシート管理元と同じ列順・列名で出力する。
- **Rationale**: フロントエンド利用条件と spec の明確化内容に一致する。変換責務を
  「絞り込みと同期判定」に限定できる。
- **Alternatives considered**:
  - フロントエンド専用スキーマへ再整形: 将来有効だが、現時点では要件にない。

## Decision 7: テスト戦略は unit / integration / contract の 3 層とする

- **Decision**: 日付判定やフィルタは unit、同期一連の流れは integration、
  CLI 引数と CSV 列構成は contract テストで検証し、すべて JUnit 5 で実行する。
- **Rationale**: 憲章の「検証を必須化」「再現可能運用」「保守性」を最も素直に満たせる。
- **Alternatives considered**:
  - integration テストのみ: 失敗原因の切り分けが難しい。
  - unit テストのみ: CLI 契約と入出力整合を守れない。

## Decision 8: 現行 `events - events.csv` の列構成を基準に設計を補正する

- **Decision**: 実データ確認結果に合わせ、識別子は `event_id`、表示名は `event_name`、
  開催判定は `start_date` / `end_date`、更新判定は `updated_at` を使う。
- **Rationale**: `specs/001-portal-event-sync/events - events.csv` の実データはこの列構成であり、
  設計と実データの不一致を先に解消しておく方が実装時の手戻りが少ない。
- **Alternatives considered**:
  - 仮想的な `yosakoi_name` 前提を維持する: 現行データと乖離する。

## Decision 9: `status` と `updated_at` の入力運用を前提条件として扱う

- **Decision**: `status` は公開判定に使い、`updated_at` は必須項目として入力される
  前提で同期ロジックを設計する。
- **Rationale**: 更新判定の信頼性を保つには `updated_at` の必須運用が必要であり、
  仕様を単純かつ検証可能に維持できる。
- **Alternatives considered**:
  - `Progress` も公開対象に含める: 元要件と衝突する。
  - `updated_at` なしでも既存更新を許可する: 誤更新リスクが高い。
