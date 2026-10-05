# ⚡️ Power Struggle — でんりょく綱引き

USB-Cケーブルでつないだ2台のスマホが、お互いの電力を取り合うAndroidアプリです。
画面をタップして綱を引き、自分側が優勢になると、自分のスマホが充電される側に切り替わります。

このリポジトリは、[Ken Kawamoto氏のPower Struggle](https://github.com/kenkawakenkenke/power-struggle)を元にした
[kumi0708のフォーク](https://github.com/kumi0708/power-struggle)です。
女の子の綱引きアニメーション、日本語の画面、小瓶から電力をすするサキュバスモードを追加しています。

**最新版のAPKと動作動画は、[v0.7のリリースページ](https://github.com/kumi0708/power-struggle/releases/tag/v0.7)からダウンロードできます。**
Android 13以降向けのデバッグAPKです。このフォークではビルドとエミュレーターでの動作を確認していますが、
実機2台でのUSB通信・電力切り替え・充電量はまだ確認していません。

## 2つのモード

### 綱引き

2頭身の女の子たちが、光るエネルギーの綱を引っぱります。
イラストまたは「タップで引っぱる！」をタップすると、自分側の女の子が綱を引きます。
引っぱるポーズ、まばたき、優勢・劣勢の表情に、腕・膝・髪・リボンの動きを重ねています。
足を地面に固定し、綱の端も手の動きに合わせています。

接続前は「おためしで遊ぶ」から、1台で画面と操作を試せます。
**おためしの電池残量はサンプルで、実際の電力移動はありません。**
実機の相手が接続されると、本番の対戦表示に戻ります。

<p align="center">
  <img src="docs/chibi-waiting.png" width="230" alt="女の子たちが待つ接続画面">
  <img src="docs/chibi-demo.png" width="230" alt="タップして綱を引くおためし画面">
  <img src="docs/chibi-split.png" width="230" alt="上下に分かれた向かい合わせの対戦画面">
</p>

### サキュバス ♡

成人のファンタジーキャラクターが、小瓶に入った電力をストローでチューチューすするモードです。
紫のコルセット、長い手袋、ニーハイブーツの参考画像をそのまま採用しています。
4つの表情を切り替えながら、吸う動き・呼吸・髪・翼が動き続けます。

- 「サキュバス ♡」を選び、接続前は「チューチューを眺める」でおためし再生できます。
- タップ操作は不要です。このモードから対戦のタップ送信や自動の電力切り替えは行いません。
- 接続中の電力エフェクトは、スマホの実際の充電方向に合わせて表示します。
- 「綱引き」に戻すと、タップ対戦を再開できます。
- 選んだモードはアプリを再起動しても保存され、1台の上下表示では両側に反映されます。

<p align="center">
  <img src="docs/succubus-waiting.png" width="230" alt="サキュバスモードの接続待ち画面">
  <img src="docs/succubus-demo.png" width="230" alt="小瓶から電力をすする自動アニメーション">
  <img src="docs/succubus-split.png" width="230" alt="上下の両側で動くサキュバス">
</p>

## 用意するもの

- Android 13以降のUSB-C対応スマホ。2台で遊ぶ場合は、両方にこのアプリをインストールします。
- 片方のAndroidにインストールし、起動した[Shizuku](https://shizuku.rikka.app/)。
- 充電とデータ通信の両方に対応したUSB-C − USB-Cケーブル。

Shizukuはワイヤレスデバッグなどで起動します。スマホの再起動後は、Shizukuも起動し直してください。
相手がアプリを動かせない端末の場合は、Android側の画面を上下に分ける1台モードを使います。
USB-C対応のiPadやiPhoneは電力だけをやり取りする相手で、Androidアプリは動作しません。
電力の切り替えに対応するかどうかは端末によって異なります。

## 接続して遊ぶ

1. Androidにアプリをインストールし、片方の端末でShizukuを起動します。
2. 両方の端末でアプリを開き、USB-Cケーブルで接続します。
3. 表示されたShizukuの権限、USBアクセス、相手側の「Power Struggleを開く」などの確認を許可します。
4. 2台のスマホを、**下端同士が向かい合うように**置きます。画面の向きは、この置き方に合わせています。
5. 「綱引き」を選び、イラストまたはボタンをタップして綱を引きます。

綱の目印が切り替え位置を越えると、電力を送る側と受け取る側が入れ替わります。
電池残量と充電・放電の表示には、各端末が取得した実際のバッテリー情報を使います。
接続できない場合は「接続の詳細」で、Shizukuの設定案内と診断ログを確認してください。

## 電力を切り替える仕組み

USB-Cの端末には、電力を供給する側と受け取る側の役割があります。
元のPower Struggleは、AndroidのUSBポートに対して次のシェルコマンドを実行し、その役割を切り替えます。

```text
dumpsys usb set-port-roles <port> <source|sink> <host|device>
```

このコマンドに必要なシェル権限を、Shizukuを通して借りています。root化は不要で、
Shizukuが必要なのは片方のAndroidだけです。

対戦データは同じUSB-Cケーブルで送ります。Shizukuを起動した側がUSBホストになり、
相手のAndroidを[Android Open Accessoryモード](https://source.android.com/docs/core/interaction/accessories/protocol)にして、
タップや対戦状態をやり取りします。電力の役割とデータ通信の役割は別なので、電力を切り替えても通信を続けられます。

このフォークでは、元の対戦・USB通信・電力制御のコードを維持しています。
画面に表示する充電方向は対戦メッセージから推測せず、端末の`BatteryManager`の読み取り値を使います。

## 動作状況と注意点

これは実験的なアプリです。元のプロジェクトではPixel同士とiPad相手での動作が報告されています。
このフォークの確認内容は、[画面・アニメーションの検証記録](docs/ui-validation.md)にまとめています。

- USBポートの切り替えコマンドは公開APIではなく、端末メーカーやAndroidのバージョンによって動作しない場合があります。
- 電池残量が少ないと給電できない端末があります。アプリには最低残量で対戦を止める機能がないため、必要な電池を使い切らないようにしてください。
- 画面表示やアニメーションでも電力を消費します。相手から減った電力が、そのまま自分の充電量になるわけではありません。
- 元のプロジェクトでは、Pixel 9 Pro XLとPixel 4 XLの組み合わせで、供給側が約1500 mA減る一方、受け取り側は約400 mA増えると報告されています。このフォークで測定した値ではありません。
- このフォークの実機2台でのUSB通信、Shizukuによる電力切り替え、実際の充電量は未検証です。

## 開発・ビルド

JDK 17以降とAndroid SDKを用意します。Android Studioでプロジェクトを開くか、次のコマンドでビルドできます。
SDKの場所は、環境変数`ANDROID_HOME`または`local.properties`の`sdk.dir`で指定してください。

```sh
./gradlew assembleDebug lintDebug
```

APKの出力先は`app/build/outputs/apk/debug/app-debug.apk`です。
接続したAndroidへビルドしてインストールする場合は、次を実行します。

```sh
./gradlew installDebug
```

アプリのIDは`com.kumi0708.powerstruggle`で、元のアプリと同じ端末にインストールできます。
現在のバージョンは0.7です。

### デバッグ用のおためし表示

デバッグビルドでは、エミュレーターでの確認用に次の起動オプションを使えます。
リリースビルドでは、これらの指定を無視します。

| オプション | 値・動作 |
| --- | --- |
| `preview_scene` | `charging`：充電側、`draining`：放電側、`split`：上下表示のおためし |
| `preview_visual` | `tug`：綱引き、`succubus`：サキュバスを最初に表示 |
| `preview_face_up` | `true`：撮影用に画面を正面向きに表示 |

サキュバスのおためしを正面向きで起動する例です。

```sh
adb shell am start -S -n com.kumi0708.powerstruggle/com.kenkawamoto.powerstruggle.MainActivity \
  --es preview_scene charging --es preview_visual succubus --ez preview_face_up true
```

### 主なファイル

| ファイル | 役割 |
| --- | --- |
| [MainActivity.kt](app/src/main/java/com/kenkawamoto/powerstruggle/MainActivity.kt) | 日本語の画面、モード選択、電池表示、タップ操作、上下表示 |
| [TugAnimation.kt](app/src/main/java/com/kenkawamoto/powerstruggle/TugAnimation.kt) | 女の子の綱引き、表情、髪・リボンのアニメーション |
| [SuccubusAnimation.kt](app/src/main/java/com/kenkawamoto/powerstruggle/SuccubusAnimation.kt) | 小瓶から電力をすするサキュバスの自動アニメーション |
| [Battle.kt](app/src/main/java/com/kenkawamoto/powerstruggle/Battle.kt) | 対戦状態、綱の位置、1台・2台モード、対戦データ |
| [PowerControl.kt](app/src/main/java/com/kenkawamoto/powerstruggle/PowerControl.kt)・[ShellService.kt](app/src/main/java/com/kenkawamoto/powerstruggle/ShellService.kt) | Shizuku経由でのUSBポートの役割切り替え |
| [UsbLink.kt](app/src/main/java/com/kenkawamoto/powerstruggle/UsbLink.kt) | Android Open AccessoryによるUSB通信 |

### イラストと制作記録

イラストと制作時の指示は[docs/art](docs/art)に保存しています。
アプリ用の画像は`app/src/main/res/drawable-nodpi/`にあります。

- 綱引き：`tug_emotes_atlas.png`、`tug_pink_pull_atlas.png`。
  [アニメーション用イラストの制作指示](docs/art/tug-animation-prompts.md)を参照してください。
- サキュバス：`succubus_sip_atlas.png`。
  [参考画像の採用記録](docs/art/succubus-reference-v0.7.md)と[元の制作指示](docs/art/succubus-sip.prompt.md)を参照してください。

元のPNGは加工せずに保存しています。アプリ側で各ポーズの範囲、足や手、小瓶の位置を指定し、動きを付けています。

## 元のプロジェクト・ライセンス

元のPower Struggleの作者はKen Kawamoto氏です。
[元のリポジトリ](https://github.com/kenkawakenkenke/power-struggle)と
[プロジェクト紹介](https://ideas.skip.work/u/kenkawakenkenke/projects/power-struggle)もご覧ください。

[MITライセンス](LICENSE) © 2026 Ken Kawamoto
