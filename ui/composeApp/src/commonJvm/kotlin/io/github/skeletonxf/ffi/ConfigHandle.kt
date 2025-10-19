package io.github.skeletonxf.ffi

import io.github.skeletonxf.data.KResult
import io.github.skeletonxf.settings.Config

class ConfigHandle private constructor(private val handle: uniffi.hnefatafl.ConfigHandle) : Config {
    companion object {
        fun new(config: String): KResult<ConfigHandle, String> = try {
            KResult.Ok(ConfigHandle(uniffi.hnefatafl.ConfigHandle(toml = config)))
        } catch (error: uniffi.hnefatafl.DeserializeException) {
            KResult.Error(
                when (error) {
                    is uniffi.hnefatafl.DeserializeException.Exception -> error.v1
                }
            )
        }

        fun default(): ConfigHandle = ConfigHandle(uniffi.hnefatafl.ConfigHandle.default())
    }

    override fun get(key: Config.StringKey) = handle.key(
        forKey = when (key) {
            Config.StringKey.Locale -> uniffi.hnefatafl.ConfigStringKey.LOCALE
        }
    )

    override fun set(
        key: Config.StringKey,
        value: String
    ) {
        handle.set(
            forKey = when (key) {
                Config.StringKey.Locale -> uniffi.hnefatafl.ConfigStringKey.LOCALE
            },
            value = value
        )
    }

    override fun getAll() = handle.asToml()
}
