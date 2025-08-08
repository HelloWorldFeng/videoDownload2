package com.app.videobox.network.model

data class HomeUrlModel(
    val urlList: List<WebsiteItem>
)

data class WebsiteItem(
    val icon: String,
    val name:String,
    val url: String
)