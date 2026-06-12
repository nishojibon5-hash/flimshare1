package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class FilmshareViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val service = DoodStreamService.create()
    private val repository = FilmshareRepository(application, db, service)
    
    // Auth & Active User State
    val userState: StateFlow<UserEntity?> = repository.getActiveUserFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeChannelState: StateFlow<ChannelEntity?> = userState
        .flatMapLatest { user ->
            if (user == null) flowOf(null) else repository.getChannelFlow(user.uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    // Feeds & Selection
    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory

    val videoFeedState: StateFlow<List<VideoEntity>> = _selectedCategory
        .flatMapLatest { category ->
            repository.getVideosByCategoryFlow(category)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active playing video & interactive overlays
    private val _activeVideoId = MutableStateFlow<String?>(null)
    val activeVideoId: StateFlow<String?> = _activeVideoId

    val activeVideoState: StateFlow<VideoEntity?> = _activeVideoId
        .flatMapLatest { id ->
            if (id == null) flowOf(null) else repository.getVideoFlow(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val commentsState: StateFlow<List<CommentEntity>> = _activeVideoId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.getCommentsFlow(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isSubscribedState: StateFlow<Boolean> = combine(
        userState,
        activeVideoState
    ) { user, video ->
        if (user == null || video == null) false
        else {
            val sub = repository.isSubscribed(user.uid, video.channelId).firstOrNull()
            sub != null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Creator Specific Streams
    val creatorVideosState: StateFlow<List<VideoEntity>> = userState
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else repository.getVideosByCategoryFlow("All").map { list ->
                list.filter { it.channelId == user.uid }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Notifications List
    val notificationsState: StateFlow<List<NotificationEntity>> = repository.getNotificationsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Loading & Operation Events
    private val _uploadingStatus = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadingStatus: StateFlow<UploadState> = _uploadingStatus

    init {
        // Simulating Google Sign In immediately if no user is authenticated to fit "instantly log in" goal
        viewModelScope.launch {
            val user = repository.getActiveUser()
            if (user == null) {
                // Auto onboarding for direct demo availability
                repository.signInWithGoogleOneTap("demo.user@gmail.com", "Demo Creator")
            }
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    // --- Authentication ---
    fun signInGoogle(email: String, name: String) {
        viewModelScope.launch {
            repository.signInWithGoogleOneTap(email, name)
        }
    }

    fun signOutUser() {
        viewModelScope.launch {
            repository.signOut()
        }
    }

    // --- Channel Onboarding ---
    fun registerChannel(name: String, handle: String, profile: String, cover: String) {
        viewModelScope.launch {
            val user = userState.value
            if (user != null) {
                repository.createChannel(user.uid, name, handle, profile, cover)
            }
        }
    }

    // --- Media Controls ---
    fun selectActiveVideo(id: String?) {
        _activeVideoId.value = id
        if (id != null) {
            viewModelScope.launch {
                repository.incrementViews(id)
            }
        }
    }

    fun likeActiveVideo() {
        val id = _activeVideoId.value ?: return
        viewModelScope.launch {
            repository.likeVideo(id)
        }
    }

    fun dislikeActiveVideo() {
        val id = _activeVideoId.value ?: return
        viewModelScope.launch {
            repository.dislikeVideo(id)
        }
    }

    fun toggleSubscriptionActive() {
        val user = userState.value ?: return
        val video = activeVideoState.value ?: return
        viewModelScope.launch {
            repository.toggleSubscription(user.uid, video.channelId)
        }
    }

    fun postComment(message: String) {
        val id = _activeVideoId.value ?: return
        if (message.isBlank()) return
        viewModelScope.launch {
            repository.postComment(id, message)
        }
    }

    // --- Uploading Engine triggers ---
    fun triggerDirectUpload(
        apiKey: String,
        title: String,
        category: String,
        tags: String,
        videoFile: File,
        thumbnailFile: File?
    ) {
        viewModelScope.launch {
            _uploadingStatus.value = UploadState.Uploading(0)
            val result = repository.uploadVideoDirect(apiKey, title, category, tags, videoFile, thumbnailFile)
            if (result.isSuccess) {
                _uploadingStatus.value = UploadState.Success
            } else {
                _uploadingStatus.value = UploadState.Error(result.exceptionOrNull()?.message ?: "Upload failed")
            }
        }
    }

    fun triggerRemoteUpload(
        apiKey: String,
        title: String,
        category: String,
        tags: String,
        remoteUrl: String,
        thumbnailUrl: String
    ) {
        viewModelScope.launch {
            _uploadingStatus.value = UploadState.Uploading(50)
            val result = repository.uploadVideoRemote(apiKey, title, category, tags, remoteUrl, thumbnailUrl)
            if (result.isSuccess) {
                _uploadingStatus.value = UploadState.Success
            } else {
                _uploadingStatus.value = UploadState.Error(result.exceptionOrNull()?.message ?: "Remote clone failed")
            }
        }
    }

    fun resetUploadState() {
        _uploadingStatus.value = UploadState.Idle
    }
}

sealed class UploadState {
    object Idle : UploadState()
    data class Uploading(val progress: Int) : UploadState()
    object Success : UploadState()
    data class Error(val error: String) : UploadState()
}
