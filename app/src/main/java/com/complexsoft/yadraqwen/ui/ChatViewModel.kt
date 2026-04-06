package com.complexsoft.yadraqwen.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.complexsoft.yadraqwen.data.ChatMessage
import com.complexsoft.yadraqwen.data.MessageRole
import com.complexsoft.yadraqwen.data.parseModelResponse
import com.yadra.GenerationStats
import com.yadra.TokenCallback
import com.yadra.YadraLLM
import com.yadra.statsCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed class LoadState {
    object Idle : LoadState()
    data class Loading(val message: String = "Iniciando…") : LoadState()
    object Ready : LoadState()
    data class Error(val message: String) : LoadState()
}

class InferenceTpsTracker(private val windowSize: Int = 8) {
    private val timestamps = ArrayDeque<Long>(windowSize + 1)
    var tokenCount: Int    = 0 ; private set
    var liveTps:    Double = 0.0 ; private set

    fun onToken() {
        val now = System.nanoTime()
        timestamps.addLast(now)
        if (timestamps.size > windowSize) timestamps.removeFirst()
        tokenCount++
        liveTps = if (timestamps.size >= 2) {
            val elapsed = (timestamps.last() - timestamps.first()) / 1_000_000_000.0
            (timestamps.size - 1) / elapsed
        } else 0.0
    }

    fun reset() {
        timestamps.clear()
        tokenCount = 0
        liveTps    = 0.0
    }
}

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val llm get() = YadraLLM.instance

    private val _loadState    = MutableStateFlow<LoadState>(LoadState.Idle)
    val loadState: StateFlow<LoadState> = _loadState.asStateFlow()

    private val _messages     = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _thinkingEnabled = MutableStateFlow(false)
    val thinkingEnabled: StateFlow<Boolean> = _thinkingEnabled.asStateFlow()

    private val tpsTracker = InferenceTpsTracker()

    private val _liveTps   = MutableStateFlow(0.0)
    val liveTps: StateFlow<Double> = _liveTps.asStateFlow()

    private val _tokenCount = MutableStateFlow(0)
    val tokenCount: StateFlow<Int> = _tokenCount.asStateFlow()

    private val _lastStats = MutableStateFlow<GenerationStats?>(null)
    val lastStats: StateFlow<GenerationStats?> = _lastStats.asStateFlow()

    private var rawBuffer      = ""
    private var thinkStartTime = 0L

    fun initialize(context: Context) {
        if (_loadState.value is LoadState.Ready) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _loadState.value = LoadState.Loading("Preparando modelo…")
                llm.loadModel(
                    context    = context,
                    maxSeqLen  = 512,
                    onProgress = { msg -> _loadState.value = LoadState.Loading(msg) }
                )
                _loadState.value = LoadState.Ready
            } catch (e: Exception) {
                _loadState.value = LoadState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    fun toggleThinking() {
        val next = !_thinkingEnabled.value
        llm.enableThinking = next
        _thinkingEnabled.value = next
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _isGenerating.value) return

        val userMsg = ChatMessage(role = MessageRole.USER, content = text.trim())
        _messages.value += userMsg

        val assistantId = UUID.randomUUID().toString()
        _messages.value += ChatMessage(
                    id                = assistantId,
                    role              = MessageRole.ASSISTANT,
                    content           = "",
                    isStreaming       = true,
                    isThinkingEnabled = _thinkingEnabled.value
                )
        _isGenerating.value = true

        rawBuffer       = ""
        thinkStartTime  = 0L
        _lastStats.value = null
        _liveTps.value   = 0.0
        _tokenCount.value = 0
        tpsTracker.reset()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                llm.chat(
                    userMessage  = text.trim(),
                    maxNewTokens = llm.defaultMaxNewTokens,
                    temperature  = llm.defaultTemperature,
                    thinking     = _thinkingEnabled.value,
                    onToken      = TokenCallback { token ->
                        // Actualizar TPS en vivo
                        tpsTracker.onToken()
                        _liveTps.value    = tpsTracker.liveTps
                        _tokenCount.value = tpsTracker.tokenCount
                        // Parsear y emitir token al chat
                        liveUpdateMessage(assistantId, token)
                    },
                    onStats = statsCallback { stats ->
                        _lastStats.value = stats
                    }
                )
            } catch (e: Exception) {
                replaceAssistantMessage(assistantId, "⚠️ Error: ${e.message}", streaming = false)
            } finally {
                finalizeAssistantMessage(assistantId)
                _isGenerating.value = false
                llm.resetContext()
            }
        }
    }

    fun clearConversation() {
        _messages.value   = emptyList()
        _lastStats.value  = null
        _liveTps.value    = 0.0
        _tokenCount.value = 0
        llm.resetContext()
    }

    private fun liveUpdateMessage(id: String, token: String) {
        rawBuffer += token
        val current = _messages.value.toMutableList()
        val idx = current.indexOfFirst { it.id == id }
        if (idx < 0) return
        val msg = current[idx]

        val openTag  = "<think>"
        val closeTag = "</think>"
        val hasOpen  = rawBuffer.contains(openTag)
        val hasClose = rawBuffer.contains(closeTag)

        when {
            hasOpen && hasClose -> {
                val openIdx  = rawBuffer.indexOf(openTag) + openTag.length
                val closeIdx = rawBuffer.indexOf(closeTag)
                val thinking = rawBuffer.substring(openIdx, closeIdx).trim().ifBlank { null }
                val response = rawBuffer.substring(closeIdx + closeTag.length).trim()
                val duration = if (!msg.isThinkingComplete && thinkStartTime > 0L)
                    ((System.currentTimeMillis() - thinkStartTime) / 1000).toInt()
                else msg.thinkingDurationSec

                current[idx] = msg.copy(
                    thinkingContent     = thinking,
                    isThinkingComplete  = true,
                    thinkingDurationSec = duration,
                    content             = response
                )
                _messages.value = current
            }
            hasOpen && !hasClose -> {
                if (thinkStartTime == 0L) thinkStartTime = System.currentTimeMillis()
                val openIdx    = rawBuffer.indexOf(openTag) + openTag.length
                val thinkSoFar = rawBuffer.substring(openIdx)
                current[idx] = msg.copy(
                    thinkingContent    = thinkSoFar,
                    isThinkingComplete = false,
                    content            = ""
                )
                _messages.value = current
            }
            _thinkingEnabled.value -> { /* esperar <think> */ }
            else -> {
                current[idx] = msg.copy(content = rawBuffer)
                _messages.value = current
            }
        }
    }

    private fun finalizeAssistantMessage(id: String) {
        val current = _messages.value.toMutableList()
        val idx = current.indexOfFirst { it.id == id }
        if (idx < 0) return
        val msg = current[idx]

        if (!msg.isThinkingComplete && rawBuffer.contains("<think>")) {
            val (thinking, complete, response) = parseModelResponse(rawBuffer)
            val duration = if (thinkStartTime > 0L)
                ((System.currentTimeMillis() - thinkStartTime) / 1000).toInt()
            else 0
            current[idx] = msg.copy(
                thinkingContent     = thinking,
                isThinkingComplete  = complete,
                thinkingDurationSec = if (complete) duration else msg.thinkingDurationSec,
                content             = response,
                isStreaming         = false
            )
        } else {
            current[idx] = msg.copy(isStreaming = false)
        }
        _messages.value = current
    }

    private fun replaceAssistantMessage(id: String, content: String, streaming: Boolean) {
        val current = _messages.value.toMutableList()
        val idx = current.indexOfFirst { it.id == id }
        if (idx < 0) return
        current[idx] = current[idx].copy(content = content, isStreaming = streaming)
        _messages.value = current
    }

    override fun onCleared() {
        super.onCleared()
        llm.destroy()
    }
}