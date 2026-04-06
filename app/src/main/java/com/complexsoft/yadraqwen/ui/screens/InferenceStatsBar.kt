package com.complexsoft.yadraqwen.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yadra.GenerationStats

@Composable
fun InferenceStatsBar(
    isGenerating: Boolean,
    liveTps: Double,
    tokenCount: Int,
    finalStats: GenerationStats?,
    modifier: Modifier = Modifier
) {
    val visible = isGenerating || finalStats != null

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180)) + expandVertically(
            tween(220, easing = FastOutSlowInEasing), expandFrom = Alignment.Top
        ),
        exit = fadeOut(tween(260)) + shrinkVertically(tween(220, easing = FastOutSlowInEasing)),
        modifier = modifier
    ) {
        Crossfade(
            targetState = isGenerating, animationSpec = tween(220), label = "stats_mode"
        ) { generating ->
            if (generating) {
                LiveTpsRow(
                    tps = liveTps, tokenCount = tokenCount, modifier = Modifier.fillMaxWidth()
                )
            } else {
                finalStats?.let { FinalStatsCard(stats = it, modifier = Modifier.fillMaxWidth()) }
            }
        }
    }
}

@Composable
private fun LiveTpsRow(
    tps: Double, tokenCount: Int, modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurfV = MaterialTheme.colorScheme.onSurfaceVariant

    // Animación suave del número de tok/s
    val animTps by animateFloatAsState(
        targetValue = tps.toFloat(),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "tps_value"
    )

    val pulseAlpha by rememberInfiniteTransition(label = "pulse_inf").animateFloat(
        initialValue = 0.35f, targetValue = 1f, animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse
        ), label = "pulse_alpha"
    )

    Row(
        modifier = modifier.padding(horizontal = 20.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(primary.copy(alpha = pulseAlpha))
        )

        val tpsText = if (animTps > 0.5f) "%.1f tok/s".format(animTps) else "…"
        Text(
            text = tpsText, style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = primary,
                letterSpacing = 0.4.sp
            )
        )

        Spacer(Modifier.weight(1f))

        if (tokenCount > 0) {
            Text(
                text = "$tokenCount tokens", style = MaterialTheme.typography.labelSmall.copy(
                    color = onSurfV
                )
            )
        }
    }
}

@Composable
private fun FinalStatsCard(
    stats: GenerationStats, modifier: Modifier = Modifier
) {
    val surfaceVar = MaterialTheme.colorScheme.surfaceVariant
    val outline = MaterialTheme.colorScheme.outlineVariant

    Row(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceVar.copy(alpha = 0.55f))
            .padding(horizontal = 0.dp, vertical = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatCell(
            label = "PREFILL",
            value = "%.0f tok/s".format(stats.prefillTps),
            detail = "${stats.prefillTokens} tok · %.0f ms".format(stats.prefillMs),
            highlight = false,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 10.dp)
        )

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(38.dp)
                .background(outline.copy(alpha = 0.6f))
        )

        StatCell(
            label = "DECODE",
            value = "%.1f tok/s".format(stats.decodeTps),
            detail = "${stats.decodeTokens} tok · %.0f ms".format(stats.decodeMs),
            highlight = true,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 10.dp)
        )
    }
}

@Composable
private fun StatCell(
    label: String, value: String, detail: String, highlight: Boolean, modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurf = MaterialTheme.colorScheme.onSurface
    val onSurfV = MaterialTheme.colorScheme.onSurfaceVariant

    val valueAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "cell_alpha"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label, style = MaterialTheme.typography.labelSmall.copy(
                color = onSurfV.copy(alpha = 0.75f),
                letterSpacing = 1.2.sp,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium
            )
        )

        Text(
            text = value, style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (highlight) primary else onSurf,
                fontSize = 16.sp
            ), modifier = Modifier.alpha(valueAlpha)
        )
        Text(
            text = detail,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall.copy(
                color = onSurfV.copy(alpha = 0.65f), fontSize = 10.sp
            )
        )
    }
}