# 契約: 公開用 CSV スキーマ

## `yosakoi_festival.csv`

- Google スプレッドシートの `events` シートと同じ公開列名・列順を維持する。
- `note` と `review` は公開しない。
- `status = Approved` かつ有効な `official_url` を持つイベントだけを出力する。
- 開催終了日は除外条件にしない。過去イベントも保持する。
- 重複 `event_id` はその ID の全行を除外する。
- 文字コードは UTF-8 とする。

## `award_winners.csv`

公開ヘッダは次の固定順序とする。

```text
event_id,award_name,team_name,result_source_url,video_url,video_source_type,status,updated_at
```

- `status = Approved` の検証済み行だけを出力する。
- `event_id` は公開対象イベントに存在しなければならない。
- `award_name`、`team_name`、`result_source_url`、`video_url`、`video_source_type`、`updated_at` は必須。
- `result_source_url` は公式結果を示す絶対 HTTPS URL とする。
- `video_url` はチャンネルやプレイリストではなく、単一動画を示す絶対 HTTPS URL とする。
- `video_source_type` は `OrganizerOfficial`、`TeamOfficial`、`General` のいずれかとする。
- `(event_id, 正規化済み team_name)` は一意とする。共同受賞は異なるチーム名で複数行にする。
- 同一イベント内で行ごとに異なる `award_name` と `result_source_url` を登録できる。
- `note` は管理用の非公開列とし、CSV へ出力しない。
- 行順は `event_id`、正規化済み `team_name` の昇順とする。

## 保存契約

2つの CSV は、全入力の取得と `Approved` 行の検証を完了してから、差分があるファイルだけ
直接書き込む。受賞行の検証失敗時はどちらも更新しない。書き込み失敗はCLI全体を失敗させ、
GitHub Actionsではcommit・pushを行わない。ローカルファイルのロールバックは行わない。
