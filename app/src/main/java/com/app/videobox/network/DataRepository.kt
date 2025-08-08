package com.app.videobox.network

import android.content.Context
import android.util.Log
import com.app.videobox.App
import com.google.gson.Gson
import com.app.videobox.network.model.HomeUrlModel
import com.app.videobox.network.model.MediaVideo
import com.app.videobox.network.model.BaseResponse
import com.app.videobox.network.model.MediaClass
import com.app.videobox.utils.DeviceUtils
import com.appsflyer.AppsFlyerLib
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.TimeZone

object DataRepository {

    val service by lazy { RetrofitManager.getService(Api::class.java) }

    private val _webUrlFlow = MutableStateFlow<HomeUrlModel?>(null)
    val webUrlFlow: StateFlow<HomeUrlModel?> = _webUrlFlow

    private val _videoClassFlow = MutableStateFlow<List<MediaClass>>(emptyList())
    val videoClassFlow: StateFlow<List<MediaClass>> = _videoClassFlow

    suspend fun getVideoClass(): BaseResponse<List<MediaClass>>? {
        return try {
            val params = ParamsEncryptUtil.encryptData(ParamsEncryptUtil.networkParams)
            val response = service.getMediaClass(params)
            response?.model?.let { classList ->
                _videoClassFlow.value = classList
                Log.d("DataRepository", "Video classes loaded: ${classList.size} categories")
            }
            response
        } catch (e: Exception) {
            Log.e("DataRepository", "getVideoClass error: ${e.message}")
            null
        }
    }
    /**
     * 获取该分类下的视频列表（支持分页）
     * @param page 页码，从1开始
     * @param pageSize 每页大小
     * @return VisionResponse<List<MiniVideo>> 视频列表响应
     */
    suspend fun getVideoList(page: Int, pageSize: Int,categoryId:Int): BaseResponse<List<MediaVideo>>? {
        return try {
            val paramsMap = mutableMapOf(
                "pageIndex" to page,
                "pageSize" to pageSize,
                "categoryId" to categoryId
            )
            val params = ParamsEncryptUtil.encryptData(ParamsEncryptUtil.networkParams, paramsMap)
            service.getMediaList(params)
        } catch (e: Exception) {
            Log.e("DataRepository", "getVideoList error: ${e.message}")
            null
        }
    }


    fun fetchWebUrlList(){
        App.coroutineScope.launch(Dispatchers.IO) {
            launch {
                val params = ParamsEncryptUtil.encryptData(ParamsEncryptUtil.networkParams)
                fetchWebUrlList(params)
                    .catch {

                    }
                    .collect { result ->
                        _webUrlFlow.value = result.model
                    }
            }

            launch {
                try {
                    val json = App.appContext().assets.open("default_url.json").use {
                        return@use it.readBytes().decodeToString()
                    }
                    val model = Gson().fromJson(json, HomeUrlModel::class.java)
                    _webUrlFlow.value = model
                }catch (e: Exception){

                }
            }
        }
    }

    private fun fetchWebUrlList(params: Map<String, Any>): Flow<BaseResponse<HomeUrlModel>> = flow {
        emit(service.getHomeWebUrl(params))
    }


    suspend fun initWork() = withContext(Dispatchers.IO) {
        try {
            val params = ParamsEncryptUtil.encryptData(ParamsEncryptUtil.networkParams)
            val result = service.initWork(params)

            Log.d("TAG", "checkUser:${result} ")
        }catch (e:Exception){
            e.printStackTrace()
        }
    }

    suspend fun jbLike(
        context: Context,
        token:String,
        sourceID: String?=null,
        campaign: String?=null,
        channel: String?=null
    ) = withContext(Dispatchers.IO){
        fun getTimeZoneOffset(): Int {
            val calendar = Calendar.getInstance()
            val timeZone: TimeZone = TimeZone.getDefault()
            return timeZone.getOffset(calendar.timeInMillis) / 3600000 // 转换为小时
        }

        try {
            val adId = try { AdvertisingIdClient.getAdvertisingIdInfo(context).id
            } catch (e: Exception) {
                e.printStackTrace()
                ""
            }
            val afId = AppsFlyerLib.getInstance().getAppsFlyerUID(context)
            val modelInfo = DeviceUtils.getDetailedDeviceInfo()

            val paramsMap = mutableMapOf(
                "gaid" to adId,
                "adId" to adId,
                "appsFlyerId" to afId,
                "registrationToken" to token,
                "model" to modelInfo,

                "timeZone" to getTimeZoneOffset()
            )
            paramsMap.apply {
                sourceID?.let { this.put("mediaSource",sourceID) }
                campaign?.let { this.put("campaign",campaign) }
                channel?.let { this.put("channel",channel) }
            }
            val params = ParamsEncryptUtil.encryptData(ParamsEncryptUtil.networkParams, paramsMap)
            val result = service.jbLike(params)


        }catch (e:Exception){
            e.printStackTrace()
        }
    }

}