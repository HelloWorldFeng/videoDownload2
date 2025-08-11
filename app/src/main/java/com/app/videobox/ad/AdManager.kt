package com.app.videobox.ad

import android.app.Activity
import android.util.Log
import com.app.videobox.ad.base.AD_TYPE_INT
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.SPStaticUtils
import com.app.videobox.ad.base.AD_TYPE_START
import com.app.videobox.ad.base.AdUnitWrapper
import com.app.videobox.ad.base.InnerAd
import com.app.videobox.utils.EventReportUtils
import java.util.TimeZone
import java.util.concurrent.ConcurrentLinkedQueue

object AdManager {

    private const val TAG = "AdLog"
    private lateinit var adConfigMap: MutableMap<String, AdUnitWrapper>
    val smallAdPool = ConcurrentLinkedQueue<AdUnitWrapper>()
    val fullAdPool = ConcurrentLinkedQueue<AdUnitWrapper>()
    private var skipTag = mutableListOf<String>()

    var navGetCallback:((AdUnitWrapper)->Unit)? = null

    var closeBlock: (() -> Unit)? = null
    var finishLoadBlock: ((Boolean) -> Unit)? = null

    var nowShowAdScene: String? = null //当前展示的广告场景

    var openShowTime = 0L
    var intShowTime = 0L


    fun initAdMapConfig(adMap: MutableMap<String, AdUnitWrapper>) {
        try {
            Log.d(TAG, "覆盖了广告配置------》: ")

            if (::adConfigMap.isInitialized.not()) {
                adConfigMap = adMap
            }else{
                adConfigMap.forEach {
                    it.value.innerAdList = adMap[it.key]?.innerAdList!!
                    it.value.type = adMap[it.key]?.type!!
                    it.value.openBtn = adMap[it.key]?.openBtn!!
                }
            }

            val showTime = SPStaticUtils.getLong("show_time",0)
            if (getTodayTime() - showTime >= 1) {
                //每个自然日重置限制
                SPStaticUtils.put("${AD_TYPE_START}_show", 0)
                SPStaticUtils.put("${AD_TYPE_INT}_show",0)
                SPStaticUtils.put("${AD_TYPE_START}_request",0)
                SPStaticUtils.put("${AD_TYPE_INT}_request",0)
                adConfigMap.map {
                    it.value.innerAdList.forEach {
                        SPStaticUtils.put("${it.adScene}_show",0)
                    }
                }
            }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }


    fun loadAdmobInstance(vararg adTypeTag: String) {
        if (!this::adConfigMap.isInitialized) {
            Log.d(TAG, "广告配置没初始化好")
            return
        }
        skipTag.clear()
        //过期清除
        fullAdPool.removeAll(
            fullAdPool.filter {
                System.currentTimeMillis() - it.time > 60 * 60 * 1000
            }.toSet()
        )

        smallAdPool.removeAll(
            smallAdPool.filter {
                System.currentTimeMillis() - it.time > 60 * 60 * 1000
            }.toSet()
        )


        AdLoaderHelper(adConfigMap).setAdCallback(
            successCallback = { adWrapper ->
                Log.d(TAG, "${adWrapper.type} 请求成功 大广告池里数量:${fullAdPool.size} 小广告池里数量:${smallAdPool.size}, hashCode:${adWrapper.getAdInstance().hashCode()}")
                if (adWrapper.type in listOf(AD_TYPE_START)) {
                    skipTag.addIfAbsent(adWrapper.type)
                }
                EventReportUtils.afEventLog(eventName = "ud_ad_action_fill", mutableMapOf<String, Any>().apply {
                    put("ad_action",11)
                    put("ad_format",adWrapper.type)
                    put("ad_unit_id",adWrapper.adNumberId)
                })
            },
            failCallBack = { code, msg, adWrapper ->
                Log.d(TAG, "${adWrapper.type} 请求失败 code:$code msg:$msg id:${adWrapper.getAdSourceId()} ")
                adWrapper.adLoading = false
                if (adWrapper.type in listOf(AD_TYPE_START)) {
                    skipTag.addIfAbsent(adWrapper.type)
                }
                EventReportUtils.afEventLog(eventName = "ud_ad_action_fill", mutableMapOf<String, Any>().apply {
                    put("ad_action",12)
                    put("ad_format",adWrapper.type)
                    put("ad_unit_id",adWrapper.adNumberId)
                    put("err_msg","$msg")
                })
            },
            showCallBack = { adWrapper ->
                val scene = getAdConfigByScene( nowShowAdScene?:"",adWrapper.type)
                //记录这个类型的展示次数
                val typeCount = SPStaticUtils.getInt("${adWrapper.type}_show",0) + 1
                SPStaticUtils.put("${adWrapper.type}_show",typeCount)

                //记录这个位置的展示次数
                if (scene != null && nowShowAdScene != null) {
                    scene.apply {
                        this.nowShowCount = SPStaticUtils.getInt("${nowShowAdScene}_show",0) + 1
                        SPStaticUtils.put("${nowShowAdScene}_show",this.nowShowCount)

                        SPStaticUtils.put("show_time",System.currentTimeMillis())
                    }
                }

                //记录开屏展示的时间戳
                if (adWrapper.type == AD_TYPE_START) {
                    openShowTime = System.currentTimeMillis()
                }
                if (adWrapper.type == AD_TYPE_INT) {
                    intShowTime = System.currentTimeMillis()
                }

                Log.d(TAG, "${adWrapper.type} 展示成功 id:${adWrapper.getAdSourceId()}, scene:${nowShowAdScene} hashCode:${adWrapper.getAdInstance().hashCode()},展示次数:${scene?.nowShowCount},限制次数:${scene?.showCount}")
            },
            clickCallBack = {
                val scene = getAdConfigByScene( nowShowAdScene?:"",it.type)
                //记录这个的位置的点击次数

                if (scene != null && nowShowAdScene != null) {
                    scene.apply {
                        this.nowClickCount = SPStaticUtils.getInt("${nowShowAdScene}_click",0) + 1
                        SPStaticUtils.put("${nowShowAdScene!!}_click",this.nowClickCount)
                    }
                }

                Log.d(TAG, "当前展示的广告场景:${nowShowAdScene} 点击次数:${scene?.nowClickCount}")

                EventReportUtils.afEventLog(eventName = "ud_ad_action_impression", mutableMapOf<String, Any>().apply {
                    put("ad_action",41)
                    put("ad_format",it.type)
                    put("ad_unit_id",it.adNumberId)
                    scene?.let {
                        put("ad_scenes",scene.adScene)
                    }
                })
            },
            closeCallBack ={
                Log.d(TAG, "当前关闭的广告场景:${nowShowAdScene} ")

                val scene = getAdConfigByScene( nowShowAdScene?:"",it.type)
                EventReportUtils.afEventLog(eventName = "ud_ad_action_impression", mutableMapOf<String, Any>().apply {
                    put("ad_action",51)
                    put("ad_format",it.type)
                    put("ad_unit_id",it.adNumberId)
                    scene?.let {
                        put("ad_scenes",scene.adScene)
                    }
                })
            }
        ).loadAdInstance(*adTypeTag)
    }

    fun touchFinishBlock() {
        finishLoadBlock?.invoke(false)
        finishLoadBlock = null
    }

    private fun getTodayTime(): Long {
        val currentTime = System.currentTimeMillis()
        return currentTime - (currentTime + TimeZone.getDefault().rawOffset) % (24 * 60 * 60 * 1000L)
    }

    fun canSpeedAnim(): Boolean {
        if (!this::adConfigMap.isInitialized) return false

        adConfigMap[AD_TYPE_START]?.openBtn?.let { if (!it) skipTag.add(AD_TYPE_START) }

        if (fullAdPool.any { it.type == AD_TYPE_START }) {
            skipTag.add(AD_TYPE_START)
        }


        return AD_TYPE_START in skipTag
    }

    private fun MutableList<String>.addIfAbsent(element: String) {
        if (!contains(element)) add(element)
    }

    fun getSmallAdFromPool(
        adScene:String = "", //广告展示场景
        adType:String = "", //从池里拿什么类型
        block:(AdUnitWrapper)->Unit) {
        if (!this::adConfigMap.isInitialized || !AppUtils.isAppForeground()) return

        navGetCallback = block
        nowShowAdScene = adScene

        //检查这个场景 限制条件权限 true达到限制
        val showBool = checkAdSceneShow(adScene,adType)
        if (showBool) {
            Log.d(TAG, "展示限制:${adScene}")
            return
        }
        val clickBool = checkAdSceneClick(adScene, adType)
        if (clickBool) {
            Log.d(TAG, "点击限制:${adScene}")
            return
        }
        //广告场景开关
        val sceneBool = getAdConfigByScene(adScene,adType)?.showBtn ?: true
        if (sceneBool.not()) {
            return
        }

        if (smallAdPool.none { it.type == adType }) {
            //广告池里 没有这个类型
            adConfigMap[adType]?.let {
                if (it.openBtn) {
                    AdLoaderHelper.needFillNavAd = true
                    loadAdmobInstance(adType)
                } else {
                    Log.d(TAG, "${adScene}广告位关闭，不请求")
                }
            }

        }

        smallAdPool.find { it.type == adType }?.let { adWrapper->
            //找到配置中的广告位
            adWrapper.innerAdList.find { it.adScene == adScene }.let {
                smallAdPool.remove(adWrapper)
                navGetCallback?.invoke(adWrapper)
                AdLoaderHelper.needFillNavAd = false
            }
            return
        }

    }


    fun getFullAdFromPool(
        activity: Activity,
        adScene:String = "", //广告展示场景
        adType:String = "", //从池里拿什么类型
        closeAction: () -> Unit,
    ) {

        if (!this::adConfigMap.isInitialized || !AppUtils.isAppForeground()) {
            closeAction.invoke()
            return
        }

        this.nowShowAdScene = adScene

        //检查这个场景 限制条件权限 true达到限制
        val showBool = checkAdSceneShow(adScene,adType)
        if (showBool) {
            EventReportUtils.afEventLog(eventName = "ud_ad_action_impression", mutableMapOf<String, Any>().apply {
                put("ad_action",32)
                put("error_code",1002)
                put("ad_format",adType)
                put("ad_scenes",adScene)
                put("err_msg","$adScene showBool limit")
            })
            Log.d(TAG, "场景展示限制:${adScene}")
            closeAction.invoke()
            return
        }
        if (checkAdTypeShow(adType)) {
            Log.d(TAG, "类型展示限制:${adType}")
            EventReportUtils.afEventLog(eventName = "ud_ad_action_impression", mutableMapOf<String, Any>().apply {
                put("ad_action",32)
                put("error_code",1002)
                put("ad_format",adType)
                put("ad_scenes",adScene)
                put("err_msg","$adScene one day limit")
            })
            closeAction.invoke()
            return
        }

        val clickBool = checkAdSceneClick(adScene, adType)
        if (clickBool) {
            EventReportUtils.afEventLog(eventName = "ud_ad_action_impression", mutableMapOf<String, Any>().apply {
                put("ad_action",32)
                put("error_code",1002)
                put("ad_format",adType)
                put("ad_scenes",adScene)
                put("err_msg","$adScene clickBool limit")
            })
            Log.d(TAG, "点击限制:${adScene}")
            closeAction.invoke()
            return
        }
        //广告场景开关
        val sceneBool = getAdConfigByScene(adScene,adType)?.showBtn ?: true
        if (sceneBool.not()) {
            Log.d(TAG, "广告场景关:${adScene}")
            EventReportUtils.afEventLog(eventName = "ud_ad_action_impression", mutableMapOf<String, Any>().apply {
                put("ad_action",32)
                put("error_code",1002)
                put("ad_format",adType)
                put("ad_scenes",adScene)
                put("err_msg","$adScene scene btn close")
            })
            closeAction.invoke()
            return
        }
        //间隔限制
        val interval = checkInterval(adScene, adType)
        if (interval) {
            EventReportUtils.afEventLog(eventName = "ud_ad_action_impression", mutableMapOf<String, Any>().apply {
                put("ad_action",32)
                put("error_code",1001)
                put("ad_format",adType)
                put("ad_scenes",adScene)
                put("err_msg","$adScene interval limit")
            })
            closeAction.invoke()
            return
        }
        closeBlock = closeAction
        if (fullAdPool.none { it.type == adType }) {
            //广告池里 没有这个类型
            adConfigMap[adType]?.let {
                if (it.openBtn) {
                    EventReportUtils.afEventLog(eventName = "ud_ad_action_impression", mutableMapOf<String, Any>().apply {
                        put("ad_action",32)
                        put("error_code",10011)
                        put("ad_format",adType)
                        put("ad_scenes",adScene)
                        put("err_msg","$adType Btn Close")
                    })
                    loadAdmobInstance(adType)
                } else {
                    Log.d(TAG, "${adType}广告位关闭，不请求")
                    closeAction.invoke()
                    return
                }
            }
        }

        fullAdPool.find { it.type == adType }?.let { adWrapper->
            //找到配置中的广告位
            adWrapper.innerAdList.find { it.adScene == adScene }.let {
                if (AdUtils.canCallShow(adType)) {
                    fullAdPool.remove(adWrapper)
                    adWrapper.showAdFull(activity)
                    Log.d(TAG, "${adType}展示剩余次数:${AdUtils.getRemainingShowCalls(adType)}")
                    return
                }else{
                    Log.d(TAG, "${adType} 开屏不能调用,展示剩余次数:${AdUtils.getRemainingShowCalls(adType)}")
                }

            }

        }
        //开屏类型没有广告的时候 拿插页去顶
        if (adType == AD_TYPE_START) {
            fullAdPool.find { it.type == AD_TYPE_INT }?.let { adWrapper->
                if (AdUtils.canCallShow(AD_TYPE_INT)) {
                    fullAdPool.remove(adWrapper)
                    adWrapper.showAdFull(activity)
                    Log.d(TAG, "插页展示剩余次数:${AdUtils.getRemainingShowCalls(AD_TYPE_INT)}")
                    return
                }else{
                    Log.d(TAG, "插页不能调用,展示剩余次数:${AdUtils.getRemainingShowCalls(AD_TYPE_INT)}")
                }
            }

        }
        closeAction.invoke()
    }

    //检查展示限制
    private fun checkAdSceneShow(adScene: String, adType: String): Boolean {
        val limitShowCount = getAdConfigByScene(adScene,adType)?.showCount?:10
        val nowShowCount = SPStaticUtils.getInt("${adScene}_show", 0)

        return nowShowCount >= limitShowCount
    }

    //检查展示限制
    private fun checkAdTypeShow(adType: String): Boolean {
        val nowShowCount = SPStaticUtils.getInt("${adType}_show", 0)

        if (adType == AD_TYPE_START && AdUtils.getOpenShowCountDay() != 0) {
            return nowShowCount >= AdUtils.getOpenShowCountDay()
        }
        if (adType == AD_TYPE_INT && AdUtils.getIntShowCountDay() != 0) {
            return nowShowCount >= AdUtils.getIntShowCountDay()
        }
        return false
    }

    //检查点击限制
    private fun checkAdSceneClick(adScene: String, adType: String): Boolean {
        val limitShowCount = getAdConfigByScene(adScene,adType)?.clickCount?:10
        val nowShowCount = SPStaticUtils.getInt("${adScene}_click", 0)

        return nowShowCount >= limitShowCount
    }

    //获取广告场景配置
    private fun getAdConfigByScene(adScene: String, adType: String): InnerAd? {
        return adConfigMap[adType]?.innerAdList?.find { it.adScene == adScene }
    }

    //间隔多少秒限制
    private fun checkInterval(adScene: String, adType: String): Boolean {

        //true 不展示
        if (adType == AD_TYPE_START) {
            if (AdUtils.getOpenInterval() <= 0L) {
                return false
            }
            if ((System.currentTimeMillis() - openShowTime < AdUtils.getOpenInterval())) {
                Log.d(TAG, "${adType}:展示时间限制---->${AdUtils.getOpenInterval() / 1000L}")
                return true
            }
            return false
        }

        if (adType == AD_TYPE_INT) {
            if (AdUtils.getIntInterval() <= 0L) {
                return false
            }
            if ((System.currentTimeMillis() - intShowTime < AdUtils.getIntInterval())) {
                Log.d(TAG, "${adType}:展示时间限制---->${AdUtils.getIntInterval() / 1000L}")
                return true
            }
            return false
        }
        return false
    }
}