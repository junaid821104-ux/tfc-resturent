package com.example.ui.screens.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TfcCrimson
import com.example.ui.theme.TfcJetBlack
import com.example.ui.theme.TfcPureWhite
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val scale = remember { Animatable(0.82f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(1f, animationSpec = tween(420))
        alpha.animateTo(1f, animationSpec = tween(320))
        delay(850)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(TfcJetBlack, Color(0xFF1F0709), TfcJetBlack)
                )
            )
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(32.dp)
                .scale(scale.value)
        ) {
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(TfcCrimson)
                    .border(2.dp, TfcPureWhite.copy(alpha = 0.25f), RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "TFC",
                    style = MaterialTheme.typography.displayLarge.copy(
                        color = TfcPureWhite,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = "TFC RESTAURANT",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = TfcPureWhite,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp
                ),
                modifier = Modifier.alpha(alpha.value)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "SIGNATURE CRISPY KITCHEN",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = TfcCrimson,
                    letterSpacing = 2.sp
                ),
                modifier = Modifier.alpha(alpha.value)
            )
            Spacer(Modifier.height(36.dp))
            CircularProgressIndicator(
                color = TfcCrimson,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
