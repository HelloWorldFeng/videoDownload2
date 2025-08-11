package com.app.videobox.network.model

data class NotifyModel(
    val content: String,
    val id: Int,
    val imageUrl: String,
    val title: String,
    val videoUrl: String,
    val pageType: Int,
)