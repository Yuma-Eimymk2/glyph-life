package com.eimymk2.glyphlife

/**
 * 13×13 のライフゲーム盤面を、Glyph Matrix SDK が期待する
 * 25×25 = 625 要素の IntArray に変換する。
 *
 * 実機検証 (2026-05-09):
 * - SDK は 25×25 配列を物理 137 LED の全体（背面の Glyph Matrix 全域）に
 *   マッピングする
 * - 13×13 を中央配置すると物理 LED の半分くらいの範囲にしか表示されない
 *
 * → 13×13 を 25×25 全体に「拡大マッピング」する。
 *   各 25×25 セルは、対応する 13×13 セルの値を最近傍法で取得する。
 *
 * （以前は中央配置で動作するように見えていたが、それは R-pentomino が
 *   中央 5×5 範囲に集中していたため、たまたま見えていただけだった）
 */
object GlyphRenderer {

    private const val OUTPUT_WIDTH = 25
    private const val OUTPUT_HEIGHT = 25
    private const val GAME_WIDTH = 13
    private const val GAME_HEIGHT = 13

    /**
     * ライフゲームの盤面を Glyph Matrix 用の 625 要素配列に変換する。
     * 13×13 の各セルが、25×25 の対応する 1〜2x1〜2 ブロックにマッピングされる。
     *
     * @param grid 13×13 の盤面。grid[row][col] が 0 なら消灯、1 なら点灯
     * @param brightness 点灯セルの明度 (0-255)
     * @return 625 要素の IntArray、setMatrixFrame() に渡せる形
     */
    fun render(grid: Array<IntArray>, brightness: Int): IntArray {
        require(grid.size == GAME_HEIGHT) { "grid height must be $GAME_HEIGHT" }
        require(grid.all { it.size == GAME_WIDTH }) { "grid width must be $GAME_WIDTH" }

        val output = IntArray(OUTPUT_WIDTH * OUTPUT_HEIGHT)

        for (outRow in 0 until OUTPUT_HEIGHT) {
            for (outCol in 0 until OUTPUT_WIDTH) {
                // 25×25 の (outRow, outCol) を 13×13 のどのセルから取るか
                // 最近傍法: 整数除算で対応セルを決定
                val gameRow = outRow * GAME_HEIGHT / OUTPUT_HEIGHT
                val gameCol = outCol * GAME_WIDTH / OUTPUT_WIDTH

                if (grid[gameRow][gameCol] == 1) {
                    output[outRow * OUTPUT_WIDTH + outCol] = brightness
                }
            }
        }

        return output
    }
}