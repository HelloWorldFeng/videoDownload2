package com.app.videobox.network.model

data class BaseResponse<T>(
    val status: Int,
    val msg: String,
    val ext: SysTime,
    val model: T
)

data class SysTime(
    val systime: Long
)
