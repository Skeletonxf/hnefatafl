package io.github.skeletonxf.logging

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicLong

/**
 * Creates timber when logging events.
 */
class ForestLogger : Logger {
    private val logs: MutableStateFlow<List<Tree>> = MutableStateFlow(listOf())
    private val lastID = AtomicLong()
    val timber: StateFlow<List<Tree>> = logs

    override fun log(level: LogLevel, message: String, error: Throwable?) {
        val id = lastID.getAndIncrement()
        val felled = Tree(message = message, error = error, level = level, id = TreeIdentifier(id))
        logs.update { trees -> trees + felled }
    }

    fun dismiss(tree: TreeIdentifier) {
        logs.update { trees -> trees.filter { it.id != tree } }
    }
}