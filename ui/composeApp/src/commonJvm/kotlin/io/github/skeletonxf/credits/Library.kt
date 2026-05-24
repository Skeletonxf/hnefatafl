package io.github.skeletonxf.credits

import io.github.skeletonxf.data.KResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class Library(
    val name: String,
    val description: String,
    val url: String?,
    val licenses: List<License>,
) {
    companion object {
        private val jsonConfiguration = Json

        fun from(
            json: String
        ): KResult<List<Library>, IllegalArgumentException> = try {
            KResult.Ok(
                jsonConfiguration.decodeFromString<List<LibraryData>>(json).map { artifact ->
                    Library(
                        name = artifact.name,
                        description = "${artifact.group}:${artifact.artifact}",
                        url = artifact.scm.url,
                        licenses = artifact.licenses.map { license ->
                            License(
                                name = license.name,
                                url = license.url,
                            )
                        } + artifact.unknownLicenses.map { license ->
                            License(
                                name = license.name,
                                url = license.url,
                            )
                        },
                    )
                }
            )
        } catch (exception: IllegalArgumentException) {
            KResult.Error(exception)
        }
    }
}

data class License(
    val name: String,
    val url: String,
)

@Serializable
private data class LibraryData(
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
