package com.example.data

import android.content.Context
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.UUID

class FlimshareRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val doodService: DoodStreamService
) {
    private val userDao = database.userDao()
    private val channelDao = database.channelDao()
    private val videoDao = database.videoDao()
    private val commentDao = database.commentDao()
    private val subscriptionDao = database.subscriptionDao()
    private val notificationDao = database.notificationDao()

    init {
        // Pre-populate database with Bilibili/YouTube style initial video posts,
        // categories, and inline feed ads so first launch has high fidelity.
        CoroutineScope(Dispatchers.IO).launch {
            if (videoDao.getAllVideosFlow().firstOrNull()?.isEmpty() == true) {
                prepopulateVideos()
            }
        }
        createNotificationChannel()
    }

    // --- Authentication Flow ---
    fun getActiveUserFlow(): Flow<UserEntity?> = userDao.getActiveUserFlow()
    suspend fun getActiveUser(): UserEntity? = userDao.getActiveUser()

    suspend fun signInWithGoogleOneTap(email: String, displayName: String): UserEntity {
        val uid = UUID.nameUUIDFromBytes(email.toByteArray()).toString()
        // Standard user avatar derived from name
        val avatar = "https://api.dicebear.com/7.x/bottts/png?seed=${email.replace("@", "")}"
        val existingUser = userDao.getActiveUser()
        
        val user = if (existingUser != null && existingUser.uid == uid) {
            existingUser.copy(isAuthenticated = true)
        } else {
            UserEntity(
                uid = uid,
                displayName = displayName,
                email = email,
                photoUrl = avatar,
                isAuthenticated = true,
                hasChannel = false
            )
        }
        userDao.insertUser(user)
        return user
    }

    suspend fun signOut() {
        val currentUser = userDao.getActiveUser()
        if (currentUser != null) {
            userDao.insertUser(currentUser.copy(isAuthenticated = false))
        }
    }

    // --- Channel Logic ---
    fun getChannelFlow(uid: String): Flow<ChannelEntity?> = channelDao.getChannelFlow(uid)
    suspend fun getChannel(uid: String): ChannelEntity? = channelDao.getChannel(uid)
    fun getAllChannelsFlow(): Flow<List<ChannelEntity>> = channelDao.getAllChannelsFlow()

    suspend fun createChannel(
        uid: String,
        channelName: String,
        handleName: String,
        profilePhotoUrl: String,
        coverPhotoUrl: String
    ): ChannelEntity {
        val cleanHandle = if (handleName.startsWith("@")) handleName else "@$handleName"
        val channel = ChannelEntity(
            uid = uid,
            channelName = channelName,
            handleName = cleanHandle,
            profilePhotoUrl = profilePhotoUrl.ifEmpty { "https://api.dicebear.com/7.x/identicon/png?seed=$channelName" },
            coverPhotoUrl = coverPhotoUrl.ifEmpty { "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800" },
            subscriberCount = 142, // start with some nice, organic starter audience
            videoCount = 0
        )
        channelDao.insertChannel(channel)
        
        // Update user state
        val user = userDao.getActiveUser()
        if (user != null && user.uid == uid) {
            userDao.insertUser(user.copy(hasChannel = true, channelId = uid))
        }
        return channel
    }

    // --- Core Feed & Video Queries ---
    fun getVideosFlow(): Flow<List<VideoEntity>> = videoDao.getAllVideosFlow()
    fun getVideosByCategoryFlow(category: String): Flow<List<VideoEntity>> {
        return if (category == "All") getVideosFlow() else videoDao.getVideosByCategoryFlow(category)
    }
    fun getVideoFlow(id: String): Flow<VideoEntity?> = videoDao.getVideoFlow(id)
    suspend fun incrementViews(id: String) = videoDao.incrementViews(id)

    // --- Social Actions (Likes/Subscribe/Chat) ---
    suspend fun likeVideo(id: String): VideoEntity? {
        val video = videoDao.getVideo(id) ?: return null
        if (video.isLiked) {
            videoDao.setLiked(id, -1, false)
        } else {
            val deltaLike = 1
            val deltaDislike = if (video.isDisliked) -1 else 0
            videoDao.setLiked(id, deltaLike, true)
            if (deltaDislike != 0) videoDao.setDisliked(id, deltaDislike, false)
        }
        return videoDao.getVideo(id)
    }

    suspend fun dislikeVideo(id: String): VideoEntity? {
        val video = videoDao.getVideo(id) ?: return null
        if (video.isDisliked) {
            videoDao.setDisliked(id, -1, false)
        } else {
            val deltaDislike = 1
            val deltaLike = if (video.isLiked) -1 else 0
            videoDao.setDisliked(id, deltaDislike, true)
            if (deltaLike != 0) videoDao.setLiked(id, deltaLike, false)
        }
        return videoDao.getVideo(id)
    }

    fun isSubscribed(userId: String, channelId: String): Flow<SubscriptionEntity?> {
        return subscriptionDao.isSubscribedFlow(userId, channelId)
    }

    suspend fun toggleSubscription(userId: String, channelId: String): Boolean {
        val existing = subscriptionDao.getSubscription(userId, channelId)
        return if (existing != null) {
            subscriptionDao.deleteSubscription(existing)
            channelDao.updateSubscriberCount(channelId, -1)
            false
        } else {
            subscriptionDao.insertSubscription(SubscriptionEntity(userId, channelId))
            channelDao.updateSubscriberCount(channelId, 1)
            
            // Push Notification trigger
            triggerSystemNotification(
                title = "New Subscription!",
                body = "You successfully subscribed to this channel's streams.",
                null
            )
            true
        }
    }

    fun getCommentsFlow(videoId: String): Flow<List<CommentEntity>> = commentDao.getCommentsForVideoFlow(videoId)
    
    suspend fun postComment(videoId: String, message: String) {
        val user = userDao.getActiveUser() ?: return
        val comment = CommentEntity(
            id = UUID.randomUUID().toString(),
            videoId = videoId,
            senderUid = user.uid,
            senderName = user.displayName,
            senderAvatar = user.photoUrl,
            message = message,
            timestamp = System.currentTimeMillis()
        )
        commentDao.insertComment(comment)
    }

    // --- Uploading Flow ---
    suspend fun uploadVideoDirect(
        apiKey: String,
        title: String,
        category: String,
        tags: String,
        tempVideoFile: File,
        tempThumbFile: File?
    ): Result<VideoEntity> = withContext(Dispatchers.IO) {
        try {
            val user = userDao.getActiveUser() ?: return@withContext Result.failure(Exception("Not Authenticated"))
            val channel = channelDao.getChannel(user.uid) ?: return@withContext Result.failure(Exception("Create a channel first"))

            val videoId = UUID.randomUUID().toString().substring(0, 8)
            val finalThumbnail = tempThumbFile?.absolutePath ?: "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=600"
            
            // If actual upload CDN key is valid (not default placeholder)
            val videoUrl = if (apiKey.isNotEmpty() && apiKey != "YOUR_DOODSTREAM_API_KEY_HERE") {
                if (!tempVideoFile.exists() || tempVideoFile.length() == 0L) {
                    tempVideoFile.parentFile?.mkdirs()
                    // Create standard valid tiny mp4 structure header bytes
                    tempVideoFile.writeBytes(byteArrayOf(
                        0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70,
                        0x6d, 0x70, 0x34, 0x32, 0x00, 0x00, 0x00, 0x00,
                        0x6d, 0x6f, 0x6f, 0x76, 0x00, 0x00, 0x00, 0x08,
                        0x6d, 0x76, 0x68, 0x64
                    ))
                }
                
                val serverResp = doodService.getUploadServer(apiKey)
                if (serverResp.isSuccessful) {
                    val serverBody = serverResp.body()
                    if (serverBody == null) {
                        throw Exception("Flimshare upload server response body is null")
                    }
                    if (serverBody.status != 200) {
                        throw Exception("Flimshare server error: ${serverBody.msg}")
                    }
                    val serverUrl = serverBody.result ?: "https://doodapi.co/"
                    
                    val fileBody = tempVideoFile.asRequestBody("video/*".toMediaTypeOrNull())
                    val filePart = MultipartBody.Part.createFormData("file", tempVideoFile.name, fileBody)
                    val keyBody = apiKey.toRequestBody("text/plain".toMediaTypeOrNull())
                    val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
                    
                    val uploadResp = doodService.uploadFileDirect(serverUrl, keyBody, filePart, titleBody)
                    if (uploadResp.isSuccessful) {
                        val uploadBody = uploadResp.body()
                        if (uploadBody == null) {
                            throw Exception("Flimshare file upload response body is null")
                        }
                        if (uploadBody.status != 200) {
                            throw Exception("Flimshare file upload error: ${uploadBody.msg}")
                        }
                        val firstResult = uploadBody.result?.firstOrNull()
                        if (firstResult != null) {
                            firstResult.download_url
                        } else {
                            throw Exception("Flimshare file upload returned empty results list")
                        }
                    } else {
                        throw Exception("Flimshare direct upload failed with HTTP code ${uploadResp.code()}")
                    }
                } else {
                    throw Exception("Failed to contact Flimshare upload server with HTTP code ${serverResp.code()}")
                }
            } else {
                // Return high-fidelity fallback playback source for offline testing
                "https://storage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"
            }

            val videoEntity = VideoEntity(
                id = videoId,
                title = title,
                category = category,
                tags = tags,
                thumbnailUrl = finalThumbnail,
                videoUrl = videoUrl,
                channelId = channel.uid,
                channelName = channel.channelName,
                channelAvatar = channel.profilePhotoUrl,
                uploadTime = "Just now",
                views = 1,
                likes = 1,
                dislikes = 0
            )
            videoDao.insertVideo(videoEntity)

            // Trigger FCM system notification simulation
            triggerSystemNotification(
                title = "New Upload: $title",
                body = "Your subscribed channel '${channel.channelName}' uploaded a new video!",
                videoThumbnail = finalThumbnail
            )

            Result.success(videoEntity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadVideoRemote(
        apiKey: String,
        title: String,
        category: String,
        tags: String,
        remoteUrl: String,
        thumbnailUrl: String
    ): Result<VideoEntity> = withContext(Dispatchers.IO) {
        try {
            val user = userDao.getActiveUser() ?: return@withContext Result.failure(Exception("Not Authenticated"))
            val channel = channelDao.getChannel(user.uid) ?: return@withContext Result.failure(Exception("Create a channel first"))

            val videoId = UUID.randomUUID().toString().substring(0, 8)
            val finalThumb = thumbnailUrl.ifEmpty { "https://images.unsplash.com/photo-1542204172-e7052809d836?w=600" }

            var videoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
            if (apiKey.isNotEmpty() && apiKey != "YOUR_DOODSTREAM_API_KEY_HERE") {
                val resp = doodService.remoteUpload(apiKey, remoteUrl, title)
                if (resp.isSuccessful) {
                    val respBody = resp.body()
                    if (respBody == null) {
                        throw Exception("Flimshare remote upload response body is null")
                    }
                    if (respBody.status != 200) {
                        throw Exception("Flimshare remote upload error: ${respBody.msg}")
                    }
                    val code = respBody.result?.filecode
                    if (code != null) {
                        videoUrl = "https://doodstream.com/e/$code"
                    } else {
                        throw Exception("Flimshare remote upload did not return a valid filecode")
                    }
                } else {
                    throw Exception("Flimshare remote upload failed with HTTP code ${resp.code()}")
                }
            }

            val videoEntity = VideoEntity(
                id = videoId,
                title = title,
                category = category,
                tags = tags,
                thumbnailUrl = finalThumb,
                videoUrl = videoUrl,
                channelId = channel.uid,
                channelName = channel.channelName,
                channelAvatar = channel.profilePhotoUrl,
                uploadTime = "Just now",
                views = 0,
                likes = 1,
                dislikes = 0
            )
            videoDao.insertVideo(videoEntity)

            // Dynamic Notification pushes
            triggerSystemNotification(
                title = "Uploaded Successfully (Remote Clone)",
                body = "Processed '${title}' remotely onto Flimshare CDN accounts.",
                videoThumbnail = finalThumb
            )

            Result.success(videoEntity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Real-time Notifications Lists ---
    fun getNotificationsFlow(): Flow<List<NotificationEntity>> = notificationDao.getAllNotificationsFlow()
    suspend fun markNotificationRead(id: String) = notificationDao.markAsRead(id)

    // --- Internal Notification Engine ---
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Flimshare Push Stream"
            val descriptionText = "Instant notification alerts for subscribed videos & system updates."
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("flimshare_push_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    suspend fun triggerSystemNotification(title: String, body: String, videoThumbnail: String?) {
        val uniqueId = UUID.randomUUID().toString()
        val notificationItem = NotificationEntity(
            id = uniqueId,
            title = title,
            body = body,
            iconUrl = "android.resource://${context.packageName}/drawable/ic_launcher_foreground",
            videoThumbnail = videoThumbnail,
            timestamp = System.currentTimeMillis()
        )
        notificationDao.insertNotification(notificationItem)

        // Native Android system notification manager call
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, "flimshare_push_channel")
            .setSmallIcon(android.R.drawable.ic_menu_slideshow)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(uniqueId.hashCode(), builder.build())
    }

    // --- Mock Data Setups for High Visual Quality (Pre-population) ---
    private suspend fun prepopulateVideos() {
        val list = listOf(
            VideoEntity(
                id = "demo_video_1",
                title = "Solo Leveling - Season 2 Official Cinematic Trailer Recap",
                category = "Anime",
                tags = "solo leveling, anime, recap",
                thumbnailUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600",
                videoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                channelId = "chan_anime_hub",
                channelName = "AnimeHub Studio",
                channelAvatar = "https://api.dicebear.com/7.x/identicon/png?seed=animehub",
                views = 124500,
                likes = 12000,
                dislikes = 42,
                uploadTime = "2 hours ago"
            ),
            VideoEntity(
                id = "demo_video_2",
                title = "Ad Space: Supercharge Your Tech Skills with Flimshare Premium AD",
                category = "Tech",
                tags = "premium, ad, sponsored",
                thumbnailUrl = "https://images.unsplash.com/photo-1460925895917-afdab827c52f?w=600",
                videoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                channelId = "sponsored_ad_channel",
                channelName = "Flimshare Ad Engine",
                channelAvatar = "https://api.dicebear.com/7.x/identicon/png?seed=flimshare",
                views = 420,
                likes = 8,
                dislikes = 1,
                uploadTime = "Sponsored",
                isAd = true // Flagged as a Native inline feed AD! Look and feel exact to organics.
            ),
            VideoEntity(
                id = "demo_video_3",
                title = "Why AI is Redefining Custom Android Architectures in 2026",
                category = "Tech",
                tags = "tech, coding, ai, kotlin",
                thumbnailUrl = "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=600",
                videoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                channelId = "chan_tech_bytes",
                channelName = "TechBytes Daily",
                channelAvatar = "https://api.dicebear.com/7.x/identicon/png?seed=techbytes",
                views = 89200,
                likes = 9422,
                dislikes = 120,
                uploadTime = "1 day ago"
            ),
            VideoEntity(
                id = "demo_video_4",
                title = "Modern Lofi Music Session for Deep Coding Concentration",
                category = "Music",
                tags = "lofi, music, work, study",
                thumbnailUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600",
                videoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                channelId = "chan_chill_lofi",
                channelName = "LofiChill Records",
                channelAvatar = "https://api.dicebear.com/7.x/identicon/png?seed=lofichill",
                views = 562100,
                likes = 31405,
                dislikes = 89,
                uploadTime = "3 days ago"
            ),
            VideoEntity(
                id = "demo_video_5",
                title = "10 Funniest Tech Bloopers & Workspace Fail Compilation",
                category = "Comedy",
                tags = "comedy, fails, fun",
                thumbnailUrl = "https://images.unsplash.com/photo-1548372290-8d01b6c8e78c?w=600",
                videoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                channelId = "chan_lol_tech",
                channelName = "CodingFails LOL",
                channelAvatar = "https://api.dicebear.com/7.x/identicon/png?seed=loltech",
                views = 45200,
                likes = 4900,
                dislikes = 15,
                uploadTime = "1 week ago"
            ),
            VideoEntity(
                id = "demo_video_6",
                title = "Top 5 Upcoming Sci-Fi Thrillers and Movie Speculations (2026)",
                category = "Movies",
                tags = "movies, cinema, scifi, trailers",
                thumbnailUrl = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=600",
                videoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                channelId = "chan_cinema_talk",
                channelName = "CinemaTalk",
                channelAvatar = "https://api.dicebear.com/7.x/identicon/png?seed=cinematalk",
                views = 78000,
                likes = 6125,
                dislikes = 54,
                uploadTime = "2 weeks ago"
            )
        )
        videoDao.insertVideos(list)

        // Seed some organic channel profiles in the DB to support subscription features
        val channels = listOf(
            ChannelEntity(
                uid = "chan_anime_hub",
                channelName = "AnimeHub Studio",
                handleName = "@animehub",
                profilePhotoUrl = "https://api.dicebear.com/7.x/identicon/png?seed=animehub",
                coverPhotoUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800",
                subscriberCount = 249000,
                videoCount = 142
            ),
            ChannelEntity(
                uid = "chan_tech_bytes",
                channelName = "TechBytes Daily",
                handleName = "@techbytes",
                profilePhotoUrl = "https://api.dicebear.com/7.x/identicon/png?seed=techbytes",
                coverPhotoUrl = "https://images.unsplash.com/photo-1620121692029-d088224ddc74?w=800",
                subscriberCount = 125000,
                videoCount = 89
            ),
            ChannelEntity(
                uid = "chan_chill_lofi",
                channelName = "LofiChill Records",
                handleName = "@lofichill",
                profilePhotoUrl = "https://api.dicebear.com/7.x/identicon/png?seed=lofichill",
                coverPhotoUrl = "https://images.unsplash.com/photo-1557683316-973673baf926?w=800",
                subscriberCount = 1004000,
                videoCount = 650
            ),
            ChannelEntity(
                uid = "chan_lol_tech",
                channelName = "CodingFails LOL",
                handleName = "@loltech",
                profilePhotoUrl = "https://api.dicebear.com/7.x/identicon/png?seed=loltech",
                coverPhotoUrl = "https://images.unsplash.com/photo-1579546929518-9e396f3cc809?w=800",
                subscriberCount = 67200,
                videoCount = 42
            ),
            ChannelEntity(
                uid = "chan_cinema_talk",
                channelName = "CinemaTalk",
                handleName = "@cinematalk",
                profilePhotoUrl = "https://api.dicebear.com/7.x/identicon/png?seed=cinematalk",
                coverPhotoUrl = "https://images.unsplash.com/photo-1543536448-d209d2d13a1c?w=800",
                subscriberCount = 148000,
                videoCount = 98
            )
        )
        channels.forEach { channelDao.insertChannel(it) }

        // Seed some realistic, polite starter live comments
        val comments = listOf(
            CommentEntity("c1", "demo_video_1", "Alex K.", "https://api.dicebear.com/7.x/adventurer/png?seed=Alex", "u1", "OMG! Solo Leveling Season 2 is going to break the internet!", System.currentTimeMillis() - 600000),
            CommentEntity("c2", "demo_video_1", "SubAnimeFan", "https://api.dicebear.com/7.x/adventurer/png?seed=Fan", "u2", "Wait, did they change the director? The sequence at 1:12 looks incredible.", System.currentTimeMillis() - 300000),
            CommentEntity("c3", "demo_video_3", "Dev_Kotlin", "https://api.dicebear.com/7.x/adventurer/png?seed=Kotlin", "u3", "Super insightful video. Custom Android architectures really clean up composition times.", System.currentTimeMillis() - 400000),
            CommentEntity("c4", "demo_video_4", "Concentration_Only", "https://api.dicebear.com/7.x/adventurer/png?seed=Study", "u4", "Streaming this while reading compiler error logs. Safe haven.", System.currentTimeMillis() - 900000)
        )
        comments.forEach { commentDao.insertComment(it) }
    }
}
