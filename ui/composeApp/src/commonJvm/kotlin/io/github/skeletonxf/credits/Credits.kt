package io.github.skeletonxf.credits

import io.github.skeletonxf.data.KResult
import io.github.skeletonxf.ui.Res
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

fun interface Credits {
    suspend fun getLibraries(): KResult<List<Library>, Throwable>
}

class AndroidCredits(
    private val ioDispatcher: CoroutineDispatcher
) : Credits {
    override suspend fun getLibraries() = withContext(ioDispatcher) {
        Library.fromGradle(Res.readBytes("files/artifacts.json").toString(Charsets.UTF_8))
    }
}

class RustCredits(
    private val ioDispatcher: CoroutineDispatcher
) : Credits {
    override suspend fun getLibraries() = withContext(ioDispatcher) {
        Library.fromRust(uniffi.hnefatafl.licensesJson())
    }
}

class CombinedCredits(
    val credits: List<Credits>
) : Credits {
    override suspend fun getLibraries(): KResult<List<Library>, Throwable> {
        val libraries = credits.map { credits -> credits.getLibraries() }
        return if (libraries.all { it is KResult.Ok }) {
            KResult.Ok(
                libraries
                    .mapNotNull { it.okOrNull() }
                    .flatten()
                    .sortedBy { library -> library.name.uppercase() }
            )
        } else {
            val error = libraries.first { result -> result is KResult.Error }
            KResult.Error(error.errorOrNull() ?: Throwable("Unable to find error"))
        }
    }
}

// TODO: SVG icon credits