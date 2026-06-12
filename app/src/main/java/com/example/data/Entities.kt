package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val uid: String,
    val displayName: String,
    val email: String,
    val photoUrl: String,
    val isAuthenticated: Boolean,
    val hasChannel: Boolean,
    val channelId: String? = null
)

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val uid: String, // One channel per Google UID
    val channelName: String,
    val handleName: String, // e.g. @handle
    val profilePhotoUrl: String,
    val coverPhotoUrl: String,
    val subscriberCount: Int = 0,
    val videoCount: Int = 0
)

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey val id: String, // Youtube video ID or remote URL / DoodStream file code
    val title: String,
    val category: String, // e.g., "Anime", "Movies", "Tech", "Comedy", "Music"
    val tags: String, // comma separated
    val thumbnailUrl: String,
    val videoUrl: String,
    val channelId: String,
    val channelName: String,
    val channelAvatar: String,
    val views: Int = 0,
    val likes: Int = 0,
    val dislikes: Int = 0,
    val uploadTime: String,
    val isAd: Boolean = false,
    val isLiked: Boolean = false,
    val isDisliked: Boolean = false
)

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey val id: String,
    val videoId: String,
    val senderName: String,
    val senderAvatar: String,
    val senderUid: String,
    val message: String,
    val timestamp: Long
)

@Entity(tableName = "subscriptions", primaryKeys = ["userId", "channelId"])
data class SubscriptionEntity(
    val userId: String,
    val channelId: String
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val iconUrl: String,
    val videoThumbnail: String? = null,
    val timestamp: Long,
    val isRead: Boolean = false
)
