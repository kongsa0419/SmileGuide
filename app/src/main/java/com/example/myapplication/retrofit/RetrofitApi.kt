package com.example.myapplication.retrofit

import com.example.myapplication.BuildConfig
import com.example.myapplication.service.AilabtoolsService
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Headers
import java.io.File


object RetrofitApi {
    const val AILABTOOLS_BASE_URL = "https://www.ailabapi.com/"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    private val retrofit_for_bgrm: Retrofit by lazy {
        Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .baseUrl(AILABTOOLS_BASE_URL)
            .build()
    }

    val getAilabtoolsService: AilabtoolsService by lazy {
        retrofit_for_bgrm.create(AilabtoolsService::class.java)
    }
}