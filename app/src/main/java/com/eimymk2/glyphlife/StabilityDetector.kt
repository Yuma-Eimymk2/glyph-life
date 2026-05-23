package com.eimymk2.glyphlife

/**
 * ライフゲームの盤面が「安定状態」に達したかを検出する。
 *
 * 直近 N 世代分の盤面ハッシュを保持し、新しい盤面のハッシュが
 * 履歴のいずれかと一致したら「安定」とみなす。
 *
 * 検出できるパターン:
 * - 静止物（Block, Beehive 等）: 直前の世代と一致
 * - 振動子: 周期 ≤ historySize のもの
 *   - Blinker (周期2), Toad (周期2) → 検出可能
 *   - Pulsar (周期3) → 検出可能
 *   - 周期 > historySize の長周期パターン → 検出不可（呼び出し側でタイムアウト処理）
 *
 * @param historySize 何世代前まで遡って比較するか（SPECでは6）
 */
class StabilityDetector(
    private val historySize: Int = 6,
) {

    init {
        require(historySize >= 1) { "historySize must be >= 1" }
    }

    /**
     * 直近の盤面ハッシュ履歴。
     * インデックス0が最も古く、末尾が最新。
     */
    private val history = ArrayDeque<Int>()

    /**
     * 新しい盤面を追加し、それが過去の履歴と一致するかを判定する。
     *
     * 副作用として、新しい盤面が履歴に追加される。
     * 履歴が historySize を超えたら古いものから削除される。
     *
     * @param grid 現在の盤面
     * @return 直近 historySize 世代の中に同じ盤面があれば true
     */
    fun addAndCheck(grid: Array<IntArray>): Boolean {
        val hash = hashGrid(grid)  // ← 変更

        val isStable = history.contains(hash)

        history.addLast(hash)
        while (history.size > historySize) {
            history.removeFirst()
        }

        return isStable
    }

    /**
     * 盤面の中身ベースでハッシュを計算する。
     *
     * Kotlin の Array<IntArray>.contentDeepHashCode() は IntArray の
     * 参照ハッシュを使ってしまうケースがあるため、自前で実装する。
     */
    private fun hashGrid(grid: Array<IntArray>): Int {
        // 各セルを文字に変換し、文字列のハッシュを使う。
        // 31 * hash + element の合成方式は、構造的に異なる盤面で
        // 偶然同じ値になるケースがあるため避ける。
        val sb = StringBuilder(grid.size * (grid.firstOrNull()?.size ?: 0))
        for (row in grid) {
            for (cell in row) {
                sb.append(if (cell == 1) '1' else '0')
            }
        }
        return sb.toString().hashCode()
    }

    /**
     * 履歴をクリアする。新しいゲーム開始時に呼ぶ。
     */
    fun reset() {
        history.clear()
    }

    /**
     * 現在の履歴サイズ（テスト・デバッグ用）。
     */
    fun historyCount(): Int = history.size
}