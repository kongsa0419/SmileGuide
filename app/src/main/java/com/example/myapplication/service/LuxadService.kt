package com.example.myapplication.service

import com.example.myapplication.BuildConfig
import com.example.myapplication.dto.AilabApiResponse
import com.example.myapplication.dto.LuxadApiResponse
import okhttp3.MultipartBody
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

// API : https://luxand.cloud/emotion-recognition-api
interface LuxadService {
    @Multipart
    @Headers("token:${BuildConfig.api_key_luxand}")
    @POST("photo/emotions")
    suspend fun getEmotionResult(
        @Part  photo : MultipartBody.Part
    ) : LuxadApiResponse
}