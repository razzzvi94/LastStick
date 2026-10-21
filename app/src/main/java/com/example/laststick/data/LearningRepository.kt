package com.example.laststick.data

import com.example.laststick.ai.QLearningAgent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Progress summary of the learning agent, for display. */
data class LearningInfo(val games: Int, val explorationPercent: Int)

/** Owns the single [QLearningAgent] instance and keeps it persisted through a [LearningStore]. */
class LearningRepository(private val store: LearningStore) {
    private val mutex = Mutex()
    private var agent: QLearningAgent? = null

    private val _info = MutableStateFlow<LearningInfo?>(null)
    val info: StateFlow<LearningInfo?> = _info.asStateFlow()

    /** Loads the agent on first use; falls back to a fresh agent if saved data is missing/invalid. */
    suspend fun agent(): QLearningAgent = mutex.withLock { loadLocked() }

    private suspend fun loadLocked(): QLearningAgent {
        agent?.let { return it }
        val loaded = withContext(Dispatchers.IO) { store.load() }
        val created = loaded?.let { QLearningAgent.fromSnapshot(it) } ?: QLearningAgent()
        agent = created
        publish(created)
        return created
    }

    suspend fun save() {
        val current = mutex.withLock { agent } ?: return
        withContext(Dispatchers.IO) { store.save(current.snapshot()) }
        publish(current)
    }

    /** Self-play training on a background dispatcher, then persists the result. */
    suspend fun trainSelfPlay(games: Int) {
        val a = agent()
        withContext(Dispatchers.Default) { a.trainSelfPlay(games) }
        save()
    }

    suspend fun reset() {
        mutex.withLock {
            withContext(Dispatchers.IO) { store.clear() }
            val fresh = QLearningAgent()
            agent = fresh
            publish(fresh)
        }
    }

    private fun publish(a: QLearningAgent) {
        _info.value = LearningInfo(a.episodes, Math.round(a.epsilon * 100).toInt())
    }
}
