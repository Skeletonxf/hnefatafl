package io.github.skeletonxf.logging

data class Tree(val message: String, val error: Throwable?, val level: LogLevel, val id: TreeIdentifier)