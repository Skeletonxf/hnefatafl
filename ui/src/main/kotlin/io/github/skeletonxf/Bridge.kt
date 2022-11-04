package io.github.skeletonxf

import io.github.skeletonxf.bindings.JavaTest
import io.github.skeletonxf.bindings.bindings_h
import java.nio.file.Path
import java.nio.file.Paths

class Bridge {
    init {
        // There has to be a nicer way to work out our path than hacking it with strings
        val projectRoot = Paths.get("").toAbsolutePath().toString().removeSuffix("/ui")
        System.load("$projectRoot/target/debug/libhnefatafl.so")
    }

    fun helloWorld() {
        JavaTest.helloWorld()
        bindings_h.hello_from_rust()
    }
}