package com.app.videobox.network

import com.google.gson.JsonElement
import com.app.videobox.network.model.HomeUrlModel
import com.app.videobox.network.model.MeidaVideo
import com.app.videobox.network.model.BaseResponse
import com.app.videobox.network.model.JbLikeModel
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.QueryMap

interface Api {

    /** 获取分类数据 */
    @POST("V2/Video/GetVideoCategories")
    suspend fun getMediaClass(@Body map: Map<String, @JvmSuppressWildcards Any>): JsonElement


    /** 获取分类下的视频数据 */
    @POST("V2/Video/GetVideosByCategory")
    suspend fun getMediaList(@Body map: Map<String, @JvmSuppressWildcards Any>): BaseResponse<List<MeidaVideo>>

    @POST("/V2/Video/GetHomeUrl")
    suspend fun getHomeWebUrl(@Body map: Map<String, @JvmSuppressWildcards Any>): BaseResponse<HomeUrlModel>

    @POST("https://center.vdownloaderhd.com/v1/Center/JbLike")
    suspend fun jbLike(@Body map: Map<String, @JvmSuppressWildcards Any>): BaseResponse<JbLikeModel>

    @POST("/V1/Init/InitWork")
    suspend fun initWork(@Body map: Map<String, @JvmSuppressWildcards Any>): JsonElement


}