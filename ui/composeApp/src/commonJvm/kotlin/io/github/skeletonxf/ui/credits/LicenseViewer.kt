package io.github.skeletonxf.ui.credits

import BackButton
import TitleHeader
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.skeletonxf.credits.License
import io.github.skeletonxf.ui.shaderGradient
import io.github.skeletonxf.ui.theme.HnefataflColors
import kotlinx.serialization.Serializable

@Serializable
data class LicenseDetail(
    val libraryName: String,
    val license: License,
)

@Composable
fun LicenseViewerScreen(
    onBack: () -> Unit,
    license: LicenseDetail,
) {
    LicenseViewerContent(
        onBack = onBack,
        libraryName = license.libraryName,
        license = license.license,
    )
}

@Composable
fun LicenseViewerContent(
    onBack: () -> Unit,
    libraryName: String,
    license: License,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .shaderGradient(color1 = HnefataflColors.grey, color2 = Color.White)
            .safeDrawingPadding()
    ) {
        TitleHeader(
            start = {
                BackButton(onClick = onBack, modifier = Modifier.padding(horizontal = 8.dp))
            },
            title = {
                Text(text = libraryName, fontSize = 24.sp)
            },
            modifier = Modifier.background(color = MaterialTheme.colorScheme.background)
        )
        val scrollState = rememberScrollState()
        Box {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SelectionContainer {
                    Text(
                        text = license.name,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                SelectionContainer {
                    // Don't center align the text itself because the copy
                    // is typically written with spaces used to pad titles
                    // assuming left alignment.
                    Text(text = license.text)
                }
            }
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(scrollState),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            )
        }
    }
}
