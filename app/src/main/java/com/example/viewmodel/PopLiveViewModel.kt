package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PopLiveRepository
import com.example.model.CoinTransaction
import com.example.model.GiftCatalogItem
import com.example.model.LiveStream
import com.example.model.StreamCategory
import com.example.model.StreamMessage
import com.example.model.UiState
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class PopLiveViewModel(
    private val repository: PopLiveRepository,
    val currentUserId: String,
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow<StreamCategory?>(null)
    val selectedCategory: StateFlow<StreamCategory?> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeStreamId = MutableStateFlow<String?>(null)

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    init {
        viewModelScope.launch {
            val authUser = Firebase.auth.currentUser
            repository.ensureUserProfile(
                userId = currentUserId,
                displayName = authUser?.displayName,
                avatarUrl = authUser?.photoUrl?.toString(),
            )
        }
    }

    val userProfileState: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId)
            .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = unwrapFirestoreError(error)
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing profile", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load profile"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading,
            )

    val liveStreamsState: StateFlow<UiState<List<LiveStream>>> =
        repository.observePublicLiveStreams()
            .map<List<LiveStream>, UiState<List<LiveStream>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = unwrapFirestoreError(error)
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing live streams", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load live rooms"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading,
            )

    val transactionsState: StateFlow<UiState<List<CoinTransaction>>> =
        repository.observeTransactions(currentUserId)
            .map<List<CoinTransaction>, UiState<List<CoinTransaction>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = unwrapFirestoreError(error)
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing transactions", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load wallet history"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading,
            )

    val activeStreamState: StateFlow<UiState<LiveStream?>> =
        _activeStreamId.flatMapLatest { streamId ->
            if (streamId.isNullOrBlank()) {
                flowOf(UiState.Success(null))
            } else {
                repository.observeStream(streamId)
                    .map<LiveStream?, UiState<LiveStream?>> { UiState.Success(it) }
                    .catch { error ->
                        val firestoreError = unwrapFirestoreError(error)
                        if (firestoreError == null && error is CancellationException) throw error
                        val rootError = firestoreError ?: error
                        Log.w(TAG, "Error observing active stream", rootError)
                        emit(UiState.Error(rootError.message ?: "Failed to load stream room"))
                    }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading,
        )

    val activeMessagesState: StateFlow<UiState<List<StreamMessage>>> =
        _activeStreamId.flatMapLatest { streamId ->
            if (streamId.isNullOrBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeStreamMessages(streamId)
                    .map<List<StreamMessage>, UiState<List<StreamMessage>>> { UiState.Success(it) }
                    .catch { error ->
                        val firestoreError = unwrapFirestoreError(error)
                        if (firestoreError == null && error is CancellationException) throw error
                        val rootError = firestoreError ?: error
                        Log.w(TAG, "Error observing stream messages", rootError)
                        emit(UiState.Error(rootError.message ?: "Failed to load live chat"))
                    }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading,
        )

    fun selectCategory(category: StreamCategory?) {
        _selectedCategory.value = category
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setActiveStream(streamId: String?) {
        _activeStreamId.value = streamId
    }

    fun launchDemoStreams() {
        val profile = (userProfileState.value as? UiState.Success)?.data
            ?: UserProfile(userId = currentUserId, displayName = "PopLive Star")
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.launchFeaturedDemoStreams(profile)
            _isBusy.value = false
            result.onSuccess {
                _events.tryEmit("4 live rooms are now streaming!")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Unable to start demo rooms")
            }
        }
    }

    fun startBroadcast(
        title: String,
        category: StreamCategory,
        pkActive: Boolean,
        pkOpponent: String,
        onStarted: (String) -> Unit,
    ) {
        val profile = (userProfileState.value as? UiState.Success)?.data
            ?: UserProfile(userId = currentUserId, displayName = "PopLive Star")
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.createLiveStream(
                profile = profile,
                title = title,
                category = category,
                pkActive = pkActive,
                pkOpponentName = pkOpponent,
                streamUrl = "",
                coverKey = "camera",
            )
            _isBusy.value = false
            result.onSuccess { streamId ->
                _activeStreamId.value = streamId
                onStarted(streamId)
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to go live")
            }
        }
    }

    fun endBroadcast(streamId: String, onEnded: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.endLiveStream(streamId)
            _isBusy.value = false
            result.onSuccess {
                onEnded()
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to end stream")
            }
        }
    }

    fun togglePk(stream: LiveStream, enablePk: Boolean) {
        viewModelScope.launch {
            repository.togglePkBattle(stream, enablePk)
                .onFailure { err ->
                    _events.tryEmit(err.localizedMessage ?: "Unable to update PK battle")
                }
        }
    }

    fun sendChatMessage(stream: LiveStream, body: String) {
        val profile = (userProfileState.value as? UiState.Success)?.data
            ?: UserProfile(userId = currentUserId, displayName = "Viewer")
        viewModelScope.launch {
            repository.sendChatMessage(stream, profile, body)
                .onFailure { err ->
                    _events.tryEmit(err.localizedMessage ?: "Message failed to send")
                }
        }
    }

    fun sendLike(stream: LiveStream) {
        viewModelScope.launch {
            repository.sendLike(stream)
        }
    }

    fun adjustViewerCount(stream: LiveStream, delta: Long) {
        viewModelScope.launch {
            repository.joinOrLeaveStream(stream, delta)
        }
    }

    fun sendGift(stream: LiveStream, gift: GiftCatalogItem, comboCount: Int = 1) {
        val profile = (userProfileState.value as? UiState.Success)?.data
        if (profile == null) {
            _events.tryEmit("Loading your wallet...")
            return
        }
        viewModelScope.launch {
            repository.sendGift(stream, profile, gift, comboCount)
                .onSuccess {
                    _events.tryEmit("Sent ${gift.name} x$comboCount!")
                }
                .onFailure { err ->
                    _events.tryEmit(err.localizedMessage ?: "Gift could not be sent")
                }
        }
    }

    fun claimDailyBonus() {
        val profile = (userProfileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            _isBusy.value = true
            repository.claimDailyBonus(profile)
                .onSuccess { _events.tryEmit("+500 PopCoins claimed!") }
                .onFailure { err -> _events.tryEmit(err.localizedMessage ?: "Claim failed") }
            _isBusy.value = false
        }
    }

    fun rechargeCoins(packTitle: String, coins: Long) {
        val profile = (userProfileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            _isBusy.value = true
            repository.rechargeCoins(profile, packTitle, coins)
                .onSuccess { _events.tryEmit("+$coins PopCoins added to wallet!") }
                .onFailure { err -> _events.tryEmit(err.localizedMessage ?: "Recharge failed") }
            _isBusy.value = false
        }
    }

    fun updateProfile(displayName: String, bio: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.updateProfile(currentUserId, displayName, bio)
                .onSuccess {
                    _events.tryEmit("Profile updated")
                    onDone()
                }
                .onFailure { err ->
                    _events.tryEmit(err.localizedMessage ?: "Update failed")
                }
            _isBusy.value = false
        }
    }

    private fun unwrapFirestoreError(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

    private companion object {
        const val TAG = "PopLiveVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
