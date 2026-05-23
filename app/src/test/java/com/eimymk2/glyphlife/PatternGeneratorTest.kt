package com.eimymk2.glyphlife

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class PatternGeneratorTest {

    @Test
    fun `seedFromTime - 同じ時刻なら同じシード`() {
        val seed1 = PatternGenerator.seedFromTime(2026, 5, 9, 14, 30)
        val seed2 = PatternGenerator.seedFromTime(2026, 5, 9, 14, 30)
        assertEquals(seed1, seed2)
    }

    @Test
    fun `seedFromTime - 1分違うとシードが違う`() {
        val seed1 = PatternGenerator.seedFromTime(2026, 5, 9, 14, 30)
        val seed2 = PatternGenerator.seedFromTime(2026, 5, 9, 14, 31)
        assertNotEquals(seed1, seed2)
    }

    @Test
    fun `seedFromTime - 期待されるフォーマット YYYYMMDDHHmm`() {
        val seed = PatternGenerator.seedFromTime(2026, 5, 9, 14, 30)
        assertEquals(202605091430L, seed)
    }

    @Test
    fun `generate - 同じシードなら同じパターン`() {
        val mask = CircularMask.createGlyphMatrix()
        val grid1 = PatternGenerator.generate(seed = 12345L, mask = mask)
        val grid2 = PatternGenerator.generate(seed = 12345L, mask = mask)
        assertTrue(grid1.contentDeepEquals(grid2))
    }

    @Test
    fun `generate - 違うシードなら違うパターン`() {
        val mask = CircularMask.createGlyphMatrix()
        val grid1 = PatternGenerator.generate(seed = 1L, mask = mask)
        val grid2 = PatternGenerator.generate(seed = 2L, mask = mask)
        assertFalse(grid1.contentDeepEquals(grid2))
    }

    @Test
    fun `generate - 円外には絶対セルが置かれない`() {
        val mask = CircularMask.createGlyphMatrix()
        val grid = PatternGenerator.generate(seed = 99999L, mask = mask)

        for (row in grid.indices) {
            for (col in grid[row].indices) {
                if (!mask[row][col] && grid[row][col] == 1) {
                    throw AssertionError("円外 ($row, $col) にセルが配置されている")
                }
            }
        }
    }

    @Test
    fun `generate - density 35パーセントなら生存セル数がほぼ48個`() {
        val mask = CircularMask.createGlyphMatrix()
        val grid = PatternGenerator.generate(seed = 42L, mask = mask, density = 0.35)

        val aliveCount = grid.sumOf { row -> row.count { it == 1 } }
        // 137 * 0.35 = 47.95 → 47個
        assertEquals(47, aliveCount)
    }

    @Test
    fun `generate - density 1_0なら円内全セルが生存`() {
        val mask = CircularMask.createGlyphMatrix()
        val grid = PatternGenerator.generate(seed = 0L, mask = mask, density = 1.0)

        val aliveCount = grid.sumOf { row -> row.count { it == 1 } }
        assertEquals(137, aliveCount)
    }

    @Test
    fun `generate - density 0_0なら全セルが死亡`() {
        val mask = CircularMask.createGlyphMatrix()
        val grid = PatternGenerator.generate(seed = 0L, mask = mask, density = 0.0)

        val aliveCount = grid.sumOf { row -> row.count { it == 1 } }
        assertEquals(0, aliveCount)
    }
}