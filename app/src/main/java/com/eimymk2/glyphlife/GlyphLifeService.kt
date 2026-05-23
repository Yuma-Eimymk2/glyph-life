package com.eimymk2.glyphlife

import android.content.Context
import android.util.Log
import com.nothing.ketchum.GlyphMatrixManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.coroutines.coroutineContext

/**
 * Glyph Life の Toy Service。
 *
 * onBind されると以下を繰り返す:
 *  1. ライフゲームを1サイクル実行 (最大60秒、停止条件で早期終了あり)
 *  2. 20秒以上生存していれば演出を再生
 *  3. ランダム時間待機 (テスト中: 30秒-1分、本番: 3-7分)
 *  4. 1に戻る
 *
 * onUnbind されると上記ループは中断される。
 * 次に伏せた時は新しい bind で1から始まる。
 */
class GlyphLifeService : GlyphMatrixService("Glyph-Life") {

    private val backgroundScope = CoroutineScope(Dispatchers.IO)
    private val uiScope = CoroutineScope(Dispatchers.Main)

    override fun performOnServiceConnected(
        context: Context,
        glyphMatrixManager: GlyphMatrixManager
    ) {
        Log.d(TAG, "performOnServiceConnected: starting cycle loop")

        backgroundScope.launch {
            var cycleNumber = 0
            while (isActive) {
                cycleNumber++
                Log.d(TAG, "==== cycle $cycleNumber start ====")

                // 1サイクル実行
                runOneLifeGameCycle(glyphMatrixManager)

                // 待機時間をランダムに決定
                if (!isActive) break  // 中断チェック
                val waitMs = (MIN_WAIT_MS..MAX_WAIT_MS).random()
                Log.d(TAG, "next cycle in ${waitMs}ms (${waitMs / 1000}s)")
                delay(waitMs)
            }
            Log.d(TAG, "cycle loop ended")
        }
    }

    /**
     * ライフゲームを1サイクル実行する。
     * - 時刻シードでパターン生成
     * - 最大60秒またはアプリ側停止条件まで進化させる
     * - 20秒以上生存していたら演出を再生
     */
    private suspend fun runOneLifeGameCycle(glyphMatrixManager: GlyphMatrixManager) {
        // 円形マスク
        val mask = CircularMask.createGlyphMatrix()

        // 現在時刻からシードを作って初期パターン生成
        val seed = currentTimeSeed()
        Log.d(TAG, "seed=$seed, density=$DENSITY")
        val initialGrid = PatternGenerator.generate(
            seed = seed,
            mask = mask,
            density = DENSITY
        )

        // ライフゲームエンジン
        val engine = LifeGameEngine(width = 13, height = 13, mask = mask)
        engine.setGrid(initialGrid)

        // 安定検出器
        val stabilityDetector = StabilityDetector()

        // 描画ループ
        val startTime = System.currentTimeMillis()
        var generation = 0
        var stopReason: String? = null

        // suspend関数内なので isActive チェックには coroutineContext を使う
        while (coroutineContext[kotlinx.coroutines.Job]?.isActive == true) {
            // 1) 現盤面を Glyph Matrix に表示
            val frame = GlyphRenderer.render(engine.getGrid(), BRIGHTNESS)
            uiScope.launch {
                glyphMatrixManager.setMatrixFrame(frame)
            }

            // 2) 停止条件チェック
            val elapsedMs = System.currentTimeMillis() - startTime

            if (elapsedMs >= MAX_DURATION_MS) {
                stopReason = "timeout (${elapsedMs}ms)"
                break
            }
            if (engine.isExtinct()) {
                stopReason = "extinct at gen=$generation"
                break
            }
            if (stabilityDetector.addAndCheck(engine.getGrid())) {
                stopReason = "stable at gen=$generation"
                break
            }

            if (generation % 10 == 0) {
                Log.d(TAG, "generation=$generation, elapsed=${elapsedMs}ms")
            }

            // 3) 次世代へ
            delay(GENERATION_INTERVAL_MS)
            engine.step()
            generation++
        }

        Log.d(TAG, "render loop stopped: $stopReason")

        // 演出または静かな消灯
        val elapsedMs = System.currentTimeMillis() - startTime
        if (elapsedMs >= GRACEFUL_ENDING_THRESHOLD_MS) {
            Log.d(TAG, "playing ending animation (lived ${elapsedMs}ms)")
            val direction = directionFromSeed(seed)
            Log.d(TAG, "ending direction: $direction")
            EndingAnimation.runEndingSequence(
                glyphMatrixManager = glyphMatrixManager,
                uiScope = uiScope,
                brightness = BRIGHTNESS,
                flowDirection = direction,
            )
        } else {
            Log.d(TAG, "no ending animation (only ${elapsedMs}ms)")
            uiScope.launch {
                val emptyFrame = IntArray(OUTPUT_FRAME_SIZE)
                glyphMatrixManager.setMatrixFrame(emptyFrame)
            }
        }
    }

    override fun performOnServiceDisconnected(context: Context) {
        Log.d(TAG, "performOnServiceDisconnected: cancelling render loop")
        backgroundScope.cancel()
    }

    /**
     * 現在時刻から YYYYMMDDHHmm 形式のシードを作る。
     */
    private fun currentTimeSeed(): Long {
        val cal = Calendar.getInstance()
        return PatternGenerator.seedFromTime(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH) + 1,
            day = cal.get(Calendar.DAY_OF_MONTH),
            hour = cal.get(Calendar.HOUR_OF_DAY),
            minute = cal.get(Calendar.MINUTE),
        )
    }

    /**
     * シード値から決定論的に演出の方向を選ぶ。
     */
    private fun directionFromSeed(seed: Long): EndingAnimation.FlowDirection {
        val directions = EndingAnimation.FlowDirection.values()
        val index = ((seed % directions.size) + directions.size) % directions.size
        return directions[index.toInt()]
    }

    private companion object {
        private const val TAG = "GlyphLifeService"
        private const val GENERATION_INTERVAL_MS = 500L
        private const val BRIGHTNESS = 153
        private const val DENSITY = 0.35

        // 停止条件
        private const val MAX_DURATION_MS = 56_000L  // 演出込みで約60秒
        private const val GRACEFUL_ENDING_THRESHOLD_MS = 20_000L

        // 出力配列サイズ
        private const val OUTPUT_FRAME_SIZE = 25 * 25

        // ★サイクル間の待機時間
        // テスト中: 短め
        private const val MIN_WAIT_MS = 30_000L   // 30秒
        private const val MAX_WAIT_MS = 60_000L   // 1分
        // 本番に切り替える時はこちら:
        // private const val MIN_WAIT_MS = 3 * 60_000L   // 3分
        // private const val MAX_WAIT_MS = 7 * 60_000L   // 7分
    }
}