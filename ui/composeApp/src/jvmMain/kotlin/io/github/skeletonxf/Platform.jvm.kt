package io.github.skeletonxf

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
    override val headerAlignment = HeaderAlignment.Center
}

actual fun getPlatform(): Platform = JVMPlatform()
