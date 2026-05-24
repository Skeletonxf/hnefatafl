package io.github.skeletonxf.ui.credits

import BackButton
import LoadingSpinner
import TitleHeader
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.skeletonxf.credits.AndroidCredits
import io.github.skeletonxf.credits.Credits
import io.github.skeletonxf.credits.Library
import io.github.skeletonxf.data.KResult
import io.github.skeletonxf.functions.launchUnit
import io.github.skeletonxf.ui.localBackgroundScope
import io.github.skeletonxf.ui.shaderGradient
import io.github.skeletonxf.ui.state.StateHolder
import io.github.skeletonxf.ui.strings.LocalStrings
import io.github.skeletonxf.ui.theme.HnefataflColors
import io.github.skeletonxf.ui.theme.PreviewSurface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

data class CreditsState(
    private val scope: CoroutineScope,
    private val credits: Credits,
): StateHolder {

    val state = mutableStateOf<State>(State.NoContent(isLoading = false, error = null))

    fun load() = scope.launchUnit {
        val current = state.value
        if (current is State.Content || current is State.NoContent && current.isLoading) {
            return@launchUnit
        }
        val result = credits.getLibraries()
        state.value = when (result) {
            is KResult.Error -> State.NoContent(isLoading = false, error = result.err)
            is KResult.Ok -> State.Content(credits = result.ok)
        }
    }

    sealed class State: StateHolder.StatefulLoading {
        data class NoContent(
            override val isLoading: Boolean,
            override val error: Throwable?,
        ) : State()
        data class Content(
            val credits: List<Library>
        ) : State() {
            override val isLoading: Boolean = false
            override val error: Throwable? = null
        }
    }

    companion object {
        @Composable
        fun remember(): CreditsState {
            val scope = localBackgroundScope
            return remember(scope) {
                CreditsState(
                    scope = localBackgroundScope,
                    credits = AndroidCredits(ioDispatcher = Dispatchers.IO)
                )
            }
        }
    }
}

@Composable
fun CreditsContent(
    onBack: () -> Unit,
) = Surface {
    val creditsState = CreditsState.remember()
    val state by creditsState.state
    // TODO: More suitable trigger for state refresh? Android has
    // lifecycles tied to the backstack entry but not sure we have
    // those on desktop
    LaunchedEffect(state) {
        val s = state
        if (s is CreditsState.State.NoContent && !s.isLoading && !s.isError) {
            creditsState.load()
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .shaderGradient(color1 = HnefataflColors.grey, color2 = Color.White)
            .safeDrawingPadding()
    ) {
        CreditsMenu(onBack = onBack, state = state)
    }
}

@Composable
fun CreditsMenu(
    onBack: () -> Unit,
    state: CreditsState.State,
) = Column {
    val strings = LocalStrings.current.credits
    TitleHeader(
        start = {
            BackButton(onClick = onBack, modifier = Modifier.padding(horizontal = 8.dp))
        },
        title = {
            Text(text = strings.title, fontSize = 24.sp)
        },
        modifier = Modifier.background(color = MaterialTheme.colorScheme.background)
    )
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item(key = "StartingGap") {
                Spacer(modifier = Modifier.height(16.dp))
            }
            when (state) {
                is CreditsState.State.Content -> {
                    items(state.credits) { library ->
                        Text(text = library.name)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                // TODO: Error state
                is CreditsState.State.NoContent -> {
                    if (state.isLoading) {
                        item(key = "Loading") {
                            LoadingSpinner(size = 32.dp, strokeWidth = 4.dp)
                        }
                    }
                }
            }
            item(key = "Footer") {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
@Preview
private fun CreditsMenuPreview() = PreviewSurface {
    CreditsMenu(onBack = {}, state = CreditsState.State.Content(credits = listOf()))
}

@Composable
@Preview
private fun CreditsMenuLoadingPreview() = PreviewSurface {
    CreditsMenu(
        onBack = {},
        state = CreditsState.State.NoContent(isLoading = true, error = null)
    )
}
