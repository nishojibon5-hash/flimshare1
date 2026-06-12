package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.VideoEntity
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    viewModel: FilmshareViewModel,
    onVideoClick: (VideoEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val videos by viewModel.videoFeedState.collectAsState()
    val activeCategory by viewModel.selectedCategory.collectAsState()
    
    val categories = listOf("All", "Anime", "Movies", "Tech", "Comedy", "Music")

    Scaffold(
        containerColor = Color(0xFF0C0C0E),
        topBar = {
            Column(
                modifier = Modifier
                    .background(Color(0xFF0A0A0D))
                    .statusBarsPadding()
            ) {
                // Main Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filmshare",
                        color = Color(0xFFFFD600), // Pure Yellow
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.testTag("app_brand_title")
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        IconButton(
                            onClick = { /* Search functionality simulated or expanded */ },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1B1B22))
                                .testTag("search_icon_trigger")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search videos",
                                tint = Color.LightGray
                            )
                        }
                    }
                }

                // Horizontally Scrollable Categories
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = activeCategory == category
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) Color(0xFFFFD600) else Color(0xFF1E1E26))
                                .clickable { viewModel.selectCategory(category) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .testTag("category_tag_$category")
                        ) {
                            Text(
                                text = category,
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Horizontal Carousel Slider (Featured announcements, etc.)
            item {
                FeaturedCarousel()
            }

            // Video list feed interspersed with Native ads
            if (videos.isEmpty()) {
                item {
                    EmptyStatePlaceholder()
                }
            } else {
                items(videos) { video ->
                    if (video.isAd) {
                        NativeAdCard(ad = video)
                    } else {
                        VideoFeedCard(
                            video = video,
                            onClick = { onVideoClick(video) }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeaturedCarousel() {
    val carouselImages = listOf(
        "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=800",
        "https://images.unsplash.com/photo-1579546929518-9e396f3cc809?w=800",
        "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800"
    )

    val carouselDetails = listOf(
        Pair("Featured Event", "Unlock Cinematic Streaming Across All Domains"),
        Pair("Bilibili Collab", "Exclusive Anime Streaming Week - Daily Drops"),
        Pair("Filmshare Monetize", "Empowering Independent Creators globally")
    )

    val pagerState = rememberPagerState(pageCount = { carouselImages.size })

    // Auto-scroll loop
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            val nextPage = (pagerState.currentPage + 1) % carouselImages.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("featured_carousel_banner")
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF2C2C35), RoundedCornerShape(12.dp))
        ) { page ->
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(carouselImages[page])
                        .crossfade(true)
                        .build(),
                    contentDescription = "Featured Banner Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Dark Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xCC050508)),
                                startY = 30f
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFFD600), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = carouselDetails[page].first,
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = carouselDetails[page].second,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Pager indicator dots
        Row(
            Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(carouselImages.size) { iteration ->
                val color = if (pagerState.currentPage == iteration) Color(0xFFFFD600) else Color(0xFF3A3A45)
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(6.dp)
                )
            }
        }
    }
}

@Composable
fun VideoFeedCard(
    video: VideoEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() }
            .testTag("video_post_card_${video.id}"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131317)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(video.thumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Video Thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Length/tag placement
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(Color(0xE6000000), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = video.category,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Info row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(video.channelAvatar)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Channel Creator Avatar",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color(0xFFFFD600).copy(alpha = 0.5f), CircleShape)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = video.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = video.channelName,
                            color = Color(0xFF8E8E9F),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Box(modifier = Modifier.size(3.dp).background(Color(0xFF8E8E9F), CircleShape))
                        Text(
                            text = formatViews(video.views) + " views",
                            color = Color(0xFF8E8E9F),
                            fontSize = 12.sp
                        )
                        Box(modifier = Modifier.size(3.dp).background(Color(0xFF8E8E9F), CircleShape))
                        Text(
                            text = video.uploadTime,
                            color = Color(0xFF8E8E9F),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NativeAdCard(ad: VideoEntity) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("inline_feed_native_ad"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131317)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFFFD600).copy(alpha = 0.2f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(ad.thumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Advertisement Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Inline ad badge inside thumbnail
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color(0xFFFFD600), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "AD",
                        color = Color.Black,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF332F1E))
                        .border(1.dp, Color(0xFFFFD600), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Sponsored Star Marker",
                        tint = Color(0xFFFFD600),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = ad.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = ad.channelName,
                            color = Color(0xFFFFD600),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(modifier = Modifier.size(3.dp).background(Color(0xFF8E8E9F), CircleShape))
                        Text(
                            text = "Promoted Inline",
                            color = Color(0xFF8E8E9F),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStatePlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "📺 No Streams Found",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Try picking a different category level from the scrolls above.",
            color = Color.Gray,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

fun formatViews(views: Int): String {
    return when {
        views >= 1000000 -> String.format("%.1fM", views / 1000000f)
        views >= 1000 -> String.format("%.1fK", views / 1000f)
        else -> views.toString()
    }
}
