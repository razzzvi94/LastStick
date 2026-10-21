package com.example.laststick.ai

import com.example.laststick.data.FileLearningStore
import com.example.laststick.game.GameRules
import com.example.laststick.game.GameState
import com.example.laststick.game.Move
import com.example.laststick.game.Player
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.random.Random

class QLearningAgentTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun agent(seed: Int = 1, config: QLearningConfig = QLearningConfig()) =
        QLearningAgent(config, random = Random(seed))

    @Test
    fun chosenMovesAreAlwaysLegal() {
        val a = agent()
        for (a0 in 0..3) for (b in 0..5) for (c in 0..7) {
            val rows = listOf(a0, b, c)
            if (rows.all { it == 0 }) continue
            repeat(5) {
                assertTrue(GameRules.isLegal(GameState(rows, Player.AI), a.chooseMove(rows)))
                assertTrue(GameRules.isLegal(GameState(rows, Player.AI), a.greedyMove(rows)))
            }
        }
    }

    @Test
    fun legalActionsMatchBoard() {
        assertEquals(15, GameRules.legalMoves(listOf(3, 5, 7)).size)
        assertEquals(listOf(Move(1, 1)), GameRules.legalMoves(listOf(0, 1, 0)))
    }

    @Test
    fun terminalRewardIsLossForPlayerTakingLastStick() {
        assertEquals(-1.0, QLearningAgent.terminalReward(), 0.0)
        val a = agent(config = QLearningConfig(alpha = 1.0))
        a.learn(listOf(0, 1, 0), Move(1, 1))
        assertEquals(-1.0, a.table[listOf(0, 1, 0), Move(1, 1)], 1e-12)
    }

    @Test
    fun nonTerminalUpdateUsesNegatedBestOpponentValue() {
        val a = agent(config = QLearningConfig(alpha = 0.5, gamma = 0.9))
        a.table[listOf(0, 1, 1), Move(1, 1)] = 1.0
        a.table[listOf(0, 1, 1), Move(2, 1)] = 0.25
        // Mover plays [0,1,2] -> [0,1,1]. target = -0.9 * 1.0, old = 0.
        a.learn(listOf(0, 1, 2), Move(2, 1))
        assertEquals(0.5 * (-0.9), a.table[listOf(0, 1, 2), Move(2, 1)], 1e-12)
        a.learn(listOf(0, 1, 2), Move(2, 1))
        assertEquals(-0.45 + 0.5 * (-0.9 + 0.45), a.table[listOf(0, 1, 2), Move(2, 1)], 1e-12)
    }

    @Test
    fun greedyPicksHighestQ() {
        val a = agent()
        a.table[listOf(2, 2, 0), Move(0, 2)] = 0.7
        a.table[listOf(2, 2, 0), Move(1, 1)] = 0.1
        repeat(20) { assertEquals(Move(0, 2), a.greedyMove(listOf(2, 2, 0))) }
    }

    @Test
    fun explorationDecaysButStaysAtOrAboveMinimum() {
        val cfg = QLearningConfig(initialEpsilon = 0.5, minEpsilon = 0.05, epsilonDecay = 0.5)
        val a = agent(config = cfg)
        a.finishEpisode()
        assertEquals(0.25, a.epsilon, 1e-12)
        repeat(50) { a.finishEpisode() }
        assertEquals(0.05, a.epsilon, 1e-12)
        assertEquals(51, a.episodes)
    }

    @Test
    fun snapshotRoundTripsThroughText() {
        val a = agent()
        a.trainSelfPlay(200)
        val snap = a.snapshot()
        val restored = LearningSnapshot.parse(snap.serialize())
        assertNotNull(restored)
        assertEquals(snap.table, restored!!.table)
        assertEquals(snap.epsilon, restored.epsilon, 0.0)
        assertEquals(snap.episodes, restored.episodes)
    }

    @Test
    fun snapshotSurvivesFilePersistence() {
        val store = FileLearningStore(File(tmp.root, "q.txt"))
        val a = agent()
        a.trainSelfPlay(100)
        assertTrue(store.save(a.snapshot()))
        val loaded = QLearningAgent.fromSnapshot(store.load()!!)
        assertEquals(a.table, loaded.table)
        assertEquals(a.epsilon, loaded.epsilon, 0.0)
        assertEquals(a.episodes, loaded.episodes)
    }

    @Test
    fun missingOrCorruptDataIsIgnored() {
        val file = File(tmp.root, "bad.txt")
        val store = FileLearningStore(file)
        file.writeText("")
        assertNull(store.load())
        file.writeText("garbage\nmore garbage")
        assertNull(store.load())
        file.writeText("LASTSTICK_Q1\nepsilon=0.1\nepisodes=3\n3,5,7|0:9=1.0\n") // illegal move
        assertNull(store.load())
        file.writeText("LASTSTICK_Q1\nepsilon=NaN\nepisodes=3\n")
        assertNull(store.load())
        file.writeText("LASTSTICK_Q1\nepsilon=0.1\nepisodes=3\n3,5,7|0:1=abc\n")
        assertNull(store.load())
        assertNull(FileLearningStore(File(tmp.root, "nope.txt")).load())
    }

    @Test
    fun clearRemovesSavedData() {
        val store = FileLearningStore(File(tmp.root, "q2.txt"))
        store.save(agent().snapshot())
        assertNotNull(store.load())
        store.clear()
        assertNull(store.load())
    }

    @Test
    fun selfPlayTrainingImprovesPlayAgainstRandomOpponent() {
        val trained = agent(seed = 7)
        trained.trainSelfPlay(20_000)
        val rnd = Random(99)
        var wins = 0
        val games = 400
        repeat(games) { g ->
            var rows = listOf(3, 5, 7)
            var agentToMove = g % 2 == 0
            var agentLost = false
            while (rows.any { it > 0 }) {
                val move = if (agentToMove) trained.greedyMove(rows) else GameRules.legalMoves(rows).random(rnd)
                rows = rows.toMutableList().also { it[move.row] -= move.count }
                if (rows.all { it == 0 }) agentLost = agentToMove
                agentToMove = !agentToMove
            }
            if (!agentLost) wins++
        }
        assertTrue("trained agent only won $wins/$games vs random", wins >= games * 0.85)
    }
}
