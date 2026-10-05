package com.smarttranslator.app.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Text
import com.smarttranslator.app.presentation.theme.SmartTranslatorTheme

// ── Design Tokens ─────────────────────────────────────────────
object AppColors {
    val Background    = Color(0xFF000000)
    val Surface       = Color(0xFF1C1C1E)
    val SurfaceHigh   = Color(0xFF2C2C2E)
    val TextPrimary   = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF8E8E93)
    val TextTertiary  = Color(0xFF3A3A3C)
    val AccentBlue    = Color(0xFF0A84FF)
    val AccentGreen   = Color(0xFF30D158)
    val Destructive   = Color(0xFFFF453A)
    val Separator     = Color(0xFF2C2C2E)
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
    val focusRequester = remember { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
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

        // Campo de texto
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
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                textStyle = TextStyle(
                    color = AppColors.TextPrimary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                ),
                cursorBrush = SolidColor(AppColors.AccentBlue),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onTranslate() }),
                maxLines = 2,
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                        if (text.isEmpty()) {
                            Text(
                                "Escribe aquí...",
                                color = AppColors.TextTertiary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                        inner()
                    }
                }
            )
        }

        Spacer(Modifier.height(8.dp))

        // Botón traducir
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.horizontalGradient(listOf(Color(0xFF0A84FF), Color(0xFF0050C8)))
                )
                .clickable { onTranslate() }
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Traducir",
                color = Color.White,
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

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
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

// ── Íconos vectoriales (sin emojis) ──────────────────────────

@Composable
fun MicIcon(size: androidx.compose.ui.unit.Dp, color: Color) {
    androidx.compose.foundation.Canvas(
        modifier = Modifier.size(size)
    ) {
        val w = this.size.width
        val h = this.size.height
        val paint = androidx.compose.ui.graphics.Paint().apply {
            this.color = color
            strokeWidth = w * 0.08f
            style = androidx.compose.ui.graphics.PaintingStyle.Stroke
        }
        // Cuerpo del micrófono
        drawContext.canvas.drawRoundRect(
            left = w * 0.33f, top = h * 0.05f,
            right = w * 0.67f, bottom = h * 0.62f,
            radiusX = w * 0.17f, radiusY = h * 0.17f,
            paint = paint.apply { style = androidx.compose.ui.graphics.PaintingStyle.Fill; this.color = color.copy(alpha = 0.9f) }
        )
        // Arco
        drawContext.canvas.drawArc(
            left = w * 0.18f, top = h * 0.30f,
            right = w * 0.82f, bottom = h * 0.82f,
            startAngle = 0f, sweepAngle = 180f,
            useCenter = false,
            paint = paint.apply { style = androidx.compose.ui.graphics.PaintingStyle.Stroke; this.color = color }
        )
        // Línea base
        drawContext.canvas.drawLine(
            androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.82f),
            androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.95f),
            paint
        )
        drawContext.canvas.drawLine(
            androidx.compose.ui.geometry.Offset(w * 0.33f, h * 0.95f),
            androidx.compose.ui.geometry.Offset(w * 0.67f, h * 0.95f),
            paint
        )
    }
}

@Composable
fun WaveIcon(size: androidx.compose.ui.unit.Dp, color: Color) {
    androidx.compose.foundation.Canvas(
        modifier = Modifier.size(size)
    ) {
        val w = this.size.width
        val h = this.size.height
        val paint = androidx.compose.ui.graphics.Paint().apply {
            this.color = color
            strokeWidth = w * 0.09f
            style = androidx.compose.ui.graphics.PaintingStyle.Stroke
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        }
        // 3 líneas de onda
        val lines = listOf(
            Pair(0.1f, 0.9f) to Pair(0.25f, 0.75f),
            Pair(0.2f, 0.8f) to Pair(0.5f, 0.5f),
            Pair(0.35f, 0.65f) to Pair(0.8f, 0.2f)
        )
        val offsets = listOf(
            0.15f to 0.5f,
            0.38f to 0.5f,
            0.62f to 0.5f,
            0.85f to 0.5f
        )
        val heights = listOf(0.3f, 0.6f, 0.9f, 0.5f)
        offsets.forEachIndexed { i, (x, _) ->
            val barH = heights[i]
            drawContext.canvas.drawLine(
                androidx.compose.ui.geometry.Offset(w * x, h * (0.5f - barH / 2)),
                androidx.compose.ui.geometry.Offset(w * x, h * (0.5f + barH / 2)),
                paint
            )
        }
    }
}
