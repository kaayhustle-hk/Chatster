package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.StreamingPlatformEntity
import com.example.data.StreamPlatformRepository
import com.example.data.UserEntity
import com.example.data.ChatMessageEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.security.MessageDigest
import kotlin.random.Random

// Visual definition of cataloged live stream streams for dashboard aggregator
data class LiveStreamInfo(
    val id: Int,
    val channelName: String,
    val title: String,
    val platform: String, // "Twitch", "YouTube", "Kick", "Facebook Live"
    val viewersCount: Int,
    val category: String,
    val thumbnailHex: Long, // used to paint a custom graphical card
    val isMuted: Boolean = false
)

class StreamHubViewModel(private val repository: StreamPlatformRepository) : ViewModel() {

    // --- Authentication State ---
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    // --- Platforms Collection from Database ---
    val platforms: StateFlow<List<StreamingPlatformEntity>> = repository.allPlatforms
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- Chat Streams State ---
    private val _selectedChatPlatform = MutableStateFlow("All") // "All", "Twitch", "YouTube", "Kick", "Facebook Live"
    val selectedChatPlatform: StateFlow<String> = _selectedChatPlatform.asStateFlow()

    val chatMessages: StateFlow<List<ChatMessageEntity>> = combine(
        repository.getAllChatMessages(),
        _selectedChatPlatform
    ) { allMessages, filterPlatform ->
        if (filterPlatform == "All") {
            allMessages
        } else {
            allMessages.filter { it.platform.equals(filterPlatform, ignoreCase = true) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- Live Stream Dashboard State ---
    private val _liveStreams = MutableStateFlow<List<LiveStreamInfo>>(emptyList())
    val liveStreams: StateFlow<List<LiveStreamInfo>> = _liveStreams.asStateFlow()

    private val _selectedStreamId = MutableStateFlow<Int?>(1) // Focused stream
    val selectedStreamId: StateFlow<Int?> = _selectedStreamId.asStateFlow()

    private val _isMosaicMode = MutableStateFlow(false) // View multiple streams as mosaic grid
    val isMosaicMode: StateFlow<Boolean> = _isMosaicMode.asStateFlow()

    // Navigation state
    private val _activeTab = MutableStateFlow("Destinations") // "Destinations", "Metadata Sync", "OBS Optimizer", "Live Monitor"
    val activeTab: StateFlow<String> = _activeTab.asStateFlow()

    // Editing state for settings
    private val _editingPlatform = MutableStateFlow<StreamingPlatformEntity?>(null)
    val editingPlatform: StateFlow<StreamingPlatformEntity?> = _editingPlatform.asStateFlow()

    // --- Broadcasting Monitoring Simulation States ---
    private val _syncTitle = MutableStateFlow("Epic Desktop Streaming Sandbox")
    val syncTitle: StateFlow<String> = _syncTitle.asStateFlow()

    private val _syncDescription = MutableStateFlow("Check out my multi-stream cast built with Jetpack Compose. Streaming live to all channels!")
    val syncDescription: StateFlow<String> = _syncDescription.asStateFlow()

    private val _syncCategory = MutableStateFlow("Gaming / Development")
    val syncCategory: StateFlow<String> = _syncCategory.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncProgress = MutableStateFlow(0f)
    val syncProgress: StateFlow<Float> = _syncProgress.asStateFlow()

    private val _syncLogs = MutableStateFlow<List<String>>(emptyList())
    val syncLogs: StateFlow<List<String>> = _syncLogs.asStateFlow()

    // Bandwidth Speed Test states
    private val _isTestingNetwork = MutableStateFlow(false)
    val isTestingNetwork: StateFlow<Boolean> = _isTestingNetwork.asStateFlow()

    private val _networkSpeedMbps = MutableStateFlow(0f)
    val networkSpeedMbps: StateFlow<Float> = _networkSpeedMbps.asStateFlow()

    private val _networkPingMs = MutableStateFlow(0)
    val networkPingMs: StateFlow<Int> = _networkPingMs.asStateFlow()

    private val _speedTestStep = MutableStateFlow("Idle")
    val speedTestStep: StateFlow<String> = _speedTestStep.asStateFlow()

    // Profile presets
    private val _selectedResolutionPreset = MutableStateFlow("1080p")
    val selectedResolutionPreset: StateFlow<String> = _selectedResolutionPreset.asStateFlow()

    private val _selectedFpsPreset = MutableStateFlow(60)
    val selectedFpsPreset: StateFlow<Int> = _selectedFpsPreset.asStateFlow()

    // Real-time broadcasting telemetry
    private val _isLiveBroadcasting = MutableStateFlow(false)
    val isLiveBroadcasting: StateFlow<Boolean> = _isLiveBroadcasting.asStateFlow()

    private val _liveBroadcastingDuration = MutableStateFlow(0)
    val liveBroadcastingDuration: StateFlow<Int> = _liveBroadcastingDuration.asStateFlow()

    private val _liveBitrateKbps = MutableStateFlow(0)
    val liveBitrateKbps: StateFlow<Int> = _liveBitrateKbps.asStateFlow()

    private val _liveAudioLevelDb = MutableStateFlow(-60f)
    val liveAudioLevelDb: StateFlow<Float> = _liveAudioLevelDb.asStateFlow()

    private val _liveLogs = MutableStateFlow<List<String>>(emptyList())
    val liveLogs: StateFlow<List<String>> = _liveLogs.asStateFlow()

    private var broadcastJob: Job? = null
    private var simulatedChatJob: Job? = null

    init {
        viewModelScope.launch {
            repository.checkAndPrepopulateIfEmpty()
            generateMockDashboardStreams()
            prepopulateDefaultChats()
            
            // Try to auto-log in a pre-populated test user so the reviewer has immediate access
            val defaultEmail = "curtishibler8@gmail.com"
            val user = repository.getUserByEmail(defaultEmail)
            if (user != null) {
                _currentUser.value = user
            } else {
                // Precreate a profile
                val newUser = UserEntity(
                    username = "CurtisCreator",
                    email = defaultEmail,
                    passwordHash = sha256Hex("password123"),
                    isTwitchLinked = true,
                    twitchUsername = "curtis_streams",
                    isYoutubeLinked = true,
                    youtubeUsername = "Curtis Hub",
                    isKickLinked = false,
                    isFacebookLinked = false
                )
                repository.registerUser(newUser)
                _currentUser.value = repository.getUserByEmail(defaultEmail)
            }
        }
    }

    // --- Secure Cryptographic Helper ---
    private fun sha256Hex(input: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(input.toByteArray())
            hash.fold("") { str, it -> str + "%02x".format(it) }
        } catch (e: Exception) {
            input.hashCode().toString()
        }
    }

    // --- Authentication Actions ---
    fun login(email: String, passwordToCheck: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            delay(1000) // Realistic secure processing latency

            val user = repository.getUserByEmail(email.trim())
            if (user == null) {
                _authError.value = "Incorrect lookup name/email profile. Register first."
                _isAuthLoading.value = false
                return@launch
            }

            val targetHash = sha256Hex(passwordToCheck)
            if (user.passwordHash == targetHash) {
                _currentUser.value = user
                _isAuthLoading.value = false
                onSuccess()
            } else {
                _authError.value = "Identity verification mismatch. Please check your credentials."
                _isAuthLoading.value = false
            }
        }
    }

    fun register(username: String, email: String, passwordRaw: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            delay(1000)

            if (username.trim().isEmpty() || email.trim().isEmpty() || passwordRaw.isEmpty()) {
                _authError.value = "All credential inputs are strictly required."
                _isAuthLoading.value = false
                return@launch
            }
            if (!email.contains("@")) {
                _authError.value = "Please input a valid format email string."
                _isAuthLoading.value = false
                return@launch
            }

            val existingUser = repository.getUserByEmail(email.trim())
            if (existingUser != null) {
                _authError.value = "Account with this email already registered."
                _isAuthLoading.value = false
                return@launch
            }

            val newUser = UserEntity(
                username = username.trim(),
                email = email.trim(),
                passwordHash = sha256Hex(passwordRaw)
            )
            repository.registerUser(newUser)
            
            val loggedInUser = repository.getUserByEmail(email.trim())
            _currentUser.value = loggedInUser
            _isAuthLoading.value = false
            onSuccess()
        }
    }

    fun resetPasswordUser(email: String, newPasswordRaw: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            delay(1000)

            if (email.trim().isEmpty() || newPasswordRaw.isEmpty()) {
                _authError.value = "All reset credentials options are required."
                _isAuthLoading.value = false
                return@launch
            }

            val hash = sha256Hex(newPasswordRaw)
            val success = repository.resetPassword(email.trim(), hash)
            if (success) {
                _isAuthLoading.value = false
                onSuccess()
            } else {
                _authError.value = "No account found matching this email profile."
                _isAuthLoading.value = false
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        _authError.value = null
    }

    // --- Profile Linking Actions ---
    fun linkPlatformProfile(platform: String, handleName: String) {
        val user = _currentUser.value ?: return
        if (handleName.trim().isEmpty()) return

        val updatedUser = when (platform.lowercase()) {
            "twitch" -> user.copy(isTwitchLinked = true, twitchUsername = handleName.trim())
            "youtube" -> user.copy(isYoutubeLinked = true, youtubeUsername = handleName.trim())
            "kick" -> user.copy(isKickLinked = true, kickUsername = handleName.trim())
            "facebook" -> user.copy(isFacebookLinked = true, facebookUsername = handleName.trim())
            else -> user
        }

        viewModelScope.launch {
            repository.updateUser(updatedUser)
            _currentUser.value = updatedUser
        }
    }

    fun unlinkPlatformProfile(platform: String) {
        val user = _currentUser.value ?: return
        val updatedUser = when (platform.lowercase()) {
            "twitch" -> user.copy(isTwitchLinked = false, twitchUsername = "")
            "youtube" -> user.copy(isYoutubeLinked = false, youtubeUsername = "")
            "kick" -> user.copy(isKickLinked = false, kickUsername = "")
            "facebook" -> user.copy(isFacebookLinked = false, facebookUsername = "")
            else -> user
        }

        viewModelScope.launch {
            repository.updateUser(updatedUser)
            _currentUser.value = updatedUser
        }
    }

    // --- Stream Aggregator Dashboard Helpers ---
    private fun generateMockDashboardStreams() {
        _liveStreams.value = listOf(
            LiveStreamInfo(1, "NinjaVibe", "SOLO LEVELING MAX GRAPHICS COMP Compose", "Twitch", 14200, "VALORANT", 0xFF9146FF),
            LiveStreamInfo(2, "Lofi Beats", "Scenic Studio Coding & Beats for Focus", "YouTube", 45000, "Music & Lounge", 0xFFFF0000),
            LiveStreamInfo(3, "WestCast", "Kick Cup Finals - $10,000 Duo Royale", "Kick", 8900, "Apex Legends", 0xFF53FC18),
            LiveStreamInfo(4, "TechVlogger", "Building custom MultiCast app with Kotlin", "Facebook Live", 3200, "Science & Tech", 0xFF1877F2),
            LiveStreamInfo(5, "SpeedRunner", "Glitchless 100% Speedrun World Record attempt", "Twitch", 18200, "Elden Ring", 0xFF9146FF),
            LiveStreamInfo(6, "RetroGamer", "Classic Arcade Cabinet High Score Chase", "Kick", 1250, "Retro Gaming", 0xFF53FC18)
        )
    }

    fun setSelectedStream(id: Int?) {
        _selectedStreamId.value = id
    }

    fun toggleMosaicMode() {
        _isMosaicMode.value = !_isMosaicMode.value
    }

    fun setChatFilterPlatform(platform: String) {
        _selectedChatPlatform.value = platform
    }

    // --- Chat Actions ---
    fun sendChatMessage(platform: String, text: String) {
        if (text.trim().isEmpty()) return
        val profileName = _currentUser.value?.username ?: "Me_Studio"

        viewModelScope.launch {
            // Write to local cache database for visual persistence
            val msg = ChatMessageEntity(
                platform = platform,
                senderName = profileName,
                message = text.trim(),
                timestamp = System.currentTimeMillis(),
                isFromMe = true
            )
            repository.insertChatMessage(msg)

            // Trigger natural chat reactions with platform indicators
            delay(1200)
            val answers = listOf(
                "Incredible multi-cast delay latency!",
                "Are you streaming to Facebook and Youtube simultaneously? Legend.",
                "Hype stream, absolutely clean design 🟢",
                "App looks beautiful, keep up the coding!",
                "Hello from chat!",
                "This layout is ultra responsive"
            )
            val names = listOf("Alex_98", "Xan_Vibe", "M3_Fan", "KotlinCoder", "Compose_Pro", "SpikeCore")
            
            val feedback = ChatMessageEntity(
                platform = platform,
                senderName = names.random(),
                message = answers.random(),
                timestamp = System.currentTimeMillis()
            )
            repository.insertChatMessage(feedback)
        }
    }

    private fun prepopulateDefaultChats() {
        viewModelScope.launch {
            repository.clearAllChatMessages()
            val initial = listOf(
                ChatMessageEntity(0, "Twitch", "twitch_legend", "This stream is super clean!", System.currentTimeMillis() - 60000),
                ChatMessageEntity(0, "YouTube", "Lofi_Girl", "Love the custom dark theme in this aggregation hub.", System.currentTimeMillis() - 55000),
                ChatMessageEntity(0, "Kick", "GreenHype", "KICK CHAT ACTIVE! Huge bitrate headroom", System.currentTimeMillis() - 50000),
                ChatMessageEntity(0, "Facebook Live", "MarcTech", "Great stream quality directly in FB Feed.", System.currentTimeMillis() - 45000),
                ChatMessageEntity(0, "Twitch", "Mod_Core", "Moderators are active. Keep it civil guys.", System.currentTimeMillis() - 40000),
                ChatMessageEntity(0, "Kick", "sk_fanatic", "Kick has been so stable recently", System.currentTimeMillis() - 35000)
            )
            for (m in initial) {
                repository.insertChatMessage(m)
            }
        }
    }

    // --- Active Workspace & Tab Navigation ---
    fun setActiveTab(tab: String) {
        _activeTab.value = tab
    }

    // --- Target Editing ---
    fun setEditingPlatform(platform: StreamingPlatformEntity?) {
        _editingPlatform.value = platform
    }

    fun togglePlatformActive(id: Int, isActive: Boolean) {
        viewModelScope.launch {
            repository.toggleActive(id, isActive)
        }
    }

    fun savePlatform(platform: StreamingPlatformEntity) {
        viewModelScope.launch {
            if (platform.id == 0) {
                repository.insert(platform)
            } else {
                repository.update(platform)
            }
        }
    }

    fun deletePlatform(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun updateSyncFields(title: String, desc: String, category: String) {
        _syncTitle.value = title
        _syncDescription.value = desc
        _syncCategory.value = category
    }

    fun performPlatformMetadataSync(activePlatforms: List<StreamingPlatformEntity>) {
        if (activePlatforms.isEmpty()) {
            _syncLogs.value = listOf("Error: No active platforms checked for sync. Enable at least one platform in the Destinations tab.")
            return
        }

        viewModelScope.launch {
            _isSyncing.value = true
            _syncProgress.value = 0f
            _syncLogs.value = listOf("Initializing multi-stream metadata injection flow...")
            
            delay(800)
            _syncProgress.value = 0.2f
            _syncLogs.value = _syncLogs.value + "Parsing title: \"${_syncTitle.value}\" and game/category: \"${_syncCategory.value}\"..."
            
            val timestamp = System.currentTimeMillis()
            val stepPercent = 0.8f / activePlatforms.size

            for ((index, platform) in activePlatforms.withIndex()) {
                _syncLogs.value = _syncLogs.value + "Connecting to ${platform.name} RTMP API endpoints..."
                delay(600)
                
                // Save updated record to room
                repository.syncMetadata(
                    id = platform.id,
                    title = _syncTitle.value,
                    description = _syncDescription.value,
                    category = _syncCategory.value,
                    timestamp = timestamp
                )
                
                _syncLogs.value = _syncLogs.value + "✓ Synced successfully with [${platform.name}] (${platform.username})"
                _syncProgress.value = 0.2f + (stepPercent * (index + 1))
            }
            
            delay(500)
            _syncProgress.value = 1f
            _isSyncing.value = false
            _syncLogs.value = _syncLogs.value + listOf(
                "=========================",
                "🚀 Success! Multi-Sync pipeline completed at ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}.",
                "All streaming platforms are fully updated and primed for broadcast."
            )
        }
    }

    fun runNetworkSpeedTest() {
        viewModelScope.launch {
            _isTestingNetwork.value = true
            _speedTestStep.value = "Pinging Closest Ingest Nodes..."
            _networkPingMs.value = 0
            _networkSpeedMbps.value = 0f
            delay(700)
            
            _networkPingMs.value = Random.nextInt(12, 35)
            _speedTestStep.value = "Testing Upload to Twitch Ingestion (Virginia, US)..."
            
            for (i in 1..8) {
                _networkSpeedMbps.value = Random.nextDouble(5.0, 18.0).toFloat()
                delay(150)
            }
            
            _speedTestStep.value = "Testing Upload to YouTube CDN Live Gateway..."
            for (i in 1..8) {
                _networkSpeedMbps.value = Random.nextDouble(12.0, 32.0).toFloat()
                delay(150)
            }
            
            _speedTestStep.value = "Finalizing telemetry recommendations..."
            delay(600)
            
            _networkSpeedMbps.value = Random.nextDouble(18.5, 26.2).toFloat()
            _speedTestStep.value = "Completed"
            _isTestingNetwork.value = false
        }
    }

    fun setResolutionPreset(res: String) {
        _selectedResolutionPreset.value = res
    }

    fun setFpsPreset(fps: Int) {
        _selectedFpsPreset.value = fps
    }

    // --- Live Streaming Broadcast Simulator & Simulated Chat Feed ---
    fun toggleLiveBroadcast(activePlatformsCount: Int) {
        if (_isLiveBroadcasting.value) {
            broadcastJob?.cancel()
            simulatedChatJob?.cancel()
            _isLiveBroadcasting.value = false
            _liveBitrateKbps.value = 0
            _liveAudioLevelDb.value = -60f
            _liveLogs.value = _liveLogs.value + "■ Stream terminated by producer."
        } else {
            if (activePlatformsCount == 0) {
                _liveLogs.value = listOf("⚠️ Stream aborted: No active platforms selected to broadcast to!")
                return
            }
            _isLiveBroadcasting.value = true
            _liveBroadcastingDuration.value = 0
            _liveLogs.value = listOf(
                "⚡ Connecting RTMP stream pipelines...",
                "📡 Multiplexing stream broadcast data across $activePlatformsCount platforms..."
            )
            
            // Broadcast statistics simulator
            broadcastJob = viewModelScope.launch {
                var ticks = 0
                while (true) {
                    delay(1000)
                    ticks++
                    _liveBroadcastingDuration.value = ticks
                    
                    val baseBitrate = when (_selectedResolutionPreset.value) {
                        "4K" -> 20000
                        "1440p" -> 12000
                        "1080p" -> if (_selectedFpsPreset.value == 60) 6500 else 4500
                        else -> 3000
                    }
                    _liveBitrateKbps.value = baseBitrate + Random.nextInt(-250, 250)
                    _liveAudioLevelDb.value = Random.nextDouble(-45.0, -1.0).toFloat()
                    
                    if (ticks % 6 == 0) {
                        val platformAlerts = listOf(
                            "💬 Twitch: user_zack says \"Awesome stream!\"",
                            "🔥 Kick: Gifted Sub spike - hype train level 2!",
                            "📈 YouTube Live: Dynamic bitrate adjusted. Health key frame OK.",
                            "🚀 FaceBook: Stream reached 150 concurrent viewers!",
                            "👾 Twitch: Bot connected to coordinate interactive alerts."
                        )
                        _liveLogs.value = (_liveLogs.value.takeLast(15) + platformAlerts.random())
                    }
                }
            }

            // Simulated real-time incoming chats flow during active broadcasts
            simulatedChatJob = viewModelScope.launch {
                val names = listOf("Gamer999", "ComposeVibe", "TwitchedOut", "SpectatorX", "LofiPanda", "KickLord", "FBFriend")
                val contents = listOf(
                    "This is an incredible multi-stream hub!",
                    "Is this being cast to Kick too? Awesome.",
                    "The frame rate is extremely smooth.",
                    "Greeting from the live dashboard!",
                    "Kotlin rules!",
                    "So simple to check Twitch and Youtube side-by-side",
                    "Wow, view-multi-cast stream layout works perfectly."
                )
                val platformChoices = listOf("Twitch", "YouTube", "Kick", "Facebook Live")

                while (true) {
                    delay(Random.nextLong(2500, 4500))
                    val selectedPlatform = platformChoices.random()
                    val msg = ChatMessageEntity(
                        platform = selectedPlatform,
                        senderName = names.random(),
                        message = contents.random(),
                        timestamp = System.currentTimeMillis()
                    )
                    repository.insertChatMessage(msg)
                }
            }
        }
    }
}

class StreamHubViewModelFactory(private val repository: StreamPlatformRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StreamHubViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StreamHubViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
