package com.app.videobox.network.model

data class MediaClass(
    val categoryName: String,
    val description: String,
    val icon: String,
    val id: Int,
    val sort: Int,
    val videoCount: Int
)