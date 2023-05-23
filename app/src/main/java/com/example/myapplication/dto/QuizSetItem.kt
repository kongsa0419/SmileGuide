package com.example.myapplication.dto


import com.google.gson.annotations.SerializedName

class QuizSet : ArrayList<QuizSetItem>()

data class QuizSetItem(
    @SerializedName("answer")
    val answer: String,
    @SerializedName("context")
    val context: String,
    @SerializedName("explanation")
    val explanation: String,
    @SerializedName("options")
    val options: List<String>,
    @SerializedName("problem")
    val problem: String
)

