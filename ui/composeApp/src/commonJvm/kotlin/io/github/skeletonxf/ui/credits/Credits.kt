package io.github.skeletonxf.ui.credits

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import io.github.skeletonxf.ui.strings.LocalStrings
import io.github.skeletonxf.ui.theme.HnefataflColors
import io.github.skeletonxf.ui.theme.PreviewSurface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

data class CreditsState(
    private val scope: CoroutineScope,
    private val credits: Credits,
) {

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

    sealed class State {
        data class NoContent(val isLoading: Boolean, val error: Throwable?) : State()
        data class Content(val credits: List<Library>) : State()
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
        if (s is CreditsState.State.NoContent && !s.isLoading && s.error == null) {
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
    Box(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Button(onClick = onBack, modifier = Modifier.padding(horizontal = 12.dp)) {
            Text(text = strings.back)
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // TODO: Move Title into header, need an actual header component
            item(key = "Title") {
                Text(text = strings.title, fontSize = 24.sp)
                Spacer(modifier = Modifier.height(16.dp))
            }
            if (state is CreditsState.State.Content) {
                items(state.credits) { library ->
                    Text(text = library.name)
                    Spacer(modifier = Modifier.height(8.dp))
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