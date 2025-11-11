package io.github.skeletonxf

import android.content.Context
import io.github.skeletonxf.settings.AndroidFilePaths
import io.github.skeletonxf.ui.setup

/**
 * Singleton app state for the Android application.
 */
class AppGlobalState private constructor(
    private val applicationContext: Context,
) {
    val filePaths = AndroidFilePaths(applicationContext)
    val environment = setup(filePaths = filePaths)

    companion object {
        private val instance: AppGlobalState? = null

        fun getInstance(
            applicationContext: Context
        ): AppGlobalState = synchronized(this) {
            instance ?: AppGlobalState(applicationContext)
        }
    }
}