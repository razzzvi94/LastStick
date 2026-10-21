package com.example.laststick.game

enum class Player {
    HUMAN,
    AI;

    val opponent: Player
        get() = if (this == HUMAN) AI else HUMAN
}
