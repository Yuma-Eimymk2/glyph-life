# Glyph Life

> *Conway's Game of Life, quietly running on the back of your Nothing Phone (4a) Pro.*
>
> *Nothing Phone (4a) Pro の Glyph Matrix で、ライフゲームをひっそり動かす Glyph Toy。*

## Concept / コンセプト

> *"The moment you put your phone down, a small story of life begins on its back,*
> *quietly playing out and quietly ending."*
>
> **「スマホを手放した瞬間、背面で小さな生命の物語が始まり、静かに終わっている」**

No UI, no configuration. While your phone is face down, patterns of life appear on the
Glyph Matrix, evolve under Conway's rules, and gracefully fade — perhaps with a final
glider drifting toward the edge of the world.

設定不要・操作不要。スマホを伏せている間、Glyph Matrix の上で生命のパターンが
生まれ、進化し、静かに消えていきます。長く生き残った世代には、最後に Glider が
旅立つささやかな餞があります。

## Features / 特徴

- **Set and forget** — No UI. Just register as your AOD Toy.  
  **設定不要** — UI なし。AOD Toy として登録するだけ
- **Time-seeded patterns** — Each minute brings a new initial pattern.  
  **時刻シード** — 毎分ごとに違う初期パターン
- **Stillness-aware** — Stops automatically when patterns stabilize, go extinct, or time out.  
  **自動停止** — 安定・絶滅・タイムアウトを自動検出
- **Graceful endings** — Long-lived generations earn a brief ceremonial farewell.  
  **餞の演出** — 20秒以上生存した世代には終焉の演出
- **Cyclic life** — While face-down, new lives begin every few minutes.  
  **連続的な生命** — 伏せている間、数分ごとに新しい生命が始まる

## Usage / 使い方

1. Install via Nothing Playground or from [Releases](https://github.com/Yuma-Eimymk2/glyph-life/releases).  
   Nothing Playground または下の [Releases](https://github.com/Yuma-Eimymk2/glyph-life/releases) からインストール
2. Go to **Settings → Glyph Interface → Flip to Glyph → Always-on Glyph Toy**.  
   **設定 → Glyph Interface → Flip to Glyph → Always-on Glyph Toy** を開く
3. Select **Glyph Life**.  
   **Glyph Life** を選択
4. Place your phone face down. Wait.  
   スマホを伏せて待つ

## Requirements / 動作環境

- Nothing Phone (4a) Pro
- Nothing OS 4.1 (Android 16) or later / Nothing OS 4.1 (Android 16) 以降

## License

MIT — see [LICENSE](LICENSE).

## Acknowledgements / 謝辞

- Conway's Game of Life (John Horton Conway, 1970)
- The aesthetic of "quiet endings" inspired by Fujiko F. Fujio's short story *"One Day..."* (ある日…)  
  終焉の演出は藤子・F・不二雄『ある日…』にインスパイアされています
- Built with [Glyph Matrix Developer Kit](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit)

---

*Made by [Yuma-Eimymk2](https://github.com/Yuma-Eimymk2).*
