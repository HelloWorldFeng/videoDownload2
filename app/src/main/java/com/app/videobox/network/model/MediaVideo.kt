package com.app.videobox.network.model

data class MediaVideo(
    val author: String,
    val id: Int,
    val imageURL: String,
    val link: String,
    val publishDate: String,
    val title: String,
    val videoURL: String,
    val urlType: Int
)