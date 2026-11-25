package com.smarttranslator.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarttranslator.app.presentation.network.RetrofitClient
import com.smarttranslator.app.presentation.network.TranslateRequest
import com.smarttranslator.app.presentation.network.TranslationData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.SocketTimeoutException

// ── Estados de la UI ──────────────────────────────────────────

sealed class TranslatorState {
    object Idle : TranslatorState()
    object Listening : TranslatorState()
    object Loading : TranslatorState()
    data class Success(
        val original: String,
        val translated: String,
        val savedToHistory: Boolean = true
    ) : TranslatorState()
    data class Error(
        val message: String,
        val canRetry: Boolean = true
    ) : TranslatorState()
}

enum class InputMode { VOICE, TEXT }
enum class LangDirection { ES_EN, EN_ES }

// ── ViewModel ─────────────────────────────────────────────────

class TranslatorViewModel : ViewModel() {

    private val _state = MutableStateFlow<TranslatorState>(TranslatorState.Idle)
    val state: StateFlow<TranslatorState> = _state.asStateFlow()

    private val _history = MutableStateFlow<List<TranslationData>>(emptyList())
    val history: StateFlow<List<TranslationData>> = _history.asStateFlow()

    private val _showHistory = MutableStateFlow(false)
    val showHistory: StateFlow<Boolean> = _showHistory.asStateFlow()

    private val _inputMode = MutableStateFlow(InputMode.VOICE)
    val inputMode: StateFlow<InputMode> = _inputMode.asStateFlow()

    private val _langDirection = MutableStateFlow(LangDirection.ES_EN)
    val langDirection: StateFlow<LangDirection> = _langDirection.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isLoadingHistory = MutableStateFlow(false)
    val isLoadingHistory: StateFlow<Boolean> = _isLoadingHistory.asStateFlow()

    // ── Acciones ──────────────────────────────────────────────

    fun setListening() {
        _state.value = TranslatorState.Listening
    }

    fun setInputText(text: String) {
        _inputText.value = text
    }

    fun toggleInputMode() {
        _inputMode.value = if (_inputMode.value == InputMode.VOICE) InputMode.TEXT else InputMode.VOICE
        reset()
    }

    fun toggleLangDirection() {
        _langDirection.value = if (_langDirection.value == LangDirection.ES_EN) LangDirection.EN_ES else LangDirection.ES_EN
        reset()
    }

    fun translateFromText() {
        val text = _inputText.value.trim()
        if (text.isBlank()) {
            _state.value = TranslatorState.Error("Escribe algo para traducir", canRetry = false)
            return
        }
        translate(text)
    }

    fun translate(spokenText: String) {
        val cleanText = spokenText.trim()
        if (cleanText.isBlank()) {
            _state.value = TranslatorState.Error("No se detectó texto", canRetry = true)
            return
        }

        _state.value = TranslatorState.Loading

        val sourceLang = if (_langDirection.value == LangDirection.ES_EN) "ES" else "EN"
        val targetLang = if (_langDirection.value == LangDirection.ES_EN) "EN" else "ES"

        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.translate(
                    TranslateRequest(cleanText, source_lang = sourceLang, target_lang = targetLang)
                )
                if (response.success && response.data != null) {
                    _state.value = TranslatorState.Success(
                        original = response.data.original_text,
                        translated = response.data.translated_text,
                        savedToHistory = response.saved
                    )
                    _inputText.value = ""
                    if (_showHistory.value) loadHistory()
                } else {
                    _state.value = TranslatorState.Error("Error al traducir. Intenta de nuevo.")
                }
            } catch (e: ConnectException) {
                _state.value = TranslatorState.Error("Sin conexión al servidor")
            } catch (e: SocketTimeoutException) {
                _state.value = TranslatorState.Error("Tiempo de espera agotado")
            } catch (e: Exception) {
                _state.value = TranslatorState.Error("Error: ${e.message ?: "desconocido"}")
            }
        }
    }

    fun loadHistory() {
        _isLoadingHistory.value = true
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getHistory()
                if (response.success) {
                    _history.value = response.data
                }
            } catch (_: Exception) {
                // No mostrar error en historial, simplemente queda vacío
            } finally {
                _isLoadingHistory.value = false
            }
        }
    }

    fun toggleHistory() {
        _showHistory.value = !_showHistory.value
        if (_showHistory.value) loadHistory()
    }

    fun reset() {
        _state.value = TranslatorState.Idle
    }
}
