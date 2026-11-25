package com.smarttranslator.app.presentation

import android.Manifest
import android.app.RemoteInput
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.input.RemoteInputIntentHelper
import com.smarttranslator.app.presentation.network.TranslationData
import com.smarttranslator.app.presentation.theme.SmartTranslatorTheme

// ── Design Tokens ─────────────────────────────────────────────
object AppColors {
    val Background    = Color(0xFF000000)
    val Surface       = Color(0xFF1C1C1E)
    val SurfaceHigh   = Color(0xFF2C2C2E)
    val TextPrimary   = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFAEAEB2)
    val TextHint      = Color(0xFF7C7C80)
    val AccentBlue    = Color(0xFF0A84FF)
    val AccentBlueDim = Color(0xFF0A3A70)
    val AccentGreen   = Color(0xFF30D158)
    val Destructive   = Color(0xFFFF453A)
    val GlassStroke   = Color(0xFF3A3A3C)
}

// ── MainActivity ──────────────────────────────────────────────
class MainActivity : ComponentActivity() {

    private val viewModel: TranslatorViewModel by viewModels()
    private var speechRecognizer: SpeechRecognizer? = null

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) startVoiceListening() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmartTranslatorTheme {
                TranslatorApp(
                    viewModel = viewModel,
                    onMicClick = { checkAndStartListening() }
                )
            }
        }
    }

    private fun checkAndStartListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED) startVoiceListening()
        else requestPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun startVoiceListening() {
        vibrate(50)
        viewModel.setListening()
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: ""
                viewModel.translate(text)
            }
            override fun onError(error: Int) { viewModel.reset() }
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speechRecognizer?.startListening(intent)
    }

    @Suppress("DEPRECATION")
    private fun vibrate(ms: Long) {
        getSystemService(Vibrator::class.java)
            ?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
    }
}

// ── App Root ──────────────────────────────────────────────────
@Composable
fun TranslatorApp(viewModel: TranslatorViewModel, onMicClick: () -> Unit) {
    val state       by viewModel.state.collectAsStateWithLifecycle()
    val showHistory by viewModel.showHistory.collectAsStateWithLifecycle()
    val history     by viewModel.history.collectAsStateWithLifecycle()
    val inputMode   by viewModel.inputMode.collectAsStateWithLifecycle()
    val inputText   by viewModel.inputText.collectAsStateWithLifecycle()
    val isLoading   by viewModel.isLoadingHistory.collectAsStateWithLifecycle()
    val langDir     by viewModel.langDirection.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = showHistory,
            transitionSpec = {
                fadeIn(tween(300)) togetherWith fadeOut(tween(200))
            },
            label = "screen"
        ) { isHistory ->
            if (isHistory) {
                HistoryScreen(
                    history   = history,
                    isLoading = isLoading,
                    onClose   = { viewModel.toggleHistory() }
                )
            } else {
                MainScreen(
                    state        = state,
                    inputMode    = inputMode,
                    langDir      = langDir,
                    inputText    = inputText,
                    onMicClick   = onMicClick,
                    onTextChange = { viewModel.setInputText(it) },
                    onTranslate  = { viewModel.translateFromText() },
                    onToggleMode = { viewModel.toggleInputMode() },
                    onToggleLang = { viewModel.toggleLangDirection() },
                    onHistory    = { viewModel.toggleHistory() },
                    onReset      = { viewModel.reset() }
                )
            }
        }
    }
}

// ── Pantalla principal — todo centrado para pantalla circular ─
@Composable
fun MainScreen(
    state: TranslatorState,
    inputMode: InputMode,
    langDir: LangDirection,
    inputText: String,
    onMicClick: () -> Unit,
    onTextChange: (String) -> Unit,
    onTranslate: () -> Unit,
    onToggleMode: () -> Unit,
    onToggleLang: () -> Unit,
    onHistory: () -> Unit,
    onReset: () -> Unit
) {
    val langLabel = if (langDir == LangDirection.ES_EN) "ES → EN" else "EN → ES"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = state to inputMode,
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
            label = "content"
        ) { (s, mode) ->
            when (s) {
                is TranslatorState.Idle ->
                    if (mode == InputMode.TEXT)
                        TextScreen(inputText, onTextChange, onTranslate, onToggleMode, onToggleLang, onHistory, langLabel)
                    else
                        VoiceScreen(onMicClick, onToggleMode, onToggleLang, onHistory, langLabel)

                is TranslatorState.Listening -> ListeningScreen()
                is TranslatorState.Loading   -> LoadingScreen()
                is TranslatorState.Success   -> SuccessScreen(s.original, s.translated, onReset)
                is TranslatorState.Error     -> ErrorScreen(s.message, s.canRetry, onReset)
            }
        }
    }
}

// ── Pantalla Voz ──────────────────────────────────────────────
@Composable
fun VoiceScreen(
    onMicClick: () -> Unit,
    onToggleMode: () -> Unit,
    onToggleLang: () -> Unit,
    onHistory: () -> Unit,
    langLabel: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Toggle de idioma
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(AppColors.Surface)
                .border(1.dp, AppColors.AccentBlue.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .clickable { onToggleLang() }
                .padding(horizontal = 14.dp, vertical = 4.dp)
        ) {
            Text(
                text = langLabel,
                color = AppColors.AccentBlue,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
        }

        Spacer(Modifier.height(12.dp))

        // Botón micrófono vectorial
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF1A5CCC), Color(0xFF003080))
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(listOf(Color(0xFF4D9FFF), Color(0xFF0050C8))),
                    shape = CircleShape
                )
                .clickable { onMicClick() },
            contentAlignment = Alignment.Center
        ) {
            MicIcon(size = 30.dp, color = Color.White)
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Hablar",
            color = AppColors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = "Toca el micrófono",
            color = AppColors.TextSecondary,
            fontSize = 9.sp
        )

        Spacer(Modifier.height(14.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmallActionButton(label = "Texto", onClick = onToggleMode)
            SmallActionButton(label = "Historial", onClick = onHistory)
        }
    }
}

// ── Pantalla Texto ────────────────────────────────────────────
@Composable
fun TextScreen(
    text: String,
    onTextChange: (String) -> Unit,
    onTranslate: () -> Unit,
    onToggleMode: () -> Unit,
    onToggleLang: () -> Unit,
    onHistory: () -> Unit,
    langLabel: String
) {
    // Launcher para RemoteInput nativo de Wear OS
    val inputKey = "text_input"
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val bundle = RemoteInput.getResultsFromIntent(result.data ?: return@rememberLauncherForActivityResult)
        val captured = bundle?.getCharSequence(inputKey)?.toString() ?: return@rememberLauncherForActivityResult
        onTextChange(captured)
    }

    fun openKeyboard() {
        val remoteInput = RemoteInput.Builder(inputKey)
            .setLabel("Escribe en español...")
            .build()
        val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
        RemoteInputIntentHelper.putRemoteInputsExtra(intent, listOf(remoteInput))
        launcher.launch(intent)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Toggle idioma
        Text(
            text = langLabel,
            color = AppColors.AccentBlue,
            fontSize = 10.sp,
            letterSpacing = 0.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(AppColors.Surface)
                .border(1.dp, AppColors.AccentBlue.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .clickable { onToggleLang() }
                .padding(horizontal = 12.dp, vertical = 3.dp)
        )

        Spacer(Modifier.height(10.dp))

        // Botón para abrir teclado nativo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(AppColors.Surface)
                .border(
                    width = 1.dp,
                    color = if (text.isNotEmpty()) AppColors.AccentBlue else AppColors.GlassStroke,
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { openKeyboard() }
                .padding(horizontal = 10.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (text.isEmpty()) "Toca para escribir..." else text,
                color = if (text.isEmpty()) AppColors.TextTertiary else AppColors.TextPrimary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.height(8.dp))

        // Botón traducir
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (text.isNotEmpty())
                        Brush.horizontalGradient(listOf(Color(0xFF0A84FF), Color(0xFF0050C8)))
                    else
                        Brush.horizontalGradient(listOf(AppColors.Surface, AppColors.Surface))
                )
                .clickable { if (text.isNotEmpty()) onTranslate() }
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Traducir",
                color = if (text.isNotEmpty()) Color.White else AppColors.TextTertiary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmallActionButton(label = "Voz", onClick = onToggleMode)
            SmallActionButton(label = "Historial", onClick = onHistory)
        }
    }
}

// ── Botón acción pequeño ──────────────────────────────────────
@Composable
fun SmallActionButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.GlassStroke, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            color = AppColors.AccentBlue,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ── Escuchando ────────────────────────────────────────────────
@Composable
fun ListeningScreen() {
    val inf = rememberInfiniteTransition(label = "p")
    val pulse by inf.animateFloat(
        initialValue = 0.9f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "pulse"
    )
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .scale(pulse)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(listOf(Color(0xFF1A6B35), Color(0xFF0A3018)))
                )
                .border(
                    1.5.dp,
                    Brush.linearGradient(listOf(Color(0xFF30D158), Color(0xFF1A8038))),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            WaveIcon(size = 26.dp, color = Color.White)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Escuchando",
            color = AppColors.AccentGreen,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Habla en español",
            color = AppColors.TextSecondary,
            fontSize = 10.sp
        )
    }
}

// ── Cargando ──────────────────────────────────────────────────
@Composable
fun LoadingScreen() {
    val inf = rememberInfiniteTransition(label = "l")
    val rotation by inf.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "rot"
    )
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(AppColors.Surface)
                .border(
                    width = 2.dp,
                    brush = Brush.sweepGradient(
                        listOf(Color.Transparent, AppColors.AccentBlue)
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "ES",
                color = AppColors.AccentBlue,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Traduciendo...",
            color = AppColors.TextSecondary,
            fontSize = 12.sp
        )
    }
}

// ── Resultado ─────────────────────────────────────────────────
@Composable
fun SuccessScreen(original: String, translated: String, onReset: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Original
        Text(
            text = original,
            color = AppColors.TextSecondary,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(6.dp))

        // Card resultado con gradiente
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF0D2B52), Color(0xFF071428))
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF1A5299), Color(0xFF0A2952))
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = translated,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.height(12.dp))

        // Botón nueva traducción
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(AppColors.Surface)
                .border(1.dp, AppColors.GlassStroke, RoundedCornerShape(20.dp))
                .clickable { onReset() }
                .padding(horizontal = 18.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Nueva traducción",
                color = AppColors.AccentBlue,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ── Error ─────────────────────────────────────────────────────
@Composable
fun ErrorScreen(message: String, canRetry: Boolean, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color(0xFF2C0A0A))
                .border(1.dp, AppColors.Destructive.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("!", color = AppColors.Destructive, fontSize = 24.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            color = AppColors.Destructive,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
        if (canRetry) {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(AppColors.Surface)
                    .border(1.dp, AppColors.Destructive.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .clickable { onRetry() }
                    .padding(horizontal = 16.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "Reintentar",
                    color = AppColors.Destructive,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ── Historial ─────────────────────────────────────────────────
@Composable
fun HistoryScreen(
    history: List<com.smarttranslator.app.presentation.network.TranslationData>,
    isLoading: Boolean,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        // Header
        Text(
            text = "Historial",
            color = AppColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        when {
            isLoading -> Box(Modifier.weight(1f), Alignment.Center) {
                Text("Cargando...", color = AppColors.TextSecondary, fontSize = 11.sp)
            }
            history.isEmpty() -> Box(Modifier.weight(1f), Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(AppColors.Surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("0", color = AppColors.TextSecondary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "Sin traducciones",
                        color = AppColors.TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
            else -> LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(history) { HistoryCard(it) }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Botón volver
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(AppColors.Surface)
                .border(1.dp, AppColors.GlassStroke, RoundedCornerShape(20.dp))
                .clickable { onClose() }
                .padding(horizontal = 20.dp, vertical = 5.dp)
                .padding(bottom = 8.dp)
        ) {
            Text(
                text = "Volver",
                color = AppColors.AccentBlue,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun HistoryCard(item: com.smarttranslator.app.presentation.network.TranslationData) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.GlassStroke, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = item.original_text,
                color = AppColors.TextSecondary,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.translated_text,
                color = AppColors.TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ── Íconos vectoriales ────────────────────────────────────────

@Composable
fun MicIcon(size: Dp, color: Color) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.09f
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.34f, h * 0.04f),
            size = Size(w * 0.32f, h * 0.56f),
            cornerRadius = CornerRadius(w * 0.16f)
        )
        drawArc(
            color = color,
            startAngle = 0f, sweepAngle = 180f, useCenter = false,
            topLeft = Offset(w * 0.18f, h * 0.28f),
            size = Size(w * 0.64f, h * 0.52f),
            style = Stroke(stroke, cap = StrokeCap.Round)
        )
        drawLine(color, Offset(w * 0.5f, h * 0.80f), Offset(w * 0.5f, h * 0.95f), stroke, StrokeCap.Round)
        drawLine(color, Offset(w * 0.34f, h * 0.95f), Offset(w * 0.66f, h * 0.95f), stroke, StrokeCap.Round)
    }
}

@Composable
fun WaveIcon(size: Dp, color: Color, level: Float = 0.5f) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val xs = listOf(0.14f, 0.38f, 0.62f, 0.86f)
        val base = listOf(0.35f, 0.7f, 0.9f, 0.5f)
        xs.forEachIndexed { i, x ->
            val barH = (base[i] * (0.4f + level * 0.6f)).coerceAtLeast(0.15f)
            drawLine(
                color,
                Offset(w * x, h * (0.5f - barH / 2)),
                Offset(w * x, h * (0.5f + barH / 2)),
                strokeWidth = w * 0.11f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun KeyboardIcon(size: Dp, color: Color) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawRoundRect(
            color = color,
            topLeft = Offset(0f, h * 0.2f),
            size = Size(w, h * 0.6f),
            cornerRadius = CornerRadius(w * 0.12f),
            style = Stroke(w * 0.08f)
        )
        val r = w * 0.05f
        for (row in 0..1) for (col in 0..3) {
            drawCircle(color, r, Offset(w * (0.2f + col * 0.2f), h * (0.38f + row * 0.14f)))
        }
        drawLine(color, Offset(w * 0.3f, h * 0.66f), Offset(w * 0.7f, h * 0.66f), w * 0.08f, StrokeCap.Round)
    }
}

@Composable
fun HistoryIcon(size: Dp, color: Color) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val stroke = w * 0.09f
        drawCircle(color, w * 0.42f, center, style = Stroke(stroke))
        drawLine(color, center, Offset(center.x, w * 0.24f), stroke, StrokeCap.Round)
        drawLine(color, center, Offset(w * 0.68f, center.y + w * 0.1f), stroke, StrokeCap.Round)
    }
}
