package io.github.skeletonxf.settings

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.skeletonxf.data.KResult
import io.github.skeletonxf.data.andThen
import io.github.skeletonxf.ffi.ConfigHandle
import io.github.skeletonxf.ffi.FFIError
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

val LocalSettings = staticCompositionLocalOf { Settings.new() }

interface Setting<T : Any> {
    val value: State<T>
    val default: T
    val initializationError: Throwable?
    fun set(value: T): KResult<Unit, Throwable>
}

interface Settings {
    val locale: Setting<String>

    companion object {
        fun new(): Settings = ConfigFileSettings(Paths.get("."))
    }
}

private class ConfigFileSettings(private val path: Path) : Settings {
    override val locale: Setting<String>

    private fun readConfig(): KResult<Config, Throwable> = KResult
        .runCatching { Files.readAllLines(path).joinToString(separator = "\n") }
        .andThen { config -> ConfigHandle.new(config).mapError { it.toThrowable() } }

    init {
        val config = readConfig()
        locale = ConfigSetting(config = config, get = Config::getLocale, default = "en-GB")
    }
}

private data class ConfigSetting<T: Any>(
    val config: KResult<Config, Throwable>,
    private val get: (Config) -> KResult<T, FFIError<Unit?>>,
    override val default: T,
): Setting<T> {
    private val result = config.andThen { config -> get(config).mapError { it.toThrowable() } }
    override val value = mutableStateOf(result.okOrNull() ?: default)
    override val initializationError = result.errorOrNull()
    override fun set(value: T): KResult<Unit, Throwable> {
        this.value.value = value
        // TODO: Try to update config file
        return KResult.Ok(Unit)
    }
}