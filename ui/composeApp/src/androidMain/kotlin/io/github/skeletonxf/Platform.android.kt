package io.github.skeletonxf

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val headerAlignment = HeaderAlignment.Left
}

actual fun getPlatform(): Platform = AndroidPlatform()