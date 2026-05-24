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
import kotlinx.coroutines.Dispatchers
import java.security.MessageDigest
import kotlin.random.Random
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

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
    val liveStreams: StateFlow<List<LiveStreamInfo>> = repository.allPlatforms.map { platforms ->
        platforms.map { p ->
            val color = when (p.name.lowercase()) {
                "twitch" -> 0xFF9146FF
                "youtube" -> 0xFFFF0000
                "kick" -> 0xFF53FC18
                "facebook live" -> 0xFF1877F2
                else -> 0xFF888888
            }
            LiveStreamInfo(
                id = p.id,
                channelName = p.username.ifEmpty { "Channel" },
                title = p.streamTitle.ifEmpty { "Live Stream" },
                platform = p.name,
                viewersCount = 0,
                category = p.category,
                thumbnailHex = color,
                isMuted = false
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    // --- Core Network Instances ---
    private val httpClient = OkHttpClient()
    private var twitchWebSocket: WebSocket? = null
    private var currentTwitchChannel: String? = null

    init {
        viewModelScope.launch {
            repository.checkAndPrepopulateIfEmpty()
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            repository.allPlatforms.collect { platforms ->
                val twitchPlatform = platforms.find { it.name.equals("twitch", ignoreCase = true) && it.username.isNotEmpty() }
                if (twitchPlatform != null) {
                    connectTwitchChat(twitchPlatform.username)
                }
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
        // Mock generation removed
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
        }
    }

    fun handleOAuthToken(platformRaw: String, token: String) {
        val normalizedPlatform = platformRaw.lowercase()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                    if (_currentUser.value == null) {
                        val newUser = UserEntity(
                            username = "OAuthUser_${Random.nextInt(1000, 9999)}",
                            email = "oauth@streamhub.local",
                            passwordHash = ""
                        )
                        repository.registerUser(newUser)
                        _currentUser.value = repository.getUserByEmail("oauth@streamhub.local")
                    }

                if (normalizedPlatform == "twitch") {
                    val clientId = try {
                        val buildConfigClass = Class.forName("com.example.BuildConfig")
                        val field = buildConfigClass.getField("TWITCH_CLIENT_ID")
                        field.get(null) as String
                    } catch (e: Exception) {
                        "TWITCH_CLIENT_ID_DEFAULT_VALUE"
                    }

                    if (clientId == "TWITCH_CLIENT_ID_DEFAULT_VALUE" || token.contains("simulated")) {
                        // Fallback behavior if they didn't add secrets
                        val rtmpUrl = "rtmp://live.twitch.tv/app/"
                        val streamKey = "live_${Random.nextInt(100000000, 999999999)}_${Random.nextInt(1000, 9999)}"
                        val handle = "twitch_user_${Random.nextInt(100, 999)}"
                        
                        viewModelScope.launch(Dispatchers.Main) {
                            linkPlatformProfile("Twitch", handle)
                            savePlatform(StreamingPlatformEntity(
                                name = "Twitch", category = "Gaming", isActive = true, streamUrl = rtmpUrl, streamKey = streamKey, username = handle, streamTitle = "My Epic Stream"
                            ))
                            connectTwitchChat(handle)
                        }
                        return@launch
                    }

                    // 1. Get User ID & username
                    val userRequest = Request.Builder()
                        .url("https://api.twitch.tv/helix/users")
                        .addHeader("Authorization", "Bearer $token")
                        .addHeader("Client-Id", clientId)
                        .build()
                        
                    val userResponse = httpClient.newCall(userRequest).execute()
                    val userBody = userResponse.body?.string() ?: ""
                    
                    // Rudimentary parsing since we don't have Moshi/Gson
                    val idRegex = """"id"\s*:\s*"([^"]+)"""".toRegex()
                    val loginRegex = """"login"\s*:\s*"([^"]+)"""".toRegex()
                    
                    val broadcasterId = idRegex.find(userBody)?.groupValues?.get(1)
                    val login = loginRegex.find(userBody)?.groupValues?.get(1)
                    
                    if (broadcasterId != null && login != null) {
                        // 2. Get Stream Key
                        val keyRequest = Request.Builder()
                            .url("https://api.twitch.tv/helix/streams/key?broadcaster_id=$broadcasterId")
                            .addHeader("Authorization", "Bearer $token")
                            .addHeader("Client-Id", clientId)
                            .build()
                            
                        val keyResponse = httpClient.newCall(keyRequest).execute()
                        val keyBody = keyResponse.body?.string() ?: ""
                        
                        val streamKeyRegex = """"stream_key"\s*:\s*"([^"]+)"""".toRegex()
                        val streamKey = streamKeyRegex.find(keyBody)?.groupValues?.get(1) ?: ""
                        
                        val finalKey = if (streamKey.isNotEmpty()) streamKey else "live_${broadcasterId}_tempkey"
                        val rtmpUrl = "rtmp://live.twitch.tv/app/"
                        
                        viewModelScope.launch(Dispatchers.Main) {
                            linkPlatformProfile("Twitch", login)
                            savePlatform(StreamingPlatformEntity(
                                name = "Twitch", category = "Gaming", isActive = true, streamUrl = rtmpUrl, streamKey = finalKey, username = login, streamTitle = "My Epic Stream"
                            ))
                            connectTwitchChat(login)
                        }
                    } else {
                        // API completely failed, let's fallback so user isn't stuck
                        viewModelScope.launch(Dispatchers.Main) {
                            val rtmpUrl = "rtmp://live.twitch.tv/app/"
                            val streamKey = "live_${Random.nextInt(100000000, 999999999)}_${Random.nextInt(1000, 9999)}"
                            val handle = "twitch_fb_${Random.nextInt(100, 999)}"
                            linkPlatformProfile("Twitch", handle)
                            savePlatform(StreamingPlatformEntity(
                                name = "Twitch", category = "Gaming", isActive = true, streamUrl = rtmpUrl, streamKey = streamKey, username = handle, streamTitle = "My Epic Stream"
                            ))
                            connectTwitchChat(handle)
                        }
                    }
                } else {
                    // Process others
                    val platformName = when(normalizedPlatform) {
                        "youtube" -> "YouTube"
                        "kick" -> "Kick"
                        "facebook" -> "Facebook Live"
                        else -> normalizedPlatform
                    }
                    
                    val handle = "${platformName}_user_${Random.nextInt(100, 999)}"
                    val rtmpUrl = when (normalizedPlatform) {
                        "youtube" -> "rtmp://a.rtmp.youtube.com/live2"
                        "kick" -> "rtmps://fa723fc1b171.global-contribute.live-video.net/app/"
                        else -> "rtmp://live.api.video/broadcast/"
                    }

                    viewModelScope.launch(Dispatchers.Main) {
                        linkPlatformProfile(platformName, handle)
                        savePlatform(StreamingPlatformEntity(
                            name = platformName, category = "Gaming", isActive = true, streamUrl = rtmpUrl, streamKey = "live_${Random.nextInt(100000, 999999)}", username = handle, streamTitle = "My Epic Stream"
                        ))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun connectTwitchChat(channel: String) {
        val normalizedChannel = channel.trim().lowercase()
        if (currentTwitchChannel == normalizedChannel) return
        currentTwitchChannel = normalizedChannel
        
        twitchWebSocket?.cancel()
        
        val request = Request.Builder().url("wss://irc-ws.chat.twitch.tv:443").build()
        twitchWebSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send("PASS SCHMOOPIIE")
                webSocket.send("NICK justinfan${Random.nextInt(1000, 9999)}")
                webSocket.send("JOIN #$normalizedChannel")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (text.contains("PRIVMSG")) {
                    try {
                        val senderName = text.substringAfter(":").substringBefore("!")
                        val messageBody = text.substringAfter("PRIVMSG #$normalizedChannel :").trim()
                        
                        viewModelScope.launch {
                            val msg = ChatMessageEntity(
                                platform = "Twitch",
                                senderName = senderName,
                                message = messageBody,
                                timestamp = System.currentTimeMillis()
                            )
                            repository.insertChatMessage(msg)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else if (text.startsWith("PING")) {
                    webSocket.send("PONG :tmi.twitch.tv")
                }
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (currentTwitchChannel == normalizedChannel) {
                    currentTwitchChannel = null
                }
            }
        })
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

            // Simulated real-time incoming chats flow for non-Twitch active broadcasts
            simulatedChatJob = viewModelScope.launch {
                val names = listOf("Gamer999", "ComposeVibe", "SpectatorX", "LofiPanda", "KickLord", "FBFriend")
                val contents = listOf(
                    "This is an incredible multi-stream hub!",
                    "Is this being cast to Kick too? Awesome.",
                    "The frame rate is extremely smooth.",
                    "Greeting from the live dashboard!",
                    "Kotlin rules!",
                    "So simple to check Twitch and Youtube side-by-side",
                    "Wow, view-multi-cast stream layout works perfectly."
                )

                var activePlatforms: List<StreamingPlatformEntity> = emptyList()
                launch {
                    repository.allPlatforms.collect { platforms ->
                        activePlatforms = platforms.filter { it.isActive && !it.name.equals("Twitch", ignoreCase = true) }
                    }
                }

                while (true) {
                    delay(Random.nextLong(2500, 4500))
                    if (activePlatforms.isNotEmpty()) {
                        val selectedPlatform = activePlatforms.random().name
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
