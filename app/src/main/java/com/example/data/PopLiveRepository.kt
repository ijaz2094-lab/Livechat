package com.example.data

import android.content.Context
import com.example.R
import com.example.model.CoinTransaction
import com.example.model.GiftCatalogItem
import com.example.model.LiveStream
import com.example.model.MessageType
import com.example.model.StreamCategory
import com.example.model.StreamMessage
import com.example.model.StreamStatus
import com.example.model.StreamVisibility
import com.example.model.TxType
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class PopLiveRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun unwrapFirestoreException(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Refusing cross-user profile observation." }
        val docRef = db.collection("users").document(userId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val fsError = unwrapFirestoreException(error)
                    if (fsError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsError ?: error, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    fun observeTransactions(userId: String): Flow<List<CoinTransaction>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Refusing cross-user transaction observation." }
        val path = "users/$userId/transactions"
        emitAll(
            db.collection("users")
                .document(userId)
                .collection("transactions")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(40)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CoinTransaction::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val fsError = unwrapFirestoreException(error)
                    if (fsError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsError ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observePublicLiveStreams(): Flow<List<LiveStream>> = flow {
        val uid = requireUserId()
        val path = "live_streams"
        emitAll(
            db.collection(path)
                .whereEqualTo("visibility", StreamVisibility.PUBLIC.wireValue)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        LiveStream::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<LiveStream> { it.status == StreamStatus.LIVE.wireValue }
                            .thenByDescending { it.viewerCount }
                            .thenByDescending { it.createdAt?.seconds ?: 0L }
                    )
                }
                .catch { error ->
                    val fsError = unwrapFirestoreException(error)
                    if (fsError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsError ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeStream(streamId: String): Flow<LiveStream?> = flow {
        val uid = requireUserId()
        val docRef = db.collection("live_streams").document(streamId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            LiveStream::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val fsError = unwrapFirestoreException(error)
                    if (fsError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsError ?: error, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    fun observeStreamMessages(streamId: String): Flow<List<StreamMessage>> = flow {
        val uid = requireUserId()
        val path = "live_streams/$streamId/messages"
        emitAll(
            db.collection("live_streams")
                .document(streamId)
                .collection("messages")
                .whereEqualTo("streamVisibility", StreamVisibility.PUBLIC.wireValue)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        StreamMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt?.toDate()?.time ?: 0L }
                        .takeLast(60)
                }
                .catch { error ->
                    val fsError = unwrapFirestoreException(error)
                    if (fsError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsError ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(
        userId: String,
        displayName: String?,
        avatarUrl: String?,
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val safeName = (displayName?.trim().takeIf { !it.isNullOrEmpty() } ?: "PopLive Star")
                    .take(40)
                val safeAvatar = avatarUrl?.takeIf { it.isNotBlank() }?.take(500)
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "bio" to "Streaming live vibes on PopLive",
                    "avatarUrl" to safeAvatar,
                    "coinBalance" to 2500L,
                    "diamondsEarned" to 0L,
                    "level" to 5L,
                    "followingCount" to 0L,
                    "followersCount" to 0L,
                    "dailyCheckInDay" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ).filterValues { it != null }
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        userId: String,
        displayName: String,
        bio: String,
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val cleanName = displayName.trim().take(40)
            val cleanBio = bio.trim().take(160)
            require(cleanName.isNotEmpty()) { "Display name cannot be empty." }
            db.collection("users").document(uid).update(
                mapOf(
                    "displayName" to cleanName,
                    "bio" to cleanBio,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun createLiveStream(
        profile: UserProfile,
        title: String,
        category: StreamCategory,
        pkActive: Boolean,
        pkOpponentName: String?,
        streamUrl: String = "",
        coverKey: String = "camera",
    ): Result<String> {
        var docPath = "live_streams"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().ifEmpty { "${profile.displayName}'s Live Stage" }.take(80)
            val cleanHostName = profile.displayName.trim().ifEmpty { "PopLive Star" }.take(40)
            val cleanOpponent = pkOpponentName?.trim()?.takeIf { it.isNotEmpty() }?.take(40)
            val streamRef = db.collection("live_streams").document()
            docPath = streamRef.path
            val msgRef = streamRef.collection("messages").document()

            val batch = db.batch()
            val streamPayload = mapOf(
                "hostId" to uid,
                "hostName" to cleanHostName,
                "hostAvatarUrl" to profile.avatarUrl?.takeIf { it.isNotBlank() }?.take(500),
                "hostLevel" to profile.level.coerceIn(1L, 999L),
                "title" to cleanTitle,
                "category" to category.wireValue,
                "coverKey" to coverKey,
                "streamUrl" to streamUrl.take(500),
                "visibility" to StreamVisibility.PUBLIC.wireValue,
                "status" to StreamStatus.LIVE.wireValue,
                "pkActive" to pkActive,
                "pkOpponentName" to if (pkActive) (cleanOpponent ?: "Challenger Team") else null,
                "pkHostScore" to 0L,
                "pkOpponentScore" to 0L,
                "viewerCount" to 1L,
                "totalGiftsCoins" to 0L,
                "likeCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            ).filterValues { it != null }

            val welcomeMsg = mapOf(
                "streamId" to streamRef.id,
                "streamVisibility" to StreamVisibility.PUBLIC.wireValue,
                "hostId" to uid,
                "senderId" to uid,
                "senderName" to cleanHostName,
                "senderLevel" to profile.level.coerceIn(1L, 999L),
                "messageType" to MessageType.SYSTEM.wireValue,
                "body" to "Welcome to $cleanTitle! Tap the heart or send a gift.",
                "createdAt" to FieldValue.serverTimestamp(),
            )

            batch.set(streamRef, streamPayload)
            batch.set(msgRef, welcomeMsg)
            batch.commit().await()
            Result.success(streamRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun launchFeaturedDemoStreams(profile: UserProfile): Result<Unit> {
        return try {
            val uid = requireUserId()
            val hostName = profile.displayName.trim().ifEmpty { "PopLive Host" }.take(40)
            val seeds = listOf(
                DemoSeed(
                    title = "Midnight Acoustic & Requests",
                    hostAlias = "Aria Acoustic",
                    hostLevel = 24L,
                    category = StreamCategory.MUSIC,
                    coverKey = "acoustic",
                    streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                    pkActive = true,
                    pkOpponent = "DJ Nova",
                    pkHostScore = 1480L,
                    pkOpponentScore = 1220L,
                    viewerCount = 342L,
                    likeCount = 1290L,
                    totalGifts = 2450L,
                ),
                DemoSeed(
                    title = "Neon Street Choreography",
                    hostAlias = "Kairo Moves",
                    hostLevel = 19L,
                    category = StreamCategory.DANCE,
                    coverKey = "dance",
                    streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                    pkActive = false,
                    pkOpponent = null,
                    pkHostScore = 0L,
                    pkOpponentScore = 0L,
                    viewerCount = 218L,
                    likeCount = 860L,
                    totalGifts = 980L,
                ),
                DemoSeed(
                    title = "Ranked Arena Duo Stream",
                    hostAlias = "Mina Plays",
                    hostLevel = 31L,
                    category = StreamCategory.GAMING,
                    coverKey = "gaming",
                    streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                    pkActive = true,
                    pkOpponent = "Apex Squad",
                    pkHostScore = 3100L,
                    pkOpponentScore = 2740L,
                    viewerCount = 514L,
                    likeCount = 2410L,
                    totalGifts = 5200L,
                ),
                DemoSeed(
                    title = "Late Night Lounge Talk",
                    hostAlias = hostName,
                    hostLevel = profile.level.coerceIn(1L, 999L),
                    category = StreamCategory.TALK,
                    coverKey = "talkshow",
                    streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoy.mp4",
                    pkActive = false,
                    pkOpponent = null,
                    pkHostScore = 0L,
                    pkOpponentScore = 0L,
                    viewerCount = 129L,
                    likeCount = 430L,
                    totalGifts = 620L,
                ),
            )

            val batch = db.batch()
            seeds.forEach { seed ->
                val streamRef = db.collection("live_streams").document()
                val streamPayload = mapOf(
                    "hostId" to uid,
                    "hostName" to seed.hostAlias.take(40),
                    "hostLevel" to seed.hostLevel,
                    "title" to seed.title,
                    "category" to seed.category.wireValue,
                    "coverKey" to seed.coverKey,
                    "streamUrl" to seed.streamUrl,
                    "visibility" to StreamVisibility.PUBLIC.wireValue,
                    "status" to StreamStatus.LIVE.wireValue,
                    "pkActive" to seed.pkActive,
                    "pkOpponentName" to seed.pkOpponent,
                    "pkHostScore" to seed.pkHostScore,
                    "pkOpponentScore" to seed.pkOpponentScore,
                    "viewerCount" to seed.viewerCount,
                    "totalGiftsCoins" to seed.totalGifts,
                    "likeCount" to seed.likeCount,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ).filterValues { it != null }
                batch.set(streamRef, streamPayload)

                val msgRef = streamRef.collection("messages").document()
                batch.set(
                    msgRef,
                    mapOf(
                        "streamId" to streamRef.id,
                        "streamVisibility" to StreamVisibility.PUBLIC.wireValue,
                        "hostId" to uid,
                        "senderId" to uid,
                        "senderName" to seed.hostAlias.take(40),
                        "senderLevel" to seed.hostLevel,
                        "messageType" to MessageType.SYSTEM.wireValue,
                        "body" to "Room is LIVE! Say hello in chat or launch a gift combo.",
                        "createdAt" to FieldValue.serverTimestamp(),
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "live_streams")
            Result.failure(e)
        }
    }

    suspend fun endLiveStream(streamId: String): Result<Unit> {
        val path = "live_streams/$streamId"
        return try {
            requireUserId()
            db.collection("live_streams").document(streamId).update(
                mapOf(
                    "status" to StreamStatus.ENDED.wireValue,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun togglePkBattle(
        stream: LiveStream,
        enablePk: Boolean,
        opponentName: String = "Challenger Star",
    ): Result<Unit> {
        val path = "live_streams/${stream.id}"
        return try {
            val uid = requireUserId()
            check(stream.hostId == uid) { "Only host can toggle PK mode." }
            if (stream.status == StreamStatus.ENDED.wireValue) {
                return Result.failure(IllegalStateException("Stream has ended."))
            }
            db.collection("live_streams").document(stream.id).update(
                mapOf(
                    "pkActive" to enablePk,
                    "pkOpponentName" to opponentName.take(40),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun sendChatMessage(
        stream: LiveStream,
        profile: UserProfile,
        body: String,
    ): Result<Unit> {
        val cleanBody = body.trim().take(200)
        if (cleanBody.isEmpty()) return Result.success(Unit)
        val path = "live_streams/${stream.id}/messages"
        return try {
            val uid = requireUserId()
            val msgRef = db.collection("live_streams")
                .document(stream.id)
                .collection("messages")
                .document()
            msgRef.set(
                mapOf(
                    "streamId" to stream.id,
                    "streamVisibility" to stream.visibility,
                    "hostId" to stream.hostId,
                    "senderId" to uid,
                    "senderName" to profile.displayName.trim().ifEmpty { "Viewer" }.take(40),
                    "senderLevel" to profile.level.coerceIn(1L, 999L),
                    "messageType" to MessageType.CHAT.wireValue,
                    "body" to cleanBody,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun sendLike(stream: LiveStream): Result<Unit> {
        if (stream.status == StreamStatus.ENDED.wireValue) return Result.success(Unit)
        val path = "live_streams/${stream.id}"
        return try {
            requireUserId()
            db.collection("live_streams").document(stream.id).update(
                mapOf(
                    "likeCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun joinOrLeaveStream(stream: LiveStream, delta: Long): Result<Unit> {
        if (stream.status == StreamStatus.ENDED.wireValue) return Result.success(Unit)
        val path = "live_streams/${stream.id}"
        return try {
            requireUserId()
            val nextViewerCount = (stream.viewerCount + delta).coerceAtLeast(0L)
            db.collection("live_streams").document(stream.id).update(
                mapOf(
                    "viewerCount" to nextViewerCount,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun sendGift(
        stream: LiveStream,
        profile: UserProfile,
        gift: GiftCatalogItem,
        comboCount: Int = 1,
    ): Result<Unit> {
        val totalCost = gift.coinCost * comboCount.coerceAtLeast(1)
        if (stream.status == StreamStatus.ENDED.wireValue) {
            return Result.failure(IllegalStateException("Broadcast has already ended."))
        }
        if (profile.coinBalance < totalCost) {
            return Result.failure(IllegalStateException("Not enough PopCoins. Claim daily bonus or top up in Wallet!"))
        }
        val path = "live_streams/${stream.id}"
        return try {
            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            val txRef = userRef.collection("transactions").document()
            val streamRef = db.collection("live_streams").document(stream.id)
            val msgRef = streamRef.collection("messages").document()

            val batch = db.batch()
            val userUpdates = mutableMapOf<String, Any>(
                "coinBalance" to FieldValue.increment(-totalCost),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            if (stream.hostId == uid) {
                userUpdates["diamondsEarned"] = FieldValue.increment(totalCost)
            }
            batch.update(userRef, userUpdates)

            batch.set(
                txRef,
                mapOf(
                    "userId" to uid,
                    "txType" to TxType.GIFT_SENT.wireValue,
                    "title" to "Sent ${gift.name} x$comboCount".take(80),
                    "coinDelta" to -totalCost,
                    "streamId" to stream.id,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            )

            val streamUpdates = mutableMapOf<String, Any>(
                "totalGiftsCoins" to FieldValue.increment(totalCost),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            if (stream.pkActive) {
                streamUpdates["pkHostScore"] = FieldValue.increment(totalCost)
            }
            batch.update(streamRef, streamUpdates)

            batch.set(
                msgRef,
                mapOf(
                    "streamId" to stream.id,
                    "streamVisibility" to stream.visibility,
                    "hostId" to stream.hostId,
                    "senderId" to uid,
                    "senderName" to profile.displayName.trim().ifEmpty { "Gifter" }.take(40),
                    "senderLevel" to profile.level.coerceIn(1L, 999L),
                    "messageType" to MessageType.GIFT.wireValue,
                    "body" to "Sent ${gift.name} x$comboCount!",
                    "giftName" to gift.name,
                    "giftCoins" to totalCost,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            )

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun claimDailyBonus(profile: UserProfile): Result<Unit> {
        val path = "users/${profile.userId}"
        return try {
            val uid = requireUserId()
            check(uid == profile.userId) { "Signed-in user mismatch." }
            val userRef = db.collection("users").document(uid)
            val txRef = userRef.collection("transactions").document()
            val bonusCoins = 500L
            val nextDay = (profile.dailyCheckInDay + 1L).coerceAtMost(3650L)

            val batch = db.batch()
            batch.update(
                userRef,
                mapOf(
                    "coinBalance" to FieldValue.increment(bonusCoins),
                    "dailyCheckInDay" to nextDay,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.set(
                txRef,
                mapOf(
                    "userId" to uid,
                    "txType" to TxType.DAILY_BONUS.wireValue,
                    "title" to "Daily Check-in Day $nextDay",
                    "coinDelta" to bonusCoins,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun rechargeCoins(
        profile: UserProfile,
        packTitle: String,
        coins: Long,
    ): Result<Unit> {
        val path = "users/${profile.userId}"
        return try {
            val uid = requireUserId()
            check(uid == profile.userId) { "Signed-in user mismatch." }
            val userRef = db.collection("users").document(uid)
            val txRef = userRef.collection("transactions").document()

            val batch = db.batch()
            batch.update(
                userRef,
                mapOf(
                    "coinBalance" to FieldValue.increment(coins),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.set(
                txRef,
                mapOf(
                    "userId" to uid,
                    "txType" to TxType.RECHARGE.wireValue,
                    "title" to packTitle.take(80),
                    "coinDelta" to coins,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private data class DemoSeed(
        val title: String,
        val hostAlias: String,
        val hostLevel: Long,
        val category: StreamCategory,
        val coverKey: String,
        val streamUrl: String,
        val pkActive: Boolean,
        val pkOpponent: String?,
        val pkHostScore: Long,
        val pkOpponentScore: Long,
        val viewerCount: Long,
        val likeCount: Long,
        val totalGifts: Long,
    )
}
