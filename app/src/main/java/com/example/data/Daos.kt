package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users LIMIT 1")
    fun getActiveUserFlow(): Flow<UserEntity?>

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getActiveUser(): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("DELETE FROM users")
    suspend fun clearUsers()
}

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels WHERE uid = :uid")
    fun getChannelFlow(uid: String): Flow<ChannelEntity?>

    @Query("SELECT * FROM channels WHERE uid = :uid")
    suspend fun getChannel(uid: String): ChannelEntity?

    @Query("SELECT * FROM channels")
    fun getAllChannelsFlow(): Flow<List<ChannelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: ChannelEntity)

    @Query("UPDATE channels SET subscriberCount = subscriberCount + :delta WHERE uid = :channelId")
    suspend fun updateSubscriberCount(channelId: String, delta: Int)
}

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos ORDER BY uploadTime DESC")
    fun getAllVideosFlow(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE category = :category ORDER BY uploadTime DESC")
    fun getVideosByCategoryFlow(category: String): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE id = :id LIMIT 1")
    fun getVideoFlow(id: String): Flow<VideoEntity?>

    @Query("SELECT * FROM videos WHERE id = :id LIMIT 1")
    suspend fun getVideo(id: String): VideoEntity?

    @Query("SELECT * FROM videos WHERE channelId = :channelId ORDER BY uploadTime DESC")
    fun getVideosByChannelFlow(channelId: String): Flow<List<VideoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<VideoEntity>)

    @Query("UPDATE videos SET views = views + 1 WHERE id = :id")
    suspend fun incrementViews(id: String)

    @Query("UPDATE videos SET likes = likes + :delta, isLiked = :liked, isDisliked = false WHERE id = :id")
    suspend fun setLiked(id: String, delta: Int, liked: Boolean)

    @Query("UPDATE videos SET dislikes = dislikes + :delta, isDisliked = :disliked, isLiked = false WHERE id = :id")
    suspend fun setDisliked(id: String, delta: Int, disliked: Boolean)
}

@Dao
interface CommentDao {
    @Query("SELECT * FROM comments WHERE videoId = :videoId ORDER BY timestamp ASC")
    fun getCommentsForVideoFlow(videoId: String): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)
}

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions WHERE userId = :userId AND channelId = :channelId LIMIT 1")
    suspend fun getSubscription(userId: String, channelId: String): SubscriptionEntity?

    @Query("SELECT * FROM subscriptions WHERE userId = :userId AND channelId = :channelId LIMIT 1")
    fun isSubscribedFlow(userId: String, channelId: String): Flow<SubscriptionEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSubscription(subscription: SubscriptionEntity)

    @Delete
    suspend fun deleteSubscription(subscription: SubscriptionEntity)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotificationsFlow(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)
}
