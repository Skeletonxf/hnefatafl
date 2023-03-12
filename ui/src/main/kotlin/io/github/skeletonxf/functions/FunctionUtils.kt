package io.github.skeletonxf.functions

fun (() -> Unit).then(op: () -> Unit): () -> Unit = {
    this()
    op()
}