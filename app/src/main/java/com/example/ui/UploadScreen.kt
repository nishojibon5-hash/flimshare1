package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.*
import java.io.File
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
    viewModel: FilmshareViewModel,
    onUploadSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeChannel by viewModel.activeChannelState.collectAsState()
    val uploadState by viewModel.uploadingStatus.collectAsState()

    var activeTabIdx by remember { mutableStateOf(0) } // 0 = Direct, 1 = Remote

    // Direct Upload Fields State
    var directTitle by remember { mutableStateOf("") }
    var directTags by remember { mutableStateOf("") }
    var directCategory by remember { mutableStateOf("Anime") }
    var selectedVideoPath by remember { mutableStateOf<String?>(null) }
    var selectedThumbPath by remember { mutableStateOf<String?>(null) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    // Remote Upload Fields State
    var remoteTitle by remember { mutableStateOf("") }
    var remoteTags by remember { mutableStateOf("") }
    var remoteCategory by remember { mutableStateOf("Movies") }
    var remoteVideoUrl by remember { mutableStateOf("") }
    var remoteThumbUrl by remember { mutableStateOf("") }

    val categories = listOf("Anime", "Movies", "Tech", "Comedy", "Music")
    
    // Retrieve Filmshare API key securely from BuildConfig containing .env injection
    val doodstreamApiKey = remember {
        // Fallback to placeholder if not configured
        val key = BuildConfig.DOODSTREAM_API_KEY
        if (key.isNullOrEmpty() || key == "YOUR_DOODSTREAM_API_KEY_HERE") "" else key
    }

    var manualApiKey by remember { mutableStateOf(doodstreamApiKey) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0E))
            .statusBarsPadding()
            .padding(top = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Stream Studio",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("upload_main_header")
            )

            // Dynamic channel branding indicator
            activeChannel?.let { chan ->
                Box(
                    modifier = Modifier
                        .background(Color(0xFF1E1E26), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = chan.channelName,
                        color = Color(0xFFFFD600),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // API Key Settings panel
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111115)),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2C35))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.VpnKey, contentDescription = "API key icon", tint = Color(0xFFFFD600), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Filmshare CDN Access Key Configuration",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = manualApiKey,
                    onValueChange = { manualApiKey = it },
                    placeholder = { Text("Enter Filmshare Server API Key (or empty for sandbox mode)", color = Color.Gray, fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF050508),
                        unfocusedContainerColor = Color(0xFF050508),
                        focusedBorderColor = Color(0xFFFFD600),
                        unfocusedBorderColor = Color(0xFF23232C),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_key_input"),
                    shape = RoundedCornerShape(8.dp)
                )
                if (manualApiKey.isEmpty()) {
                    Text(
                        text = "Offline demo simulation mode is active. Published streams will auto-generate with robust playback links.",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 4.dp),
                        lineHeight = 13.sp
                    )
                }
            }
        }

        // Tab Selector (Side by side choices)
        TabRow(
            selectedTabIndex = activeTabIdx,
            containerColor = Color(0xFF0E0E12),
            contentColor = Color(0xFFFFD600),
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[activeTabIdx]),
                    color = Color(0xFFFFD600)
                )
            },
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF23232C), RoundedCornerShape(8.dp))
        ) {
            Tab(
                selected = activeTabIdx == 0,
                onClick = { activeTabIdx = 0 },
                text = { Text("Direct File Upload", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                modifier = Modifier.testTag("tab_direct_upload")
            )
            Tab(
                selected = activeTabIdx == 1,
                onClick = { activeTabIdx = 1 },
                text = { Text("Remote URL Import", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                modifier = Modifier.testTag("tab_remote_upload")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (activeChannel == null) {
            // Must have a channel before publishing
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF221111)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🔒 Channel Registration Mandatory",
                        color = Color.Red,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "To publish videos or stream direct assets, you must first register your creator handle in the Profile tab on the bottom bar.",
                        color = Color.White,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
            return
        }

        when (uploadState) {
            is UploadState.Uploading -> {
                val progress = (uploadState as UploadState.Uploading).progress
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        progress = { if (progress == 0) 0.3f else progress / 100f },
                        color = Color(0xFFFFD600),
                        strokeWidth = 6.dp,
                        modifier = Modifier
                            .size(72.dp)
                            .testTag("upload_progress_indicator")
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (progress == 0) "Publishing & pushing to Filmshare Secure Servers..." else "Background processing: $progress%",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Do not close the application during export.",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            UploadState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.CheckCircle, "success icon", tint = Color.Green, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Stream Published Successfully!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            viewModel.resetUploadState()
                            onUploadSuccess()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600), contentColor = Color.Black)
                    ) {
                        Text("View Feed", fontWeight = FontWeight.Bold)
                    }
                }
            }
            is UploadState.Error -> {
                val err = (uploadState as UploadState.Error).error
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.Error, "error icon", tint = Color.Red, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Publishing Failed", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(err, color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.resetUploadState() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E26), contentColor = Color.White)
                    ) {
                        Text("Try Again")
                    }
                }
            }
            UploadState.Idle -> {
                // Raw Field Forms
                if (activeTabIdx == 0) {
                    // --- Direct File Upload Form ---
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Title Input
                        OutlinedTextField(
                            value = directTitle,
                            onValueChange = { directTitle = it },
                            label = { Text("Video Title", color = Color.LightGray) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFFD600),
                                unfocusedBorderColor = Color(0xFF23232C),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("direct_title_input")
                        )

                        // File picker simulation rows
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Card(
                                onClick = { selectedVideoPath = "/internal/sdcard/videos/sintel_master.mp4" },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(100.dp)
                                    .testTag("video_picker_trigger"),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF131317)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedVideoPath != null) Color(0xFFFFD600) else Color(0xFF23232C))
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Movie,
                                        contentDescription = "video icon",
                                        tint = if (selectedVideoPath != null) Color(0xFFFFD600) else Color.LightGray
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (selectedVideoPath != null) "Video Selected" else "Select Video File",
                                        fontSize = 11.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Card(
                                onClick = { selectedThumbPath = "https://images.unsplash.com/photo-1542204172-e7052809d836?w=600" },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(100.dp)
                                    .testTag("thumb_picker_trigger"),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF131317)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedThumbPath != null) Color(0xFFFFD600) else Color(0xFF23232C))
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Photo,
                                        contentDescription = "photo icon",
                                        tint = if (selectedThumbPath != null) Color(0xFFFFD600) else Color.LightGray
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (selectedThumbPath != null) "Thumb Selected" else "Select Thumbnail",
                                        fontSize = 11.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Tags Input
                        OutlinedTextField(
                            value = directTags,
                            onValueChange = { directTags = it },
                            label = { Text("Tags (comma separated)", color = Color.LightGray) },
                            placeholder = { Text("lofi, coding, tech") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFFD600),
                                unfocusedBorderColor = Color(0xFF23232C),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("direct_tags_input")
                        )

                        // Category Dropdown
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = directCategory,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Category Level", color = Color.LightGray) },
                                trailingIcon = {
                                    IconButton(onClick = { categoryDropdownExpanded = !categoryDropdownExpanded }) {
                                        Icon(Icons.Filled.ArrowDropDown, "dropdown", tint = Color.White)
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFFFD600),
                                    unfocusedBorderColor = Color(0xFF23232C),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { categoryDropdownExpanded = !categoryDropdownExpanded }
                                    .testTag("category_dropdown_trigger")
                            )

                            DropdownMenu(
                                expanded = categoryDropdownExpanded,
                                onDismissRequest = { categoryDropdownExpanded = false },
                                modifier = Modifier.background(Color(0xFF131317))
                            ) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat, color = Color.White) },
                                        onClick = {
                                            directCategory = cat
                                            categoryDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Direct Publish BUTTON
                        Button(
                            onClick = {
                                if (directTitle.isBlank()) return@Button
                                val mockVid = File(context.filesDir, "mock_video.mp4")
                                val mockThumb = selectedThumbPath?.let { File(context.filesDir, "mock_thumb.jpg") }
                                viewModel.triggerDirectUpload(
                                    manualApiKey,
                                    directTitle,
                                    directCategory,
                                    directTags,
                                    mockVid,
                                    mockThumb
                                )
                            },
                            enabled = directTitle.isNotBlank() && selectedVideoPath != null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFD600),
                                contentColor = Color.Black,
                                disabledContainerColor = Color.Gray
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("direct_publish_button")
                        ) {
                            Text("Publish Direct Stream", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                } else {
                    // --- Remote URL Imports Form ---
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = remoteTitle,
                            onValueChange = { remoteTitle = it },
                            label = { Text("Stream Title", color = Color.LightGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFFD600),
                                unfocusedBorderColor = Color(0xFF23232C),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("remote_title_input")
                        )

                        OutlinedTextField(
                            value = remoteVideoUrl,
                            onValueChange = { remoteVideoUrl = it },
                            label = { Text("Remote Video URL (e.g. mp4 link)", color = Color.LightGray) },
                            placeholder = { Text("https://example.com/stream.mp4") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFFD600),
                                unfocusedBorderColor = Color(0xFF23232C),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("remote_url_input")
                        )

                        OutlinedTextField(
                            value = remoteThumbUrl,
                            onValueChange = { remoteThumbUrl = it },
                            label = { Text("Remote Thumbnail Image URL (optional)", color = Color.LightGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFFD600),
                                unfocusedBorderColor = Color(0xFF23232C),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("remote_thumb_url_input")
                        )

                        OutlinedTextField(
                            value = remoteTags,
                            onValueChange = { remoteTags = it },
                            label = { Text("Tags (comma separated)", color = Color.LightGray) },
                            placeholder = { Text("recap, movie") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFFD600),
                                unfocusedBorderColor = Color(0xFF23232C),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("remote_tags_input")
                        )

                        // Category Selection for Remotes
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = remoteCategory,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Category Level", color = Color.LightGray) },
                                trailingIcon = {
                                    IconButton(onClick = { categoryDropdownExpanded = !categoryDropdownExpanded }) {
                                        Icon(Icons.Filled.ArrowDropDown, "dropdown", tint = Color.White)
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFFFD600),
                                    unfocusedBorderColor = Color(0xFF23232C),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { categoryDropdownExpanded = !categoryDropdownExpanded }
                            )

                            DropdownMenu(
                                expanded = categoryDropdownExpanded,
                                onDismissRequest = { categoryDropdownExpanded = false },
                                modifier = Modifier.background(Color(0xFF131317))
                            ) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat, color = Color.White) },
                                        onClick = {
                                            remoteCategory = cat
                                            categoryDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Remote clone Trigger BUTTON
                        Button(
                            onClick = {
                                if (remoteTitle.isBlank() || remoteVideoUrl.isBlank()) return@Button
                                viewModel.triggerRemoteUpload(
                                    manualApiKey,
                                    remoteTitle,
                                    remoteCategory,
                                    remoteTags,
                                    remoteVideoUrl,
                                    remoteThumbUrl
                                )
                            },
                            enabled = remoteTitle.isNotBlank() && remoteVideoUrl.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFD600),
                                contentColor = Color.Black,
                                disabledContainerColor = Color.Gray
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("remote_publish_button")
                        ) {
                            Text("Queue Remote Import", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}
