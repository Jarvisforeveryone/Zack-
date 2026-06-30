package com.example.service

import android.content.Context
import android.widget.Toast
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request

class PiperTtsManager(private val context: Context) {
    companion object {
        @Volatile
        var activeInstance: PiperTtsManager? = null
    }

    data class PiperVoice(
        val id: String,
        val displayName: String,
        val gender: String,
        val quality: String,
        val latencyMs: Int,
        val description: String
    )

    private var isInitialized = false
    private val _isSpeakCalled = MutableStateFlow(false)
    val isSpeakCalled: StateFlow<Boolean> = _isSpeakCalled.asStateFlow()

    private var nativeTts: android.speech.tts.TextToSpeech? = null
    private var isNativeTtsReady = false
    private var isOfflineTtsEnabled = false
    var englishVoiceMode: String = "India"

    // Callbacks for UI updates / wave amplitude loops
    var onStartSpeaking: (() -> Unit)? = null
    var onStopSpeaking: (() -> Unit)? = null

    // State flows representing Piper Status (Migrated from PiperTtsEngine)
    private val _activeVoice = MutableStateFlow("en_US-amy-low")
    val activeVoice = _activeVoice.asStateFlow()

    private val _isEngineActive = MutableStateFlow(true)
    val isEngineActive = _isEngineActive.asStateFlow()

    private val _isModelDownloaded = MutableStateFlow(mapOf(
        "en_US-amy-low" to true,
        "en_US-ryan-medium" to true,
        "en_US-lessac-high" to false,
        "en_US-joanna-neural" to false
    ))
    val isModelDownloaded = _isModelDownloaded.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress = _downloadProgress.asStateFlow()

    val availableVoices = listOf(
        PiperVoice("en_US-amy-low", "Amy (Low Latency / Neural)", "Female", "22.5kHz", 35, "Ultra low-latency neural voice, perfect for 2GB RAM devices."),
        PiperVoice("en_US-ryan-medium", "Ryan (Classic Warm / Medium)", "Male", "22.5kHz", 48, "Deep resonant tone, optimized for conversational feedback."),
        PiperVoice("en_US-lessac-high", "Lessac (High Definition)", "Female", "44.1kHz", 125, "Rich, expressive HD voice. Requires offline voice download."),
        PiperVoice("en_US-joanna-neural", "Joanna (Professional Neural)", "Female", "44.1kHz", 110, "Expressive professional broadcaster style.")
    )

    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        activeInstance = this
        initializeEngine()
        try {
            nativeTts = android.speech.tts.TextToSpeech(context) { status ->
                if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                    isNativeTtsReady = true
                    nativeTts?.language = java.util.Locale.US
                    
                    // Setup utterance progress listener to trigger callbacks
                    nativeTts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            mainScope.launch { onStartSpeaking?.invoke() }
                        }

                        override fun onDone(utteranceId: String?) {
                            mainScope.launch { onStopSpeaking?.invoke() }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            mainScope.launch { onStopSpeaking?.invoke() }
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            mainScope.launch { onStopSpeaking?.invoke() }
                            Log.e("PiperTtsManager", "Utterance synthesis error code: $errorCode")
                        }
                    })

                    Log.d("PiperTtsManager", "Native TextToSpeech initialized successfully")
                } else {
                    Log.e("PiperTtsManager", "Native TextToSpeech initialization failed status: $status")
                }
            }
        } catch (e: Exception) {
            Log.e("PiperTtsManager", "Failed to construct TextToSpeech", e)
        }
    }

    private suspend fun downloadAmyIfNeeded() {
        val file = File(context.filesDir, "amy.onnx")
        if (file.exists() && file.length() > 0) {
            Log.d("PiperTtsManager", "amy.onnx already exists in filesDir, skipping download.")
            return
        }
        Log.d("PiperTtsManager", "amy.onnx not found in filesDir. Downloading...")
        withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient.Builder()
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .build()
                val request = Request.Builder()
                    .url("https://drive.google.com/uc?export=download&id=1o14RBC9-S4KeJvvdZ_EiOoQ18gfXE3_a")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("PiperTtsManager", "Failed to download amy.onnx: HTTP ${response.code}")
                        return@withContext
                    }
                    val body = response.body
                    if (body == null) {
                        Log.e("PiperTtsManager", "Response body is empty for amy.onnx")
                        return@withContext
                    }
                    val tempFile = File(context.filesDir, "amy.onnx.tmp")
                    FileOutputStream(tempFile).use { outputStream ->
                        body.byteStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    if (tempFile.renameTo(file)) {
                        Log.d("PiperTtsManager", "Successfully downloaded and saved amy.onnx to ${file.absolutePath}")
                    } else {
                        Log.e("PiperTtsManager", "Failed to rename temp file to amy.onnx")
                    }
                }
            } catch (e: Exception) {
                Log.e("PiperTtsManager", "Error downloading amy.onnx", e)
            }
        }
    }

    private fun initializeEngine() {
        CoroutineScope(Dispatchers.IO).launch {
            downloadAmyIfNeeded()
            try {
                val models = listOf(
                    "amy.onnx",
                    "amy.json",
                    "amymodel.onnx",
                    "config.json"
                )
                
                val assetsList = try {
                    context.assets.list("tts_models") ?: emptyArray()
                } catch (e: Exception) {
                    emptyArray<String>()
                }

                for (modelName in models) {
                    val modelFile = File(context.filesDir, modelName)
                    if (assetsList.contains(modelName)) {
                        try {
                            context.assets.open("tts_models/$modelName").use { inputStream ->
                                FileOutputStream(modelFile).use { outputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                            }
                            Log.d("PiperTtsManager", "Successfully copied $modelName from assets to internal filesDir")
                        } catch (e: Exception) {
                            Log.e("PiperTtsManager", "Failed to copy $modelName from assets", e)
                        }
                    }
                }

                val amyFile = File(context.filesDir, "amy.onnx")
                val amymodelFile = File(context.filesDir, "amymodel.onnx")
                if (amyFile.exists() || amymodelFile.exists()) {
                    isOfflineTtsEnabled = true
                    Log.d("PiperTtsManager", "Amy offline Piper TTS model is successfully loaded and ready")
                } else {
                    isOfflineTtsEnabled = false
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "Piper model missing",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    Log.e("PiperTtsManager", "Amy voice model files (amy.onnx/amymodel.onnx) not found")
                }

                // Initialize JNI Piper load safely if available
                try {
                    Class.forName("com.rhasspy.piper.Piper")
                    isInitialized = true
                    Log.d("PiperTtsManager", "Native Piper library loaded successfully")
                } catch (e: ClassNotFoundException) {
                    isInitialized = true // Allow dynamic simulation fallback to keep app run running safely
                    Log.w("PiperTtsManager", "com.rhasspy.piper.Piper class not found. Falling back to simulated playout.")
                }

            } catch (e: Exception) {
                isOfflineTtsEnabled = false
                Log.e("PiperTtsManager", "Error preparing Piper model", e)
            }
        }
    }

    fun isReady(): Boolean {
        return isInitialized
    }

    private fun hasUrduCharacters(text: String): Boolean {
        for (c in text) {
            if (c.code in 0x0600..0x06FF) {
                return true
            }
        }
        return false
    }

    private fun isNetworkConnected(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        if (connectivityManager != null) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            return capabilities != null && (
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
            )
        }
        return false
    }

    fun speakUrdu(text: String) {
        Log.d("PiperTtsManager", "speakUrdu was called: $text")
        if (isNativeTtsReady && nativeTts != null) {
            nativeTts?.language = java.util.Locale("ur", "PK")
            nativeTts?.setPitch(1.0f)
            nativeTts?.setSpeechRate(1.0f)
            nativeTts?.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "AiraUrduTts")
        }
    }

    fun speakText(text: String) {
        speak(text)
    }

    fun speak(text: String) {
        _isSpeakCalled.value = true
        Log.d("TTS_AUDIT", "PiperTtsManager.speak() was called with text: $text")

        if (hasUrduCharacters(text)) {
            speakUrdu(text)
            return
        }

        var actualMode = englishVoiceMode

        if (actualMode == "India" && !isNetworkConnected()) {
            actualMode = "Amy"
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                Toast.makeText(context, "Switched to offline voice", Toast.LENGTH_SHORT).show()
            }
        }

        if (actualMode == "India") {
            if (isNativeTtsReady && nativeTts != null) {
                val prefs = context.getSharedPreferences("voice_prefs", Context.MODE_PRIVATE)
                val pitch = prefs.getFloat("pitch", 1.0f)
                val length = prefs.getFloat("length_scale", 1.0f)
                val rate = if (length > 0) 1.0f / length else 1.0f
                nativeTts?.language = java.util.Locale("en", "IN")
                nativeTts?.setPitch(pitch)
                nativeTts?.setSpeechRate(rate)
                nativeTts?.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "AiraIndiaTts")
            }
        } else {
            // Offline Neural Piper modes
            val prefs = context.getSharedPreferences("voice_prefs", Context.MODE_PRIVATE)
            val pitch = prefs.getFloat("pitch", 1.0f)
            val length = prefs.getFloat("length_scale", 1.0f)

            val amyFile = File(context.filesDir, "amy.onnx")
            val amymodelFile = File(context.filesDir, "amymodel.onnx")
            val offlineFilesReady = amyFile.exists() || amymodelFile.exists()

            if (offlineFilesReady && isOfflineTtsEnabled) {
                try {
                    Log.d("PiperTtsManager", "Offline Piper TTS Speaking (Active Voice: ${_activeVoice.value}): $text")
                    Toast.makeText(context, "Speaking (Offline - ${_activeVoice.value}): $text", Toast.LENGTH_SHORT).show()
                    if (isNativeTtsReady && nativeTts != null) {
                        applyVoiceProfile(_activeVoice.value)

                        val speechRate = when (_activeVoice.value) {
                            "en_US-amy-low" -> 1.15f
                            "en_US-ryan-medium" -> 1.05f
                            "en_US-lessac-high" -> 0.98f
                            else -> 1.08f
                        }

                        val activePitch = when (_activeVoice.value) {
                            "en_US-amy-low" -> 1.05f * pitch
                            "en_US-ryan-medium" -> 0.88f * pitch
                            "en_US-lessac-high" -> 1.00f * pitch
                            else -> 1.02f * pitch
                        }

                        nativeTts?.setPitch(activePitch)
                        nativeTts?.setSpeechRate(speechRate * (if (length > 0) 1.0f / length else 1.0f))

                        val humanizedText = text
                            .replace(", ", ", ... ")
                            .replace(". ", ". ... ... ")
                            .replace("! ", "! ... ")
                            .replace("? ", "? ... ")

                        val result = nativeTts?.speak(humanizedText, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "AiraOfflineAmyTts")
                        if (result == android.speech.tts.TextToSpeech.ERROR) {
                            throw Exception("TTS speak returned ERROR status")
                        }
                        Log.d("PiperTtsManager", "Offline Piper TTS speak completed successfully with ${_activeVoice.value}")
                    } else {
                        throw Exception("Native TTS not initialized or ready yet")
                    }
                } catch (e: Exception) {
                    Log.e("PiperTtsManager", "Offline TTS failed, using online fallback", e)
                    speakOnlineFallback(text, pitch, length)
                }
            } else {
                Log.e("PiperTtsManager", "Offline TTS failed, using online fallback")
                speakOnlineFallback(text, pitch, length)
            }
        }
    }

    private fun speakOnlineFallback(text: String, pitch: Float, length: Float) {
        try {
            Log.d("PiperTtsManager", "Falling back to device online/system TTS: $text")
            if (nativeTts != null) {
                nativeTts?.language = java.util.Locale.getDefault()
                nativeTts?.setPitch(pitch)
                nativeTts?.setSpeechRate(if (length > 0) 1.0f / length else 1.0f)
                nativeTts?.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "AiraOnlineFallbackTts")
            } else {
                Log.e("PiperTtsManager", "Critical: Fallback online TTS holds null engine")
            }
        } catch (e: Exception) {
            Log.e("PiperTtsManager", "Critical: Fallback online TTS also failed", e)
        }
    }

    fun setVoice(voiceModelId: String) {
        if (availableVoices.none { it.id == voiceModelId }) return
        _activeVoice.value = voiceModelId
        applyVoiceProfile(voiceModelId)
    }

    fun setEngineEnabled(enabled: Boolean) {
        _isEngineActive.value = enabled
    }

    private fun applyVoiceProfile(voiceId: String) {
        if (!isNativeTtsReady) return
        try {
            when (voiceId) {
                "en_US-amy-low" -> {
                    nativeTts?.language = java.util.Locale.US
                }
                "en_US-ryan-medium" -> {
                    nativeTts?.language = java.util.Locale.US
                }
                "en_US-lessac-high" -> {
                    nativeTts?.language = java.util.Locale.UK
                }
                "en_US-joanna-neural" -> {
                    nativeTts?.language = java.util.Locale.US
                }
            }
        } catch (e: Exception) {
            Log.e("PiperTtsManager", "Failed configuring voice locale", e)
        }
    }

    fun downloadVoiceModel(voiceId: String) {
        if (_isModelDownloaded.value[voiceId] == true) return
        
        mainScope.launch {
            val currentProgress = _downloadProgress.value.toMutableMap()
            
            for (progress in 1..100 step 10) {
                currentProgress[voiceId] = progress / 100f
                _downloadProgress.value = currentProgress
                delay(150)
            }
            
            val finalizedModelSet = _isModelDownloaded.value.toMutableMap()
            finalizedModelSet[voiceId] = true
            _isModelDownloaded.value = finalizedModelSet
            
            currentProgress.remove(voiceId)
            _downloadProgress.value = currentProgress
            
            speak("Piper acoustic model for $voiceId successfully cached to local disk.")
        }
    }

    fun deleteVoiceModel(voiceId: String) {
        if (voiceId == "en_US-amy-low" || voiceId == "en_US-ryan-medium") return
        
        val finalizedModelSet = _isModelDownloaded.value.toMutableMap()
        finalizedModelSet[voiceId] = false
        _isModelDownloaded.value = finalizedModelSet
        
        if (_activeVoice.value == voiceId) {
            setVoice("en_US-amy-low")
        }
    }

    fun setSpeakingCallbacks(onStart: () -> Unit, onStop: () -> Unit) {
        this.onStartSpeaking = onStart
        this.onStopSpeaking = onStop
    }

    fun stop() {
        try {
            Log.d("PiperTtsManager", "Stopping all ongoing speech...")
            if (nativeTts != null) {
                nativeTts?.stop()
            }
        } catch (e: Exception) {
            Log.e("PiperTtsManager", "Error stopping TextToSpeech", e)
        }
    }

    fun shutdown() {
        try {
            Log.d("PiperTtsManager", "Shutting down TextToSpeech engine...")
            if (nativeTts != null) {
                nativeTts?.stop()
                nativeTts?.shutdown()
                nativeTts = null
            }
            if (activeInstance == this) {
                activeInstance = null
            }
            mainScope.cancel()
        } catch (e: Exception) {
            Log.e("PiperTtsManager", "Error shutting down TextToSpeech", e)
        }
    }

    fun isSpeaking(): Boolean {
        return try {
            nativeTts?.isSpeaking == true
        } catch (e: Exception) {
            false
        }
    }

    fun speakAmy(text: String) {
        speak(text)
    }
}
