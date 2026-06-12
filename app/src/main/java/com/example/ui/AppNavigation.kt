package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

sealed class Screen(val title: String, val iconFilled: ImageVector, val iconOutlined: ImageVector, val tag: String) {
    object Home : Screen("Home", Icons.Filled.Home, Icons.Outlined.Home, "tab_home")
    object Notifications : Screen("Inbox", Icons.Filled.Notifications, Icons.Outlined.Notifications, "tab_notifications")
    object Upload : Screen("Upload", Icons.Filled.Add, Icons.Outlined.Add, "tab_upload")
    object Discover : Screen("Discover", Icons.Filled.Explore, Icons.Outlined.Explore, "tab_discover")
    object Profile : Screen("Profile", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle, "tab_profile")
}

@Composable
fun AppNavigation(
    viewModel: FlimshareViewModel,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf<Screen>(Screen.Home) }
    val activeVideo by viewModel.activeVideoState.collectAsState()

    // Overlay Player View is presented if there is an active playing video, mimicking YouTube/Bilibili
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0E)) // Premium Default Dark Canvas
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Main content based on selection
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (activeTab) {
                    Screen.Home -> HomeScreen(viewModel = viewModel, onVideoClick = { video ->
                        viewModel.selectActiveVideo(video.id)
                    })
                    Screen.Notifications -> NotificationScreen(viewModel = viewModel)
                    Screen.Upload -> UploadScreen(viewModel = viewModel, onUploadSuccess = {
                        activeTab = Screen.Home
                    })
                    Screen.Discover -> DiscoverScreen(viewModel = viewModel)
                    Screen.Profile -> ProfileScreen(viewModel = viewModel)
                }
            }

            // Bottom Navigation Bar Layout (5 Tabs)
            NavigationBar(
                containerColor = Color(0xFF131317),
                tonalElevation = 8.dp,
                modifier = Modifier.navigationBarsPadding(),
                windowInsets = WindowInsets(0)
            ) {
                val tabs = listOf(
                    Screen.Home,
                    Screen.Notifications,
                    Screen.Upload, // Center Action Button
                    Screen.Discover,
                    Screen.Profile
                )

                tabs.forEach { screen ->
                    val isSelected = activeTab == screen
                    
                    if (screen == Screen.Upload) {
                        // Prominent Elevated [ + ] central tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { activeTab = Screen.Upload },
                            icon = {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFFD600)) // Pure Yellow Brand Accent
                                        .testTag("upload_fab_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = "Upload Screen Trigger",
                                        tint = Color.Black,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = "Upload",
                                    fontSize = 10.sp,
                                    color = Color(0xFF8E8E9F),
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.testTag(screen.tag)
                        )
                    } else {
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { 
                                activeTab = screen
                                if (screen == Screen.Discover) {
                                    // De-select active video back to explore
                                    viewModel.selectActiveVideo(null)
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) screen.iconFilled else screen.iconOutlined,
                                    contentDescription = screen.title,
                                    tint = if (isSelected) Color(0xFFFFD600) else Color(0xFF8E8E9F)
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontSize = 11.sp,
                                    color = if (isSelected) Color(0xFFFFD600) else Color(0xFF8E8E9F),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color(0xFF23232C)
                            ),
                            modifier = Modifier.testTag(screen.tag)
                        )
                    }
                }
            }
        }

        // Animated Video Details Overlay (Player sheet overlay on top of standard content)
        AnimatedVisibility(
            visible = activeVideo != null,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(400)) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(400)) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0C0C0E))
            ) {
                VideoPlayerScreen(
                    viewModel = viewModel,
                    onBackClick = { viewModel.selectActiveVideo(null) }
                )
            }
        }
    }
}
