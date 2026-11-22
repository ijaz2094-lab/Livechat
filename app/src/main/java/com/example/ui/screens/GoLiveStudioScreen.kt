package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.model.StreamCategory
import com.example.ui.theme.CoralPink
import com.example.ui.theme.CosmicObsidian
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.VelvetOutline
import com.example.ui.theme.VelvetSurface
import com.example.ui.theme.WarmGold
import com.example.ui.video.CameraBroadcasterPreview
import com.example.ui.video.StudioFilter
import com.example.ui.video.rememberMicrophoneLevel

@Composable
fun GoLiveStudioScreen(
    isBusy: Boolean,
    onStartBroadcast: (
        title: String,
        category: StreamCategory,
        pkActive: Boolean,
        pkOpponent: String,
    ) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasCameraPermission = result[Manifest.permission.CAMERA] == true || hasCameraPermission
        hasAudioPermission = result[Manifest.permission.RECORD_AUDIO] == true || hasAudioPermission
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission || !hasAudioPermission) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            )
        }
    }

    var useFrontCamera by remember { mutableStateOf(true) }
    var torchEnabled by remember { mutableStateOf(false) }
    var micMuted by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(StudioFilter.NATURAL) }

    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(StreamCategory.MUSIC) }
    var pkBattleEnabled by remember { mutableStateOf(false) }
    var opponentName by remember { mutableStateOf("Challenger Star") }

    val micLevel = rememberMicrophoneLevel(
        hasAudioPermission = hasAudioPermission,
        micMuted = micMuted,
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CosmicObsidian),
    ) {
        if (hasCameraPermission) {
            CameraBroadcasterPreview(
                useFrontCamera = useFrontCamera,
                torchEnabled = torchEnabled,
                studioFilter = selectedFilter,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFF281042), CosmicObsidian))
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = VelvetSurface,
                    border = BorderStroke(1.dp, VelvetOutline),
                    modifier = Modifier.padding(24.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Videocam,
                            contentDescription = null,
                            tint = CoralPink,
                            modifier = Modifier.size(44.dp),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Camera & Mic Access",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Enable camera and microphone permissions to preview your real-time PopLive video feed before going live.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                permissionLauncher.launch(
                                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                                )
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = CoralPink),
                            modifier = Modifier.testTag("grant_camera_permissions_button"),
                        ) {
                            Text("Enable Camera Studio")
                        }
                    }
                }
            }
        }

        // Gradient scrim for top & bottom studio controls
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x880B0714),
                            Color.Transparent,
                            Color(0xD80B0714),
                        )
                    )
                )
        )

        // Top studio control strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = CosmicObsidian.copy(alpha = 0.72f),
                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.GraphicEq,
                        contentDescription = "Microphone audio level",
                        tint = if (micMuted) Color.Gray else ElectricCyan,
                        modifier = Modifier.size((16 + micLevel * 8).dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (micMuted) "Mic Muted" else "Studio Ready",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = CircleShape,
                    color = CosmicObsidian.copy(alpha = 0.72f),
                    onClick = { micMuted = !micMuted },
                    modifier = Modifier.testTag("studio_mic_toggle"),
                ) {
                    Icon(
                        imageVector = if (micMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                        contentDescription = "Toggle mic",
                        tint = if (micMuted) CoralPink else Color.White,
                        modifier = Modifier.padding(11.dp).size(20.dp),
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = CosmicObsidian.copy(alpha = 0.72f),
                    onClick = {
                        useFrontCamera = !useFrontCamera
                        if (useFrontCamera) torchEnabled = false
                    },
                    modifier = Modifier.testTag("studio_flip_camera"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Cameraswitch,
                        contentDescription = "Switch camera",
                        tint = Color.White,
                        modifier = Modifier.padding(11.dp).size(20.dp),
                    )
                }

                if (!useFrontCamera) {
                    Surface(
                        shape = CircleShape,
                        color = CosmicObsidian.copy(alpha = 0.72f),
                        onClick = { torchEnabled = !torchEnabled },
                        modifier = Modifier.testTag("studio_torch_toggle"),
                    ) {
                        Icon(
                            imageVector = if (torchEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                            contentDescription = "Toggle flash",
                            tint = if (torchEnabled) WarmGold else Color.White,
                            modifier = Modifier.padding(11.dp).size(20.dp),
                        )
                    }
                }
            }
        }

        // Bottom setup card
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = VelvetSurface.copy(alpha = 0.94f),
            border = BorderStroke(1.dp, VelvetOutline),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Studio filter pills
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 8.dp),
                ) {
                    items(StudioFilter.entries) { filter ->
                        val active = filter == selectedFilter
                        Surface(
                            shape = CircleShape,
                            color = if (active) ElectricCyan else CosmicObsidian,
                            modifier = Modifier.clickable { selectedFilter = filter },
                        ) {
                            Text(
                                text = filter.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (active) CosmicObsidian else Color.White,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 80) title = it },
                    placeholder = { Text(stringResource(R.string.hint_stream_title)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CoralPink,
                        unfocusedBorderColor = VelvetOutline,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("studio_title_input"),
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(StreamCategory.entries) { category ->
                        val active = selectedCategory == category
                        FilterChip(
                            selected = active,
                            onClick = { selectedCategory = category },
                            label = { Text(category.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CoralPink,
                                selectedLabelColor = Color.White,
                            ),
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = WarmGold,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Start with PK Battle Mode",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    Switch(
                        checked = pkBattleEnabled,
                        onCheckedChange = { pkBattleEnabled = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = CoralPink),
                        modifier = Modifier.testTag("studio_pk_switch"),
                    )
                }

                if (pkBattleEnabled) {
                    OutlinedTextField(
                        value = opponentName,
                        onValueChange = { if (it.length <= 40) opponentName = it },
                        label = { Text("PK Opponent Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Button(
                    onClick = {
                        onStartBroadcast(title, selectedCategory, pkBattleEnabled, opponentName)
                    },
                    enabled = !isBusy,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = CoralPink),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("start_live_broadcast_button"),
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(imageVector = Icons.Filled.Videocam, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.action_go_live_now),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }
    }
}
