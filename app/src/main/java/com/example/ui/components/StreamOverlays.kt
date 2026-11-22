package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.GiftCatalogItem
import com.example.model.MessageType
import com.example.model.PopLiveGifts
import com.example.model.StreamMessage
import com.example.ui.theme.CoralPink
import com.example.ui.theme.CosmicObsidian
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.VelvetOutline
import com.example.ui.theme.VelvetSurface
import com.example.ui.theme.VelvetSurfaceHigh
import com.example.ui.theme.WarmGold
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun PkBattleProgressBar(
    hostName: String,
    opponentName: String,
    hostScore: Long,
    opponentScore: Long,
    modifier: Modifier = Modifier,
) {
    val total = (hostScore + opponentScore).coerceAtLeast(1L)
    val rawRatio = if (hostScore == 0L && opponentScore == 0L) 0.5f else (hostScore.toFloat() / total.toFloat())
    val animatedRatio by animateFloatAsState(
        targetValue = rawRatio.coerceIn(0.12f, 0.88f),
        animationSpec = spring(dampingRatio = 0.72f),
        label = "pk_ratio",
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CosmicObsidian.copy(alpha = 0.75f),
        border = BorderStroke(1.dp, VelvetOutline.copy(alpha = 0.8f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$hostName • $hostScore",
                    style = MaterialTheme.typography.labelLarge,
                    color = CoralPink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    shape = CircleShape,
                    color = WarmGold,
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = CosmicObsidian,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = "PK",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = CosmicObsidian,
                        )
                    }
                }
                Text(
                    text = "$opponentScore • $opponentName",
                    style = MaterialTheme.typography.labelLarge,
                    color = ElectricCyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(ElectricCyan),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedRatio)
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(listOf(CoralPink, Color(0xFFFF6B9D)))
                        ),
                )
            }
        }
    }
}

@Composable
fun FloatingGiftBanner(
    giftMessage: StreamMessage?,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(false) }
    var lastShownId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(giftMessage?.id, giftMessage?.body) {
        if (giftMessage != null && giftMessage.id != lastShownId) {
            lastShownId = giftMessage.id
            visible = true
            delay(3000L)
            visible = false
        }
    }

    AnimatedVisibility(
        visible = visible && giftMessage != null,
        enter = slideInHorizontally(initialOffsetX = { -it }, animationSpec = spring()) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
        modifier = modifier,
    ) {
        val msg = giftMessage ?: return@AnimatedVisibility
        Surface(
            shape = CircleShape,
            color = CosmicObsidian.copy(alpha = 0.88f),
            border = BorderStroke(
                1.5.dp,
                Brush.horizontalGradient(listOf(WarmGold, CoralPink)),
            ),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(WarmGold.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.CardGiftcard,
                        contentDescription = null,
                        tint = WarmGold,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = msg.senderName,
                        style = MaterialTheme.typography.labelLarge,
                        color = WarmGold,
                        maxLines = 1,
                    )
                    Text(
                        text = msg.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private data class HeartParticle(
    val id: Int,
    val startXFraction: Float,
    val drift: Float,
    val color: Color,
    val progress: Animatable<Float, *> = Animatable(0f),
)

@Composable
fun FloatingHeartsOverlay(
    triggerCount: Int,
    modifier: Modifier = Modifier,
) {
    val particles = remember { mutableStateListOf<HeartParticle>() }
    val heartColors = remember {
        listOf(CoralPink, Color(0xFFFF6B9D), WarmGold, ElectricCyan, Color(0xFFFF4D6D))
    }

    LaunchedEffect(triggerCount) {
        if (triggerCount <= 0) return@LaunchedEffect
        val particle = HeartParticle(
            id = triggerCount,
            startXFraction = 0.78f + (Random.nextFloat() - 0.5f) * 0.14f,
            drift = (Random.nextFloat() - 0.5f) * 80f,
            color = heartColors[triggerCount % heartColors.size],
        )
        particles.add(particle)
    }

    particles.forEach { particle ->
        LaunchedEffect(particle.id) {
            particle.progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            )
            particles.remove(particle)
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { particle ->
            val t = particle.progress.value
            val alpha = (1f - t).coerceIn(0f, 1f)
            val startY = size.height * 0.85f
            val endY = size.height * 0.28f
            val y = startY + (endY - startY) * t
            val x = size.width * particle.startXFraction + sin(t * 6.28f) * 26f + particle.drift * t
            val r = 18f + (1f - t) * 8f

            val path = Path().apply {
                moveTo(x, y + r * 0.35f)
                cubicTo(x - r, y - r * 0.45f, x - r * 1.1f, y + r * 0.55f, x, y + r * 1.25f)
                cubicTo(x + r * 1.1f, y + r * 0.55f, x + r, y - r * 0.45f, x, y + r * 0.35f)
                close()
            }
            drawPath(path = path, color = particle.color.copy(alpha = alpha * 0.9f))
        }
    }
}

@Composable
fun LiveChatFeed(
    messages: List<StreamMessage>,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.heightIn(max = 220.dp),
    ) {
        items(messages, key = { it.id.ifEmpty { "${it.senderId}_${it.body.hashCode()}" } }) { msg ->
            val msgType = msg.messageTypeEnum()
            val bgColor = when (msgType) {
                MessageType.GIFT -> CoralPink.copy(alpha = 0.42f)
                MessageType.SYSTEM -> ElectricCyan.copy(alpha = 0.25f)
                else -> CosmicObsidian.copy(alpha = 0.55f)
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = bgColor,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = WarmGold.copy(alpha = 0.25f),
                    ) {
                        Text(
                            text = "Lv.${msg.senderLevel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = WarmGold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${msg.senderName}: ",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (msgType == MessageType.GIFT) WarmGold else ElectricCyan,
                        maxLines = 1,
                    )
                    Text(
                        text = msg.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GiftPickerBottomSheet(
    coinBalance: Long,
    onSendGift: (GiftCatalogItem, Int) -> Unit,
    onGoToWallet: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedGift by remember { mutableStateOf(PopLiveGifts.catalog.first()) }
    var comboMultiplier by remember { mutableIntStateOf(1) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = VelvetSurface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Send Virtual Gift",
                    style = MaterialTheme.typography.titleLarge,
                )
                Surface(
                    shape = CircleShape,
                    color = VelvetSurfaceHigh,
                    modifier = Modifier.clickable(onClick = onGoToWallet),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MonetizationOn,
                            contentDescription = null,
                            tint = WarmGold,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$coinBalance",
                            style = MaterialTheme.typography.labelLarge,
                            color = WarmGold,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 12.dp),
                modifier = Modifier.heightIn(max = 260.dp),
            ) {
                items(PopLiveGifts.catalog, key = { it.id }) { gift ->
                    val isSelected = selectedGift.id == gift.id
                    val accent = Color(gift.accentHex)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) accent.copy(alpha = 0.2f) else VelvetSurfaceHigh,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) accent else VelvetOutline,
                        ),
                        modifier = Modifier
                            .clickable { selectedGift = gift }
                            .testTag("gift_item_${gift.id}"),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                        ) {
                            Icon(
                                imageVector = giftIconForId(gift.id),
                                contentDescription = gift.name,
                                tint = accent,
                                modifier = Modifier.size(30.dp),
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = gift.name,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${gift.coinCost} coins",
                                style = MaterialTheme.typography.labelSmall,
                                color = WarmGold,
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 5, 10).forEach { mult ->
                        val active = comboMultiplier == mult
                        Surface(
                            shape = CircleShape,
                            color = if (active) CoralPink else VelvetSurfaceHigh,
                            modifier = Modifier.clickable { comboMultiplier = mult },
                        ) {
                            Text(
                                text = "x$mult",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            )
                        }
                    }
                }

                val totalCost = selectedGift.coinCost * comboMultiplier
                if (coinBalance < totalCost) {
                    TextButton(onClick = onGoToWallet) {
                        Text("Top Up Coins", color = WarmGold)
                    }
                } else {
                    Button(
                        onClick = { onSendGift(selectedGift, comboMultiplier) },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = CoralPink),
                        modifier = Modifier.testTag("confirm_send_gift_button"),
                    ) {
                        Text("Send ($totalCost)")
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

private fun giftIconForId(id: String): ImageVector = when (id) {
    "rose" -> Icons.Filled.LocalFlorist
    "heart" -> Icons.Filled.Favorite
    "mic" -> Icons.Filled.Mic
    "crown" -> Icons.Filled.WorkspacePremium
    "car" -> Icons.Filled.DirectionsCar
    "rocket" -> Icons.Filled.RocketLaunch
    else -> Icons.Filled.AutoAwesome
}
