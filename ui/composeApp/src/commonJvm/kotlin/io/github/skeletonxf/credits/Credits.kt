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
        Library.from(Res.readBytes("files/artifacts.json").toString(Charsets.UTF_8))
    }
}

// TODO: Rust source credits, SVG icon credits, credits implementation that can combine them all