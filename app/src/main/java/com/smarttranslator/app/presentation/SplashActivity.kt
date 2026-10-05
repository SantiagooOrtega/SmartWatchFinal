package com.smarttranslator.app.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import com.smarttranslator.app.presentation.theme.SmartTranslatorTheme
import kotlinx.coroutines.delay

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmartTranslatorTheme {
                SplashScreen {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            }
        }
    }
}

@Composable
fun SplashScreen(onFinished: () -> Unit) {

    // Escala del logo con spring
    var startAnim by remember { mutableStateOf(false) }
    val logoScale by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0.3f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label = "logoScale"
    )

    // Anillo pulsante
    val inf = rememberInfiniteTransition(label = "ring")
    val ringScale by inf.animateFloat(
        initialValue = 1f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(1000, easing = EaseOut), RepeatMode.Restart),
        label = "rScale"
    )
    val ringAlpha by inf.animateFloat(
        initialValue = 0.5f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1000, easing = EaseOut), RepeatMode.Restart),
        label = "rAlpha"
    )

    // Fade del texto
    var textAlpha by remember { mutableFloatStateOf(0f) }
    val animTextAlpha by animateFloatAsState(
        targetValue = textAlpha,
        animationSpec = tween(500),
        label = "textAlpha"
    )

    LaunchedEffect(Unit) {
        startAnim = true
        delay(400)
        textAlpha = 1f
        delay(2000)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Logo animado ──────────────────────────────────
            Box(contentAlignment = Alignment.Center) {
                // Anillo exterior
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .scale(ringScale)
                        .alpha(ringAlpha)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    Color(0xFF0A84FF).copy(alpha = 0.4f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                // Logo principal
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .scale(logoScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFF1C6DD0), Color(0xFF003A8C))
                            )
                        )
                        .border(
                            1.5.dp,
                            Brush.linearGradient(listOf(Color(0xFF5BA4FF), Color(0xFF0050E6))),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🌐", fontSize = 26.sp)
                }
            }

            // ── Textos ────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.alpha(animTextAlpha)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Smart",
                        color = AppColors.TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Translator",
                        color = AppColors.AccentBlue,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 0.5.sp
                    )
                }

                // Badge ES→EN
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF0A84FF), Color(0xFF30D158))
                            )
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "ES  →  EN",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }
            }
        }
    }
}
