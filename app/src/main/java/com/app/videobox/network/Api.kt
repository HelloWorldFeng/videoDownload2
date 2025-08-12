package com.app.videobox.network

import com.app.videobox.network.model.AdultModel
import com.google.gson.JsonElement
import com.app.videobox.network.model.HomeUrlModel
import com.app.videobox.network.model.MediaVideo
import com.app.videobox.network.model.BaseResponse
import com.app.videobox.network.model.JbLikeModel
import com.app.videobox.network.model.MediaClass
import com.app.videobox.network.model.NotifyModel
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.QueryMap

interface Api {

    /** 获取分类数据 */
    @POST("V2/Video/GetVideoCategories")
    suspend fun getMediaClass(@Body map: Map<String, @JvmSuppressWildcards Any>): BaseResponse<List<MediaClass>>


    /** 获取分类下的视频数据 */
    @POST("V2/Video/GetVideosByCategory")
    suspend fun getMediaList(@Body map: Map<String, @JvmSuppressWildcards Any>): BaseResponse<List<MediaVideo>>

    @POST("/V2/Video/GetHomeUrl")
    suspend fun getHomeWebUrl(@Body map: Map<String, @JvmSuppressWildcards Any>): BaseResponse<HomeUrlModel>

    @POST("https://center.langtranspro.com/v1/Center/JbLike")
    suspend fun jbLike(@Body map: Map<String, @JvmSuppressWildcards Any>): BaseResponse<JbLikeModel>

    @POST("/V1/Init/InitWork")
    suspend fun initWork(@Body map: Map<String, @JvmSuppressWildcards Any>): JsonElement


    @POST("/V2/PushMessage/GetPushMessageData")
    suspend fun GetPushMessageData(@Body map: Map<String, @JvmSuppressWildcards Any>): BaseResponse<NotifyModel>


    @GET("/V1/User/CheckUser")
    suspend fun checkAdultUserModel(
        @QueryMap map: Map<String, @JvmSuppressWildcards Any>
    ): BaseResponse<AdultModel>

    @POST("/V2/Report/CallBack")
    suspend fun feedbackApi(@Body map: Map<String, @JvmSuppressWildcards Any>): JsonElement
}