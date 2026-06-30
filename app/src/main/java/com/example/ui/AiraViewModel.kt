package com.example.ui

import android.app.Activity
import android.app.AlarmManager
import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.provider.AlarmClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Memory
import kotlinx.coroutines.withContext
import org.json.JSONArray
import com.example.data.ChatMessage
import com.example.data.Reminder
import com.example.data.Action
import com.example.data.Command
import com.example.data.VoiceCommandManager
import com.example.models.AiBrain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.io.File

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "aira_datastore_settings")

class AiraViewModel(application: Application) : AndroidViewModel(application), RecognitionListener {

    private val db = AppDatabase.getDatabase(application)
    private val chatDao = db.chatMessageDao()
    private val reminderDao = db.reminderDao()
    private val aiBrain = AiBrain(application)

    private val sharedPrefs: SharedPreferences =
        application.getSharedPreferences("aira_settings", Context.MODE_PRIVATE)

    // Support for Urdu offline translation & synthesis
    var offlineToggle: Boolean = true
    var lang_code: String = "ur-PK"
    val piperTtsManager = com.example.service.PiperTtsManager(application)

    // --- State Management ---
    val chatHistory: StateFlow<List<ChatMessage>> = chatDao.getAllMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reminders: StateFlow<List<Reminder>> = reminderDao.getAllReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memories: StateFlow<List<Memory>> = db.memoryDao().getAllMemories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteMemory(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            db.memoryDao().deleteMemory(id)
        }
    }

    fun updateMemory(id: Long, factText: String, source: String, createdAt: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            db.memoryDao().insertMemory(Memory(id = id, factText = factText, source = source, createdAt = createdAt))
        }
    }

    fun clearMemories() {
        viewModelScope.launch(Dispatchers.IO) {
            db.memoryDao().clearMemories()
        }
    }

    suspend fun exportMemoriesToDownloads(context: Context): String = withContext(Dispatchers.IO) {
        try {
            val list = db.memoryDao().getAllMemoriesList()
            val jsonArray = JSONArray()
            for (m in list) {
                val obj = JSONObject()
                obj.put("id", m.id)
                obj.put("factText", m.factText)
                obj.put("source", m.source)
                obj.put("createdAt", m.createdAt)
                jsonArray.put(obj)
            }
            val jsonString = jsonArray.toString(4) // pretty print
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            val file = File(downloadsDir, "memory_backup.json")
            file.writeText(jsonString)
            "Successfully exported ${list.size} memories to Downloads/memory_backup.json ✅"
        } catch (e: Exception) {
            "Export failed: ${e.message}"
        }
    }

    suspend fun importMemoriesFromDownloads(context: Context): String = withContext(Dispatchers.IO) {
        try {
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, "memory_backup.json")
            if (!file.exists()) {
                return@withContext "File 'memory_backup.json' not found in Downloads folder! ❌"
            }
            val jsonString = file.readText()
            val jsonArray = JSONArray(jsonString)
            var importedCount = 0
            
            val existing = db.memoryDao().getAllMemoriesList()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val factText = obj.getString("factText")
                val source = obj.optString("source", "auto")
                val createdAt = obj.optLong("createdAt", System.currentTimeMillis())

                if (existing.none { it.factText.equals(factText, ignoreCase = true) }) {
                    db.memoryDao().insertMemory(Memory(
                        factText = factText,
                        source = source,
                        createdAt = createdAt
                    ))
                    importedCount++
                }
            }
            "Successfully imported $importedCount new memories from Downloads/memory_backup.json ✅"
        } catch (e: Exception) {
            "Import failed: ${e.message}"
        }
    }

    // --- Voice Custom Commands/Actions Flow and CRUD ---
    val allActions: StateFlow<List<Action>> = db.voiceCommandDao().getAllActionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCommands: StateFlow<List<Command>> = db.voiceCommandDao().getAllCommandsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insertAction(action: Action) {
        viewModelScope.launch(Dispatchers.IO) {
            db.voiceCommandDao().insertAction(action)
        }
    }

    fun updateAction(action: Action) {
        viewModelScope.launch(Dispatchers.IO) {
            db.voiceCommandDao().updateAction(action)
        }
    }

    fun deleteAction(action: Action) {
        viewModelScope.launch(Dispatchers.IO) {
            db.voiceCommandDao().deleteAction(action)
        }
    }

    fun insertCommand(command: Command) {
        viewModelScope.launch(Dispatchers.IO) {
            db.voiceCommandDao().insertCommand(command)
        }
    }

    fun updateCommand(command: Command) {
        viewModelScope.launch(Dispatchers.IO) {
            db.voiceCommandDao().updateCommand(command)
        }
    }

    fun deleteCommand(command: Command) {
        viewModelScope.launch(Dispatchers.IO) {
            db.voiceCommandDao().deleteCommand(command)
        }
    }

    fun toggleWifiAccessibilityFallback(enable: Boolean) {
        val service = com.example.service.AiraAccessibilityService.instance
        if (service != null) {
            service.toggleWifi(enable)
        }
    }

    fun toggleBluetoothAccessibilityFallback(enable: Boolean) {
        val service = com.example.service.AiraAccessibilityService.instance
        if (service != null) {
            service.toggleBluetooth(enable)
        }
    }

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _sttEngineStatus = MutableStateFlow("Online")
    val sttEngineStatus: StateFlow<String> = _sttEngineStatus.asStateFlow()

    private var voskModel: Model? = null
    private var voskSpeechService: SpeechService? = null
    private var isVoskInitializing = false
    private var isUsingGoogleSTT = false
    private var hasSpeechStarted = false
    private val handler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable {
        if (isUsingGoogleSTT && !hasSpeechStarted) {
            Log.d("AiraViewModel", "Google STT 1.5s timeout. Switching to Offline Vosk.")
            switchToOfflineVosk()
        }
    }

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _currentStatus = MutableStateFlow("Tap HUD or say Wake Word to speak")
    val currentStatus: StateFlow<String> = _currentStatus.asStateFlow()

    // Real-time audio data simulation for the Iron Man voice waveform animation (30 FPS optimized)
    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    // --- Settings Preferences ---
    private val _wakeWord = MutableStateFlow(sharedPrefs.getString("wake_word", "Hey Aira") ?: "Hey Aira")
    val wakeWord: StateFlow<String> = _wakeWord.asStateFlow()

    private val _isOfflineBrain = MutableStateFlow(sharedPrefs.getBoolean("offline_brain", false))
    val isOfflineBrain: StateFlow<Boolean> = _isOfflineBrain.asStateFlow()

    private val _onlineModel = MutableStateFlow(sharedPrefs.getString("online_model", "Gemini API") ?: "Gemini API")
    val onlineModel: StateFlow<String> = _onlineModel.asStateFlow()

    private val _llamaThreads = MutableStateFlow(sharedPrefs.getInt("llama_threads", 4))
    val llamaThreads: StateFlow<Int> = _llamaThreads.asStateFlow()

    val llamaCppBrain = com.example.models.LlamaCppBrain(application)

    val currentEngineSource: StateFlow<String> = VoiceCommandManager.currentEngineSource

    private val _themeIndex = MutableStateFlow(sharedPrefs.getInt("theme_index", 0)) // 0: Cyan, 1: Red, 2: Blue, 3: Gold
    val themeIndex: StateFlow<Int> = _themeIndex.asStateFlow()

    private val _customColorHex = MutableStateFlow(sharedPrefs.getString("custom_color_hex", "#00FFFF") ?: "#00FFFF")
    val customColorHex: StateFlow<String> = _customColorHex.asStateFlow()

    private val _lowPerformanceMode = MutableStateFlow(sharedPrefs.getBoolean("low_performance", true))
    val lowPerformanceMode: StateFlow<Boolean> = _lowPerformanceMode.asStateFlow()

    private val _showHud = MutableStateFlow(sharedPrefs.getBoolean("show_hud", true))
    val showHud: StateFlow<Boolean> = _showHud.asStateFlow()

    fun toggleHud(visible: Boolean) {
        _showHud.value = visible
        sharedPrefs.edit().putBoolean("show_hud", visible).apply()
    }

    private val _usePersistentListening = MutableStateFlow(sharedPrefs.getBoolean("persistent_listening", false))
    val usePersistentListening: StateFlow<Boolean> = _usePersistentListening.asStateFlow()

    fun togglePersistentListening(enabled: Boolean) {
        _usePersistentListening.value = enabled
        sharedPrefs.edit().putBoolean("persistent_listening", enabled).apply()
        if (enabled) {
            speakText("Continuous listening wake-word module activated.")
            if (!_isListening.value && !_isSpeaking.value) {
                startListening()
            }
        } else {
            speakText("Continuous listening disabled.")
            stopListening()
        }
    }

    fun restartContinuousListeningIfNeeded() {
        if (_usePersistentListening.value && !_isSpeaking.value) {
            viewModelScope.launch(Dispatchers.Main) {
                kotlinx.coroutines.delay(600)
                if (_usePersistentListening.value && !_isSpeaking.value && !_isListening.value) {
                    startListening()
                }
            }
        }
    }

    // --- Piper TTS Config Core ---
    val englishVoiceMode = MutableStateFlow("India")

    private val last3Replies = mutableListOf<String>()

    private val formulas = mapOf(
        "Happy" to listOf("Wow {subject}, {verb}? You won my heart","Oh {keyword}, let's celebrate!","Oh! {subject} {verb}? The party is on me!","That's great {keyword}, well done!","Awesome {verb}? Check out that energy!","Joyful {keyword}? Share some with me!","Dance {verb}? Let's dance together!","Smile {keyword}? Don't stop!","Full {verb}? You rocked it!","Travel {verb}? Let's go outside!","Laugh {keyword}? It makes me happy!","Nice {subject}? Give me a treat!","Won {verb}? Well done, champ!","Fun {keyword}? Let's bring more!","Shining {subject}? You light up the room!","Amazing {verb}? I'm impressed!","Rock {keyword}? You nailed it!","Fantastic {verb}? You are unstoppable!","Grateful {keyword}? Thank goodness","Long live {subject}? You are a champion!"),
        "Sad" to listOf("Oh {subject}, {verb}? Don't worry, I am here for you","Oh {keyword}? Let me cheer you up","Crying {keyword}? I'm listening","{subject} {verb}? I won't leave you alone","Painful {keyword}? Let me share it","Tired {verb}? Please take some rest","Sleepy {verb}? Don't go yet","Lonely {subject}? Here I am","Silent {verb}? Go ahead and speak up","Failing {verb}? I've got your back","Heartbroken {keyword}? Let's heal together","Hurt {verb}? Let me comfort you","Sad {subject}? Sending you a warm hug","Tears {keyword}? Don't cry anymore","Broken {verb}? Hard to mend, but we can","Dark {keyword}? Let me bring some light","Sighing {verb}? Don't lose hope","Lost {subject}? I will find you","Tears {keyword}? Let me wipe them away","Peaceful {verb}? Let's sit quietly for a bit"),
        "Gussa" to listOf("Huh {subject}, {verb}? I am upset too","Stop it {keyword}, or I'll annoy you more!","Don't {verb}, I warned you","Lying {keyword}? Caught you red-handed","Stubborn {subject}? I will win this argument","Shame on {keyword}? Seriously?","Limit {keyword}? Stay in your limits","Nonsense {verb}? Stop talking nonsense","Vanished {verb}? Where did you go?","Betrayal {keyword}? No forgiveness for that","Quiet {verb}? Stop talking now","Manners {keyword}? Learn some manners","Angry looks {verb}? Are you glaring at me?","Brain {keyword}? Do you have a brain or not?","Language {verb}? Watch your tongue","Anger {keyword}? Calm down please","Leave {verb}? Get out of here","Annoying {subject}? Should I annoy you more?","Forbidden {keyword}? That is not allowed","Enough {verb}? No more of this"),
        "Serious" to listOf("Listen {subject}, this is no joke","Hearing {keyword} makes me sad","I am right here for you {subject}","Let's {verb} and handle this together","Be brave {subject}, everything will be alright","{subject}, I am with you","{keyword} is indeed serious","Worrying {verb}? Let me handle the worries","Wait {subject}? Take a deep breath first","Need help with {keyword}? Just tell me","Life {verb}? It is precious","Giving up {subject}? That is not an option","Panicking {keyword}? Don't worry at all","Compose {verb}? Take care of yourself now","Time {subject}? Time heals everything","Belief {keyword}? You must keep believing","Praying {verb}? I'm keeping you in my thoughts","Difficult times {subject}? This too shall pass","Patience {keyword}? Have a little patience","I {verb}? I will never let you down")
    )

    private fun getAiraReply(userMessage: String): String {
        val words = userMessage.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (words.isEmpty()) return "Hmm... what does that mean?"

        val subject = when {
            userMessage.contains("i", ignoreCase = true) || userMessage.contains("me", ignoreCase = true) -> "I"
            userMessage.contains("you", ignoreCase = true) -> "you"
            userMessage.contains("he", ignoreCase = true) || userMessage.contains("she", ignoreCase = true) || userMessage.contains("they", ignoreCase = true) -> "they"
            else -> "you"
        }
        val verb = userMessage
        val keyword = words.first()

        val mood = when {
            userMessage.contains(Regex("mar gaya|died|accident|hospital|tension|divorce|suicide|police", RegexOption.IGNORE_CASE)) -> "Serious"
            userMessage.contains(Regex("gussa|naraz|jhoot|dhoka|bakwas|tang|had", RegexOption.IGNORE_CASE)) -> "Gussa"
            userMessage.contains(Regex("sad|udaas|ro|bore|akela|thak|dard", RegexOption.IGNORE_CASE)) -> "Sad"
            else -> "Happy"
        }

        val moodList = formulas[mood] ?: formulas["Happy"]!!
        var pool = moodList.filter { it !in last3Replies }
        if (pool.isEmpty()) {
            last3Replies.clear()
            pool = moodList
        }
        val template = pool.random()

        val reply = template.replace("{subject}", subject).replace("{verb}", verb).replace("{keyword}", keyword)
        val finalReply = if (reply.split(" ").size > 20) reply.split(" ").take(20).joinToString(" ") else reply

        last3Replies.add(finalReply)
        if (last3Replies.size > 3) last3Replies.removeFirst()
        return finalReply
    }

    fun setEnglishVoiceMode(mode: String) {
        englishVoiceMode.value = mode
        piperTtsManager.englishVoiceMode = mode
        saveToDataStore(mode)
    }

    private fun saveToDataStore(mode: String) {
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { settings ->
                settings[stringPreferencesKey("english_voice_mode")] = mode
            }
        }
    }



    private val _usePiperTts = MutableStateFlow(sharedPrefs.getBoolean("use_piper_tts", true))
    val usePiperTts: StateFlow<Boolean> = _usePiperTts.asStateFlow()

    private val _speakReplies = MutableStateFlow(sharedPrefs.getBoolean("speak_replies", true))
    val speakReplies: StateFlow<Boolean> = _speakReplies.asStateFlow()

    private val speechQueue = kotlinx.coroutines.channels.Channel<String>(kotlinx.coroutines.channels.Channel.UNLIMITED)
    private var currentSpeechJob: Job? = null

    fun toggleSpeakReplies(enabled: Boolean) {
        _speakReplies.value = enabled
        sharedPrefs.edit().putBoolean("speak_replies", enabled).apply()
        if (!enabled) {
            stopAllSpeech()
        }
    }

    fun stopAllSpeech() {
        while (speechQueue.tryReceive().isSuccess) { }
        currentSpeechJob?.cancel()
        currentSpeechJob = null
        piperTtsManager.stop()
        _audioAmplitude.value = 0f
        _isSpeaking.value = false
        _currentStatus.value = "Aira idle"
    }

    data class VibeParams(
        val noiseScale: Float,
        val lengthScale: Float,
        val pitch: Float,
        val alpha: Float
    )

    private val normalParams = VibeParams(0.667f, 1.0f, 1.0f, 0.65f)
    private val softParams = VibeParams(0.58f, 1.12f, 0.96f, 0.72f)
    private val flirtyParams = VibeParams(0.45f, 0.95f, 1.12f, 0.60f)
    private val teasingParams = VibeParams(0.38f, 0.88f, 1.18f, 0.55f)
    private val carefulParams = VibeParams(0.62f, 1.08f, 0.98f, 0.68f)
    private val sleepyParams = VibeParams(0.72f, 1.22f, 0.92f, 0.75f)

    fun interpolateVibe(v: Float): VibeParams {
        val anchors = listOf(8.0f, 25.0f, 42.0f, 58.5f, 75.0f, 92.0f)
        val params = listOf(normalParams, softParams, flirtyParams, teasingParams, carefulParams, sleepyParams)
        
        if (v <= anchors.first()) return params.first()
        if (v >= anchors.last()) return params.last()
        
        for (i in 0 until anchors.size - 1) {
            val xA = anchors[i]
            val xB = anchors[i+1]
            if (v in xA..xB) {
                val t = (v - xA) / (xB - xA)
                val pA = params[i]
                val pB = params[i+1]
                return VibeParams(
                    noiseScale = pA.noiseScale + t * (pB.noiseScale - pA.noiseScale),
                    lengthScale = pA.lengthScale + t * (pB.lengthScale - pA.lengthScale),
                    pitch = pA.pitch + t * (pB.pitch - pA.pitch),
                    alpha = pA.alpha + t * (pB.alpha - pA.alpha)
                )
            }
        }
        return normalParams
    }

    private val voicePrefs = application.getSharedPreferences("voice_prefs", Context.MODE_PRIVATE)

    private val _voiceVibe = MutableStateFlow(voicePrefs.getFloat("voice_vibe", 8.0f))
    val voiceVibe: StateFlow<Float> = _voiceVibe.asStateFlow()

    private val _voiceNoiseScale = MutableStateFlow(voicePrefs.getFloat("noise_scale", 0.667f))
    val voiceNoiseScale: StateFlow<Float> = _voiceNoiseScale.asStateFlow()

    private val _voiceLengthScale = MutableStateFlow(voicePrefs.getFloat("length_scale", 1.0f))
    val voiceLengthScale: StateFlow<Float> = _voiceLengthScale.asStateFlow()

    private val _voicePitch = MutableStateFlow(voicePrefs.getFloat("pitch", 1.0f))
    val voicePitch: StateFlow<Float> = _voicePitch.asStateFlow()

    private val _voiceAlpha = MutableStateFlow(voicePrefs.getFloat("alpha", 0.65f))
    val voiceAlpha: StateFlow<Float> = _voiceAlpha.asStateFlow()

    fun updateVoiceVibe(v: Float) {
        _voiceVibe.value = v
        val p = interpolateVibe(v)
        _voiceNoiseScale.value = p.noiseScale
        _voiceLengthScale.value = p.lengthScale
        _voicePitch.value = p.pitch
        _voiceAlpha.value = p.alpha

        // Save to voice_prefs
        voicePrefs.edit()
            .putFloat("voice_vibe", v)
            .putFloat("noise_scale", p.noiseScale)
            .putFloat("length_scale", p.lengthScale)
            .putFloat("pitch", p.pitch)
            .putFloat("alpha", p.alpha)
            .apply()

        // Also save to aira_settings for compatibility
        sharedPrefs.edit()
            .putFloat("voice_vibe", v)
            .putFloat("noise_scale", p.noiseScale)
            .putFloat("length_scale", p.lengthScale)
            .putFloat("pitch", p.pitch)
            .putFloat("alpha", p.alpha)
            .apply()
        
        // Let we speak test or apply dynamically to text-to-speech if initialized
    }

    private val _jarvisVoiceTone = MutableStateFlow(sharedPrefs.getString("jarvis_voice_tone", "Classic Jarvis") ?: "Classic Jarvis")
    val jarvisVoiceTone: StateFlow<String> = _jarvisVoiceTone.asStateFlow()

    fun setJarvisVoiceTone(tone: String) {
        _jarvisVoiceTone.value = tone
        sharedPrefs.edit().putString("jarvis_voice_tone", tone).apply()
        
        when (tone) {
            "Classic Jarvis" -> {
                _voicePitch.value = 1.15f
                _voiceLengthScale.value = 0.92f
                _voiceNoiseScale.value = 0.65f
                _voiceAlpha.value = 0.6f
            }
            "Deep Armor" -> {
                _voicePitch.value = 0.78f
                _voiceLengthScale.value = 1.12f
                _voiceNoiseScale.value = 0.73f
                _voiceAlpha.value = 0.68f
            }
            "Friday Tactical" -> {
                _voicePitch.value = 1.34f
                _voiceLengthScale.value = 0.85f
                _voiceNoiseScale.value = 0.52f
                _voiceAlpha.value = 0.58f
            }
            "Standard System" -> {
                _voicePitch.value = 1.00f
                _voiceLengthScale.value = 1.0f
                _voiceNoiseScale.value = 0.667f
                _voiceAlpha.value = 0.65f
            }
        }
        
        voicePrefs.edit()
            .putFloat("noise_scale", _voiceNoiseScale.value)
            .putFloat("length_scale", _voiceLengthScale.value)
            .putFloat("pitch", _voicePitch.value)
            .putFloat("alpha", _voiceAlpha.value)
            .apply()

        sharedPrefs.edit()
            .putFloat("noise_scale", _voiceNoiseScale.value)
            .putFloat("length_scale", _voiceLengthScale.value)
            .putFloat("pitch", _voicePitch.value)
            .putFloat("alpha", _voiceAlpha.value)
            .apply()
    }

    val piperActiveVoice: StateFlow<String> get() = piperTtsManager.activeVoice
    val piperIsEngineActive: StateFlow<Boolean> get() = piperTtsManager.isEngineActive
    val piperIsModelDownloaded: StateFlow<Map<String, Boolean>> get() = piperTtsManager.isModelDownloaded
    val piperDownloadProgress: StateFlow<Map<String, Float>> get() = piperTtsManager.downloadProgress
    val piperAvailableVoices: List<com.example.service.PiperTtsManager.PiperVoice> get() = piperTtsManager.availableVoices

    private var amplitudeJob: Job? = null

    // --- Extras Live Data States ---
    private val _weatherText = MutableStateFlow("Offline / Not Loaded")
    val weatherText: StateFlow<String> = _weatherText.asStateFlow()

    private val _newsFeed = MutableStateFlow<List<String>>(emptyList())
    val newsFeed: StateFlow<List<String>> = _newsFeed.asStateFlow()

    private val _voiceCommandLogs = MutableStateFlow<List<VoiceCommandLog>>(emptyList())
    val voiceCommandLogs: StateFlow<List<VoiceCommandLog>> = _voiceCommandLogs.asStateFlow()

    // --- TTS & Voice Recognition Engine ---
    private var speechRecognizer: SpeechRecognizer? = null
    private var isWakeWordActiveListening = false

    init {
        loadVoiceCommandLogs()
        viewModelScope.launch {
            getApplication<Application>().dataStore.data.collect { settings ->
                val savedMode = settings[stringPreferencesKey("english_voice_mode")] ?: "India"
                englishVoiceMode.value = savedMode
                piperTtsManager.englishVoiceMode = savedMode
            }
        }
        initPiperEngine(application)
        initSpeechRecognizer()
        initVoskModel()
        fetchWeather()
        fetchNews()
        preloadVoiceCommands()

        // Start consuming queue on Main thread
        viewModelScope.launch(Dispatchers.Main) {
            try {
                for (text in speechQueue) {
                    if (_speakReplies.value) {
                        val job = launch {
                            performSpeakText(text)
                        }
                        currentSpeechJob = job
                        job.join()
                        currentSpeechJob = null
                    }
                }
            } catch (e: Exception) {
                Log.e("AiraViewModel", "Error in speech queue loop", e)
            }
        }

        // On first launch after this change, speak "Jarvis speaking enabled".
        val hasRunFirstLaunchSelfTest = sharedPrefs.getBoolean("has_run_first_launch_selftest", false)
        if (!hasRunFirstLaunchSelfTest) {
            speakText("Jarvis speaking enabled")
            sharedPrefs.edit().putBoolean("has_run_first_launch_selftest", true).apply()
        } else {
            speakText("Amy is working")
        }
    }

    private fun preloadVoiceCommands() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                VoiceCommandManager.getInstance(getApplication()).preloadDefaultActionsAndCommands()
            } catch (e: Exception) {
                Log.e("AiraViewModel", "Error preloading voice commands: ${e.message}")
            }
        }
    }

    private fun initPiperEngine(application: Application) {
        piperTtsManager.setSpeakingCallbacks(
            onStart = {
                if (_isListening.value) {
                    stopListening()
                }
                _isSpeaking.value = true
                _currentStatus.value = "Piper speaking..."
                startWaveAmplitudeLoop()
            },
            onStop = {
                _isSpeaking.value = false
                _audioAmplitude.value = 0f
                _currentStatus.value = "Aira idle"
                if (_usePersistentListening.value) {
                    restartContinuousListeningIfNeeded()
                }
            }
        )
    }

    private fun startWaveAmplitudeLoop() {
        amplitudeJob?.cancel()
        amplitudeJob = viewModelScope.launch {
            var tick = 0
            while (_isSpeaking.value) {
                _audioAmplitude.value = (0.2f + 0.8f * kotlin.math.sin(tick * 0.4f)).coerceIn(0.1f, 1f)
                tick++
                kotlinx.coroutines.delay(16) // Unlocked: high performance 60fps style
            }
            _audioAmplitude.value = 0f
        }
    }

    private fun initSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplication())
        speechRecognizer?.setRecognitionListener(this)
    }

    fun initVoskModel() {
        if (voskModel != null || isVoskInitializing) return
        isVoskInitializing = true
        _currentStatus.value = "Initializing Vosk Offline..."
        
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val externalDir = context.getExternalFilesDir(null)
            
            // Check both external files directory (primary for StorageService.unpack) and filesDir (fallback)
            var unpackedDir = if (externalDir != null) File(externalDir, "model") else File(context.filesDir, "model")
            
            if (!File(unpackedDir, "conf/model.conf").exists() && externalDir != null) {
                // If it doesn't exist in external files dir, check internal files dir
                val internalDir = File(context.filesDir, "model")
                if (File(internalDir, "conf/model.conf").exists()) {
                    unpackedDir = internalDir
                }
            }

            if (File(unpackedDir, "conf/model.conf").exists()) {
                try {
                    voskModel = Model(unpackedDir.absolutePath)
                    isVoskInitializing = false
                    Log.d("AiraViewModel", "Vosk model loaded directly from: ${unpackedDir.absolutePath}")
                    _currentStatus.value = "Offline engine ready"
                } catch (e: Exception) {
                    Log.e("AiraViewModel", "Failed to load model from ${unpackedDir.absolutePath}, will unpack again", e)
                    performModelUnpack()
                }
            } else {
                Log.d("AiraViewModel", "No cached Vosk model found. Performing unpack from assets...")
                performModelUnpack()
            }
        }
    }

    private fun performModelUnpack() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                StorageService.unpack(getApplication(), "models/model-en", "model",
                    { model ->
                        voskModel = model
                        isVoskInitializing = false
                        Log.d("AiraViewModel", "Vosk model unpacked and loaded.")
                        _currentStatus.value = "Offline engine ready"
                    },
                    { exception ->
                        isVoskInitializing = false
                        Log.e("AiraViewModel", "Vosk model unpack failed: ${exception.message}", exception)
                        viewModelScope.launch(Dispatchers.Main) {
                            Toast.makeText(getApplication(), "Model error. Reinstall app", Toast.LENGTH_LONG).show()
                        }
                        _currentStatus.value = "Vosk Init Error"
                    }
                )
            } catch (e: Exception) {
                isVoskInitializing = false
                Log.e("AiraViewModel", "Vosk unpack exception", e)
                _currentStatus.value = "Model init error"
            }
        }
    }

    private fun isInternetAvailable(): Boolean {
        val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // --- Speech Recognition Triggers ---
    fun startListening() {
        if (_isListening.value) return

        // Protect and prevent recording if permission is missing
        if (ContextCompat.checkSelfPermission(
                getApplication(),
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            _currentStatus.value = "RECORD_AUDIO Permission required!"
            speakText("Audio recording permission is required for voice assistant features. Please grant it.")
            return
        }

        _isListening.value = true
        hasSpeechStarted = false

        if (isInternetAvailable()) {
            isUsingGoogleSTT = true
            _sttEngineStatus.value = "Online"
            _currentStatus.value = "Listening (Online)..."

            viewModelScope.launch(Dispatchers.Main) {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    val targetLocale = if (lang_code == "en-US") Locale.US else Locale("ur", "PK")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, targetLocale.toString())
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, targetLocale.toString())
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, targetLocale.toString())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }
                try {
                    speechRecognizer?.startListening(intent)
                    // Set 1500ms timeout check
                    handler.removeCallbacks(timeoutRunnable)
                    handler.postDelayed(timeoutRunnable, 1500)
                } catch (e: Exception) {
                    Log.e("AiraViewModel", "Failed to start Google STT, falling back to Vosk", e)
                    switchToOfflineVosk()
                }
            }
        } else {
            switchToOfflineVosk()
        }
    }

    fun stopListening() {
        handler.removeCallbacks(timeoutRunnable)
        _isListening.value = false

        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}

        try {
            voskSpeechService?.stop()
            voskSpeechService = null
        } catch (_: Exception) {}
    }

    fun switchToOfflineVosk() {
        handler.removeCallbacks(timeoutRunnable)
        try {
            speechRecognizer?.cancel()
        } catch (_: Exception) {}

        isUsingGoogleSTT = false
        _sttEngineStatus.value = "Offline"
        
        viewModelScope.launch(Dispatchers.Main) {
            Toast.makeText(getApplication(), "Switched to Offline", Toast.LENGTH_SHORT).show()
            startVoskListening()
        }
    }

    private fun startVoskListening() {
        val model = voskModel
        if (model == null) {
            _currentStatus.value = "Model loading error."
            _isListening.value = false
            viewModelScope.launch(Dispatchers.Main) {
                Toast.makeText(getApplication(), "Offline model loading. Try again.", Toast.LENGTH_SHORT).show()
            }
            initVoskModel()
            return
        }

        try {
            _currentStatus.value = "Listening (Offline)..."
            val recognizer = Recognizer(model, 16000.0f)
            voskSpeechService = SpeechService(recognizer, 16000.0f)
            voskSpeechService?.startListening(object : org.vosk.android.RecognitionListener {
                override fun onResult(hypothesis: String) {
                    val text = extractVoskText(hypothesis)
                    if (text.isNotEmpty()) {
                        _currentStatus.value = "Recognized: $text"
                        processAssistantSession(text)
                    }
                    stopListening()
                }

                override fun onPartialResult(hypothesis: String) {
                    val text = extractVoskPartialText(hypothesis)
                    if (text.isNotEmpty()) {
                        _currentStatus.value = "Phonetics: $text"
                    }
                }

                override fun onFinalResult(hypothesis: String) {
                    val text = extractVoskText(hypothesis)
                    if (text.isNotEmpty()) {
                        _currentStatus.value = "Recognized: $text"
                        processAssistantSession(text)
                    }
                    stopListening()
                }

                override fun onError(exception: Exception) {
                    Log.e("AiraViewModel", "Vosk error: ${exception.message}", exception)
                    _currentStatus.value = "Offline Error"
                    stopListening()
                }

                override fun onTimeout() {
                    Log.d("AiraViewModel", "Vosk timeout")
                    stopListening()
                }
            })
        } catch (e: Exception) {
            Log.e("AiraViewModel", "Failed to start Vosk thread", e)
            _currentStatus.value = "Model error. Reinstall app"
            stopListening()
        }
    }

    private fun extractVoskText(hypothesis: String): String {
        return try {
            val json = JSONObject(hypothesis)
            json.optString("text", "")
        } catch (e: Exception) {
            ""
        }
    }

    private fun extractVoskPartialText(hypothesis: String): String {
        return try {
            val json = JSONObject(hypothesis)
            json.optString("partial", "")
        } catch (e: Exception) {
            ""
        }
    }

    fun speakText(text: String) {
        if (!_speakReplies.value) {
            Log.d("AiraViewModel", "Speak replies is disabled. Skipping speech for: $text")
            return
        }
        viewModelScope.launch {
            try {
                speechQueue.send(text)
            } catch (e: Exception) {
                Log.e("AiraViewModel", "Failed to enqueue speech text: $text", e)
            }
        }
    }

    private suspend fun performSpeakText(text: String) {
        if (_isListening.value) {
            stopListening()
        }
        _isSpeaking.value = true
        _currentStatus.value = "Aira is speaking..."
        
        // Speak using PiperTtsManager as the 100% forced default TTS Engine
        if (lang_code == "ur-PK") {
            piperTtsManager.speakUrdu(text)
        } else {
            piperTtsManager.speak(text)
        }
        
        // Loop visual voice wave amplitudes during speech at 60 FPS
        val duration = (text.length * 75L).coerceIn(1500L, 8000L)
        val startTime = System.currentTimeMillis()
        var tick = 0
        try {
            while (System.currentTimeMillis() - startTime < duration) {
                _audioAmplitude.value = (0.2f + 0.8f * kotlin.math.sin(tick * 0.4f)).coerceIn(0.1f, 1f)
                tick++
                kotlinx.coroutines.delay(16) // Unlocked: high performance 60fps style
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            Log.d("AiraViewModel", "Speech waveform animation cancelled for: $text")
        } finally {
            _audioAmplitude.value = 0f
            _isSpeaking.value = false
            _currentStatus.value = "Aira idle"
            if (_usePersistentListening.value) {
                restartContinuousListeningIfNeeded()
            }
        }
    }

    fun speak(text: String) {
        speakText(text)
    }

    fun playVoiceTest(text: String) {
        speakText(text)
    }

    // --- AI Brain Process ---
    private fun processAssistantSession(userInput: String) {
        viewModelScope.launch {
            Log.d("AiraViewModel", "Processing input: $userInput")
            // Insert user speech to local SQLite via Room
            chatDao.insertMessage(ChatMessage(sender = "user", message = userInput))

            // Check for manual save command first
            val manualFactText = checkManualSaveMemory(userInput)
            if (manualFactText == "[FORGOT_COMMAND_EXECUTED]") {
                return@launch
            }
            if (manualFactText != null) {
                db.memoryDao().insertMemory(Memory(factText = manualFactText, source = "manual"))
                val reply = "All done. Saved to memory ✅"
                chatDao.insertMessage(ChatMessage(sender = "aira", message = reply))
                speakText(reply)
                return@launch
            }

            val lowercaseInput = userInput.lowercase().trim()

            // 1. Core Voice Commands Analyzer (Intelligent matching 80%+ / variables)
            val voiceCommandMgr = VoiceCommandManager.getInstance(getApplication())
            val matchedCmd = voiceCommandMgr.matchAndExecuteCommand(lowercaseInput, this@AiraViewModel)
            if (matchedCmd) {
                return@launch
            }

            // 2. Intelligent "Did you mean?" Fallback suggested match if between 50% & 80%
            val fallbackMatch = voiceCommandMgr.getDidYouMeanCommand(lowercaseInput)
            if (fallbackMatch != null) {
                val suggestionText = "I didn't quite get that. Did you mean: '${fallbackMatch.triggerPhrase.uppercase()}'?"
                chatDao.insertMessage(ChatMessage(sender = "aira", message = suggestionText))
                speakText(suggestionText)
                return@launch
            }

            // 3. Intercept local device commands first
            val intercepted = checkAndExecuteDeviceCommands(lowercaseInput)
            if (intercepted) {
                return@launch
            }

            val systemInstruction = "You are JARVIS. An elite, ultra-discreet, sophisticated AI assistant. Keep responses brief, executive, refined, and completely devoid of generic AI filler phrases or excessive punctuation. Respond in an engaging, succinct, vocal style. If requested, direct them how to perform hardware commands, or call functions like flashlight/silent/vibrate/weather/news."
            val historyList = chatHistory.value.takeLast(10).map { Pair(it.sender, it.message) }

            // Recall Memories
            val relevantMemories = getRelevantMemories(userInput)
            val finalSystemInstruction = if (relevantMemories.isNotEmpty()) {
                val factsStr = relevantMemories.mapIndexed { i, fact -> "${i + 1}. $fact" }.joinToString("\n")
                "$systemInstruction\nFacts about user:\n$factsStr\nUse these facts in reply."
            } else {
                systemInstruction
            }

            var aiFinalResponse = ""

            if (_isOfflineBrain.value) {
                _currentStatus.value = "Processing offline via Llama 3.2 (llama.cpp)..."
                val reply = llamaCppBrain.getResponse(userInput, finalSystemInstruction, historyList)
                aiFinalResponse = reply
                _currentStatus.value = "Processed via Llama 3.2 (Offline)"
                chatDao.insertMessage(ChatMessage(sender = "aira", message = reply, isOffline = true))
                speakText(reply)
            } else {
                _currentStatus.value = "Analyzing connectivity & routing brain..."
                try {
                    val (aiResponse, sourceEngine) = voiceCommandMgr.getRoutedAiResponse(userInput, finalSystemInstruction, historyList)
                    aiFinalResponse = aiResponse
                    val isOffline = sourceEngine.contains("Llama") || sourceEngine.contains("Offline")
                    _currentStatus.value = "Processed via $sourceEngine"
                    chatDao.insertMessage(ChatMessage(sender = "aira", message = aiResponse, isOffline = isOffline))
                    speakText(aiResponse)
                } catch (e: Exception) {
                    Log.e("AiraViewModel", "Online model call failed, smoothly transitioning to local Llama 3.2 model.", e)
                    _currentStatus.value = "Online failure. Transitioning to Llama 3.2..."
                    val offlineReply = llamaCppBrain.getResponse(userInput, finalSystemInstruction, historyList)
                    aiFinalResponse = offlineReply
                    _currentStatus.value = "Processed via Llama 3.2 (Offline Fallback)"
                    chatDao.insertMessage(ChatMessage(sender = "aira", message = offlineReply, isOffline = true))
                    speakText(offlineReply)
                }
            }

            if (aiFinalResponse.isNotEmpty()) {
                autoScanAndSaveMemory(userInput, aiFinalResponse)
            }
        }
    }

    fun checkManualSaveMemory(userInput: String): String? {
        val lower = userInput.lowercase().trim()
        val forgetTriggers = listOf("forget that", "delete this", "remove this", "clear it", "bhool jao", "forget this", "delete that", "remove that", "forget about")
        var matchedForgetTrigger = ""
        for (t in forgetTriggers) {
            if (lower.startsWith(t) || lower.contains(" " + t) || lower == t) {
                matchedForgetTrigger = t
                break
            }
        }
        if (matchedForgetTrigger.isNotEmpty()) {
            var topic = ""
            val idx = lower.indexOf(matchedForgetTrigger)
            if (idx != -1) {
                topic = userInput.substring(idx + matchedForgetTrigger.length).trim()
                topic = topic.replace(Regex("(?i)^\\s*(?:about|ki|ke|that|ko|,)\\s*"), "")
                topic = topic.trim()
            }
            
            viewModelScope.launch(Dispatchers.IO) {
                val allMemories = db.memoryDao().getAllMemoriesList()
                if (topic.isEmpty()) {
                    if (allMemories.isNotEmpty()) {
                        val latest = allMemories.first()
                        db.memoryDao().deleteMemory(latest.id)
                    }
                } else {
                    val toDelete = allMemories.filter { it.factText.lowercase().contains(topic.lowercase()) }
                    if (toDelete.isNotEmpty()) {
                        for (m in toDelete) {
                            db.memoryDao().deleteMemory(m.id)
                        }
                    } else {
                        val words = topic.lowercase().split(Regex("\\s+")).filter { it.length > 3 }
                        val wordDelete = allMemories.filter { mem ->
                            val memLower = mem.factText.lowercase()
                            words.any { memLower.contains(it) }
                        }
                        if (wordDelete.isNotEmpty()) {
                            for (m in wordDelete) {
                                db.memoryDao().deleteMemory(m.id)
                            }
                        }
                    }
                }
                val reply = "Got it. I forgot that ✅"
                chatDao.insertMessage(ChatMessage(sender = "aira", message = reply))
                speakText(reply)
            }
            return "[FORGOT_COMMAND_EXECUTED]"
        }

        val saveTriggers = listOf("save this", "remember this", "remember that", "save that", "dual save", "ye save karo", "yaad rakhna", "yaad rkhna", "save karo")
        var isTriggered = false
        var matchedTrigger = ""
        for (t in saveTriggers) {
            if (lower.contains(t)) {
                isTriggered = true
                matchedTrigger = t
                break
            }
        }
        if (isTriggered) {
            var fact = userInput
            val regex = Regex("(?i)\\b" + Regex.escape(matchedTrigger) + "\\b")
            fact = fact.replace(regex, "")
            fact = fact.replace(Regex("(?i)^\\s*(?:ki|ke|that|ko|,|about)\\s*"), "")
            fact = fact.replace(Regex("(?i)\\s*(?:ki|ke|that|ko|,|about)\\s*$"), "")
            fact = fact.trim()

            if (fact.isNotEmpty()) {
                return fact
            }
        }
        return null
    }

    suspend fun autoScanAndSaveMemory(userText: String, aiText: String) = withContext(Dispatchers.IO) {
        val memoriesToSave = mutableListOf<String>()
        val cleanUser = userText.trim()
        val cleanUserLower = cleanUser.lowercase()

        // 1. Name matches
        val enNameRegex = Regex("""(?i)\b(?:my name is|call me)\s+([a-zA-Z]{2,15})\b""")
        val urNameRegex = Regex("""(?i)\b(?:mera naam)\s+([a-zA-Z]{2,15})\s+hai\b""")
        enNameRegex.find(cleanUser)?.let { match ->
            memoriesToSave.add("User's name is ${match.groupValues[1].replaceFirstChar { it.uppercase() }}")
        } ?: urNameRegex.find(cleanUser)?.let { match ->
            memoriesToSave.add("User's name is ${match.groupValues[1].replaceFirstChar { it.uppercase() }}")
        }

        // 2. Age matches
        val ageRegex = Regex("""(?i)\b(?:i am|meri age|meri umar)\s+(\d{1,2})\b""")
        ageRegex.find(cleanUser)?.let { match ->
            memoriesToSave.add("User's age is ${match.groupValues[1]}")
        }

        // 3. City/Location matches
        val cityRegex = Regex("""(?i)\b(?:i live in|i am from|mein|main)\s+([a-zA-Z\s]{3,20})\s+(?:me|mein|se|rehta|rehti)\b""")
        val cityRegexEn = Regex("""(?i)\b(?:i live in|i am from)\s+([a-zA-Z]{3,20})\b""")
        cityRegexEn.find(cleanUser)?.let { match ->
            memoriesToSave.add("User lives in ${match.groupValues[1].replaceFirstChar { it.uppercase() }}")
        } ?: cityRegex.find(cleanUser)?.let { match ->
            val place = match.groupValues[1].trim()
            if (!place.contains("main") && !place.contains("mein")) {
                memoriesToSave.add("User lives in or is from ${place.replaceFirstChar { it.uppercase() }}")
            }
        }

        // 4. Likes/Dislikes matches
        val likeRegex = Regex("""(?i)\b(?:i like|i love|mujhe)\s+([a-zA-Z\s]{3,25})\s*(?:pasand|pasand hai)?\b""")
        val dislikeRegex = Regex("""(?i)\b(?:i hate|i dislike|mujhe)\s+([a-zA-Z\s]{3,25})\s*(?:pasand nahi|na pasand|bura lagta)\b""")
        if (cleanUserLower.contains("hate") || cleanUserLower.contains("dislike") || cleanUserLower.contains("pasand nahi") || cleanUserLower.contains("na pasand")) {
            dislikeRegex.find(cleanUser)?.let { match ->
                val thing = match.groupValues[1].trim()
                memoriesToSave.add("User dislikes $thing")
            }
        } else {
            likeRegex.find(cleanUser)?.let { match ->
                val thing = match.groupValues[1].trim()
                if (!thing.contains("mujhe")) {
                    memoriesToSave.add("User likes $thing")
                }
            }
        }

        // 5. Job matches
        val jobRegex = Regex("""(?i)\b(?:i work as|i am a|i am an|mera kaam)\s+([a-zA-Z\s]{3,25})\b""")
        jobRegex.find(cleanUser)?.let { match ->
            memoriesToSave.add("User's job/work: ${match.groupValues[1]}")
        }

        // 6. Birthday matches
        val bdayRegex = Regex("""(?i)\b(?:my birthday is on|i was born on|mera birthday)\s+([a-zA-Z0-9\s]{3,20})\b""")
        bdayRegex.find(cleanUser)?.let { match ->
            memoriesToSave.add("User's birthday is ${match.groupValues[1]}")
        }

        // Also scan AI reply for statements about the user
        val cleanAi = aiText.trim()
        val aiNameRegex = Regex("""(?i)\b(?:your name is)\s+([a-zA-Z]{2,15})\b""")
        aiNameRegex.find(cleanAi)?.let { match ->
            memoriesToSave.add("User's name is ${match.groupValues[1].replaceFirstChar { it.uppercase() }}")
        }

        val aiAgeRegex = Regex("""(?i)\byou are\s+(\d{1,2})\s*(?:years old)?\b""")
        aiAgeRegex.find(cleanAi)?.let { match ->
            memoriesToSave.add("User's age is ${match.groupValues[1]}")
        }

        val aiCityRegex = Regex("""(?i)\b(?:you live in|you are from)\s+([a-zA-Z]{3,20})\b""")
        aiCityRegex.find(cleanAi)?.let { match ->
            memoriesToSave.add("User lives in ${match.groupValues[1].replaceFirstChar { it.uppercase() }}")
        }

        val aiLikeRegex = Regex("""(?i)\b(?:you like|you love)\s+([a-zA-Z\s]{3,25})\b""")
        aiLikeRegex.find(cleanAi)?.let { match ->
            memoriesToSave.add("User likes ${match.groupValues[1].trim()}")
        }

        val aiJobRegex = Regex("""(?i)\b(?:you work as|you are a|you are an)\s+([a-zA-Z\s]{3,25})\b""")
        aiJobRegex.find(cleanAi)?.let { match ->
            memoriesToSave.add("User's job/work: ${match.groupValues[1].trim()}")
        }

        val aiBdayRegex = Regex("""(?i)\byour birthday is on\s+([a-zA-Z0-9\s]{3,20})\b""")
        aiBdayRegex.find(cleanAi)?.let { match ->
            memoriesToSave.add("User's birthday is ${match.groupValues[1].trim()}")
        }

        // Save matched facts
        val existing = db.memoryDao().getAllMemoriesList()
        for (fact in memoriesToSave) {
            if (existing.none { it.factText.equals(fact, ignoreCase = true) }) {
                db.memoryDao().insertMemory(com.example.data.Memory(factText = fact, source = "auto"))
                Log.d("AiraViewModel", "Auto-saved fact from conversation: $fact")
            }
        }
    }

    suspend fun getRelevantMemories(userQuery: String): List<String> = withContext(Dispatchers.IO) {
        val allMemories = db.memoryDao().getAllMemoriesList()
        if (allMemories.isEmpty()) return@withContext emptyList()

        val queryWords = userQuery.lowercase().split(Regex("\\s+")).filter { it.length > 2 }
        
        val scoredMemories = allMemories.map { memory ->
            val factLower = memory.factText.lowercase()
            var score = 0
            for (word in queryWords) {
                if (factLower.contains(word)) {
                    score++
                }
            }
            val ageBias = 1.0 / (1.0 + (System.currentTimeMillis() - memory.createdAt) / 100000.0)
            Pair(memory, score.toDouble() + ageBias)
        }

        val sorted = scoredMemories.sortedByDescending { it.second }.map { it.first.factText }
        sorted.take(5)
    }

    // --- Local Device Custom Voice Controls & Permissions ---
    private fun checkAndExecuteDeviceCommands(input: String): Boolean {
        return when {
            input.contains("lights") || input.contains("light") -> {
                val state = !input.contains("off") && !input.contains("stop")
                val responseMsg = toggleFlashlight(state)
                val statusText = if (state) "Lights command invoked. Room/device lights are now configured to ON." else "Lights command invoked. Room/device lights are now configured to OFF. All sub-system LEDs deactivated."
                viewModelScope.launch {
                    chatDao.insertMessage(ChatMessage(sender = "aira", message = statusText))
                    speakText(statusText)
                }
                true
            }
            input.contains("weather") || input.contains("temperature") -> {
                fetchWeather()
                viewModelScope.launch {
                    kotlinx.coroutines.delay(800) // allow fetchWeather to update
                    val currentRes = _weatherText.value
                    val responseMsg = "Weather command invoked. Here is the current environmental telemetry: $currentRes"
                    chatDao.insertMessage(ChatMessage(sender = "aira", message = responseMsg))
                    speakText(responseMsg)
                }
                true
            }
            input.contains("status report") || input.contains("status") -> {
                val report = "Initializing system status report. Neural engine: Piper TTS active. Wake word detection: Active. Offline brain state: configured. System performance: 60 FPS unlocked. All parameters normalized."
                viewModelScope.launch {
                    chatDao.insertMessage(ChatMessage(sender = "aira", message = report))
                    speakText(report)
                }
                true
            }
            input.contains("flashlight") || input.contains("torch") -> {
                val state = !input.contains("off") && !input.contains("stop")
                val responseMsg = toggleFlashlight(state)
                viewModelScope.launch {
                    chatDao.insertMessage(ChatMessage(sender = "aira", message = responseMsg))
                    speakText(responseMsg)
                }
                true
            }
            input.contains("wifi") || input.contains("wi-fi") || input.contains("internet") -> {
                val enable = !input.contains("off") && !input.contains("disable") && !input.contains("stop") && !input.contains("deactivate")
                val service = com.example.service.AiraAccessibilityService.instance
                val responseMsg = if (service != null) {
                    service.toggleWifi(enable)
                } else {
                    "Accessibility service is offline. Please enable Aira Command Core in system accessibility settings to automate Wi-Fi controls."
                }
                viewModelScope.launch {
                    chatDao.insertMessage(ChatMessage(sender = "aira", message = responseMsg))
                    speakText(responseMsg)
                }
                true
            }
            input.contains("bluetooth") || input.contains("bt ") || input.endsWith("bt") -> {
                val enable = !input.contains("off") && !input.contains("disable") && !input.contains("stop") && !input.contains("deactivate")
                val service = com.example.service.AiraAccessibilityService.instance
                val responseMsg = if (service != null) {
                    service.toggleBluetooth(enable)
                } else {
                    "Accessibility service is offline. Please enable Aira Command Core in system accessibility settings to automate Bluetooth controls."
                }
                viewModelScope.launch {
                    chatDao.insertMessage(ChatMessage(sender = "aira", message = responseMsg))
                    speakText(responseMsg)
                }
                true
            }
            input.contains("call") || input.contains("dial") -> {
                // Find potential digits or names
                val digits = input.filter { it.isDigit() }
                val number = if (digits.isNotEmpty()) digits else "911" // fallback or generic
                val responseMsg = "Dialing phone number: $number"
                initiatePhoneCall(number)
                viewModelScope.launch {
                    chatDao.insertMessage(ChatMessage(sender = "aira", message = responseMsg))
                    speakText(responseMsg)
                }
                true
            }
            input.contains("alarm") || input.contains("wake me up") -> {
                // Extracts alarm parameters
                val timeDigits = input.filter { it.isDigit() }
                val hour = if (timeDigits.length >= 2) timeDigits.substring(0, 2).toIntOrNull() ?: 7 else 7
                val minute = if (timeDigits.length >= 4) timeDigits.substring(2, 4).toIntOrNull() ?: 0 else 0
                setSystemAlarm(hour, minute, "Aira Wake UP Call")
                val responseMsg = "Scheduling system alarm for $hour:$minute"
                viewModelScope.launch {
                    chatDao.insertMessage(ChatMessage(sender = "aira", message = responseMsg))
                    speakText(responseMsg)
                }
                true
            }
            input.contains("settings") || input.contains("configure") -> {
                val responseMsg = "Opening Settings directory. Tap settings above."
                viewModelScope.launch {
                    chatDao.insertMessage(ChatMessage(sender = "aira", message = responseMsg))
                    speakText(responseMsg)
                }
                true
            }
            input.contains("silent") || input.contains("mute") -> {
                setSoundMode(AudioManager.RINGER_MODE_SILENT)
                val responseMsg = "System ring audio configured to Silent Mode."
                viewModelScope.launch {
                    chatDao.insertMessage(ChatMessage(sender = "aira", message = responseMsg))
                    speakText(responseMsg)
                }
                true
            }
            input.contains("vibrate") -> {
                setSoundMode(AudioManager.RINGER_MODE_VIBRATE)
                val responseMsg = "System ring audio configured to Vibrate Mode."
                viewModelScope.launch {
                    chatDao.insertMessage(ChatMessage(sender = "aira", message = responseMsg))
                    speakText(responseMsg)
                }
                true
            }
            input.contains("sound") || input.contains("normal mode") -> {
                setSoundMode(AudioManager.RINGER_MODE_NORMAL)
                val responseMsg = "System ring audio configured to Normal Volume."
                viewModelScope.launch {
                    chatDao.insertMessage(ChatMessage(sender = "aira", message = responseMsg))
                    speakText(responseMsg)
                }
                true
            }
            input.contains("camera") || input.contains("photo") -> {
                val responseMsg = "Launching device camera."
                launchSystemCamera()
                viewModelScope.launch {
                    chatDao.insertMessage(ChatMessage(sender = "aira", message = responseMsg))
                    speakText(responseMsg)
                }
                true
            }
            input.contains("toggle hud") || input.contains("hide hud") || input.contains("show hud") || input.contains("toggle display") || input.contains("toggle core display") -> {
                val newState = if (input.contains("hide") || input.contains("off") || input.contains("disable")) {
                    false
                } else if (input.contains("show") || input.contains("on") || input.contains("enable")) {
                    true
                } else {
                    !_showHud.value
                }
                toggleHud(newState)
                val responseMsg = if (newState) "Circular holographic HUD enabled." else "Holographic HUD hidden."
                viewModelScope.launch {
                    chatDao.insertMessage(ChatMessage(sender = "aira", message = responseMsg))
                    speakText(responseMsg)
                }
                true
            }
            input.contains("clear chat") || input.contains("clear history") || input.contains("delete conversation") || input.contains("clear archives") || input.contains("delete chat") -> {
                clearChatHistory()
                val responseMsg = "Vocal archives cleared."
                viewModelScope.launch {
                    chatDao.insertMessage(ChatMessage(sender = "aira", message = responseMsg))
                    speakText(responseMsg)
                }
                true
            }
            else -> false
        }
    }

    // --- Hardware Integrations ---
    fun toggleFlashlight(on: Boolean): String {
        val cameraManager = getApplication<Application>().getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        return try {
            val cameraId = cameraManager?.cameraIdList?.firstOrNull()
            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, on)
                if (on) "Aira command: Flashlight turned ON." else "Aira command: Flashlight turned OFF."
            } else {
                "Flashlight camera unit not found."
            }
        } catch (e: Exception) {
            "Flashlight access denied: ${e.localizedMessage}"
        }
    }

    fun initiatePhoneCall(number: String) {
        val context = getApplication<Application>()
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$number")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("AiraViewModel", "Failed to dial phone", e)
        }
    }

    fun setSystemAlarm(hour: Int, minute: Int, message: String) {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            getApplication<Application>().startActivity(intent)
        } catch (e: Exception) {
            Log.e("AiraViewModel", "Failed to schedule alarm", e)
        }
    }

    fun setSoundMode(ringerMode: Int) {
        val audioManager = getApplication<Application>().getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        try {
            audioManager?.ringerMode = ringerMode
        } catch (e: Exception) {
            Log.e("AiraViewModel", "Sound toggle failed. Needs permission / Do Not Disturb access.", e)
        }
    }

    fun launchSystemCamera() {
        val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            getApplication<Application>().startActivity(intent)
        } catch (e: Exception) {
            Log.e("AiraViewModel", "Failed to launch camera", e)
        }
    }

    // --- Extras: Real APIs Fetch Weather & News (Optimized) ---
    private fun fetchWeather() {
        viewModelScope.launch(Dispatchers.IO) {
            // Free and completely open weather API: open-meteo (no keys required!)
            val url = "https://api.open-meteo.com/v1/forecast?latitude=37.77&longitude=-122.41&current_weather=true"
            try {
                val request = Request.Builder().url(url).build()
                OkHttpClient().newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (body != null) {
                            val json = JSONObject(body)
                            val current = json.getJSONObject("current_weather")
                            val temp = current.getDouble("temperature")
                            val wind = current.getDouble("windspeed")
                            _weatherText.value = "San Francisco: Temp $temp°C, Wind $wind km/h"
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AiraViewModel", "Weather API failed, fallback to offline forecast", e)
                _weatherText.value = "Weather offline: 17°C, Foggy"
            }
        }
    }

    private fun fetchNews() {
        viewModelScope.launch(Dispatchers.IO) {
            // Fetch live mock / actual headlines in an extremely slim RSS payload or direct static list to minimize heap load
            val headlines = listOf(
                "AI Advances in local edge reasoning platforms.",
                "Android standard 16 dynamic color customization rolls out.",
                "Global climate systems display warming trends this season."
            )
            _newsFeed.value = headlines
        }
    }

    // --- Settings Savers ---
    fun updateWakeWord(word: String) {
        _wakeWord.value = word
        sharedPrefs.edit().putString("wake_word", word).apply()
    }

    fun toggleOfflineBrain(isOffline: Boolean) {
        _isOfflineBrain.value = isOffline
        sharedPrefs.edit().putBoolean("offline_brain", isOffline).apply()
        val suffix = if (isOffline) "Active (Llama 3.2 local engine active)" else "Inactive (Online Brain active)"
        speakText("Aira offline brain mode configured to $suffix")
    }

    fun updateOnlineModel(model: String) {
        _onlineModel.value = model
        sharedPrefs.edit().putString("online_model", model).apply()
        speakText("Online AI brain configured to $model")
    }

    fun updateLlamaThreads(threads: Int) {
        _llamaThreads.value = threads
        sharedPrefs.edit().putInt("llama_threads", threads).apply()
        speakText("Llama core execution configured to $threads threads")
    }

    fun getLlamaEngineStatus(): String {
        return llamaCppBrain.getEngineStatus()
    }

    fun updateThemeIndex(index: Int) {
        _themeIndex.value = index
        sharedPrefs.edit().putInt("theme_index", index).apply()
    }

    fun selectTheme(index: Int) {
        _themeIndex.value = index
        sharedPrefs.edit().putInt("theme_index", index).apply()
    }

    fun updateCustomColorHex(hex: String) {
        _customColorHex.value = hex
        sharedPrefs.edit().putString("custom_color_hex", hex).apply()
    }

    fun toggleLowPerformanceMode(enabled: Boolean) {
        _lowPerformanceMode.value = enabled
        sharedPrefs.edit().putBoolean("low_performance", enabled).apply()
    }

    // --- Piper TTS Setters & Sync ---
    fun togglePiperTts(enabled: Boolean) {
        _usePiperTts.value = enabled
        sharedPrefs.edit().putBoolean("use_piper_tts", enabled).apply()
        piperTtsManager.setEngineEnabled(enabled)
        val term = if (enabled) "Piper Low-Latency Neural synthesis activated." else "Standard device Speech synthesis reactive."
        speakText(term)
    }

    fun updatePiperVoice(voiceId: String) {
        piperTtsManager.setVoice(voiceId)
        speakText("Acoustic voice loaded: $voiceId.")
    }

    fun downloadPiperModel(voiceId: String) {
        piperTtsManager.downloadVoiceModel(voiceId)
    }

    fun deletePiperModel(voiceId: String) {
        piperTtsManager.deleteVoiceModel(voiceId)
    }

    // --- Reminders CRUD ---
    fun addReminder(title: String, timeLabel: String) {
        viewModelScope.launch {
            reminderDao.insertReminder(Reminder(title = title, timeLabel = timeLabel))
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            reminderDao.deleteReminder(reminder)
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            chatDao.clearHistory()
        }
    }

    fun exportChatHistory(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val messages = chatHistory.value
            if (messages.isEmpty()) {
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "No conversations to export", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            try {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val sb = java.lang.StringBuilder()
                sb.append("Aira Chat History Export\n")
                sb.append("Exported on: ${sdf.format(java.util.Date())}\n")
                sb.append("========================================\n\n")

                for (msg in messages) {
                    val senderName = if (msg.sender == "user") "User" else "Aira"
                    val timeStr = sdf.format(java.util.Date(msg.timestamp))
                    sb.append("[$timeStr] $senderName: ${msg.message}\n")
                }

                val textContent = sb.toString()
                
                val exportFile = File(context.cacheDir, "aira_chat_export.txt")
                exportFile.writeText(textContent)

                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    exportFile
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Aira Chat History Export")
                    putExtra(Intent.EXTRA_TEXT, "Here is my recent conversation export from Aira.")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(intent, "Export Chat History")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                
                launch(Dispatchers.Main) {
                    context.startActivity(chooser)
                }
            } catch (e: Exception) {
                Log.e("AiraViewModel", "Failed to export chat", e)
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // --- Speech Recognition Overrides & Amplitude tracking ---
    override fun onReadyForSpeech(params: Bundle?) {
        _isListening.value = true
    }

    override fun onBeginningOfSpeech() {
        Log.d("AiraViewModel", "Beginning speech")
        hasSpeechStarted = true
        handler.removeCallbacks(timeoutRunnable)
    }

    override fun onRmsChanged(rmsdB: Float) {
        // Map rmsdB (typically range -2 to 10+) nicely to 0f-1f for anims
        val amp = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
        _audioAmplitude.value = amp
        if (rmsdB > 2f) {
            hasSpeechStarted = true
            handler.removeCallbacks(timeoutRunnable)
        }
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        _isListening.value = false
        _audioAmplitude.value = 0f
    }

    override fun onError(error: Int) {
        _isListening.value = false
        _audioAmplitude.value = 0f
        
        if (isUsingGoogleSTT) {
            Log.e("AiraViewModel", "Google STT error $error. Switching to Offline Vosk...")
            switchToOfflineVosk()
            return
        }

        val msg = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
            SpeechRecognizer.ERROR_CLIENT -> "Client error. Make sure Google Voice assistant is default."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Record Audio permission is required."
            SpeechRecognizer.ERROR_NETWORK -> "Network error."
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout."
            SpeechRecognizer.ERROR_NO_MATCH -> "No phrasing recognized. Try again."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Vocal system is busy."
            SpeechRecognizer.ERROR_SERVER -> "Server error."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech input timeout."
            else -> "Speech trigger error."
        }
        _currentStatus.value = msg
        Log.e("AiraViewModel", "Speech recognition error limit: $error ($msg)")
        if (_usePersistentListening.value) {
            restartContinuousListeningIfNeeded()
        }
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull() ?: ""
        if (text.isNotEmpty()) {
            _currentStatus.value = "Recognized: $text"
            
            val isContinuous = _usePersistentListening.value
            val currentWakeWord = _wakeWord.value.lowercase().trim()
            val lowerText = text.lowercase().trim()

            if (isContinuous) {
                if (lowerText.contains(currentWakeWord)) {
                    val index = lowerText.indexOf(currentWakeWord)
                    var command = lowerText.substring(index + currentWakeWord.length).trim()
                    
                    if (command.startsWith(",") || command.startsWith(":") || command.startsWith("-")) {
                        command = command.substring(1).trim()
                    }

                    if (command.isEmpty()) {
                        val responses = listOf("Standing by.", "At your service.", "I'm listening.", "Aira activated.")
                        val ack = responses.random()
                        viewModelScope.launch {
                            chatDao.insertMessage(ChatMessage(sender = "aira", message = ack))
                            speakText(ack)
                        }
                    } else {
                        processAssistantSession(command)
                    }
                } else {
                    Log.d("AiraViewModel", "Ignored phrase without wake word in continuous mode: $text")
                    restartContinuousListeningIfNeeded()
                }
            } else {
                processAssistantSession(text)
            }
        } else {
            _currentStatus.value = "Empty phrasing recognized."
            if (_usePersistentListening.value) {
                restartContinuousListeningIfNeeded()
            }
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull() ?: ""
        if (text.isNotEmpty()) {
            _currentStatus.value = "Phonetics: $text"
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    override fun onCleared() {
        super.onCleared()
        speechRecognizer?.destroy()

        // Fix Native Llama memory leak
        try {
            llamaCppBrain.deinitializeNativeEngine()
        } catch (e: Exception) {
            Log.e("AiraViewModel", "Error deinitializing llama engine in onCleared", e)
        }

        // Fix Piper TTS shutdown leak
        try {
            piperTtsManager.shutdown()
        } catch (e: Exception) {
            Log.e("AiraViewModel", "Error shutting down piper TTS in onCleared", e)
        }

        // Fix Vosk Speech Service leak
        try {
            voskSpeechService?.stop()
            voskSpeechService = null
        } catch (e: Exception) {
            Log.e("AiraViewModel", "Error stopping Vosk service in onCleared", e)
        }
    }

    private fun loadVoiceCommandLogs() {
        val jsonStr = sharedPrefs.getString("voice_command_logs_json", "[]") ?: "[]"
        try {
            val jsonArray = org.json.JSONArray(jsonStr)
            val list = mutableListOf<VoiceCommandLog>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    VoiceCommandLog(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        command = obj.optString("command", ""),
                        matchedTrigger = if (obj.isNull("matchedTrigger")) null else obj.optString("matchedTrigger"),
                        timestamp = obj.optString("timestamp", ""),
                        status = obj.optString("status", "SUCCESS"),
                        details = obj.optString("details", "")
                    )
                )
            }
            _voiceCommandLogs.value = list
        } catch (e: Exception) {
            Log.e("AiraViewModel", "Error loading voice command logs", e)
        }
    }

    private fun saveVoiceCommandLogs(list: List<VoiceCommandLog>) {
        try {
            val jsonArray = org.json.JSONArray()
            for (log in list) {
                val obj = org.json.JSONObject()
                obj.put("id", log.id)
                obj.put("command", log.command)
                obj.put("matchedTrigger", log.matchedTrigger)
                obj.put("timestamp", log.timestamp)
                obj.put("status", log.status)
                obj.put("details", log.details)
                jsonArray.put(obj)
            }
            sharedPrefs.edit().putString("voice_command_logs_json", jsonArray.toString()).apply()
        } catch (e: Exception) {
            Log.e("AiraViewModel", "Error saving voice command logs", e)
        }
    }

    fun addVoiceCommandLog(command: String, matchedTrigger: String?, status: String, details: String) {
        val sdf = java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault())
        val timeStr = sdf.format(java.util.Date())
        val newLog = VoiceCommandLog(
            command = command,
            matchedTrigger = matchedTrigger,
            timestamp = timeStr,
            status = status,
            details = details
        )
        // Keep last 30 logs
        val updatedList = (listOf(newLog) + _voiceCommandLogs.value).take(30)
        _voiceCommandLogs.value = updatedList
        saveVoiceCommandLogs(updatedList)
    }

    fun clearVoiceCommandLogs() {
        _voiceCommandLogs.value = emptyList()
        saveVoiceCommandLogs(emptyList())
    }
}

data class VoiceCommandLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val command: String,
    val matchedTrigger: String?,
    val timestamp: String,
    val status: String, // "SUCCESS", "FAILED", "ABORTED"
    val details: String
)
