package io.github.skeletonxf

enum class HeaderAlignment {
    Left, Center
}

interface Platform {
    val name: String
    val headerAlignment: HeaderAlignment
}

expect fun getPlatform(): Platform