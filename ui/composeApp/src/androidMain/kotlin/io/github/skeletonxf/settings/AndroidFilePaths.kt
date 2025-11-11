package io.github.skeletonxf.settings

import android.content.Context
import java.nio.file.Path
import java.nio.file.Paths

class AndroidFilePaths(
    private val applicationContext: Context,
) : FilePaths {
    override fun settingsPath(): Path = Paths
        .get(applicationContext.filesDir.path, "settings.toml")
}