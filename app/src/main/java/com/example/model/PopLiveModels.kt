package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class StreamCategory(val wireValue: String, val label: String) {
    MUSIC("MUSIC", "Music"),
    DANCE("DANCE", "Dance"),
    GAMING("GAMING", "Gaming"),
    TALK("TALK", "Talk"),
    PK("PK", "PK Battle");

    companion object {
        fun fromWire(raw: String?): StreamCategory =
            entries.firstOrNull { it.wireValue == raw } ?: MUSIC
    }
}

enum class StreamVisibility(val wireValue: String) {
    PUBLIC("PUBLIC"),
    PRIVATE("PRIVATE");

    companion object {
        fun fromWire(raw: String?): StreamVisibility =
            entries.firstOrNull { it.wireValue == raw } ?: PUBLIC
    }
}

enum class StreamStatus(val wireValue: String) {
    LIVE("LIVE"),
    ENDED("ENDED");

    companion object {
        fun fromWire(raw: String?): StreamStatus =
            entries.firstOrNull { it.wireValue == raw } ?: LIVE
    }
}

enum class MessageType(val wireValue: String) {
    CHAT("CHAT"),
    GIFT("GIFT"),
    JOIN("JOIN"),
    SYSTEM("SYSTEM");

    companion object {
        fun fromWire(raw: String?): MessageType =
            entries.firstOrNull { it.wireValue == raw } ?: CHAT
    }
}

enum class TxType(val wireValue: String) {
    RECHARGE("RECHARGE"),
    GIFT_SENT("GIFT_SENT"),
    GIFT_RECEIVED("GIFT_RECEIVED"),
    DAILY_BONUS("DAILY_BONUS");

    companion object {
        fun fromWire(raw: String?): TxType =
            entries.firstOrNull { it.wireValue == raw } ?: RECHARGE
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "PopLive Star",
    val bio: String = "Streaming live vibes on PopLive",
    val avatarUrl: String? = null,
    val coinBalance: Long = 2500L,
    val diamondsEarned: Long = 0L,
    val level: Long = 1L,
    val followingCount: Long = 0L,
    val followersCount: Long = 0L,
    val dailyCheckInDay: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class CoinTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val txType: String = TxType.DAILY_BONUS.wireValue,
    val title: String = "",
    val coinDelta: Long = 0L,
    val streamId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class LiveStream(
    @DocumentId val id: String = "",
    val hostId: String = "",
    val hostName: String = "",
    val hostAvatarUrl: String? = null,
    val hostLevel: Long = 1L,
    val title: String = "",
    val category: String = StreamCategory.MUSIC.wireValue,
    val coverKey: String = "acoustic",
    val streamUrl: String = "",
    val visibility: String = StreamVisibility.PUBLIC.wireValue,
    val status: String = StreamStatus.LIVE.wireValue,
    val pkActive: Boolean = false,
    val pkOpponentName: String? = null,
    val pkHostScore: Long = 0L,
    val pkOpponentScore: Long = 0L,
    val viewerCount: Long = 0L,
    val totalGiftsCoins: Long = 0L,
    val likeCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun categoryEnum(): StreamCategory = StreamCategory.fromWire(category)
    fun statusEnum(): StreamStatus = StreamStatus.fromWire(status)
}

@Keep
@IgnoreExtraProperties
data class StreamMessage(
    @DocumentId val id: String = "",
    val streamId: String = "",
    val streamVisibility: String = StreamVisibility.PUBLIC.wireValue,
    val hostId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderLevel: Long = 1L,
    val messageType: String = MessageType.CHAT.wireValue,
    val body: String = "",
    val giftName: String? = null,
    val giftCoins: Long? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
) {
    fun messageTypeEnum(): MessageType = MessageType.fromWire(messageType)
}

data class GiftCatalogItem(
    val id: String,
    val name: String,
    val coinCost: Long,
    val badgeTag: String,
    val accentHex: Long,
)

object PopLiveGifts {
    val catalog: List<GiftCatalogItem> = listOf(
        GiftCatalogItem("rose", "Velvet Rose", 10L, "Popular", 0xFFFF2E7E),
        GiftCatalogItem("heart", "Neon Heart", 50L, "Combo", 0xFFFF6B9D),
        GiftCatalogItem("mic", "Gold Mic", 99L, "Stage", 0xFFFFD166),
        GiftCatalogItem("crown", "Royal Crown", 199L, "VIP", 0xFFFFB703),
        GiftCatalogItem("car", "Cyber Racer", 499L, "FX", 0xFF00E5FF),
        GiftCatalogItem("rocket", "Galaxy Rocket", 999L, "Mega", 0xFF9D4EDD),
    )
}
