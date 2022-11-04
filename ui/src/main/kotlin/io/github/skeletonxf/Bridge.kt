package io.github.skeletonxf

import io.github.skeletonxf.bindings.JavaTest

class Bridge {
    fun helloWorld() {
        JavaTest.helloWorld()
        //println("now calling bindings")
        //bindings_h.hello_from_rust()
    }
}