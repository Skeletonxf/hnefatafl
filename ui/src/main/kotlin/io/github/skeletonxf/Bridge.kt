package io.github.skeletonxf

import io.github.skeletonxf.bindings.JavaTest
import io.github.skeletonxf.bindings.bindings_h
import java.nio.file.Path

class Bridge {
    init {
        val currentDirectory = Path.of("").toAbsolutePath()
        val projectRoot = if (currentDirectory.endsWith("ui")) {
            currentDirectory.parent
        } else {
            currentDirectory
        }
        // TODO: Need to look for correct library on Windows / MacOS as well and also look for release build
        val library = projectRoot.resolve(Path.of("target/debug/libhnefatafl.so"))
        System.load(library.toString())
    }

    fun helloWorld() {
        JavaTest.helloWorld()
        bindings_h.hello_from_rust()
    }
}