package com.eimymk2.glyphlife

import kotlin.random.Random

/**
 * 時刻シードから決定論的に初期パターンを生成する。
 *
 * 同じシード（同じ分）からは同じパターンが生成されるので、
 * 1分以内に複数回起動しても同じ盤面が出る。
 *
 * 配置は mask 内のセルのみ対象。円外（LED が物理的にない場所）には
 * 何も置かない。
 *
 * 案A: シンプルなランダム配置（シャッフル方式）
 * - 円内セル（137個）をリストアップ
 * - シードから疑似乱数で順序をシャッフル
 * - 先頭 N 個 (N = density × 137) を生存にする
 */
object PatternGenerator {

    /**
     * 時刻から決定論的なシード値を生成する。
     *
     * フォーマット: YYYYMMDDHHmm を整数化
     * 例: 2026-05-09 14:30 → 202605091430L
     *
     * 同じ分なら同じシード → 同じパターンが生成される。
     */
    fun seedFromTime(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
    ): Long {
        // 各値を桁シフトで連結。月日時分は 2 桁ずつ確保
        return year.toLong() * 100_000_000L +
                month.toLong() * 1_000_000L +
                day.toLong() * 10_000L +
                hour.toLong() * 100L +
                minute.toLong()
    }

    /**
     * 初期パターンを生成する。
     *
     * @param seed 疑似乱数のシード値
     * @param mask 円形マスク（true なセルが配置候補）
     * @param density 生存セル比率（円内セル数に対する割合、0.0〜1.0）
     * @return 13×13 の盤面。grid[row][col] が 1 なら生存、0 なら死亡
     */
    fun generate(
        seed: Long,
        mask: Array<BooleanArray>,
        density: Double = 0.35,
    ): Array<IntArray> {
        require(mask.isNotEmpty()) { "mask must not be empty" }
        require(density in 0.0..1.0) { "density must be in 0.0..1.0" }

        val height = mask.size
        val width = mask[0].size
        val grid = Array(height) { IntArray(width) }

        // 1. 円内セルの座標リストを作る
        val candidates = mutableListOf<Pair<Int, Int>>()
        for (row in 0 until height) {
            for (col in 0 until width) {
                if (mask[row][col]) {
                    candidates.add(row to col)
                }
            }
        }

        if (candidates.isEmpty()) return grid

        // 2. シードから疑似乱数で順序をシャッフル
        val random = Random(seed)
        val shuffled = candidates.shuffled(random)

        // 3. 先頭 N 個を生存セルに
        val targetCount = (candidates.size * density).toInt()
        for (i in 0 until targetCount) {
            val (row, col) = shuffled[i]
            grid[row][col] = 1
        }

        return grid
    }
}