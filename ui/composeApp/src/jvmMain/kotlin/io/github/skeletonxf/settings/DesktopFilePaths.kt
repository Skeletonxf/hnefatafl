package io.github.skeletonxf.settings

import java.nio.file.Path
import java.nio.file.Paths

data object DesktopFilePaths : FilePaths {
    override fun settingsPath(): Path = Paths.get("./settings.toml")
}