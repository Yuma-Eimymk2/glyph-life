package com.eimymk2.glyphlife

/**
 * Conway's Game of Life エンジン
 *
 * 責務:
 * - 盤面の保持
 * - 次世代の計算
 * - 全セル死滅の判定
 * - マスク（生存可能領域）の適用
 *
 * 設計:
 * - mask が null なら矩形ライフゲーム（盤面全域が有効）
 * - mask があれば mask[r][c] == false のセルは常に死扱い
 * - 円形マスクは CircularMask.create() で生成可能
 */
class LifeGameEngine(
    val width: Int = 13,
    val height: Int = 13,
    val mask: Array<BooleanArray>? = null,
) {

    init {
        // mask が指定されたらサイズ整合性をチェック
        if (mask != null) {
            require(mask.size == height) { "mask height mismatch" }
            require(mask.all { it.size == width }) { "mask width mismatch" }
        }
    }

    // 現在の盤面。0 = 死, 1 = 生
    private var grid: Array<IntArray> = Array(height) { IntArray(width) }

    /**
     * 盤面を直接セットする。
     * マスクがあれば、円外は強制的に 0 にされる（不正な初期状態を防ぐ）。
     */
    fun setGrid(newGrid: Array<IntArray>) {
        require(newGrid.size == height) { "height mismatch" }
        require(newGrid.all { it.size == width }) { "width mismatch" }

        for (row in 0 until height) {
            for (col in 0 until width) {
                grid[row][col] = if (isAlive(row, col, newGrid)) 1 else 0
            }
        }
    }

    /**
     * 渡された盤面の (row, col) が「生扱いか」を判定。
     * マスク外なら強制的に false。
     */
    private fun isAlive(row: Int, col: Int, source: Array<IntArray>): Boolean {
        if (mask != null && !mask[row][col]) return false
        return source[row][col] == 1
    }

    /**
     * 現在の盤面のコピーを返す。
     */
    fun getGrid(): Array<IntArray> {
        return Array(height) { row -> grid[row].copyOf() }
    }

    /**
     * 1世代進める。
     */
    fun step() {
        val nextGrid = Array(height) { IntArray(width) }

        for (row in 0 until height) {
            for (col in 0 until width) {
                // マスク外は強制的に死
                if (mask != null && !mask[row][col]) {
                    nextGrid[row][col] = 0
                    continue
                }

                val neighbors = countNeighbors(row, col)
                val alive = grid[row][col] == 1

                nextGrid[row][col] = when {
                    alive && (neighbors == 2 || neighbors == 3) -> 1
                    !alive && neighbors == 3 -> 1
                    else -> 0
                }
            }
        }

        grid = nextGrid
    }

    /**
     * (row, col) の周囲8マスの生存セル数を数える。
     * 盤外もマスク外も「死」扱い。
     */
    private fun countNeighbors(row: Int, col: Int): Int {
        var count = 0
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue

                val r = row + dr
                val c = col + dc

                // 盤外チェック
                if (r < 0 || r >= height) continue
                if (c < 0 || c >= width) continue

                // マスク外は死扱いなので隣接にカウントしない
                if (mask != null && !mask[r][c]) continue

                count += grid[r][c]
            }
        }
        return count
    }

    /**
     * 全セル死滅しているか
     */
    fun isExtinct(): Boolean {
        for (row in 0 until height) {
            for (col in 0 until width) {
                if (grid[row][col] == 1) return false
            }
        }
        return true
    }
}