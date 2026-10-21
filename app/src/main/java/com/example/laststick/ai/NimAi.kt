package com.example.laststick.ai

import com.example.laststick.game.Move

/** Something that can pick a legal move for the player to move. */
interface NimAi {
    /** [rows] must contain at least one stick. The returned move is always legal. */
    fun chooseMove(rows: List<Int>): Move
}

class PerfectAi : NimAi {
    override fun chooseMove(rows: List<Int>): Move = MisereNimSolver.bestMove(rows)
}
