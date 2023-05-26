package com.example.myapplication.retrofit

import com.example.myapplication.BuildConfig
import com.example.myapplication.service.AilabtoolsService
import com.example.myapplication.service.LuxadService
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.Headers
import java.io.File


object RetrofitApi {
    const val AILABTOOLS_BASE_URL = "https://www.ailabapi.com/"
    const val LUXAD_BASE_URL = "https://api.luxand.cloud/"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    private val retrofit_for_bgrm: Retrofit by lazy {
        Retrofit.Builder()
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .baseUrl(AILABTOOLS_BASE_URL)
            .build()
    }

    private val retrofit_luxad : Retrofit by lazy{
        Retrofit.Builder()
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .baseUrl(LUXAD_BASE_URL)
            .build()
    }

    val getAilabtoolsService: AilabtoolsService by lazy {
        retrofit_for_bgrm.create(AilabtoolsService::class.java)
    }

    val getLuxadService : LuxadService by lazy{
        retrofit_luxad.create(LuxadService::class.java)
    }
}