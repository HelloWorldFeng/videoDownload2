package com.app.videobox.manager

data class NotifyConfig(
    val notifyScene: List<NotifyScene>,
    val androidVersion: List<AndroidVersion>
)

data class AndroidVersion(
    val notifyLimit: Int,
    val androidCode:Int,
    val openBtn: Boolean,
)
data class NotifyScene(
    val limitCount: Int,
    val openBtn: Boolean,
    val scene: String
)


data class AndroidVersionWrapper(
    val openBtn: Boolean,
    val notifyLimit: Int,

)
data class NotifyWrapper (
    val openBtn: Boolean,
    val limitCount: Int
)