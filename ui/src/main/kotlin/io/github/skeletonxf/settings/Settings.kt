package io.github.skeletonxf.settings

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.skeletonxf.data.KResult
import io.github.skeletonxf.data.andThen
import io.github.skeletonxf.ffi.ConfigHandle
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
        fun new(): Settings = ConfigFileSettings(Paths.get("./settings.toml"))
    }
}

private class ConfigFileSettings(private val path: Path) : Settings {
    override val locale: Setting<String>

    private fun readConfig(): KResult<Config, Throwable> = KResult
        .runCatching { Files.readAllLines(path).joinToString(separator = "\n") }
        .andThen { config -> ConfigHandle.new(config).mapError { it.toThrowable() } }

    override fun toString(): String = "ConfigFileSettings(locale = $locale)"

    init {
        val config = readConfig()
        locale = ConfigSetting(config = config, key = Config.StringKey.Locale, default = "en-GB")
    }
}

private data class ConfigSetting<T: Any>(
    val config: KResult<Config, Throwable>,
    val key: ConfigKey<T>,
    override val default: T,
): Setting<T> {
    private val result = config.andThen { config -> config.get(key).mapError { it.toThrowable() } }
    override val value = mutableStateOf(result.okOrNull() ?: default)
    override val initializationError = result.errorOrNull()
    override fun set(value: T): KResult<Unit, Throwable> {
        this.value.value = value
        return config.andThen { config -> config.set(key, value).mapError { it.toThrowable() } }
    }

    override fun toString(): String = "ConfigSetting(value=${value.value}, config=$config, key=$key, default=$default)"
}