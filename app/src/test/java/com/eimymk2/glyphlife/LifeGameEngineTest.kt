package com.eimymk2.glyphlife

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * LifeGameEngine の単体テスト
 *
 * 「Conway's Game of Life の有名なパターンを置いて、
 * step() を呼んだ後の盤面が期待通りか」を検証する。
 */
class LifeGameEngineTest {

    /**
     * テスト用ヘルパー: 文字列で盤面を書けるようにする。
     * '.' = 死, 'X' = 生
     */
    private fun gridOf(vararg rows: String): Array<IntArray> {
        return Array(rows.size) { r ->
            IntArray(rows[r].length) { c ->
                if (rows[r][c] == 'X') 1 else 0
            }
        }
    }

    /**
     * 2つの盤面が等しいか判定するヘルパー。
     */
    private fun assertGridEquals(expected: Array<IntArray>, actual: Array<IntArray>) {
        assertEquals("行数が違う", expected.size, actual.size)
        for (r in expected.indices) {
            assertEquals("行 $r の列数が違う", expected[r].size, actual[r].size)
            for (c in expected[r].indices) {
                assertEquals(
                    "セル ($r, $c) が違う",
                    expected[r][c],
                    actual[r][c]
                )
            }
        }
    }

    // ============================================================
    // ライフゲーム本体のテスト
    // ============================================================

    @Test
    fun `空の盤面はずっと空のまま`() {
        val engine = LifeGameEngine(width = 5, height = 5)
        engine.step()
        assertTrue("空盤面なら絶滅判定", engine.isExtinct())
    }

    @Test
    fun `孤立した1セルは次世代で死ぬ`() {
        val engine = LifeGameEngine(width = 5, height = 5)
        engine.setGrid(
            gridOf(
                ".....",
                ".....",
                "..X..",
                ".....",
                ".....",
            )
        )
        engine.step()
        assertTrue("隣接0なら死ぬ", engine.isExtinct())
    }

    @Test
    fun `Block は静止物として変化しない`() {
        // Block: 2x2の塊。生存セル各3隣接で永遠に安定
        val initial = gridOf(
            "....",
            ".XX.",
            ".XX.",
            "....",
        )
        val engine = LifeGameEngine(width = 4, height = 4)
        engine.setGrid(initial)

        engine.step()
        assertGridEquals(initial, engine.getGrid())

        // 2世代進めても変わらない
        engine.step()
        assertGridEquals(initial, engine.getGrid())
    }

    @Test
    fun `Blinker は周期2で振動する`() {
        // 縦3つ → 横3つ → 縦3つ ...
        val vertical = gridOf(
            ".....",
            "..X..",
            "..X..",
            "..X..",
            ".....",
        )
        val horizontal = gridOf(
            ".....",
            ".....",
            ".XXX.",
            ".....",
            ".....",
        )

        val engine = LifeGameEngine(width = 5, height = 5)
        engine.setGrid(vertical)

        engine.step()
        assertGridEquals(horizontal, engine.getGrid())

        engine.step()
        assertGridEquals(vertical, engine.getGrid())
    }

    @Test
    fun `Glider は4世代で1マス斜めに進む`() {
        // Glider: 5世代周期で斜め移動する有名パターン
        val gen0 = gridOf(
            "........",
            "..X.....",
            "...X....",
            ".XXX....",
            "........",
            "........",
            "........",
            "........",
        )
        // 4世代後、Glider は元の形のまま (1, 1) 移動しているはず
        val gen4 = gridOf(
            "........",
            "........",
            "...X....",
            "....X...",
            "..XXX...",
            "........",
            "........",
            "........",
        )

        val engine = LifeGameEngine(width = 8, height = 8)
        engine.setGrid(gen0)
        repeat(4) { engine.step() }

        assertGridEquals(gen4, engine.getGrid())
    }

    @Test
    fun `境界外は死として扱われる`() {
        // 左上角に1セル置いただけ。周囲8マス中5マスは盤外（=死扱い）
        // 残り3マス（右、下、右下）が全て死なので、隣接0、次世代で死ぬ
        val engine = LifeGameEngine(width = 5, height = 5)
        engine.setGrid(
            gridOf(
                "X....",
                ".....",
                ".....",
                ".....",
                ".....",
            )
        )
        engine.step()
        assertTrue("孤立した角セルは死ぬ", engine.isExtinct())
    }

    @Test
    fun `デフォルトサイズは13x13`() {
        val engine = LifeGameEngine()
        assertEquals(13, engine.width)
        assertEquals(13, engine.height)
    }

    @Test
    fun `getGrid は内部状態のコピーを返す（防御的コピー）`() {
        val engine = LifeGameEngine(width = 3, height = 3)
        engine.setGrid(
            gridOf(
                "XXX",
                "...",
                "...",
            )
        )
        val snapshot = engine.getGrid()
        // 取得した配列を改変
        snapshot[0][0] = 0
        // 内部は影響を受けないはず
        assertEquals(1, engine.getGrid()[0][0])
    }

    // ============================================================
    // 円形マスクのテスト
    // ============================================================

    @Test
    fun `マスク外のセルに生を置いてもsetGridで消される`() {
        // 4x4 の左上だけ True、右下だけ False のマスク
        val mask = arrayOf(
            booleanArrayOf(true,  true,  false, false),
            booleanArrayOf(true,  true,  false, false),
            booleanArrayOf(false, false, false, false),
            booleanArrayOf(false, false, false, false),
        )
        val engine = LifeGameEngine(width = 4, height = 4, mask = mask)

        engine.setGrid(
            gridOf(
                "XXXX",  // 右2マスはマスク外なので無効化される
                "XXXX",
                "XXXX",
                "XXXX",
            )
        )

        // 取得した盤面: マスク内（左上 2×2）だけ生のはず
        val expected = gridOf(
            "XX..",
            "XX..",
            "....",
            "....",
        )
        assertGridEquals(expected, engine.getGrid())
    }

    @Test
    fun `マスク外のセルはstep後も常に死のまま`() {
        // 全マスク内、ただし右下角だけマスク外
        val mask = Array(5) { row ->
            BooleanArray(5) { col ->
                !(row == 4 && col == 4)
            }
        }
        val engine = LifeGameEngine(width = 5, height = 5, mask = mask)
        engine.setGrid(
            gridOf(
                "XXX..",
                "XXX..",
                "XXX..",
                ".....",
                ".....",
            )
        )

        // 何世代進めても (4,4) は死のまま
        repeat(10) { engine.step() }
        assertEquals(0, engine.getGrid()[4][4])
    }

    @Test
    fun `13x13円形マスクは正確に137セルを持つ`() {
        val mask = CircularMask.createGlyphMatrix()
        val cells = CircularMask.countCells(mask)
        assertEquals("LED 数は SPEC 通り 137 個のはず", 137, cells)
    }

    @Test
    fun `円形マスクは中心対称`() {
        val mask = CircularMask.createGlyphMatrix()
        for (row in 0 until 13) {
            for (col in 0 until 13) {
                // (row, col) が円内なら (12-row, 12-col) も円内のはず
                assertEquals(
                    "対称性違反: ($row,$col) と (${12-row},${12-col})",
                    mask[row][col],
                    mask[12 - row][12 - col]
                )
            }
        }
    }

    @Test
    fun `Glyph Matrix のレイアウトは13x13`() {
        val mask = CircularMask.createGlyphMatrix()
        assertEquals(13, mask.size)
        for ((rowIdx, row) in mask.withIndex()) {
            assertEquals("行 $rowIdx の幅", 13, row.size)
        }
    }

    @Test
    fun `各行の生セル数は SPEC 通り`() {
        val mask = CircularMask.createGlyphMatrix()
        val expectedPerRow = listOf(5, 9, 11, 11, 13, 13, 13, 13, 13, 11, 11, 9, 5)
        for (row in 0 until 13) {
            val count = mask[row].count { it }
            assertEquals("行 $row のセル数", expectedPerRow[row], count)
        }
    }

    @Test
    fun `Blinkerは円形マスク内でも振動する`() {
        // 円の中心に Blinker を置く
        val mask = CircularMask.createGlyphMatrix()
        val engine = LifeGameEngine(width = 13, height = 13, mask = mask)

        // 中心 (6, 6) を中心に縦の Blinker
        val initial = Array(13) { IntArray(13) }
        initial[5][6] = 1
        initial[6][6] = 1
        initial[7][6] = 1
        engine.setGrid(initial)

        engine.step()
        // 横向きになっているはず
        assertEquals(1, engine.getGrid()[6][5])
        assertEquals(1, engine.getGrid()[6][6])
        assertEquals(1, engine.getGrid()[6][7])

        engine.step()
        // 縦に戻る
        assertEquals(1, engine.getGrid()[5][6])
        assertEquals(1, engine.getGrid()[6][6])
        assertEquals(1, engine.getGrid()[7][6])
    }

    @Test
    fun `デバッグ マスクを目視確認`() {
        val mask = CircularMask.createGlyphMatrix()
        println()
        println("=== Glyph Matrix LED Layout ===")
        for (row in mask) {
            for (cell in row) {
                print(if (cell) "● " else "  ")
            }
            println()
        }
        println("===============================")
        println("Total LEDs: ${CircularMask.countCells(mask)}")
    }
}