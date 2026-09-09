# Quickstart: Yosakoi Portal イベント・受賞データ連携

## 前提

- Java 21 以上
- Google Sheets 読み取り権限を持つサービスアカウント
- 既存の `events` シート（列変更なし）
- 新規の `award_winners` シート

`award_winners` の列は次の順序を推奨する。

```text
event_id,award_name,team_name,result_source_url,video_url,video_source_type,status,updated_at,note
```

動画確認前の行は `Progress` とし、`video_url` と `video_source_type` を空にできる。
`Approved` にする時点では公開列がすべて必須になる。

## 実行

```bash
export GOOGLE_APPLICATION_CREDENTIALS=/absolute/path/to/service-account.json
./gradlew run --args="--sheet-id <google-sheet-id> --worksheet events"
```

受賞シートは常に `award_winners` を読む。確認のみの場合は `--dry-run` を追加する。

## 同期ルール

- `events`: `Approved` かつ有効な公式 URL を持つイベントを公開し、過去開催分も保持する。
- `award_winners`: 有効な `Approved` 行のみを公開する。動画 URL は必須で、単一動画を指す HTTPS URL に限る。
- 受賞は同じ `event_id` でも賞名と結果 URL を行ごとに変えられる。チームごとに一行登録する。
- 代表動画は `OrganizerOfficial`、`TeamOfficial`、`General` の順で優先して選ぶ。
  一般投稿動画は `video_source_type=General` として人手確認後に公開できる。
- 受賞行の検証失敗時はイベント CSV と受賞 CSV の両方を更新しない。
- 2つの CSV は差分がある場合だけ直接書き込む。失敗時はcommit・pushしない。

## 生成物

- `yosakoi_festival.csv`: イベント公開データ
- `award_winners.csv`: 受賞チームと代表動画

## GitHub Actions

Secrets は `GOOGLE_SERVICE_ACCOUNT_JSON`、`GOOGLE_SHEET_ID`、`GOOGLE_WORKSHEET`、
`YOSAKOI_PORTAL_PAT` が必須。生成した2ファイルをこのリポジトリで同時に commit し、
ポータル側の assets/data にコピーして同時に commit する。

## 検証

```bash
./gradlew test
```
