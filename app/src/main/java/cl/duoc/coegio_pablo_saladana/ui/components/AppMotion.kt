package cl.duoc.coegio_pablo_saladana.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

object AppMotionDefaults {
    const val NavigationDurationMillis = 360
    const val ItemDurationMillis = 420
    const val ItemDelayMillis = 55
    const val MaximumStaggerDelayMillis = 275
    const val EndOfContentPulseMillis = 1_100
    const val ItemInitialScale = 0.97f
    const val EndOfContentMinimumScale = 0.96f
}

@Composable
fun AnimatedListItem(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(
            (index * AppMotionDefaults.ItemDelayMillis)
                .coerceAtMost(AppMotionDefaults.MaximumStaggerDelayMillis)
                .toLong()
        )
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(
            animationSpec = tween(AppMotionDefaults.ItemDurationMillis)
        ) + slideInVertically(
            animationSpec = tween(
                durationMillis = AppMotionDefaults.ItemDurationMillis,
                easing = FastOutSlowInEasing
            ),
            initialOffsetY = { height -> height / 5 }
        ) + scaleIn(
            initialScale = AppMotionDefaults.ItemInitialScale,
            animationSpec = tween(AppMotionDefaults.ItemDurationMillis)
        )
    ) {
        content()
    }
}

@Composable
fun EndOfContent(
    message: String,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "end_of_content")
    val scale by transition.animateFloat(
        initialValue = AppMotionDefaults.EndOfContentMinimumScale,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = AppMotionDefaults.EndOfContentPulseMillis,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "end_of_content_scale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 24.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = 0.7f + ((scale - AppMotionDefaults.EndOfContentMinimumScale) * 5f)
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HorizontalDivider(modifier = Modifier.fillMaxWidth(0.32f))
        Text(
            text = message,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
