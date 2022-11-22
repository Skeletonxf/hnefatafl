package io.github.skeletonxf

import io.github.skeletonxf.bindings.bindings_h

class Bridge {
    fun helloWorld() {
        println("Hello from Kotlin")
        bindings_h.hello_from_rust()
    }
}