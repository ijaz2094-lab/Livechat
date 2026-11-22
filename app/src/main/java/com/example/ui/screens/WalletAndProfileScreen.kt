package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.CoinTransaction
import com.example.model.TxType
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.ui.theme.CoralPink
import com.example.ui.theme.CosmicObsidian
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.VelvetOutline
import com.example.ui.theme.VelvetSurface
import com.example.ui.theme.VelvetSurfaceHigh
import com.example.ui.theme.WarmGold

@Composable
fun WalletAndProfileScreen(
    profileState: UiState<UserProfile?>,
    transactionsState: UiState<List<CoinTransaction>>,
    isBusy: Boolean,
    onClaimDailyBonus: () -> Unit,
    onRechargeCoins: (String, Long) -> Unit,
    onUpdateProfile: (String, String, () -> Unit) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showEditDialog by remember { mutableStateOf(false) }

    when (profileState) {
        is UiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CoralPink)
            }
        }

        is UiState.Error -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(profileState.message, color = MaterialTheme.colorScheme.error)
            }
        }

        is UiState.Success -> {
            val profile = profileState.data
            if (profile == null) {
                Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CoralPink)
                }
                return
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = modifier.fillMaxSize(),
            ) {
                item {
                    ProfileHeaderCard(
                        profile = profile,
                        onEditClick = { showEditDialog = true },
                    )
                }

                item {
                    WalletBalanceCard(
                        profile = profile,
                        isBusy = isBusy,
                        onClaimDailyBonus = onClaimDailyBonus,
                        onRechargeCoins = onRechargeCoins,
                    )
                }

                item {
                    Text(
                        text = "Recent Coin Ledger",
                        style = MaterialTheme.typography.titleLarge,
                    )
                }

                when (transactionsState) {
                    is UiState.Loading -> {
                        item { CircularProgressIndicator(color = CoralPink) }
                    }

                    is UiState.Error -> {
                        item {
                            Text(
                                text = transactionsState.message,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }

                    is UiState.Success -> {
                        if (transactionsState.data.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = VelvetSurface,
                                    border = BorderStroke(1.dp, VelvetOutline),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = "No transactions yet. Claim your daily bonus or send a virtual gift inside any live stream!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(16.dp),
                                    )
                                }
                            }
                        } else {
                            items(transactionsState.data, key = { it.id }) { tx ->
                                TransactionRow(tx = tx)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = onSignOut,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, CoralPink.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("sign_out_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            tint = CoralPink,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.action_sign_out),
                            color = CoralPink,
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            if (showEditDialog) {
                EditProfileDialog(
                    initialName = profile.displayName,
                    initialBio = profile.bio,
                    onSave = { name, bio ->
                        onUpdateProfile(name, bio) { showEditDialog = false }
                    },
                    onDismiss = { showEditDialog = false },
                )
            }
        }
    }
}

@Composable
private fun ProfileHeaderCard(
    profile: UserProfile,
    onEditClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = VelvetSurface,
        border = BorderStroke(1.dp, VelvetOutline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(CoralPink, ElectricCyan))
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!profile.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = profile.avatarUrl,
                            contentDescription = profile.displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Text(
                            text = profile.displayName.take(1).uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = profile.displayName,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = ElectricCyan.copy(alpha = 0.2f),
                        ) {
                            Text(
                                text = "Lv.${profile.level}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = profile.bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.testTag("edit_profile_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit profile",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatPill(label = "Diamonds", value = "${profile.diamondsEarned}", tint = ElectricCyan)
                StatPill(label = "Followers", value = "${profile.followersCount}", tint = CoralPink)
                StatPill(label = "Check-ins", value = "Day ${profile.dailyCheckInDay}", tint = WarmGold)
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, tint: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = tint,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WalletBalanceCard(
    profile: UserProfile,
    isBusy: Boolean,
    onClaimDailyBonus: () -> Unit,
    onRechargeCoins: (String, Long) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = VelvetSurfaceHigh,
        border = BorderStroke(
            1.5.dp,
            Brush.linearGradient(listOf(WarmGold.copy(alpha = 0.7f), CoralPink.copy(alpha = 0.6f))),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "PopCoins Wallet",
                        style = MaterialTheme.typography.labelLarge,
                        color = WarmGold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.MonetizationOn,
                            contentDescription = null,
                            tint = WarmGold,
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${profile.coinBalance}",
                            style = MaterialTheme.typography.displaySmall,
                            color = Color.White,
                        )
                    }
                }

                Button(
                    onClick = onClaimDailyBonus,
                    enabled = !isBusy,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WarmGold,
                        contentColor = CosmicObsidian,
                    ),
                    modifier = Modifier.testTag("claim_daily_bonus_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("+500 Daily")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Instant Test Coin Packs",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    "Starter Pack" to 500L,
                    "Party Pack" to 2000L,
                    "VIP Crown" to 5000L,
                ).forEach { (title, amount) ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = VelvetSurface,
                        border = BorderStroke(1.dp, VelvetOutline),
                        onClick = { if (!isBusy) onRechargeCoins(title, amount) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("recharge_pack_$amount"),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                        ) {
                            Text(
                                text = "+$amount",
                                style = MaterialTheme.typography.titleMedium,
                                color = WarmGold,
                            )
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(tx: CoinTransaction) {
    val positive = tx.coinDelta > 0
    val icon = when (TxType.fromWire(tx.txType)) {
        TxType.GIFT_SENT -> Icons.Filled.CardGiftcard
        TxType.DAILY_BONUS -> Icons.Filled.AutoAwesome
        else -> Icons.Filled.Diamond
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = VelvetSurface,
        border = BorderStroke(1.dp, VelvetOutline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                    shape = CircleShape,
                    color = (if (positive) ElectricCyan else CoralPink).copy(alpha = 0.16f),
                    modifier = Modifier.size(38.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (positive) ElectricCyan else CoralPink,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = tx.title,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = tx.txType.replace('_', ' '),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                text = if (positive) "+${tx.coinDelta}" else "${tx.coinDelta}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (positive) ElectricCyan else CoralPink,
            )
        }
    }
}

@Composable
private fun EditProfileDialog(
    initialName: String,
    initialBio: String,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var bio by remember { mutableStateOf(initialBio) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Creator Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 40) name = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { if (it.length <= 160) bio = it },
                    label = { Text("Bio") },
                    maxLines = 3,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, bio) },
                enabled = name.trim().isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = CoralPink),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
