package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.model.FollowedHost
import com.example.model.GiftCatalogItem
import com.example.model.LiveStream
import com.example.model.MessageType
import com.example.model.PartySeat
import com.example.model.StreamMessage
import com.example.model.StreamStatus
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.ui.components.FloatingGiftBanner
import com.example.ui.components.FloatingHeartsOverlay
import com.example.ui.components.GiftPickerBottomSheet
import com.example.ui.components.LiveChatFeed
import com.example.ui.components.MegaGiftStageOverlay
import com.example.ui.components.PartySeatsGrid
import com.example.ui.components.PkBattleProgressBar
import com.example.ui.components.QuickReactionBar
import com.example.ui.theme.CoralPink
import com.example.ui.theme.CosmicObsidian
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.WarmGold
import com.example.ui.video.CameraBroadcasterPreview
import com.example.ui.video.LiveVideoPlayer
import com.example.ui.video.StudioFilter
import kotlinx.coroutines.delay

@Composable
fun LiveRoomScreen(
    currentUserId: String,
    profileState: UiState<UserProfile?>,
    streamState: UiState<LiveStream?>,
    messagesState: UiState<List<StreamMessage>>,
    seatsState: UiState<List<PartySeat>>,
    followingState: UiState<List<FollowedHost>>,
    onSendMessage: (LiveStream, String) -> Unit,
    onSendLike: (LiveStream) -> Unit,
    onSendGift: (LiveStream, GiftCatalogItem, Int) -> Unit,
    onTogglePk: (LiveStream, Boolean) -> Unit,
    onTogglePartyMode: (LiveStream, Boolean) -> Unit,
    onToggleFollow: (hostId: String, hostName: String, hostLevel: Long) -> Unit,
    onClaimSeat: (LiveStream, Long) -> Unit,
    onToggleSeatMute: (String, PartySeat) -> Unit,
    onLeaveSeat: (String, PartySeat) -> Unit,
    onEndBroadcast: (String, () -> Unit) -> Unit,
    onAdjustViewer: (LiveStream, Long) -> Unit,
    onGoToWallet: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    var isMuted by remember { mutableStateOf(false) }
    var useFrontCamera by remember { mutableStateOf(true) }
    var showGiftSheet by remember { mutableStateOf(false) }
    var showEndSummary by remember { mutableStateOf<LiveStream?>(null) }
    var chatDraft by remember { mutableStateOf("") }
    var heartTrigger by remember { mutableIntStateOf(0) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }

    val stream = (streamState as? UiState.Success)?.data
    val messages = (messagesState as? UiState.Success)?.data ?: emptyList()
    val seats = (seatsState as? UiState.Success)?.data ?: emptyList()
    val followed = (followingState as? UiState.Success)?.data ?: emptyList()
    val profile = (profileState as? UiState.Success)?.data
    val latestGiftMessage = remember(messages) {
        messages.lastOrNull { it.messageTypeEnum() == MessageType.GIFT }
    }
    val isFollowingHost = remember(followed, stream?.hostId) {
        stream != null && followed.any { it.hostId == stream.hostId }
    }

    LaunchedEffect(stream?.id, stream?.status) {
        if (stream?.statusEnum() == StreamStatus.LIVE) {
            while (true) {
                delay(1000L)
                elapsedSeconds += 1
            }
        }
    }

    val hasCameraPermission = remember(context) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
    }

    DisposableEffect(stream?.id) {
        val active = stream
        if (active != null && active.hostId != currentUserId && active.statusEnum() == StreamStatus.LIVE) {
            onAdjustViewer(active, 1L)
        }
        onDispose {
            if (active != null && active.hostId != currentUserId && active.statusEnum() == StreamStatus.LIVE) {
                onAdjustViewer(active, -1L)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicObsidian),
    ) {
        if (streamState is UiState.Loading) {
            CircularProgressIndicator(
                color = CoralPink,
                modifier = Modifier.align(Alignment.Center),
            )
        } else if (stream == null) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Stream room unavailable", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onBack) { Text("Back to Discover") }
            }
        } else {
            val isLocalHostCamera = stream.hostId == currentUserId && stream.streamUrl.isBlank() && hasCameraPermission

            if (isLocalHostCamera) {
                CameraBroadcasterPreview(
                    useFrontCamera = useFrontCamera,
                    torchEnabled = false,
                    studioFilter = StudioFilter.NATURAL,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LiveVideoPlayer(
                    streamUrl = stream.streamUrl,
                    coverKey = stream.coverKey,
                    isMuted = isMuted,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0x880B0714),
                                Color.Transparent,
                                Color.Transparent,
                                Color(0xD40B0714),
                            )
                        )
                    )
            )

            FloatingHeartsOverlay(triggerCount = heartTrigger)
            MegaGiftStageOverlay(giftMessage = latestGiftMessage)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Top HUD
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = CosmicObsidian.copy(alpha = 0.72f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                            modifier = Modifier.weight(1f, fill = false),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = CoralPink,
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = stream.hostName.take(1).uppercase(),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White,
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Text(
                                        text = stream.hostName,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    val mins = elapsedSeconds / 60
                                    val secs = elapsedSeconds % 60
                                    Text(
                                        text = "Lv.${stream.hostLevel} • ${"%02d:%02d".format(mins, secs)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = WarmGold,
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = if (isFollowingHost) Color.White.copy(alpha = 0.16f) else CoralPink,
                                    modifier = Modifier
                                        .clickable {
                                            onToggleFollow(stream.hostId, stream.hostName, stream.hostLevel)
                                        }
                                        .testTag("room_follow_button"),
                                ) {
                                    Text(
                                        text = if (isFollowingHost) "Following" else "+ Follow",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = CosmicObsidian.copy(alpha = 0.72f),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Visibility,
                                        contentDescription = "Viewers",
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${stream.viewerCount}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White,
                                    )
                                }
                            }

                            if (stream.hostId == currentUserId && stream.statusEnum() == StreamStatus.LIVE) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (stream.pkActive) WarmGold else CosmicObsidian.copy(alpha = 0.72f),
                                    onClick = { onTogglePk(stream, !stream.pkActive) },
                                    modifier = Modifier.testTag("room_toggle_pk_button"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Bolt,
                                        contentDescription = "Toggle PK Battle",
                                        tint = if (stream.pkActive) CosmicObsidian else WarmGold,
                                        modifier = Modifier.padding(8.dp).size(18.dp),
                                    )
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = if (stream.partyMode) ElectricCyan else CosmicObsidian.copy(alpha = 0.72f),
                                    onClick = { onTogglePartyMode(stream, !stream.partyMode) },
                                    modifier = Modifier.testTag("room_toggle_party_button"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Groups,
                                        contentDescription = "Toggle Party Stage",
                                        tint = if (stream.partyMode) CosmicObsidian else ElectricCyan,
                                        modifier = Modifier.padding(8.dp).size(18.dp),
                                    )
                                }
                            }

                            if (isLocalHostCamera) {
                                Surface(
                                    shape = CircleShape,
                                    color = CosmicObsidian.copy(alpha = 0.72f),
                                    onClick = { useFrontCamera = !useFrontCamera },
                                    modifier = Modifier.testTag("room_flip_camera_button"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Cameraswitch,
                                        contentDescription = "Flip camera",
                                        tint = Color.White,
                                        modifier = Modifier.padding(8.dp).size(18.dp),
                                    )
                                }
                            } else {
                                Surface(
                                    shape = CircleShape,
                                    color = CosmicObsidian.copy(alpha = 0.72f),
                                    onClick = { isMuted = !isMuted },
                                    modifier = Modifier.testTag("room_mute_button"),
                                ) {
                                    Icon(
                                        imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                                        contentDescription = "Mute audio",
                                        tint = Color.White,
                                        modifier = Modifier.padding(8.dp).size(18.dp),
                                    )
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = CosmicObsidian.copy(alpha = 0.72f),
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "Join ${stream.hostName}'s live room \"${stream.title}\" on PopLive!",
                                        )
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Live Room"))
                                },
                                modifier = Modifier.testTag("room_share_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = "Share room",
                                    tint = Color.White,
                                    modifier = Modifier.padding(8.dp).size(18.dp),
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = CosmicObsidian.copy(alpha = 0.72f),
                                onClick = {
                                    if (stream.hostId == currentUserId && stream.statusEnum() == StreamStatus.LIVE) {
                                        showEndSummary = stream
                                    } else {
                                        onBack()
                                    }
                                },
                                modifier = Modifier.testTag("close_room_button"),
                            ) {
                                Icon(
                                    imageVector = if (stream.hostId == currentUserId) Icons.Filled.StopCircle else Icons.Filled.Close,
                                    contentDescription = "Leave room",
                                    tint = if (stream.hostId == currentUserId) CoralPink else Color.White,
                                    modifier = Modifier.padding(8.dp).size(18.dp),
                                )
                            }
                        }
                    }

                    if (stream.pkActive) {
                        PkBattleProgressBar(
                            hostName = stream.hostName,
                            opponentName = stream.pkOpponentName ?: "Challenger",
                            hostScore = stream.pkHostScore,
                            opponentScore = stream.pkOpponentScore,
                        )
                    }

                    if (stream.partyMode) {
                        PartySeatsGrid(
                            seats = seats,
                            currentUserId = currentUserId,
                            hostId = stream.hostId,
                            onClaimSeat = { seatNum -> onClaimSeat(stream, seatNum) },
                            onToggleMute = { seat -> onToggleSeatMute(stream.id, seat) },
                            onLeaveSeat = { seat -> onLeaveSeat(stream.id, seat) },
                        )
                    }

                    FloatingGiftBanner(giftMessage = latestGiftMessage)
                }

                // Bottom Chat & Engagement Dock
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LiveChatFeed(
                        messages = messages,
                        modifier = Modifier.fillMaxWidth(0.86f),
                    )

                    QuickReactionBar(
                        onReact = { reaction -> onSendMessage(stream, reaction) },
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = chatDraft,
                            onValueChange = { chatDraft = it },
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.hint_say_something),
                                    color = Color(0xFFBDB3D6),
                                )
                            },
                            singleLine = true,
                            shape = CircleShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = CosmicObsidian.copy(alpha = 0.78f),
                                unfocusedContainerColor = CosmicObsidian.copy(alpha = 0.68f),
                                focusedBorderColor = CoralPink,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.22f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                            ),
                            trailingIcon = {
                                if (chatDraft.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            onSendMessage(stream, chatDraft)
                                            chatDraft = ""
                                        },
                                        modifier = Modifier.testTag("send_chat_button"),
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = stringResource(R.string.action_send),
                                            tint = CoralPink,
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("room_chat_input"),
                        )

                        Surface(
                            shape = CircleShape,
                            color = WarmGold,
                            onClick = { showGiftSheet = true },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("open_gift_sheet_button"),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.CardGiftcard,
                                    contentDescription = "Send Gift",
                                    tint = CosmicObsidian,
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = CoralPink,
                            onClick = {
                                heartTrigger += 1
                                onSendLike(stream)
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("send_heart_button"),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Favorite,
                                    contentDescription = "Send Heart",
                                    tint = Color.White,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showGiftSheet && stream != null) {
        GiftPickerBottomSheet(
            coinBalance = profile?.coinBalance ?: 0L,
            onSendGift = { gift, combo ->
                onSendGift(stream, gift, combo)
                showGiftSheet = false
            },
            onGoToWallet = {
                showGiftSheet = false
                onGoToWallet()
            },
            onDismiss = { showGiftSheet = false },
        )
    }

    showEndSummary?.let { activeStream ->
        AlertDialog(
            onDismissRequest = { showEndSummary = null },
            title = { Text("End Live Broadcast?") },
            text = {
                Text(
                    "Your broadcast has ${activeStream.likeCount} likes and ${activeStream.totalGiftsCoins} gift coins earned. End stream now?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEndSummary = null
                        onEndBroadcast(activeStream.id) { onBack() }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralPink),
                ) {
                    Text(stringResource(R.string.action_end_live))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndSummary = null }) {
                    Text("Keep Streaming")
                }
            },
        )
    }
}
