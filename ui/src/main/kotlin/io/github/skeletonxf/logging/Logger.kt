package io.github.skeletonxf.logging

interface Logger {
    fun log(level: LogLevel, message: String, error: Throwable?)

    fun debug(message: String, error: Throwable? = null) = log(level = LogLevel.Debug, message = message, error = error)
    fun warn(message: String, error: Throwable? = null) = log(level = LogLevel.Warn, message = message, error = error)
    fun error(message: String, error: Throwable? = null) = log(level = LogLevel.Error, message = message, error = error)
}

object Log : Logger {
    private val loggers: MutableList<Logger> = mutableListOf()

    override fun log(level: LogLevel, message: String, error: Throwable?) = loggers
        .forEach { logger -> logger.log(level, message, error) }

    fun add(logger: Logger) = synchronized(loggers) {
        loggers.add(logger)
    }
}