package com.eimymk2.glyphlife

/**
 * Glyph Matrix の物理 LED 配置を表すマスク。
 *
 * Nothing Phone (4a) Pro の Glyph Matrix は 13×13 グリッドだが、
 * 物理 LED は円形に近い形で 137 個配置されている。
 * このオブジェクトは「どのセルが LED に対応するか」を表すマスクを生成する。
 *
 * マスクは実機の LED 配置を直接反映している。
 */
object CircularMask {

    /**
     * 13×13 Glyph Matrix の標準 LED マスク。
     *
     * '#' = LED あり（生死の対象）
     * '.' = LED なし（常に死）
     *
     * セル数: 137 個（5+9+11+11+13+13+13+13+13+11+11+9+5）
     */
    private val GLYPH_MATRIX_LAYOUT = arrayOf(
        "....#####....",
        "..#########..",
        ".###########.",
        ".###########.",
        "#############",
        "#############",
        "#############",
        "#############",
        "#############",
        ".###########.",
        ".###########.",
        "..#########..",
        "....#####....",
    )

    /**
     * Glyph Matrix の標準 13×13 マスクを生成する。
     */
    fun createGlyphMatrix(): Array<BooleanArray> {
        return fromLayout(GLYPH_MATRIX_LAYOUT)
    }

    /**
     * 文字列レイアウトから BooleanArray マスクに変換する。
     * '#' を true、それ以外を false として扱う。
     *
     * テスト用に任意のマスクを定義したい時にも使える。
     */
    fun fromLayout(layout: Array<String>): Array<BooleanArray> {
        require(layout.isNotEmpty()) { "layout is empty" }
        val width = layout[0].length
        require(layout.all { it.length == width }) { "layout rows have inconsistent width" }

        return Array(layout.size) { row ->
            BooleanArray(width) { col ->
                layout[row][col] == '#'
            }
        }
    }

    /**
     * マスクの生存セル数（true の数）を数える（デバッグ・テスト用）。
     */
    fun countCells(mask: Array<BooleanArray>): Int {
        var count = 0
        for (row in mask) {
            for (cell in row) {
                if (cell) count++
            }
        }
        return count
    }
}