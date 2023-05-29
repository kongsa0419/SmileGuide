package com.example.myapplication.service

import com.example.myapplication.BuildConfig
import com.example.myapplication.dto.AilabApiResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*


// INFO : https://velog.io/@dev_thk28/Android-Retrofit2-Multipart%EC%82%AC%EC%9A%A9%ED%95%98%EA%B8%B0-Java
interface AilabtoolsService {
    @Multipart
    @Headers("ailabapi-api-key: ${BuildConfig.api_key_ailabtools_cha}")
    @POST("api/cutout/portrait/portrait-background-removal")
    suspend fun getBackRmvdImg(
        @Part image : MultipartBody.Part,
        @Part("return_form") return_form : RequestBody?
    ): AilabApiResponse





    @Multipart
    @Headers("ailabapi-api-key:${BuildConfig.api_key_ailabtools_cha}")
    @POST("api/portrait/effects/emotion-editor")
    suspend fun getChangedImg(
        @Part image_target : MultipartBody.Part,
        @Part("service_choice") return_form : Int?=0
    ) : AilabApiResponse
}
