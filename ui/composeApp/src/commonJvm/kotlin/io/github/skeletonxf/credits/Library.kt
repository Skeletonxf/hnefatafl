package io.github.skeletonxf.credits

import io.github.skeletonxf.data.KResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class Library(
    val name: String,
    val identifier: String?,
    val url: String?,
    val licenses: List<License>,
) {
    companion object {
        private val jsonConfiguration = Json
        // Some of the fields serialized by the Rust library don't
        // look like public API so we'll just ignore them
        private val jsonConfigurationIgnoreUnknownKeys = Json { ignoreUnknownKeys = true }

        fun fromGradle(
            json: String
        ): KResult<List<Library>, IllegalArgumentException> = try {
            KResult.Ok(
                jsonConfiguration.decodeFromString<List<GradleLibrary>>(json).map { artifact ->
                    Library(
                        name = artifact.name,
                        identifier = "${artifact.group}:${artifact.artifact}",
                        url = artifact.scm.url,
                        licenses = artifact.licenses.map { license ->
                            License(
                                name = license.name,
                                url = license.url,
                                text = "", // TODO: Need to include this for compliance
                            )
                        } + artifact.unknownLicenses.map { license ->
                            License(
                                name = license.name,
                                url = license.url,
                                text = "", // TODO: Need to include this for compliance
                            )
                        },
                    )
                }
            )
        } catch (exception: IllegalArgumentException) {
            KResult.Error(exception)
        }

        fun fromRust(
            json: String
        ): KResult<List<Library>, IllegalArgumentException> = try {
            KResult.Ok(
                jsonConfigurationIgnoreUnknownKeys
                    .decodeFromString<List<RustLibrary>>(json).map { artifact ->
                        Library(
                            name = artifact.name,
                            identifier = null,
                            url = artifact.homepage ?: artifact.repository,
                            licenses = listOf(
                                License(
                                    name = artifact.licenseIdentifier ?: "Unknown license",
                                    text = artifact.licenseText ?: "Unknown license text",
                                    // TODO: Don't really need this anyway
                                    url = "",
                                )
                            ),
                        )
                    }
            )
        } catch (exception: IllegalArgumentException) {
            KResult.Error(exception)
        }
    }
}

@Serializable
data class License(
    val name: String,
    val url: String,
    val text: String,
)

@Serializable
private data class GradleLibrary(
    @SerialName("groupId")
    val group: String,
    @SerialName("artifactId")
    val artifact: String,
    val version: String,
    val name: String,
    @SerialName("spdxLicenses")
    val licenses: List<Licence>,
    val unknownLicenses: List<UnknownLicence> = listOf(),
    val scm: Link,
) {
    @Serializable
    data class Link(val url: String)

    @Serializable
    data class Licence(
        val identifier: String,
        val name: String,
        val url: String,
    )

    @Serializable
    data class UnknownLicence(
        val name: String,
        val url: String,
    )
}

@Serializable
private data class RustLibrary(
    val name: String,
    val version: String,
    val authors: List<String>,
    val description: String?,
    val homepage: String?,
    val repository: String?,
    @SerialName("license_identifier")
    val licenseIdentifier: String?,
    @SerialName("license_text")
    val licenseText: String?,
)
