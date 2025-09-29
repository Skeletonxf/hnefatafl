package io.github.skeletonxf

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform