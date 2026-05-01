# 契約: `sync-events` CLI

## 目的

運営担当者またはスケジューラが、Google スプレッドシートから公開用 CSV を生成し、
Git 管理された公開用 CSV を更新するための実行契約。

## 実行コマンド

```bash
./gradlew run --args="--sheet-id <sheet-id> --worksheet <worksheet-name>"
```

## 入力引数

| 引数 | 必須 | 説明 |
|------|------|------|
| `--sheet-id` | Yes | 管理元 Google スプレッドシート ID |
| `--worksheet` | Yes | 読み取り対象ワークシート名 |
| `--dry-run` | No | 実装する場合、反映せず判定結果のみ表示 |

出力先はリポジトリ直下の `./yosakoi_festival.csv` に固定する。

## 期待される動作

1. Google Sheets から行データを取得する
2. `Approved` のみ抽出する
3. 終了日が現在日付より前のイベントを除外する
4. 同じ `event_id` の重複を重複エラーとして除外する
5. `event_id` と `updated_at` に基づいて新規 / 更新 / 非更新を判定する
6. 差分がある場合のみ CSV を更新する

## 標準出力

正常終了時は少なくとも以下を返す。

```text
fetched=<number>
approved=<number>
new=<number>
updated=<number>
skipped=<number>
expired=<number>
duplicate_errors=<number>
invalid_updated_at=<number>
output=<path>
changed=<true|false>
```

`output` は常に `./yosakoi_festival.csv` を返す。

## 終了条件

| 終了コード | 意味 |
|-----------|------|
| `0` | 実行成功。差分あり・差分なしの両方を含む |
| `1` | 入力不正、認証失敗、シート取得失敗、CSV 書き出し失敗など |

## エラー時の扱い

- `updated_at` 不正レコードは実行全体を落とさず、対象レコードのみ更新対象外にする
- 重複 `event_id` は対象外にし、件数を出力する
- 管理元取得失敗時は既存 CSV を変更しない
