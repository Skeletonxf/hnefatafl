package io.github.skeletonxf.credits

import io.github.skeletonxf.data.KResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class Library(
    val group: String,
    val artifact: String,
    val version: String,
    val name: String,
    val url: String?,
    val licences: List<Licence>,
) {
    companion object {
        fun from(
            json: String
        ): KResult<List<Library>, IllegalArgumentException> = try {
            KResult.Ok(
                Json.decodeFromString<List<LibraryData>>(json).map { artifact ->
                    Library(
                        group = artifact.group,
                        artifact = artifact.artifact,
                        version = artifact.version,
                        name = artifact.name,
                        url = artifact.links.firstOrNull()?.url,
                        licences = artifact.licences.map { licence ->
                            Licence(
                                identifier = licence.identifier,
                                name = licence.name,
                                url = licence.url,
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

data class Licence(
    val identifier: String,
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
    val licences: List<Licence>,
    val links: List<Link>,
) {
    @Serializable
    data class Link(val url: String)

    @Serializable
    data class Licence(
        val identifier: String,
        val name: String,
        val url: String,
    )
}
