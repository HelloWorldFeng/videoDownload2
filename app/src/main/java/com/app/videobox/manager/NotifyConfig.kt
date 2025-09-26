package com.app.videobox.manager

data class NotifyConfig(
    val notifyScene: List<NotifyScene>
)

data class NotifyScene(
    val limitCount: Int,
    val openBtn: Boolean,
    val scene: String
)

data class NotifyWrapper (
    val openBtn: Boolean,
    val limitCount: Int
)