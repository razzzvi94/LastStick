package com.example.laststick.ai

import com.example.laststick.game.GameRules
import com.example.laststick.game.Move
import kotlin.random.Random

/**
 * Tunable parameters of the learning agent.
 *
 * @property alpha learning rate
 * @property gamma discount factor
 * @property initialEpsilon exploration rate of a brand-new agent
 * @property minEpsilon exploration never decays below this
 * @property epsilonDecay multiplier applied to epsilon after every finished game
 * @property trainingEpsilon fixed exploration rate used during self-play training
 */
data class QLearningConfig(
    val alpha: Double = 0.5,
    val gamma: Double = 0.99,
    val initialEpsilon: Double = 0.25,
    val minEpsilon: Double = 0.03,
    val epsilonDecay: Double = 0.97,
    val trainingEpsilon: Double = 0.3,
)

/**
 * Tabular Q-learning for Misère Nim, written as a **two-player zero-sum (negamax) learner**.
 *
 * Perspective: `Q(s, a)` is always the expected outcome *for the player who is about to play `a`
 * in `s`*. Because the opponent's gain is exactly this player's loss, the Q-learning target is
 *
 * ```
 * taking the last stick:  target = -1                      (the mover loses)
 * otherwise:              target = -gamma * max_a' Q(s', a')   (s' is the opponent's position)
 * ```
 *
 * so the table never rewards the AI for positions that favour the human. The agent learns from
 * **both players' moves** (via [learn]) – human moves are off-policy samples, which plain
 * Q-learning handles correctly. There is no guarantee the agent reaches perfect play: with finite
 * exploration, some entries may stay inaccurate. Use [PerfectAi] for guaranteed optimal play.
 */
class QLearningAgent(
    val config: QLearningConfig = QLearningConfig(),
    val table: QTable = QTable(),
    private val random: Random = Random.Default,
    epsilon: Double = config.initialEpsilon,
    episodes: Int = 0,
) : NimAi {

    var epsilon: Double = epsilon.coerceIn(0.0, 1.0)
        private set

    var episodes: Int = episodes
        private set

    /** Epsilon-greedy move choice using the agent's current exploration rate. */
    @Synchronized
    override fun chooseMove(rows: List<Int>): Move = choose(rows, epsilon)

    /** Always exploits the current Q-values (ties broken randomly). */
    @Synchronized
    fun greedyMove(rows: List<Int>): Move = choose(rows, 0.0)

    private fun choose(rows: List<Int>, exploration: Double): Move {
        val moves = GameRules.legalMoves(rows)
        require(moves.isNotEmpty()) { "No legal moves" }
        if (exploration > 0.0 && random.nextDouble() < exploration) return moves.random(random)
        val best = moves.maxOf { table[rows, it] }
        return moves.filter { table[rows, it] == best }.random(random)
    }

    /**
     * Q-learning update for the transition "the player to move played [move] in [rows]".
     * Call it for every move of a game, whoever played it.
     */
    @Synchronized
    fun learn(rows: List<Int>, move: Move) {
        require(GameRules.legalMoves(rows).contains(move)) { "Illegal move $move in $rows" }
        val after = rows.toMutableList().also { it[move.row] -= move.count }
        val target = if (after.all { it == 0 }) {
            terminalReward()
        } else {
            -config.gamma * table.maxValue(after, GameRules.legalMoves(after))
        }
        val old = table[rows, move]
        table[rows, move] = old + config.alpha * (target - old)
    }

    /** Marks a game as finished: decays exploration, never below the configured minimum. */
    @Synchronized
    fun finishEpisode() {
        episodes++
        epsilon = (epsilon * config.epsilonDecay).coerceAtLeast(config.minEpsilon)
    }

    /** Plays [games] games against itself from the standard start, learning from every move. */
    @Synchronized
    fun trainSelfPlay(games: Int, startRows: List<Int> = listOf(3, 5, 7)) {
        repeat(games) {
            var rows = startRows
            while (rows.any { it > 0 }) {
                val move = choose(rows, config.trainingEpsilon)
                learn(rows, move)
                rows = rows.toMutableList().also { it[move.row] -= move.count }
            }
            finishEpisode()
        }
    }

    @Synchronized
    fun snapshot(): LearningSnapshot = LearningSnapshot(table.copy(), epsilon, episodes)

    companion object {
        /** Reward for the player who takes the last stick (Misère: that player loses). */
        fun terminalReward(): Double = -1.0

        fun fromSnapshot(
            snapshot: LearningSnapshot,
            config: QLearningConfig = QLearningConfig(),
            random: Random = Random.Default,
        ) = QLearningAgent(config, snapshot.table.copy(), random, snapshot.epsilon, snapshot.episodes)
    }
}
