package io.github.skeletonxf.settings

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.skeletonxf.data.KResult
import io.github.skeletonxf.data.andThen
import io.github.skeletonxf.data.okOrThrow
import io.github.skeletonxf.ffi.ConfigHandle
import io.github.skeletonxf.ui.strings.getDefaultLocale
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
    fun set(value: T)
}

interface Settings {
    val locale: Setting<String>

    fun save(immediate: Boolean = false, onError: (Throwable) -> Unit)

    companion object {
        // TODO: Need to look into setting up proper DI instead of making the background scope and this
        // static objects
        internal val instance: Settings? = new(localBackgroundScope)
        // TODO: Better error handling
        fun new(ioScope: CoroutineScope): Settings? = ConfigFileSettings
            .create(Paths.get("./settings.toml"), ioScope)
            .okOrNull()
    }
}

private class ConfigFileSettings private constructor(
    private val path: Path,
    private val ioScope: CoroutineScope,
) : Settings {
    override val locale: Setting<String>
    private val config: Config

    init {
        var doSetup = false
        config = readConfig().fold(
            ok = { it },
            error = { error ->
                println(error) // TODO: Need to send this error somewhere
                doSetup = true
                ConfigHandle.default()
            },
        )
        locale = ConfigSetting.create(config = config, key = Config.StringKey.Locale).okOrThrow()
        if (doSetup) {
            locale.set(getDefaultLocale())
        }
    }

    companion object {
        fun create(
            path: Path,
            ioScope: CoroutineScope,
        ): KResult<ConfigFileSettings, Throwable> = KResult.runCatching { ConfigFileSettings(path, ioScope) }
    }

    private fun readConfig(): KResult<Config, Throwable> = KResult
        .runCatching { Files.readAllLines(path).joinToString(separator = "\n") }
        .andThen { config -> ConfigHandle.new(config).mapError { it.toThrowable() } }

    private fun writeConfig(contents: String): KResult<Unit, Throwable> = KResult
        .runCatching { Files.writeString(path, contents) }

    private fun saveConfig(): KResult<Unit, Throwable> = config
        .getAll().mapError { it.toThrowable() }
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
}

private class ConfigSetting<T: Any> private constructor(
    val config: Config,
    val key: ConfigKey<T>,
): Setting<T> {
    override val value = mutableStateOf(config.get(key).mapError { it.toThrowable() }.okOrThrow())
    override fun set(value: T) {
        this.value.value = value
        config.set(key, value).mapError { println(it.toThrowable()) }
    }
    override fun toString(): String = "ConfigSetting(value=${value.value}, config=$config, key=$key)"

    companion object {
        fun <T : Any> create(
            config: Config,
            key: ConfigKey<T>
        ): KResult<ConfigSetting<T>, Throwable> = KResult.runCatching { ConfigSetting(config, key) }
    }
}