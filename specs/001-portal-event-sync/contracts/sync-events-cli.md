# 契約: `sync-events` CLI

## 実行コマンド

```bash
./gradlew run --args="--sheet-id <sheet-id> --worksheet events"
```

| 引数 | 必須 | 説明 |
|------|------|------|
| `--sheet-id` | Yes | 管理元 Google スプレッドシート ID |
| `--worksheet` | Yes | イベントのワークシート名 |
| `--dry-run` | No | 両 CSV を書き換えず、判定結果のみ表示 |

受賞チームのワークシート名は `award_winners` に固定し、CLI 引数では変更できない。
出力先は `./yosakoi_festival.csv` と `./award_winners.csv` に固定する。

## 標準出力

イベントの取得・公開・新規・更新・スキップ・重複・不正日時件数、受賞行の
取得・承認・公開件数、動画投稿元別件数、各 CSV の変更有無と全体変更有無を出力する。

主要キーは `award_fetched`、`award_approved`、`award_published`、
`videos_organizer_official`、`videos_team_official`、`videos_general`、`event_changed`、
`award_changed`、`changed` とする。

## 終了条件

| 終了コード | 意味 |
|-----------|------|
| `0` | 実行成功。差分あり・差分なし・dry-run を含む |
| `1` | 引数不正、認証・取得・検証・書き出し失敗 |

入力取得または `Approved` 受賞行の検証に失敗した場合、既存の両 CSV は変更しない。
