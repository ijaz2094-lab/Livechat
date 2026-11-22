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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.FollowedHost
import com.example.model.LiveStream
import com.example.model.StreamCategory
import com.example.model.StreamStatus
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.ui.theme.CoralPink
import com.example.ui.theme.CosmicObsidian
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.VelvetOutline
import com.example.ui.theme.VelvetSurface
import com.example.ui.theme.VelvetSurfaceHigh
import com.example.ui.theme.WarmGold
import com.example.ui.video.coverDrawableForKey

@Composable
fun LiveDiscoveryScreen(
    profileState: UiState<UserProfile?>,
    streamsState: UiState<List<LiveStream>>,
    followingState: UiState<List<FollowedHost>>,
    selectedCategory: StreamCategory?,
    followingOnly: Boolean,
    partyOnly: Boolean,
    searchQuery: String,
    isBusy: Boolean,
    onSelectCategory: (StreamCategory?) -> Unit,
    onToggleFollowingOnly: () -> Unit,
    onTogglePartyOnly: () -> Unit,
    onResetFilters: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onWatchStream: (LiveStream) -> Unit,
    onGoLiveClick: () -> Unit,
    onLaunchDemoRooms: () -> Unit,
    onWalletClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchExpanded by remember { mutableStateOf(false) }
    val coinBalance = (profileState as? UiState.Success)?.data?.coinBalance ?: 0L
    val followedHostIds = remember(followingState) {
        ((followingState as? UiState.Success)?.data ?: emptyList()).map { it.hostId }.toSet()
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Branded Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp)),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_poplive_emblem_1795346072597),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "Live Video & Party Stages",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan,
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = VelvetSurfaceHigh,
                    border = BorderStroke(1.dp, WarmGold.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clickable(onClick = onWalletClick)
                        .testTag("header_wallet_chip"),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MonetizationOn,
                            contentDescription = "PopCoins balance",
                            tint = WarmGold,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "$coinBalance",
                            style = MaterialTheme.typography.labelLarge,
                            color = WarmGold,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = {
                        searchExpanded = !searchExpanded
                        if (!searchExpanded) onSearchQueryChange("")
                    },
                    modifier = Modifier.testTag("toggle_search_button"),
                ) {
                    Icon(
                        imageVector = if (searchExpanded) Icons.Filled.Close else Icons.Filled.Search,
                        contentDescription = "Search live rooms",
                    )
                }
            }
        }

        if (searchExpanded) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search hosts, titles, PK or party rooms...") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CoralPink,
                    unfocusedBorderColor = VelvetOutline,
                    focusedContainerColor = VelvetSurface,
                    unfocusedContainerColor = VelvetSurface,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("search_streams_input"),
            )
        }

        // Category & Mode Filter Row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == null && !followingOnly && !partyOnly,
                    onClick = onResetFilters,
                    label = { Text("All Hot") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CoralPink,
                        selectedLabelColor = Color.White,
                    ),
                    modifier = Modifier.testTag("filter_all"),
                )
            }
            item {
                FilterChip(
                    selected = partyOnly,
                    onClick = onTogglePartyOnly,
                    label = { Text("Party 6-Seat") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricCyan,
                        selectedLabelColor = CosmicObsidian,
                    ),
                    modifier = Modifier.testTag("filter_party"),
                )
            }
            item {
                FilterChip(
                    selected = followingOnly,
                    onClick = onToggleFollowingOnly,
                    label = { Text("Following") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = WarmGold,
                        selectedLabelColor = CosmicObsidian,
                    ),
                    modifier = Modifier.testTag("filter_following"),
                )
            }
            items(StreamCategory.entries) { category ->
                val selected = selectedCategory == category
                FilterChip(
                    selected = selected,
                    onClick = { onSelectCategory(if (selected) null else category) },
                    label = { Text(category.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CoralPink,
                        selectedLabelColor = Color.White,
                    ),
                    modifier = Modifier.testTag("filter_${category.wireValue}"),
                )
            }
        }

        when (streamsState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CoralPink)
                }
            }

            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = streamsState.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            is UiState.Success -> {
                val filtered = remember(
                    streamsState.data,
                    selectedCategory,
                    followingOnly,
                    partyOnly,
                    followedHostIds,
                    searchQuery,
                ) {
                    streamsState.data.filter { stream ->
                        val matchesCat = when (selectedCategory) {
                            null -> true
                            StreamCategory.PK -> stream.pkActive || stream.category == StreamCategory.PK.wireValue
                            else -> stream.category == selectedCategory.wireValue
                        }
                        val matchesFollowing = !followingOnly || stream.hostId in followedHostIds
                        val matchesParty = !partyOnly || stream.partyMode
                        val q = searchQuery.trim()
                        val matchesQuery = q.isEmpty() ||
                            stream.title.contains(q, ignoreCase = true) ||
                            stream.hostName.contains(q, ignoreCase = true)
                        matchesCat && matchesFollowing && matchesParty && matchesQuery
                    }
                }

                if (filtered.isEmpty()) {
                    EmptyLiveFeedCard(
                        isBusy = isBusy,
                        hasAnyStreams = streamsState.data.isNotEmpty(),
                        onGoLiveClick = onGoLiveClick,
                        onLaunchDemoRooms = onLaunchDemoRooms,
                        onResetFilters = onResetFilters,
                    )
                } else {
                    val spotlight = filtered.first()
                    val rest = filtered.drop(1)

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 168.dp),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            SpotlightLiveHeroCard(
                                stream = spotlight,
                                onClick = { onWatchStream(spotlight) },
                            )
                        }

                        items(rest, key = { it.id }) { stream ->
                            LiveRoomGridCard(
                                stream = stream,
                                onClick = { onWatchStream(stream) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpotlightLiveHeroCard(
    stream: LiveStream,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            1.5.dp,
            Brush.linearGradient(listOf(CoralPink.copy(alpha = 0.7f), ElectricCyan.copy(alpha = 0.6f))),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clickable(onClick = onClick)
            .testTag("spotlight_stream_card"),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = coverDrawableForKey(stream.coverKey)),
                contentDescription = stream.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0x330B0714),
                                Color(0x440B0714),
                                Color(0xE60B0714),
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LiveStatusPill(isLive = stream.statusEnum() == StreamStatus.LIVE)
                    if (stream.pkActive) {
                        Surface(shape = CircleShape, color = WarmGold) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Bolt,
                                    contentDescription = null,
                                    tint = CosmicObsidian,
                                    modifier = Modifier.size(13.dp),
                                )
                                Text(
                                    text = "PK LIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = CosmicObsidian,
                                )
                            }
                        }
                    }
                    if (stream.partyMode) {
                        Surface(shape = CircleShape, color = ElectricCyan) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Groups,
                                    contentDescription = null,
                                    tint = CosmicObsidian,
                                    modifier = Modifier.size(13.dp),
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "PARTY",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = CosmicObsidian,
                                )
                            }
                        }
                    }
                }
                ViewerCountPill(count = stream.viewerCount)
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stream.categoryEnum().label.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stream.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${stream.hostName} • Lv.${stream.hostLevel}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE1D8F5),
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Surface(
                    shape = CircleShape,
                    color = CoralPink,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Watch",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "Watch",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveRoomGridCard(
    stream: LiveStream,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = VelvetSurface,
        border = BorderStroke(1.dp, VelvetOutline),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.76f)
            .clickable(onClick = onClick)
            .testTag("stream_card_${stream.id}"),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = coverDrawableForKey(stream.coverKey)),
                contentDescription = stream.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0x440B0714),
                                Color.Transparent,
                                Color(0xEE0B0714),
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LiveStatusPill(isLive = stream.statusEnum() == StreamStatus.LIVE)
                ViewerCountPill(count = stream.viewerCount)
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(10.dp),
            ) {
                if (stream.pkActive) {
                    Text(
                        text = "PK BATTLE",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarmGold,
                    )
                } else if (stream.partyMode) {
                    Text(
                        text = "PARTY 6-SEAT",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan,
                    )
                }
                Text(
                    text = stream.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${stream.hostName} • Lv.${stream.hostLevel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFD5CBEB),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun LiveStatusPill(isLive: Boolean) {
    Surface(
        shape = CircleShape,
        color = if (isLive) CoralPink else Color(0xFF47415C),
    ) {
        Text(
            text = if (isLive) stringResource(R.string.label_live_badge) else "REPLAY",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun ViewerCountPill(count: Long) {
    Surface(
        shape = CircleShape,
        color = CosmicObsidian.copy(alpha = 0.72f),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Visibility,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(12.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun EmptyLiveFeedCard(
    isBusy: Boolean,
    hasAnyStreams: Boolean,
    onGoLiveClick: () -> Unit,
    onLaunchDemoRooms: () -> Unit,
    onResetFilters: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = VelvetSurface,
            border = BorderStroke(1.dp, VelvetOutline),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_stream_acoustic_1795346073476),
                    contentDescription = "Live stage spotlight",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(18.dp)),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (hasAnyStreams) "No matching rooms" else "The stage is yours!",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (hasAnyStreams) {
                        "Try clearing your category or following filter to discover live broadcasts."
                    } else {
                        "Open your real-time CameraX studio to broadcast live, or launch featured video & party rooms with live chat and PK battles."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(18.dp))

                if (hasAnyStreams) {
                    Button(
                        onClick = onResetFilters,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = CoralPink),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Show All Rooms")
                    }
                } else {
                    Button(
                        onClick = onGoLiveClick,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = CoralPink),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("empty_go_live_button"),
                    ) {
                        Icon(imageVector = Icons.Filled.Videocam, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_go_live_now))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onLaunchDemoRooms,
                        enabled = !isBusy,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("launch_demo_rooms_button"),
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = ElectricCyan,
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.action_launch_demo_rooms),
                                color = ElectricCyan,
                            )
                        }
                    }
                }
            }
        }
    }
}
