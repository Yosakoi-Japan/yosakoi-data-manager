# データモデル: Yosakoi Portal イベント・受賞データ連携

## 1. 管理元イベント (`SourceEvent`)

既存 `events` シートの一行。主キーは `event_id`。`event_name`、`status`、
`start_date`、`end_date`、`updated_at` を必須とし、有効な `official_url` を持つ
`Approved` 行を公開候補にする。`end_date` にかかわらず過去イベントも削除しない。

同じ `event_id` の公開候補が複数ある場合、その ID の全行を除外する。

## 2. 管理元受賞行 (`SourceAwardWinner`)

新規 `award_winners` シートの一行。

| 項目 | Approved時 | 説明 |
|------|------------|------|
| `event_id` | 必須 | 公開対象イベントへの外部キー |
| `award_name` | 必須 | 最上位賞の名称 |
| `team_name` | 必須 | 受賞チーム名 |
| `result_source_url` | 必須 | 公式結果の絶対 HTTPS URL |
| `video_url` | 必須 | 単一動画の絶対 HTTPS URL |
| `video_source_type` | 必須 | `OrganizerOfficial` / `TeamOfficial` / `General` |
| `status` | 必須 | `Approved` のみ公開 |
| `updated_at` | 必須 | ISO 8601 オフセット日時 |
| `note` | 任意 | 管理用。非公開 |

`Progress` 行は制作途中として公開項目を空にできる。公開行では
`(event_id, NFKC・空白・大文字小文字を正規化した team_name)` を一意とする。
同一イベント内で行ごとに異なる賞名や結果 URL を登録できる。

## 3. 公開受賞行 (`PublishedAwardWinner`)

検証済み `Approved` 行。`note` を除いた固定8列へ変換し、`event_id` と正規化チーム名で
安定ソートする。過去イベントの受賞行も保持する。

## 4. 同期結果 (`SyncResult`)

イベントの取得・公開・差分・過去開催・重複件数に加え、受賞の取得・承認・公開件数、
動画投稿元別件数、イベント CSV と受賞 CSV それぞれの変更有無を保持する。

## ライフサイクル

1. `events` と `award_winners` を取得する。
2. 公開可能な全 `Approved` イベントを抽出する。
3. `Approved` 受賞行の外部キー、必須値、URL、重複、イベント内整合性を検証する。
4. イベントは既存 CSV と `updated_at` でマージし、受賞 CSV は検証済み行から再構築する。
5. 差分がある CSV だけを直接書き込む。
6. 書き込み失敗時は同期を失敗させ、commit・pushへ進まない。
