package io.github.skeletonxf.logging

/**
 * Logs events to stdout
 */
class PrintLogger : Logger {
    override fun log(level: LogLevel, message: String, error: Throwable?) =
        error?.let { println("L: $message: $error") } ?: println("L: $message")
}