package io.github.skeletonxf.settings

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.skeletonxf.data.KResult
import io.github.skeletonxf.data.andThen
import io.github.skeletonxf.ffi.ConfigHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import localBackgroundScope
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

val LocalSettings = staticCompositionLocalOf { Settings.instance }

interface Setting<T : Any> {
    val value: State<T>
    val default: T
    val initializationError: Throwable?
    fun set(value: T): KResult<Unit, Throwable>
}

interface Settings {
    val locale: Setting<String>

    fun save(immediate: Boolean = false, onError: (Throwable) -> Unit)

    companion object {
        // TODO: Probably need to look into setting up proper DI instead of making the background scope and this
        // static objects
        internal val instance = new(localBackgroundScope)
        fun new(ioScope: CoroutineScope): Settings = ConfigFileSettings(Paths.get("./settings.toml"), ioScope)
    }
}

private class ConfigFileSettings(
    private val path: Path,
    private val ioScope: CoroutineScope,
) : Settings {
    override val locale: Setting<String>
    private val config: KResult<Config, Throwable>

    private fun readConfig(): KResult<Config, Throwable> = KResult
        .runCatching { Files.readAllLines(path).joinToString(separator = "\n") }
        .andThen { config -> ConfigHandle.new(config).mapError { it.toThrowable() } }

    private fun writeConfig(contents: String): KResult<Unit, Throwable> = KResult
        .runCatching { Files.writeString(path, contents) }

    private fun saveConfig(): KResult<Unit, Throwable> = config
        .andThen { config -> config.getAll().mapError { it.toThrowable() } }
        .andThen { toml -> writeConfig(toml) }

    override fun toString(): String = "ConfigFileSettings(locale = $locale)"

    private var saveJob: Job? = null
    override fun save(immediate: Boolean, onError: (Throwable) -> Unit) = synchronized(this) {
        saveJob?.cancel()
        saveJob = if (immediate) {
            saveConfig().mapError { onError(it) }
            null
        } else {
            // Delay the save for a few seconds so that we can batch updates if the user is changing multiple settings
            saveJob(onError)
        }
    }

    private fun saveJob(onError: (Throwable) -> Unit) = ioScope.launch {
        delay(timeMillis = 5000)
        saveConfig().mapError { onError(it) }
    }

    init {
        // actually need to handle file not existing, add method on Rust side to create default ConfigHandle?
        // can move definitions for defaults to Rust then
        config = readConfig()
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