package com.example.ui

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.CommentEntity
import com.example.data.VideoEntity

@Composable
fun DiscoverScreen(
    viewModel: FilmshareViewModel,
    modifier: Modifier = Modifier
) {
    val videos by viewModel.videoFeedState.collectAsState()
    var searchPhrase by remember { mutableStateOf("") }

    val filteredVideos = remember(videos, searchPhrase) {
        if (searchPhrase.isEmpty()) videos 
        else videos.filter { it.title.contains(searchPhrase, ignoreCase = true) || it.tags.contains(searchPhrase, ignoreCase = true) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0E))
            .statusBarsPadding()
            .padding(top = 16.dp)
    ) {
        // Search Header
        Text(
            text = "Discover Creators",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("discover_title")
        )

        OutlinedTextField(
            value = searchPhrase,
            onValueChange = { searchPhrase = it },
            placeholder = { Text("Search tags, titles or channels...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = Color.Gray) },
            trailingIcon = {
                if (searchPhrase.isNotEmpty()) {
                    IconButton(onClick = { searchPhrase = "" }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear search", tint = Color.LightGray)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF131317),
                unfocusedContainerColor = Color(0xFF131317),
                focusedBorderColor = Color(0xFFFFD600),
                unfocusedBorderColor = Color(0xFF23232C),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("discover_search_input"),
            shape = RoundedCornerShape(24.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Grid of results
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (filteredVideos.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🔍 No Matches Found", color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text("Try typing another tag like 'lofi' or 'recap'", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            } else {
                items(filteredVideos) { video ->
                    VideoFeedCard(
                        video = video,
                        onClick = { viewModel.selectActiveVideo(video.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun VideoPlayerScreen(
    viewModel: FilmshareViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeVideo by viewModel.activeVideoState.collectAsState()
    val comments by viewModel.commentsState.collectAsState()
    val isSubscribed by viewModel.isSubscribedState.collectAsState()
    
    val context = LocalContext.current
    var chatMessageText by remember { mutableStateOf("") }

    if (activeVideo == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFFFFD600))
        }
        return
    }

    val video = activeVideo!!

    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .background(Color(0xFF040405))
    ) {
        // Player Surface (High Fidelity Native Android Decoding)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        val videoUri = Uri.parse(video.videoUrl)
                        setVideoURI(videoUri)
                        
                        // Attaching native forward/reverse controllers
                        val controller = MediaController(ctx)
                        controller.setAnchorView(this)
                        setMediaController(controller)
                        
                        requestFocus()
                        start()
                    }
                },
                update = { view ->
                    val curUri = Uri.parse(video.videoUrl)
                    // Safe check if stream source shifts dynamically
                    view.setVideoURI(curUri)
                    view.start()
                },
                modifier = Modifier.fillMaxSize()
            )

            // Back floating trigger
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x7F000000))
                    .testTag("player_back_trigger")
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Minimize video", tint = Color.White)
            }
        }

        // Active Metadata details
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = video.title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${formatViews(video.views)} views  •  Uploaded ${video.uploadTime}",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                // Interactions (Like/Dislike)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.likeActiveVideo() },
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (video.isLiked) Color(0xFFFFD600) else Color(0xFF1E1E26))
                            .testTag("video_like_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (video.isLiked) Icons.Filled.ThumbUp else Icons.Filled.ThumbUp,
                                contentDescription = "Like video",
                                tint = if (video.isLiked) Color.Black else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = video.likes.toString(),
                                color = if (video.isLiked) Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.dislikeActiveVideo() },
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (video.isDisliked) Color(0xFFFFD600) else Color(0xFF1E1E26))
                            .testTag("video_dislike_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ThumbDown,
                                contentDescription = "Dislike video",
                                tint = if (video.isDisliked) Color.Black else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = video.dislikes.toString(),
                                color = if (video.isDisliked) Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Divider(color = Color(0xFF1C1C24), modifier = Modifier.padding(vertical = 12.dp))

            // Channel Creator Onboarding subscription Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(video.channelAvatar)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Channel image",
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color(0xFFFFD600), CircleShape)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = video.channelName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Button(
                    onClick = { viewModel.toggleSubscriptionActive() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSubscribed) Color(0xFF1B1B22) else Color(0xFFFFD600),
                        contentColor = if (isSubscribed) Color.White else Color.Black
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("subscribe_button")
                ) {
                    Text(
                        text = if (isSubscribed) "Subscribed" else "Subscribe",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // Community Live Chat/DM Segment
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                .testTag("channel_live_chat_card"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111115)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Chat Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1B1B22))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color.Green, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Live Stream Community Chat",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Chat Messages Feed
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    reverseLayout = false
                ) {
                    if (comments.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillParentMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Send a message to start the conversation!",
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        items(comments) { comment ->
                            ChatBubbleNode(comment = comment)
                        }
                    }
                }

                // Chat Input Tray
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF17171C))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = chatMessageText,
                        onValueChange = { chatMessageText = it },
                        placeholder = { Text("Say something...", color = Color.Gray, fontSize = 13.sp) },
                        maxLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0F0F12),
                            unfocusedContainerColor = Color(0xFF0F0F12),
                            focusedBorderColor = Color(0xFFFFD600),
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field"),
                        shape = RoundedCornerShape(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (chatMessageText.isNotBlank()) {
                                viewModel.postComment(chatMessageText)
                                chatMessageText = ""
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color(0xFFFFD600)
                        ),
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("chat_send_button")
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = "Send Message", tint = Color.Black, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubbleNode(comment: CommentEntity) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(comment.senderAvatar)
                .crossfade(true)
                .build(),
            contentDescription = "User Avatar",
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column {
            Text(
                text = comment.senderName,
                color = Color(0xFFFFD600),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            Text(
                text = comment.message,
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
