package com.eimymk2.glyphlife

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * StabilityDetector の単体テスト
 *
 * 「ライフゲームの典型的な安定/振動パターンで、
 * 安定検出が正しく発火するか」を検証する。
 */
class StabilityDetectorTest {

    /**
     * テスト用ヘルパー: 文字列で盤面を作る。
     */
    private fun gridOf(vararg rows: String): Array<IntArray> {
        return Array(rows.size) { r ->
            IntArray(rows[r].length) { c ->
                if (rows[r][c] == 'X') 1 else 0
            }
        }
    }

    // ============================================================
    // 基本動作のテスト
    // ============================================================

    @Test
    fun `初回の盤面追加では安定と判定されない`() {
        val detector = StabilityDetector()
        val grid = gridOf("X.", ".X")
        assertFalse(detector.addAndCheck(grid))
    }

    @Test
    fun `同じ盤面を2回連続で追加すると2回目で安定判定`() {
        val detector = StabilityDetector()
        val grid = gridOf("X.", ".X")

        assertFalse("初回は false", detector.addAndCheck(grid))
        assertTrue("2回目は true", detector.addAndCheck(grid))
    }

    @Test
    fun `異なる盤面は安定と判定されない`() {
        val detector = StabilityDetector()
        val grid1 = gridOf("X.", ".X")
        val grid2 = gridOf(".X", "X.")

        assertFalse(detector.addAndCheck(grid1))
        assertFalse(detector.addAndCheck(grid2))
    }

    @Test
    fun `reset 後は履歴がクリアされる`() {
        val detector = StabilityDetector()
        val grid = gridOf("X.", ".X")

        detector.addAndCheck(grid)
        detector.reset()
        assertEquals(0, detector.historyCount())

        // reset 後の初回追加は false（新規扱い）
        assertFalse(detector.addAndCheck(grid))
    }

    // ============================================================
    // ライフゲームの典型パターンのテスト
    // ============================================================

    @Test
    fun `静止物 Block の連続検出`() {
        // ライフゲームで Block を1世代進めても変化しない
        // 同じ盤面が連続して投入されるシナリオを模擬
        val detector = StabilityDetector()
        val block = gridOf(
            "....",
            ".XX.",
            ".XX.",
            "....",
        )

        assertFalse("世代0は新規", detector.addAndCheck(block))
        assertTrue("世代1で前世代と一致", detector.addAndCheck(block))
        assertTrue("世代2でも一致", detector.addAndCheck(block))
    }

    @Test
    fun `振動子 Blinker 周期2 の検出`() {
        // 縦と横を交互に追加するシナリオ
        val detector = StabilityDetector()
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

        assertFalse("世代0", detector.addAndCheck(vertical))
        assertFalse("世代1", detector.addAndCheck(horizontal))
        assertTrue("世代2でvertical再登場、世代0と一致", detector.addAndCheck(vertical))
        assertTrue("世代3でhorizontal再登場、世代1と一致", detector.addAndCheck(horizontal))
    }

    // ============================================================
    // 履歴サイズの境界テスト
    // ============================================================

    @Test
    fun `履歴サイズを超えた古いパターンは検出されない`() {
        // historySize=2 なら、3世代前のパターンは「見えない」はず
        val detector = StabilityDetector(historySize = 2)
        val a = gridOf("X.", "..")
        val b = gridOf(".X", "..")
        val c = gridOf("..", "X.")

        detector.addAndCheck(a)  // 履歴: [a]
        detector.addAndCheck(b)  // 履歴: [a, b]
        detector.addAndCheck(c)  // 履歴: [b, c]（aは追い出された）

        // 再度 a を追加 → 履歴に a はないので false
        assertFalse("古すぎる履歴は検出されない", detector.addAndCheck(a))
    }

    @Test
    fun `historySize 1 でも動作する`() {
        // 直前のみと比較するモード
        val detector = StabilityDetector(historySize = 1)
        val grid = gridOf("X.", ".X")

        assertFalse(detector.addAndCheck(grid))
        assertTrue("直前と一致", detector.addAndCheck(grid))
    }

    @Test
    fun `historySize が 0 以下だとエラー`() {
        try {
            StabilityDetector(historySize = 0)
            assert(false) { "例外が投げられるはず" }
        } catch (e: IllegalArgumentException) {
            // 期待通り
        }
    }

    // ============================================================
    // LifeGameEngine との結合テスト
    // ============================================================

    @Test
    fun `Block を LifeGameEngine で進めて安定検出`() {
        // 実際のライフゲーム挙動と組み合わせる
        val engine = LifeGameEngine(width = 4, height = 4)
        engine.setGrid(
            gridOf(
                "....",
                ".XX.",
                ".XX.",
                "....",
            )
        )
        val detector = StabilityDetector()

        // 1世代目で履歴に追加（安定とは判定されない）
        assertFalse(detector.addAndCheck(engine.getGrid()))
        engine.step()

        // 2世代目: Block は変化しないので前世代と一致 → 安定
        assertTrue(detector.addAndCheck(engine.getGrid()))
    }

    @Test
    fun `Blinker を LifeGameEngine で進めて周期2検出`() {
        val engine = LifeGameEngine(width = 5, height = 5)
        engine.setGrid(
            gridOf(
                ".....",
                "..X..",
                "..X..",
                "..X..",
                ".....",
            )
        )
        val detector = StabilityDetector()

        // 世代0: 縦
        assertFalse(detector.addAndCheck(engine.getGrid()))
        engine.step()

        // 世代1: 横
        assertFalse(detector.addAndCheck(engine.getGrid()))
        engine.step()

        // 世代2: 縦に戻る → 世代0と一致 → 安定検出
        assertTrue(detector.addAndCheck(engine.getGrid()))
    }

    @Test
    fun `デバッグ用 ハッシュ確認`() {
        val a = gridOf(
            ".....",
            "..X..",
            "..X..",
            "..X..",
            ".....",
        )
        val b = gridOf(
            ".....",
            ".....",
            ".XXX.",
            ".....",
            ".....",
        )
        // 注: contentDeepHashCode() は罠があったので、実装は自前ハッシュに変更済み
        // ここでは StabilityDetector の挙動で間接確認する
        val detector = StabilityDetector()
        detector.addAndCheck(a)
        val bResult = detector.addAndCheck(b)
        println("a と b は別盤面と認識される? ${!bResult}")
        assertFalse("a と b は別の盤面なのに安定と判定されてはいけない", bResult)
    }

    @Test
    fun `デバッグ用2 自前ハッシュ確認`() {
        val a = gridOf(
            ".....",
            "..X..",
            "..X..",
            "..X..",
            ".....",
        )
        val b = gridOf(
            ".....",
            ".....",
            ".XXX.",
            ".....",
            ".....",
        )

        // 各行を Int 配列として直接ダンプ
        println("=== a の中身 ===")
        for ((i, row) in a.withIndex()) {
            println("行$i: ${row.toList()} hash=${row.contentHashCode()}")
        }
        println("=== b の中身 ===")
        for ((i, row) in b.withIndex()) {
            println("行$i: ${row.toList()} hash=${row.contentHashCode()}")
        }

        // 自前ハッシュ計算を再現
        var hashA = 1
        for (row in a) hashA = 31 * hashA + row.contentHashCode()
        var hashB = 1
        for (row in b) hashB = 31 * hashB + row.contentHashCode()

        println("hashA = $hashA")
        println("hashB = $hashB")
        println("等しい? ${hashA == hashB}")
    }
}