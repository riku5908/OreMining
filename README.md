# はじめに
・本リポジトリはJava学習者の「りく」（Xアカウント：@RK4111719290707）が作成したMineCraftプラグイン「Ore Mining」に関するものです。

・Webアプリケーション開発をするために、今まで学習してきたことのアウトプット作品として作成いたしました。

# Minecraft用 鉱石採掘ゲーム（Ore Mining Game）
## ゲーム概要
・本ゲームはMinecraft Java版で利用できるプラグイン「鉱石採掘ゲーム」です。

・制限時間内にさまざまな種類の鉱石を採掘し、その鉱石の種類に応じたポイントを獲得していくことが目的です。

・プレイヤーは採掘した鉱石ごとに異なるスコアを得られ、最終的な得点で競います。

## プレイ動画
https://github.com/user-attachments/assets/a49e29a3-70d8-48d7-8092-c043f586e8c1

## ゲーム詳細
・制限時間：30秒

・ゲーム開始：任意の場所で /oremining とコマンドを入力するとゲーム開始となります。

・装備の配布：ゲーム開始時、メインハンドにネザライトのツルハシ、オフハンドに松明64個が配布されます。

　※ 事前に持っていたアイテムは失われますのでご注意ください。

・体力と空腹度：ゲーム開始時には体力と空腹度が最大に回復します。

・鉱石ブロック：開始コマンドを入力するとプレイヤーの周りに6×6×6(合計216個のブロック）が生成されます。

・得点：鉱石の種類によって点数と出現率が決まっており、出現率が低い鉱石を採掘するとより高い点数を獲得できます。

・スコア表示：制限時間終了後、獲得したスコアが表示され、データベースに保存されます。

・スコア確認：/oremining list とコマンド入力で過去のスコアを確認できます。

## コマンド
| コマンド | 説明 |
| --- | --- |
| `/oremining` | ゲームを開始する |
| `/oremining list` | ゲームをプレイしたスコア情報を表示 |

## 得点（鉱石別）
・ダイアモンド鉱石 70P

・エメラルド鉱石 50P

・金鉱石 40p

・レッドストーン鉱石 30p

・ラピスラズリ鉱石 20p

・石炭鉱石 10P

・鉄鉱石 5p

・石 1P

## 開発環境
・開発言語：Java Oracle OpenJDK 21.0.5

・アプリケーション：Minecraft 1.21.3

・サーバ：paper 1.21.4

## データベース構成
テーブル：player_score

| 属性 | 設定値 |
| --- | --- |
| ユーザー名 | ※ |
| パスワード | ※ |
| URL | ※ |
| データベース名 | ore_mining |
| テーブル名 | player_score |

（※）は自身のローカル環境に合わせてご使用ください。(mybatis-config.xmlで設定します)

## データベースの接続方法
1. 自身のローカル環境でMySQLに接続してください。
2. 以下のコマンドを順に実行してください。

```
CREATE DATABASE ore_mining;
```

```
USE ore_mining;
```

```
CREATE TABLE player_score(id int auto_increment, player_name varchar(100), score int, registered_at datetime, primary key(id)) DEFAULT CHARSET=utf8;
```
## こだわりポイント
・自動ブロック生成：ゲーム開始時にブロックを出現させることで、地下にいなくてもいつでもゲームを楽しめるようにしました。

・ブロックの出現：ブロックの種類によって出現確率を変更し、種類ごとに獲得スコアも異なるように設定を行った。

・鉱石の種類とスコアの表示：ゲーム内で採掘される各鉱石には異なるスコアが割り当てられており、採掘時に鉱石の種類とスコアがメッセージに表示されます。

・カウントダウン：ゲーム終了5秒前になるとカウントダウンが始まる設定を行い、プレイヤーがゲーム終了時間がわかるように実行しました。

## 今後　実装予定の機能
・強制テレポート：ゲーム開始コマンドを入力後、採掘場にテレポートする。

・タイムバーの表示：制限時間を伸ばし、残りのゲーム時間がわかるタイムバーを表示する。

・難易度設定

## おわりに
・Java学習者のアウトプットとして、リポジトリ公開させていただきました。

・感想・コメント等ございましたら、Xアカウントまでご連絡いただけますと幸いです。

　：https://x.com/RK4111719290707
