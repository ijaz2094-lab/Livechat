package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.FollowedHost
import com.example.model.RankedCreator
import com.example.ui.theme.CoralPink
import com.example.ui.theme.CosmicObsidian
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.VelvetOutline
import com.example.ui.theme.VelvetSurface
import com.example.ui.theme.VelvetSurfaceHigh
import com.example.ui.theme.WarmGold
import com.example.ui.video.coverDrawableForKey

@Composable
fun RankingsScreen(
    creators: List<RankedCreator>,
    following: List<FollowedHost>,
    isBusy: Boolean,
    onWatchStreamId: (String) -> Unit,
    onToggleFollow: (hostId: String, hostName: String, hostLevel: Long) -> Unit,
    onLaunchDemoRooms: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val followedIds = following.map { it.hostId }.toSet()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = WarmGold.copy(alpha = 0.2f),
                    modifier = Modifier.size(42.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = WarmGold,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
                Column {
                    Text(
                        text = "Top Star Rankings",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = "Live hosts ranked by gift coins & fan hearts",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (creators.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = VelvetSurface,
                    border = BorderStroke(1.dp, VelvetOutline),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp),
                    ) {
                        Text(
                            text = "No ranked stars yet",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Launch featured live rooms or go live to climb the Top Star Leaderboard.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onLaunchDemoRooms,
                            enabled = !isBusy,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = CoralPink),
                        ) {
                            Text("Launch Demo Live Rooms")
                        }
                    }
                }
            }
        } else {
            item {
                TopThreePodium(
                    topThree = creators.take(3),
                    onWatchStreamId = onWatchStreamId,
                )
            }

            itemsIndexed(creators, key = { index, item -> "${index}_${item.hostName}" }) { index, creator ->
                val isFollowing = creator.hostId in followedIds
                CreatorRankRow(
                    rank = index + 1,
                    creator = creator,
                    isFollowing = isFollowing,
                    onWatch = { creator.activeStreamId?.let(onWatchStreamId) },
                    onToggleFollow = {
                        onToggleFollow(creator.hostId, creator.hostName, creator.hostLevel)
                    },
                )
            }
        }
    }
}

@Composable
private fun TopThreePodium(
    topThree: List<RankedCreator>,
    onWatchStreamId: (String) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = VelvetSurfaceHigh,
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(listOf(WarmGold.copy(alpha = 0.65f), CoralPink.copy(alpha = 0.5f))),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom,
        ) {
            if (topThree.size > 1) {
                PodiumSpot(
                    rank = 2,
                    creator = topThree[1],
                    ringColor = Color(0xFFD6DCE5),
                    avatarSize = 62,
                    onClick = { topThree[1].activeStreamId?.let(onWatchStreamId) },
                    modifier = Modifier.weight(1f),
                )
            }
            PodiumSpot(
                rank = 1,
                creator = topThree[0],
                ringColor = WarmGold,
                avatarSize = 78,
                onClick = { topThree[0].activeStreamId?.let(onWatchStreamId) },
                modifier = Modifier.weight(1.1f),
            )
            if (topThree.size > 2) {
                PodiumSpot(
                    rank = 3,
                    creator = topThree[2],
                    ringColor = Color(0xFFE09F67),
                    avatarSize = 58,
                    onClick = { topThree[2].activeStreamId?.let(onWatchStreamId) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PodiumSpot(
    rank: Int,
    creator: RankedCreator,
    ringColor: Color,
    avatarSize: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable(enabled = creator.activeStreamId != null, onClick = onClick),
    ) {
        Surface(
            shape = CircleShape,
            color = ringColor,
        ) {
            Text(
                text = "#$rank",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = CosmicObsidian,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.dp),
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .size(avatarSize.dp)
                .clip(CircleShape)
                .background(ringColor)
                .padding(2.5.dp)
                .clip(CircleShape),
        ) {
            Image(
                painter = painterResource(id = coverDrawableForKey(creator.coverKey)),
                contentDescription = creator.hostName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = creator.hostName,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${creator.totalGiftCoins} coins",
            style = MaterialTheme.typography.labelSmall,
            color = ringColor,
        )
    }
}

@Composable
private fun CreatorRankRow(
    rank: Int,
    creator: RankedCreator,
    isFollowing: Boolean,
    onWatch: () -> Unit,
    onToggleFollow: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = VelvetSurface,
        border = BorderStroke(1.dp, VelvetOutline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("rank_row_$rank"),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "$rank",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (rank <= 3) WarmGold else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(22.dp),
            )

            Image(
                painter = painterResource(id = coverDrawableForKey(creator.coverKey)),
                contentDescription = creator.hostName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape),
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = creator.hostName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Lv.${creator.hostLevel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.MonetizationOn,
                            contentDescription = null,
                            tint = WarmGold,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${creator.totalGiftCoins}",
                            style = MaterialTheme.typography.labelSmall,
                            color = WarmGold,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = CoralPink,
                            modifier = Modifier.size(13.dp),
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${creator.totalLikes}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Surface(
                shape = CircleShape,
                color = if (isFollowing) VelvetSurfaceHigh else CoralPink.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, if (isFollowing) VelvetOutline else CoralPink),
                onClick = onToggleFollow,
            ) {
                Text(
                    text = if (isFollowing) "Following" else "+ Follow",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isFollowing) MaterialTheme.colorScheme.onSurfaceVariant else CoralPink,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }

            if (creator.activeStreamId != null) {
                Surface(
                    shape = CircleShape,
                    color = CoralPink,
                    onClick = onWatch,
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Join live stream",
                        tint = Color.White,
                        modifier = Modifier.padding(7.dp).size(16.dp),
                    )
                }
            }
        }
    }
}
