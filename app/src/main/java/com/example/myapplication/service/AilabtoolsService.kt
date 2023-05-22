package com.example.myapplication.service

import com.example.myapplication.BuildConfig
import com.example.myapplication.CaptureResult
import com.example.myapplication.dto.AilabApiResponse
import com.example.myapplication.dto.Image
import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*
import java.io.File



// INFO : https://velog.io/@dev_thk28/Android-Retrofit2-Multipart%EC%82%AC%EC%9A%A9%ED%95%98%EA%B8%B0-Java
interface AilabtoolsService {

    @Multipart
    @Headers("ailabapi-api-key: ${BuildConfig.api_key_ailabtools}")
    @POST("api/cutout/portrait/portrait-background-removal")
    fun getBackRmvdImg(
        @Part image : MultipartBody.Part,
        @Part("return_form") return_form : RequestBody?
    ): Call<AilabApiResponse>


//    @Multipart
//    @Headers("ailabapi-api-key:${BuildConfig.api_key_ailabtools}")
//    @POST("cutout/portrait/portrait-background-removal")
//    fun getBackRmvdImg(
//        @PartMap params : Map<String, RequestBody>
//    ): Call<AilabApiResponse>






    // Define the Retrofit interface for the API
    interface ApiService {
        @Multipart
        @POST("portrait/background-removal")
        fun uploadImage(
            @Part image: MultipartBody.Part
        ): Call<AilabApiResponse>
    }










    @Multipart
    @Headers("ailabapi-api-key:${BuildConfig.api_key_ailabtools}")
    @POST("")
    fun getChangedImg(@Part imgFile : MultipartBody.Part) : Call<AilabApiResponse>
}
