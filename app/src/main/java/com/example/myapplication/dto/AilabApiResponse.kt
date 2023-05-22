package com.example.myapplication.dto

import com.squareup.moshi.Json


data class AilabApiResponse(
    @Json(name = "data")
    val `data`: Data?,
    @Json(name = "error_code")
    val errorCode: Int?,
    @Json(name = "error_code_str")
    val errorCodeStr: Int?,
    @Json(name = "error_msg")
    val errorMsg: String?,
    @Json(name = "log_id")
    val logId: String?,
    @Json(name = "request_id")
    val requestId: String?
)

data class Data(
    @Json(name="image_url")
    val image_url: String?,
    @Json(name = "elements")
    val elements: List<Element?>?
)

data class Element(
    @Json(name = "height")
    val height: Int?,
    @Json(name = "image_url")
    val imageUrl: String?,
    @Json(name = "width")
    val width: Int?,
    @Json(name = "x")
    val x: Int?,
    @Json(name = "y")
    val y: Int?
)

data class Image(
    @Json(name = "image")
    val image: String?
)
