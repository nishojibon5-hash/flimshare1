package com.example.data

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*

interface DoodStreamService {

    @GET("api/upload/server")
    suspend fun getUploadServer(
        @Query("key") apiKey: String
    ): Response<DoodServerResponse>

    @Multipart
    @POST
    suspend fun uploadFileDirect(
        @Url uploadServerUrl: String,
        @Part("api_key") apiKey: RequestBody,
        @Part file: MultipartBody.Part,
        @Part("title") title: RequestBody? = null
    ): Response<DoodUploadResponse>

    @GET("api/upload/url")
    suspend fun remoteUpload(
        @Query("key") apiKey: String,
        @Query("url") remoteUrl: String,
        @Query("title") title: String? = null
    ): Response<DoodRemoteResponse>

    companion object {
        private const val BASE_URL = "https://doodapi.co/"

        fun create(): DoodStreamService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(DoodStreamService::class.java)
        }
    }
}

// Response Models
data class DoodServerResponse(
    val msg: String,
    val result: String?, // This contains the dynamic upload server URL
    val status: Int
)

data class DoodUploadResponse(
    val msg: String,
    val result: List<DoodUploadResult>?,
    val status: Int
)

data class DoodUploadResult(
    val filecode: String,
    val download_url: String,
    val title: String,
    val size: String
)

data class DoodRemoteResponse(
    val msg: String,
    val result: DoodRemoteResult?,
    val status: Int
)

data class DoodRemoteResult(
    val filecode: String?,
    val file_id: String?
)
