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
import android.speech.tts.TextToSpeech
import java.util.Locale
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
    val Surface       = Color(0xFF222224)   // tarjetas estilo watchOS
    val SurfaceHigh   = Color(0xFF2C2C2E)
    val TextPrimary   = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF98989D)   // secondaryLabel
    val TextHint      = Color(0xFF636366)   // tertiaryLabel
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
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    /** Nivel de voz normalizado 0..1, para animar la pantalla "Escuchando". */
    private val voiceLevel = mutableFloatStateOf(0f)
    private val partialText = mutableStateOf("")

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startVoiceListening()
        else viewModel.showError("Permiso de micrófono denegado", canRetry = false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this) { status -> ttsReady = status == TextToSpeech.SUCCESS }
        setContent {
            SmartTranslatorTheme {
                TranslatorApp(
                    viewModel = viewModel,
                    voiceLevel = voiceLevel.floatValue,
                    partialText = partialText.value,
                    onMicClick = { checkAndStartListening() },
                    onCancelListening = { cancelListening() },
                    onSpeak = { text, lang -> speak(text, lang) }
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
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            viewModel.showError("Reconocimiento de voz no disponible. Usa texto.", canRetry = false)
            return
        }
        vibrate(40)
        partialText.value = ""
        voiceLevel.floatValue = 0f
        viewModel.setListening()
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val lang = if (viewModel.langDirection.value == LangDirection.ES_EN) "es-ES" else "en-US"
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: ""
                vibrate(30)
                viewModel.translate(text)
            }
            override fun onPartialResults(partialResults: Bundle?) {
                partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()?.let { partialText.value = it }
            }
            override fun onRmsChanged(rmsdB: Float) {
                // rmsdB va aprox. de -2 a 10
                voiceLevel.floatValue = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            }
            override fun onError(error: Int) {
                val msg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No te escuché bien"
                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Sin red para reconocer voz"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Falta permiso de micrófono"
                    SpeechRecognizer.ERROR_CLIENT -> null // cancelado por el usuario
                    else -> "Error de voz ($error)"
                }
                if (msg == null) viewModel.reset() else viewModel.showError(msg, canRetry = true)
            }
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { voiceLevel.floatValue = 0f }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speechRecognizer?.startListening(intent)
    }

    /** Lee en voz alta la traducción con el motor de voz del reloj. */
    private fun speak(text: String, lang: String) {
        val engine = tts ?: return
        if (!ttsReady) return
        val locale = if (lang == "EN") Locale.US else Locale("es", "ES")
        if (engine.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE) engine.language = locale
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "translation")
    }

    private fun cancelListening() {
        speechRecognizer?.cancel()
        viewModel.reset()
    }

    private fun vibrate(ms: Long) {
        getSystemService(Vibrator::class.java)
            ?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        tts?.shutdown()
    }
}

// ── App Root ──────────────────────────────────────────────────
@Composable
fun TranslatorApp(
    viewModel: TranslatorViewModel,
    voiceLevel: Float,
    partialText: String,
    onMicClick: () -> Unit,
    onCancelListening: () -> Unit,
    onSpeak: (String, String) -> Unit
) {
    val state       by viewModel.state.collectAsStateWithLifecycle()
    val showHistory by viewModel.showHistory.collectAsStateWithLifecycle()
    val history     by viewModel.history.collectAsStateWithLifecycle()
    val inputMode   by viewModel.inputMode.collectAsStateWithLifecycle()
    val inputText   by viewModel.inputText.collectAsStateWithLifecycle()
    val isLoading   by viewModel.isLoadingHistory.collectAsStateWithLifecycle()
    val historyErr  by viewModel.historyError.collectAsStateWithLifecycle()
    val langDir     by viewModel.langDirection.collectAsStateWithLifecycle()

    // Botón/gesto "atrás" del reloj: vuelve a la pantalla principal en vez de cerrar la app
    BackHandler(enabled = showHistory) { viewModel.toggleHistory() }
    BackHandler(enabled = !showHistory && state !is TranslatorState.Idle) {
        if (state is TranslatorState.Listening) onCancelListening() else viewModel.reset()
    }

    AppScaffold(
        modifier = Modifier.background(AppColors.Background),
        timeText = { TimeText() }
    ) {
        AnimatedContent(
            targetState = showHistory,
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) },
            label = "screen"
        ) { isHistory ->
            if (isHistory) {
                HistoryScreen(
                    history   = history,
                    isLoading = isLoading,
                    error     = historyErr,
                    onRetry   = { viewModel.loadHistory() },
                    onClose   = { viewModel.toggleHistory() }
                )
            } else {
                MainScreen(
                    state        = state,
                    inputMode    = inputMode,
                    langDir      = langDir,
                    inputText    = inputText,
                    voiceLevel   = voiceLevel,
                    partialText  = partialText,
                    onMicClick   = onMicClick,
                    onCancel     = onCancelListening,
                    onSpeak      = onSpeak,
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

// ── Pantalla principal ────────────────────────────────────────
@Composable
fun MainScreen(
    state: TranslatorState,
    inputMode: InputMode,
    langDir: LangDirection,
    inputText: String,
    voiceLevel: Float,
    partialText: String,
    onMicClick: () -> Unit,
    onCancel: () -> Unit,
    onSpeak: (String, String) -> Unit,
    onTextChange: (String) -> Unit,
    onTranslate: () -> Unit,
    onToggleMode: () -> Unit,
    onToggleLang: () -> Unit,
    onHistory: () -> Unit,
    onReset: () -> Unit
) {
    val (from, to) = if (langDir == LangDirection.ES_EN) "ES" to "EN" else "EN" to "ES"

    AnimatedContent(
        targetState = state to inputMode,
        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) },
        label = "content"
    ) { (s, mode) ->
        when (s) {
            is TranslatorState.Idle ->
                if (mode == InputMode.TEXT)
                    TextScreen(inputText, from, to, onTextChange, onTranslate, onToggleMode, onToggleLang, onHistory)
                else
                    VoiceScreen(from, to, onMicClick, onToggleMode, onToggleLang, onHistory)

            is TranslatorState.Listening -> ListeningScreen(from, voiceLevel, partialText, onCancel)
            is TranslatorState.Loading   -> LoadingScreen(from, to)
            is TranslatorState.Success   -> SuccessScreen(s.original, s.translated, from, to, s.savedToHistory, { onSpeak(s.translated, to) }, onReset)
            is TranslatorState.Error     -> ErrorScreen(s.message, s.canRetry, onReset)
        }
    }
}

// ── Selector de idioma (chip) ─────────────────────────────────
@Composable
fun LangChip(from: String, to: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .heightIn(min = 36.dp)
            .clip(RoundedCornerShape(50))
            .background(AppColors.Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(from, color = AppColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        SwapIcon(14.dp, AppColors.AccentBlue)
        Text(to, color = AppColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

// ── Botón circular con icono (48dp = objetivo táctil mínimo en Wear) ──
@Composable
fun RoundIconButton(
    label: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(AppColors.SurfaceHigh)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) { icon() }
        Spacer(Modifier.height(2.dp))
        Text(label, color = AppColors.TextSecondary, fontSize = 10.sp)
    }
}

// ── Botón ancho tipo píldora ──────────────────────────────────
@Composable
fun PillButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    color: Color = AppColors.AccentBlue,
    filled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(50))
            .then(
                if (filled && enabled) Modifier.background(color)
                else Modifier.background(if (enabled) color.copy(alpha = 0.2f) else AppColors.Surface)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = when {
                !enabled -> AppColors.TextHint
                filled -> Color.White
                else -> color
            },
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ── Pantalla Voz ──────────────────────────────────────────────
@Composable
fun VoiceScreen(
    from: String,
    to: String,
    onMicClick: () -> Unit,
    onToggleMode: () -> Unit,
    onToggleLang: () -> Unit,
    onHistory: () -> Unit
) {
    ScreenScaffold {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(14.dp)) // espacio para la hora
            LangChip(from, to, onToggleLang)
            Spacer(Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RoundIconButton("Texto", onToggleMode) { KeyboardIcon(20.dp, AppColors.TextPrimary) }

                // Micrófono principal con halo
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(AppColors.AccentBlue)
                            .clickable(onClick = onMicClick),
                        contentAlignment = Alignment.Center
                    ) {
                        MicIcon(size = 30.dp, color = Color.White)
                    }
                }

                RoundIconButton("Historial", onHistory) { HistoryIcon(20.dp, AppColors.TextPrimary) }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "Toca para hablar",
                color = AppColors.TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

// ── Pantalla Texto ────────────────────────────────────────────
@Composable
fun TextScreen(
    text: String,
    from: String,
    to: String,
    onTextChange: (String) -> Unit,
    onTranslate: () -> Unit,
    onToggleMode: () -> Unit,
    onToggleLang: () -> Unit,
    onHistory: () -> Unit
) {
    val inputKey = "text_input"
    val inputHint = if (from == "ES") "Escribe en español…" else "Write in English…"

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data ?: return@rememberLauncherForActivityResult
        val captured = RemoteInput.getResultsFromIntent(data)
            ?.getCharSequence(inputKey)?.toString() ?: return@rememberLauncherForActivityResult
        onTextChange(captured)
    }

    fun openKeyboard() {
        val remoteInput = RemoteInput.Builder(inputKey).setLabel(inputHint).build()
        val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
        RemoteInputIntentHelper.putRemoteInputsExtra(intent, listOf(remoteInput))
        launcher.launch(intent)
    }

    val listState = rememberScalingLazyListState()
    ScreenScaffold(scrollState = listState) { contentPadding ->
        ScalingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item { LangChip(from, to, onToggleLang) }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(AppColors.Surface)
                        .border(
                            1.dp,
                            if (text.isNotEmpty()) AppColors.AccentBlue else AppColors.GlassStroke,
                            RoundedCornerShape(18.dp)
                        )
                        .clickable { openKeyboard() }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = text.ifEmpty { inputHint },
                        color = if (text.isEmpty()) AppColors.TextHint else AppColors.TextPrimary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            item {
                PillButton(
                    label = "Traducir",
                    onClick = onTranslate,
                    enabled = text.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    RoundIconButton("Voz", onToggleMode) { MicIcon(20.dp, AppColors.TextPrimary) }
                    RoundIconButton("Historial", onHistory) { HistoryIcon(20.dp, AppColors.TextPrimary) }
                }
            }
        }
    }
}

// ── Escuchando ────────────────────────────────────────────────
@Composable
fun ListeningScreen(from: String, voiceLevel: Float, partialText: String, onCancel: () -> Unit) {
    val level by animateFloatAsState(voiceLevel, tween(120), label = "lvl")
    val inf = rememberInfiniteTransition(label = "p")
    val breathe by inf.animateFloat(
        initialValue = 0.96f, targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "breathe"
    )

    ScreenScaffold {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(96.dp)) {
                // Halo que crece con la voz
                Box(
                    Modifier
                        .size(96.dp)
                        .scale(0.75f + level * 0.25f)
                        .clip(CircleShape)
                        .background(AppColors.AccentGreen.copy(alpha = 0.18f + level * 0.2f))
                )
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .scale(breathe)
                        .clip(CircleShape)
                        .background(AppColors.AccentGreen)
                        .clickable(onClick = onCancel),
                    contentAlignment = Alignment.Center
                ) {
                    WaveIcon(size = 30.dp, color = Color.White, level = level)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = partialText.ifEmpty { if (from == "ES") "Habla en español…" else "Speak in English…" },
                color = if (partialText.isEmpty()) AppColors.AccentGreen else AppColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text("Toca para cancelar", color = AppColors.TextHint, fontSize = 10.sp)
        }
    }
}

// ── Cargando ──────────────────────────────────────────────────
@Composable
fun LoadingScreen(from: String, to: String) {
    ScreenScaffold {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(80.dp), strokeWidth = 4.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$from → $to", color = AppColors.AccentBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("Traduciendo", color = AppColors.TextSecondary, fontSize = 10.sp)
            }
        }
    }
}

// ── Resultado (desplazable con la corona) ─────────────────────
@Composable
fun SuccessScreen(
    original: String,
    translated: String,
    from: String,
    to: String,
    savedToHistory: Boolean,
    onSpeak: () -> Unit,
    onReset: () -> Unit
) {
    // Habla la traducción automáticamente al mostrarla
    LaunchedEffect(translated) { onSpeak() }

    val listState = rememberScalingLazyListState()
    // Ajusta el tamaño de letra a la longitud para que frases cortas se lean de un vistazo
    val resultSize = when {
        translated.length < 25 -> 20.sp
        translated.length < 60 -> 17.sp
        else -> 15.sp
    }

    ScreenScaffold(scrollState = listState) { contentPadding ->
        ScalingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Text(
                    text = "$from  ·  $original",
                    color = AppColors.TextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(AppColors.Surface)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (to == "EN") "INGLÉS" else "ESPAÑOL",
                        color = AppColors.AccentBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.6.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = translated,
                        color = Color.White,
                        fontSize = resultSize,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = resultSize * 1.2f
                    )
                }
            }
            if (!savedToHistory) {
                item {
                    Text(
                        "No se guardó en el historial",
                        color = AppColors.TextHint,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
            item {
                PillButton("Escuchar", onClick = onSpeak, filled = false, modifier = Modifier.fillMaxWidth())
            }
            item {
                PillButton("Nueva", onClick = onReset, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

// ── Error ─────────────────────────────────────────────────────
@Composable
fun ErrorScreen(message: String, canRetry: Boolean, onRetry: () -> Unit) {
    ScreenScaffold {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(AppColors.Destructive.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                ExclamationIcon(22.dp, AppColors.Destructive)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                color = AppColors.TextPrimary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            PillButton(
                label = if (canRetry) "Reintentar" else "Volver",
                onClick = onRetry,
                filled = false,
                color = if (canRetry) AppColors.Destructive else AppColors.AccentBlue
            )
        }
    }
}

// ── Historial ─────────────────────────────────────────────────
@Composable
fun HistoryScreen(
    history: List<TranslationData>,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onClose: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    ScreenScaffold(scrollState = listState) { contentPadding ->
        ScalingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Text("Historial", color = AppColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            when {
                isLoading -> item {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp).padding(top = 8.dp))
                }
                error != null -> {
                    item {
                        Text(error, color = AppColors.Destructive, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                    item { PillButton("Reintentar", onRetry, filled = false) }
                }
                history.isEmpty() -> item {
                    Text(
                        "Aún no hay traducciones",
                        color = AppColors.TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
                else -> items(history, key = { it.id }) { HistoryCard(it) }
            }
            item { PillButton("Volver", onClose, filled = false, modifier = Modifier.padding(top = 4.dp)) }
        }
    }
}

@Composable
fun HistoryCard(item: TranslationData) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(AppColors.Surface)
            .clickable { expanded = !expanded }
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = "${item.source_lang} · ${item.original_text}",
            color = AppColors.TextSecondary,
            fontSize = 11.sp,
            maxLines = if (expanded) Int.MAX_VALUE else 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = item.translated_text,
            color = AppColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = if (expanded) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis
        )
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

@Composable
fun SwapIcon(size: Dp, color: Color) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val s = w * 0.11f
        drawLine(color, Offset(w * 0.1f, h * 0.32f), Offset(w * 0.9f, h * 0.32f), s, StrokeCap.Round)
        drawLine(color, Offset(w * 0.9f, h * 0.32f), Offset(w * 0.7f, h * 0.12f), s, StrokeCap.Round)
        drawLine(color, Offset(w * 0.9f, h * 0.68f), Offset(w * 0.1f, h * 0.68f), s, StrokeCap.Round)
        drawLine(color, Offset(w * 0.1f, h * 0.68f), Offset(w * 0.3f, h * 0.88f), s, StrokeCap.Round)
    }
}

@Composable
fun ExclamationIcon(size: Dp, color: Color) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawLine(color, Offset(w / 2, h * 0.12f), Offset(w / 2, h * 0.6f), w * 0.16f, StrokeCap.Round)
        drawCircle(color, w * 0.09f, Offset(w / 2, h * 0.86f))
    }
}
