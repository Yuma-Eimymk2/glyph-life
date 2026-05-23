package com.eimymk2.glyphlife

import com.nothing.ketchum.GlyphMatrixManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ライフゲーム終了時の演出アニメーション。
 *
 * 20秒以上生存した「観察に値する旅をした生命」への餞として、
 * 約7-8秒間の儀式的なアニメーションを再生する。
 *
 * 設計参考: 藤子・F・不二雄『ある日...』のラスト
 *  - 白く包まれて静かに終わる
 *  - その白さの中に、新しい生命の予兆が見える
 *
 * 構成:
 *  1. 波フェードイン (1.5秒): ある方向から光が押し寄せる
 *  2. 全点灯フラッシュ (0.3秒): 一瞬の白
 *  3. フェードアウト (1.5秒): 静かに闇へ
 *  4. Glider の旅 (約4-5秒): 中心に新生命が現れ、波と同じ方向へ旅立つ
 *
 * 波の方向と Glider の旅立ち先は同期する。
 */
object EndingAnimation {

    /**
     * 流れる方向。波と Glider の旅立ち先で共有される。
     * 波は「この方向へ向かって流れる」、Glider は「この方向へ進む」。
     */
    enum class FlowDirection {
        NE,  // 北東 (右上)
        SE,  // 南東 (右下)
        SW,  // 南西 (左下)
        NW,  // 北西 (左上)
    }

    // 13×13 グリッドの中心
    private const val CENTER_ROW = 6
    private const val CENTER_COL = 6

    // 波の射影の最大値 (-12 〜 +12 の範囲)
    private const val MAX_PROJECTION = 6.5

    // 中心からの最大距離（円の半径）
    private const val MAX_DISTANCE_FROM_CENTER = 6.5

    // 波の幅（投影座標系での幅。値が大きいほど波が広がって見える）
    private const val WAVE_WIDTH = 6.0

    // 1フレームのインターバル（ms）20 FPS
    private const val FRAME_INTERVAL_MS = 50L

    // Glider 進化の時間間隔（5 FPS）
    private const val GLIDER_INTERVAL_MS = 200L

    // Glider の最大進化世代
    private const val MAX_GLIDER_GENERATIONS = 20

    /**
     * 各方向の Glider パターン。
     * 中心からの相対座標 (dRow, dCol) のリスト。
     *
     * Glider はライフゲームで4種類の Phase × 4 方向の派生形を持つが、
     * ここでは Phase 0 を4方向分定義する。
     */
    private val GLIDER_PATTERNS: Map<FlowDirection, List<Pair<Int, Int>>> = mapOf(
        // 南東向き (デフォルト):
        // . X .
        // . . X
        // X X X
        FlowDirection.SE to listOf(
            Pair(-1, 0), Pair(0, 1), Pair(1, -1), Pair(1, 0), Pair(1, 1)
        ),
        // 北東向き (90度反時計回り):
        // X . .
        // X . X
        // X X .
        FlowDirection.NE to listOf(
            Pair(-1, -1), Pair(0, -1), Pair(0, 1), Pair(1, -1), Pair(1, 0)
        ),
        // 南西向き (90度時計回り):
        // X X X
        // X . .
        // . X .
        FlowDirection.SW to listOf(
            Pair(-1, -1), Pair(-1, 0), Pair(-1, 1), Pair(0, -1), Pair(1, 0)
        ),
        // 北西向き (180度回転):
        // . X X
        // X . X
        // . . X
        FlowDirection.NW to listOf(
            Pair(-1, 0), Pair(-1, 1), Pair(0, -1), Pair(0, 1), Pair(1, 1)
        ),
    )

    /**
     * 終了演出シーケンスを再生する。
     *
     * @param flowDirection 波と Glider の流れる方向（デフォルト: 南東）
     */
    suspend fun runEndingSequence(
        glyphMatrixManager: GlyphMatrixManager,
        uiScope: CoroutineScope,
        brightness: Int,
        flowDirection: FlowDirection = FlowDirection.SE,
    ) {
        // 1. 全LED 一瞬光る (1秒)
        runWhiteFlash(glyphMatrixManager, uiScope, brightness, 1500L)

        // 2. 外側から徐々に消える (1.5秒)
        runRadialFadeOut(glyphMatrixManager, uiScope, brightness, 1500L)

        // 3. 真っ暗な静寂 (1秒)
        delay(1500L)

        // 4. Glider が中心に出現して旅立つ
        runGliderJourney(glyphMatrixManager, uiScope, brightness, flowDirection)
    }

    /**
     * 波フェードイン: 指定方向に光の波が流れる。
     *
     * 波は最先端が明るく、後方に行くほど暗くなり、最終的に消える。
     * 全体としては「光の帯が流れていく」ような表現。
     */
    suspend fun runWaveFadeIn(
        glyphMatrixManager: GlyphMatrixManager,
        uiScope: CoroutineScope,
        brightness: Int,
        durationMs: Long,
        direction: FlowDirection,
    ) {
        val frameCount = (durationMs / FRAME_INTERVAL_MS).toInt()
        val mask = CircularMask.createGlyphMatrix()

        for (frame in 0 until frameCount) {
            val t = frame.toDouble() / frameCount.toDouble()

            // 波の最先端の位置: -MAX_PROJECTION → +MAX_PROJECTION + WAVE_WIDTH へ
            // (波が完全に画面外まで抜けるように余裕を持たせる)
            val waveFront = (t * (MAX_PROJECTION * 2 + WAVE_WIDTH) - MAX_PROJECTION)

            val grid = Array(13) { IntArray(13) }
            // 13×13 grid に対し、各セルの輝度を直接計算するため、
            // BinaryGrid (0/1) ではなく ledFrame (0-255) を作る
            val ledFrame = IntArray(13 * 13)

            for (row in 0 until 13) {
                for (col in 0 until 13) {
                    if (!mask[row][col]) continue

                    val projection = projectionForDirection(row, col, direction)
                    // 波の最先端からの距離 (負の値: 通過済み)
                    val distanceFromFront = projection - waveFront

                    val cellBrightness = when {
                        // まだ波が来ていない (波の前方)
                        distanceFromFront > 0 -> 0
                        // 波の中、または波が通過した直後
                        distanceFromFront >= -WAVE_WIDTH -> {
                            // 波の中: 最先端ほど明るく、後方ほど暗い
                            // distanceFromFront = 0 (最先端) → brightness
                            // distanceFromFront = -WAVE_WIDTH (後端) → 0
                            val ratio = 1.0 - (-distanceFromFront / WAVE_WIDTH)
                            (ratio * brightness).toInt()
                        }
                        // 波が通り過ぎた後 → 消灯
                        else -> 0
                    }

                    if (cellBrightness > 0) {
                        ledFrame[row * 13 + col] = cellBrightness
                    }
                }
            }

            // セル毎の輝度をそのまま表示するため、専用の送信関数を使う
            sendVariableBrightness(glyphMatrixManager, uiScope, ledFrame)
            delay(FRAME_INTERVAL_MS)
        }
    }

    /**
     * 外側から徐々に光が消えていく。
     *
     * 中心からの距離が大きいセル（外側）から先に消灯する。
     * 全体としては「光が中心に向かって引いていく」表現。
     */
    suspend fun runRadialFadeOut(
        glyphMatrixManager: GlyphMatrixManager,
        uiScope: CoroutineScope,
        brightness: Int,
        durationMs: Long,
    ) {
        val frameCount = (durationMs / FRAME_INTERVAL_MS).toInt()
        val mask = CircularMask.createGlyphMatrix()

        for (frame in 0 until frameCount) {
            val t = frame.toDouble() / frameCount.toDouble()

            // 閾値: 6.5 (最大距離) → 0 へ動く
            // t=0: 全部点灯 (距離 < 6.5 のセルが全部点灯)
            // t=1: 全部消灯
            val threshold = (1.0 - t) * MAX_DISTANCE_FROM_CENTER

            val grid = Array(13) { IntArray(13) }
            for (row in 0 until 13) {
                for (col in 0 until 13) {
                    if (!mask[row][col]) continue

                    val distance = distanceFromCenter(row, col)
                    // 中心に近いセル (距離 < 閾値) は点灯したまま
                    if (distance < threshold) {
                        grid[row][col] = 1
                    }
                }
            }

            sendGrid(glyphMatrixManager, uiScope, grid, brightness)
            delay(FRAME_INTERVAL_MS)
        }

        sendEmpty(glyphMatrixManager, uiScope)
    }

    /**
     * 全点灯フラッシュ: 一瞬の白。
     */
    suspend fun runWhiteFlash(
        glyphMatrixManager: GlyphMatrixManager,
        uiScope: CoroutineScope,
        brightness: Int,
        durationMs: Long,
    ) {
        val mask = CircularMask.createGlyphMatrix()
        val grid = Array(13) { row ->
            IntArray(13) { col -> if (mask[row][col]) 1 else 0 }
        }
        sendGrid(glyphMatrixManager, uiScope, grid, brightness)
        delay(durationMs)
    }

    /**
     * フェードアウト: 全LED の輝度を徐々に落としていく。
     */
    suspend fun runFadeOut(
        glyphMatrixManager: GlyphMatrixManager,
        uiScope: CoroutineScope,
        brightness: Int,
        durationMs: Long,
    ) {
        val frameCount = (durationMs / FRAME_INTERVAL_MS).toInt()
        val mask = CircularMask.createGlyphMatrix()
        val grid = Array(13) { row ->
            IntArray(13) { col -> if (mask[row][col]) 1 else 0 }
        }

        for (frame in 0 until frameCount) {
            val t = frame.toDouble() / frameCount.toDouble()
            val currentBrightness = ((1.0 - t) * brightness).toInt()
            sendGrid(glyphMatrixManager, uiScope, grid, currentBrightness)
            delay(FRAME_INTERVAL_MS)
        }

        sendEmpty(glyphMatrixManager, uiScope)
    }

    /**
     * Glider の旅: 指定方向に進む Glider が中心に出現し、円端で消える。
     */
    suspend fun runGliderJourney(
        glyphMatrixManager: GlyphMatrixManager,
        uiScope: CoroutineScope,
        brightness: Int,
        direction: FlowDirection,
    ) {
        val mask = CircularMask.createGlyphMatrix()
        val engine = LifeGameEngine(width = 13, height = 13, mask = mask)
        engine.setGrid(createGliderGrid(direction))

        // 1. フェードイン (0.5秒): Glider が中心に淡く出現
        val emergeFrames = (500L / GLIDER_INTERVAL_MS).toInt()
        for (frame in 0 until emergeFrames) {
            val t = frame.toDouble() / emergeFrames.toDouble()
            val currentBrightness = (t * brightness).toInt()
            sendGrid(glyphMatrixManager, uiScope, engine.getGrid(), currentBrightness)
            delay(GLIDER_INTERVAL_MS)
        }

        // 2. 旅: Glider がライフゲームで進化（最大 N 世代、または絶滅まで）
        for (gen in 0 until MAX_GLIDER_GENERATIONS) {
            engine.step()
            if (engine.isExtinct()) break
            sendGrid(glyphMatrixManager, uiScope, engine.getGrid(), brightness)
            delay(GLIDER_INTERVAL_MS)
        }

        // 3. 余韻
        delay(300L)

        // 4. 消灯
        sendEmpty(glyphMatrixManager, uiScope)
    }

    /**
     * 指定方向の Glider を中心 (6, 6) に配置した 13×13 grid を作る。
     */
    private fun createGliderGrid(direction: FlowDirection): Array<IntArray> {
        val grid = Array(13) { IntArray(13) }
        val cells = GLIDER_PATTERNS[direction] ?: return grid

        for ((dRow, dCol) in cells) {
            val row = CENTER_ROW + dRow
            val col = CENTER_COL + dCol
            if (row in 0..12 && col in 0..12) {
                grid[row][col] = 1
            }
        }
        return grid
    }

    /**
     * セル (row, col) の、指定方向への射影を計算する。
     *
     * 中心 (6, 6) を基準とした相対座標から計算。
     * 値が小さいほど方向の「起点側」、大きいほど「方向の先」。
     *
     * 例: NE 方向の場合、北東のセルほど大きい値を返す。
     *     → 波が流れる順序は「小さい値 → 大きい値」 = 「南西 → 北東」
     */
    private fun projectionForDirection(
        row: Int,
        col: Int,
        direction: FlowDirection,
    ): Double {
        val dRow = (row - CENTER_ROW).toDouble()
        val dCol = (col - CENTER_COL).toDouble()
        return when (direction) {
            // 北東 = 行が小さく、列が大きい方向 → -dRow + dCol
            FlowDirection.NE -> -dRow + dCol
            // 南東 = 行が大きく、列が大きい方向 → dRow + dCol
            FlowDirection.SE -> dRow + dCol
            // 南西 = 行が大きく、列が小さい方向 → dRow - dCol
            FlowDirection.SW -> dRow - dCol
            // 北西 = 行が小さく、列が小さい方向 → -dRow - dCol
            FlowDirection.NW -> -dRow - dCol
        }
    }

    /**
     * (row, col) から中心 (6, 6) までのユークリッド距離。
     */
    private fun distanceFromCenter(row: Int, col: Int): Double {
        val dx = (col - CENTER_COL).toDouble()
        val dy = (row - CENTER_ROW).toDouble()
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    /**
     * 13×13 grid を 25×25 にスケーリングして送信。
     */
    private fun sendGrid(
        glyphMatrixManager: GlyphMatrixManager,
        uiScope: CoroutineScope,
        grid: Array<IntArray>,
        brightness: Int,
    ) {
        val outputFrame = GlyphRenderer.render(grid, brightness)
        uiScope.launch {
            glyphMatrixManager.setMatrixFrame(outputFrame)
        }
    }

    /**
     * 13×13 のセル毎輝度配列 (0-255) を 25×25 にスケーリングして送信。
     * 通常の sendGrid と違い、セル毎に異なる輝度を扱える。
     */
    private fun sendVariableBrightness(
        glyphMatrixManager: GlyphMatrixManager,
        uiScope: CoroutineScope,
        ledFrame: IntArray,  // size = 169
    ) {
        // 13×13 のセル毎輝度 → 25×25 にスケーリング
        val outputFrame = IntArray(25 * 25)
        for (outRow in 0 until 25) {
            for (outCol in 0 until 25) {
                val gameRow = outRow * 13 / 25
                val gameCol = outCol * 13 / 25
                outputFrame[outRow * 25 + outCol] = ledFrame[gameRow * 13 + gameCol]
            }
        }

        uiScope.launch {
            glyphMatrixManager.setMatrixFrame(outputFrame)
        }
    }

    /**
     * 完全消灯フレームを送信。
     */
    private fun sendEmpty(
        glyphMatrixManager: GlyphMatrixManager,
        uiScope: CoroutineScope,
    ) {
        val emptyFrame = IntArray(25 * 25)
        uiScope.launch {
            glyphMatrixManager.setMatrixFrame(emptyFrame)
        }
    }
}