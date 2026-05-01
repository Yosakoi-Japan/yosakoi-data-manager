# データモデル: Yosakoi Portal イベントデータ連携

## 1. 管理元イベント (`SourceEvent`)

### 目的

Google スプレッドシートから取得した 1 行分のイベント情報を表す。

### 項目

| 項目 | 型 | 必須 | 備考 |
|------|----|------|------|
| `event_id` | string | Yes | 同一性判定キー |
| `event_name` | string | Yes | 表示名称 |
| `status` | string | Yes | `Approved` のみ公開対象 |
| `image_url` | string | No | 画像 URL |
| `official_url` | string | No | 公式サイト URL |
| `start_date` | date string | Yes | 例: `2026-06-10` |
| `end_date` | date string | Yes | 例: `2026-06-14` |
| `location` | string | No | 開催場所 |
| `team_count` | string | No | 参加チーム数表記 |
| `nearest_station` | string | No | 最寄駅情報 |
| `parking_info` | string | No | 駐車場情報 |
| `description` | string | No | 説明文 |
| `youtube_url` | string | No | 動画 URL |
| `latitude` | string | No | 緯度 |
| `longitude` | string | No | 経度 |
| `map_url` | string | No | 地図 URL |
| `updated_at` | date string | Yes | 既存更新判定に利用 |
| `note` | string | No | 補足メモ |
| `columns` | ordered map | Yes | 管理元シートと同一列構成の値集合 |

### 検証ルール

- `event_id` は空であってはならない。
- `event_name` は空であってはならない。
- `status` は文字列として取得し、`Approved` と完全一致した場合のみ公開対象候補。
- `start_date` と `end_date` は ISO 形式の日付として解釈できなければならない。
- `updated_at` は必須であり、欠落 / 不正ならそのレコードは不正データとして扱う。

## 2. 公開対象イベント (`ApprovedEvent`)

### 目的

公開候補として抽出されたイベント。期限切れ除外前の中間表現。

### 項目

| 項目 | 型 | 必須 | 備考 |
|------|----|------|------|
| `event_id` | string | Yes | `SourceEvent` から継承 |
| `event_name` | string | Yes | 表示名称 |
| `updated_at` | date string | Yes | 既存更新判定時に使用 |
| `start_date` | date string | Yes | 監査・表示用 |
| `end_date` | date string | Yes | 現在日付との比較に使用 |
| `columns` | ordered map | Yes | CSV 出力元データ |

### 状態ルール

- `status = Approved` を満たす。
- `end_date < today` の場合は「チェック対象外」に遷移する。
- 同じ `event_id` が複数ある場合は「重複エラー」に遷移する。

## 3. 既存公開 CSV レコード (`PublishedCsvRecord`)

### 目的

Git 管理された既存の公開用 CSV に保存されている 1 レコードを表し、次回同期時の比較基準にする。

### 項目

| 項目 | 型 | 必須 | 備考 |
|------|----|------|------|
| `event_id` | string | Yes | 主キー |
| `event_name` | string | Yes | 表示名称 |
| `updated_at` | date string | Yes | 前回反映時の比較基準 |
| `row_checksum` | string | Yes | 同一行内容の検証補助 |
| `columns` | ordered map | Yes | 直近反映済み列データ |

### 検証ルール

- `event_id` は一意でなければならない。
- `updated_at` が欠落 / 不正なレコードは不正データとして扱う。

## 4. 同期結果 (`SyncResult`)

### 目的

1 回の実行結果をオペレーターへ返す集約結果。

### 項目

| 項目 | 型 | 必須 | 備考 |
|------|----|------|------|
| `fetched_count` | integer | Yes | 取得件数 |
| `approved_count` | integer | Yes | `Approved` 件数 |
| `new_count` | integer | Yes | 新規保存件数 |
| `updated_count` | integer | Yes | 上書き件数 |
| `skipped_count` | integer | Yes | 非更新件数 |
| `expired_count` | integer | Yes | 期限切れ除外件数 |
| `duplicate_error_count` | integer | Yes | 重複エラー件数 |
| `invalid_updated_at_count` | integer | Yes | `updated_at` 不正件数 |
| `output_path` | string | Yes | 生成 CSV パス |

## 関係

- `SourceEvent` から `ApprovedEvent` が生成される。
- `ApprovedEvent` は `PublishedCsvRecord` と `event_id` で対応する。
- 同期処理全体の集約結果が `SyncResult` になる。

## ライフサイクル

1. `SourceEvent` を取得する。
2. `status = Approved` の行だけ `ApprovedEvent` 候補にする。
3. `end_date < today` の場合は「チェック対象外」にする。
4. 同じ `event_id` の重複があれば「重複エラー」にする。
5. 既存の公開 CSV に存在しない `event_id` は新規保存する。
6. 既存の公開 CSV に存在する場合、`updated_at` が新しい時のみ更新する。
7. `updated_at` 不正時は上書きしない。
8. 出力 CSV を更新し、`SyncResult` を返す。
