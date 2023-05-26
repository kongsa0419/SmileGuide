package com.example.myapplication.dto


import com.google.gson.annotations.SerializedName

data class LuxadApiResponse(
    @SerializedName("faces")
    val faces: List<Face?>?,
    @SerializedName("status")
    val status: String?
) {
    data class Face(
        @SerializedName("emotions")
        val emotions: Emotions?,
        @SerializedName("rectangle")
        val rectangle: Rectangle?
    ) {
        data class Emotions(
            @SerializedName("anger")
            val anger: Double?,
            @SerializedName("contempt")
            val contempt: Double?,
            @SerializedName("disgust")
            val disgust: Double?,
            @SerializedName("fear")
            val fear: Double?,
            @SerializedName("happiness")
            val happiness: Int?,
            @SerializedName("neutral")
            val neutral: Double?,
            @SerializedName("sadness")
            val sadness: Double?,
            @SerializedName("surprise")
            val surprise: Int?
        )

        data class Rectangle(
            @SerializedName("bottom")
            val bottom: Int?,
            @SerializedName("left")
            val left: Int?,
            @SerializedName("right")
            val right: Int?,
            @SerializedName("top")
            val top: Int?
        )
    }
}