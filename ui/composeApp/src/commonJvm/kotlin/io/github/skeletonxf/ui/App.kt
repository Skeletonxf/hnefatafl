package io.github.skeletonxf.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.skeletonxf.logging.ForestLogger
import io.github.skeletonxf.logging.Log
import io.github.skeletonxf.logging.LogLevel
import io.github.skeletonxf.logging.PrintLogger
import io.github.skeletonxf.logging.Tree
import io.github.skeletonxf.logging.TreeIdentifier
import io.github.skeletonxf.settings.FilePaths
import io.github.skeletonxf.settings.Setting
import io.github.skeletonxf.settings.Settings
import io.github.skeletonxf.ui.nav.NavigationRoot
import io.github.skeletonxf.ui.strings.LocalStrings
import io.github.skeletonxf.ui.strings.ProvideStrings
import io.github.skeletonxf.ui.strings.Strings
import io.github.skeletonxf.ui.theme.HnefataflMaterialTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.nio.file.Path
import java.nio.file.Paths

val localBackgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

data class Environment(
    val forestLogger: ForestLogger,
    val filePaths: FilePaths,
    val settings: Settings,
) {
    companion object {
        fun dummy(): Environment = Environment(
            forestLogger = ForestLogger(),
            filePaths = object : FilePaths {
                override fun settingsPath(): Path = Paths.get("./settings.toml")
            },
            settings = object : Settings {
                override val locale: Setting<String> = object : Setting<String> {
                    override val value = mutableStateOf("en-GB")
                    override fun set(value: String) = Unit
                }
                override fun save(immediate: Boolean) = Unit
            }
        )
    }
}

fun setup(filePaths: FilePaths): Environment {
    Log.add(PrintLogger())
    val forest = ForestLogger()
    Log.add(forest)
    return Environment(
        forestLogger = forest,
        filePaths = filePaths,
        settings = Settings.new(ioScope = localBackgroundScope, filePaths = filePaths)
    )
}

@Composable
fun App(environment: Environment) {
    val forest = environment.forestLogger
    Root(
        environment = environment,
        timber = forest.timber.collectAsState().value,
        onDismiss = forest::dismiss,
    )
}

@Composable
fun Root(
    environment: Environment,
    timber: List<Tree>,
    onDismiss: (TreeIdentifier) -> Unit,
) = HnefataflMaterialTheme {
    ProvideStrings(settings = environment.settings) {
        NavigationRoot(environment) {
            SnackbarHost(timber, onDismiss)
        }
    }
}

@Composable
private fun SnackbarHost(
    timber: List<Tree>,
    onDismiss: (TreeIdentifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current.component
    if (timber.isNotEmpty()) {
        val tree = timber.first()
        Snackbar(
            modifier = modifier.padding(16.dp),
            dismissAction = {
                TextButton(
                    onClick = { onDismiss(tree.id) },
                    modifier = Modifier.padding(horizontal = 8.dp),
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                ) {
                    Text(text = strings.ok)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            dismissActionContentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Error messages aren't localised at the moment so add a flag prefix for non-English locales to make
                // it a little more clear that the content isn't localised.
                val prefix = when (LocalStrings.current.type) {
                    Strings.Type.British, Strings.Type.American -> ""
                    Strings.Type.CastilianSpanish, Strings.Type.LatinAmericanSpanish -> "🇬🇧 "
                }
                val level = when (tree.level) {
                    LogLevel.Debug -> "D: "
                    LogLevel.Warn -> "W: "
                    LogLevel.Error -> "E: "
                }
                SelectionContainer {
                    Text(text = "$prefix$level${tree.message}", textAlign = TextAlign.Center)
                }
            }
        }
    }
}
