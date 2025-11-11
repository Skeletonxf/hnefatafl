package io.github.skeletonxf.settings

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.skeletonxf.data.KResult
import io.github.skeletonxf.data.andThen
import io.github.skeletonxf.data.okOrThrow
import io.github.skeletonxf.ffi.ConfigHandle
import io.github.skeletonxf.logging.Log
import io.github.skeletonxf.ui.localBackgroundScope
import io.github.skeletonxf.ui.strings.getDefaultLocale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

interface Setting<T : Any> {
    val value: State<T>
    fun set(value: T)
}

interface Settings {
    val locale: Setting<String>

    fun save(immediate: Boolean = false)

    companion object {
        fun new(
            ioScope: CoroutineScope,
            filePaths: FilePaths,
        ): Settings = ConfigFileSettings
            .create(filePaths.settingsPath(), ioScope)
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
                Log.debug(
                    "Unable to parse or find an existing config file, a new one has been created from defaults",
                    error
                )
                doSetup = true
                ConfigHandle.default()
            },
        )
        locale = ConfigSetting.create(config = config, key = Config.StringKey.Locale)
        if (doSetup) {
            locale.set(getDefaultLocale())
        }
    }

    companion object {
        fun create(
            path: Path,
            ioScope: CoroutineScope,
        ): ConfigFileSettings = ConfigFileSettings(path, ioScope)
            .also { config -> config.saveConfig() }
    }

    private fun readConfig(): KResult<Config, Throwable> = KResult
        .runCatching { Files.readAllLines(path).joinToString(separator = "\n") }
        .andThen { config -> ConfigHandle.new(config).mapError { Throwable(it) } }

    private fun writeConfig(contents: String): KResult<Unit, Throwable> = KResult
        .runCatching { Files.write(path, contents.encodeToByteArray()) }

    private fun saveConfig() = writeConfig(config.getAll())

    override fun toString(): String = "ConfigFileSettings(locale = $locale)"

    private var saveJob: Job? = null
    override fun save(immediate: Boolean) = synchronized(this) {
        saveJob?.cancel()
        saveJob = if (immediate) {
            saveConfig().mapError { Log.error("Unable to save", it) }
            null
        } else {
            // Delay the save for a few seconds so that we can batch updates if the user is changing multiple settings
            saveJob()
        }
    }

    private fun saveJob() = ioScope.launch {
        delay(timeMillis = 5000)
        saveConfig().mapError { Log.error("Unable to save", it) }
    }
}

private class ConfigSetting<T : Any> private constructor(
    val config: Config,
    val key: ConfigKey<T>,
) : Setting<T> {
    override val value = mutableStateOf(config.get(key))
    override fun set(value: T) {
        this.value.value = value
        config.set(key, value)
    }

    override fun toString(): String =
        "ConfigSetting(value=${value.value}, config=$config, key=$key)"

    companion object {
        fun <T : Any> create(
            config: Config,
            key: ConfigKey<T>
        ): ConfigSetting<T> = ConfigSetting(config, key)
    }
}