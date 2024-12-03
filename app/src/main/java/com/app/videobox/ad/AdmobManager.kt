package com.app.videobox.ad

import android.app.Activity
import android.util.Log
import com.app.videobox.BuildConfig
import com.app.videobox.ad.AdConst.CLICK_COUNT
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.SPStaticUtils
import com.app.videobox.ad.base.AD_TYPE_START
import com.app.videobox.ad.base.AdPlaceTag
import com.app.videobox.ad.base.AdUnitWrapper
import java.util.TimeZone
import java.util.concurrent.ConcurrentLinkedQueue

object AdmobManager {

    private const val TAG = "AdLog"
    private lateinit var adConfigMap: MutableMap<String, AdUnitWrapper>
    val smallAdPool = ConcurrentLinkedQueue<AdUnitWrapper>()
    val fullAdPool = ConcurrentLinkedQueue<AdUnitWrapper>()
    private var skipTag = mutableListOf<String>()

    var navGetCallback:((AdUnitWrapper)->Unit)? = null

    var closeBlock: (() -> Unit)? = null
    var finishLoadBlock: ((Boolean) -> Unit)? = null
    var permissionNav: Boolean = true
    var navClickCountLimit: Int = 3
    var nowClickCount: Int = SPStaticUtils.getInt(CLICK_COUNT, 0)


    var nowShowAdScene: String? = null //当前展示的广告场景

    fun initAdMapConfig(adMap: MutableMap<String, AdUnitWrapper>) {
        adConfigMap = adMap

        val showTime = SPStaticUtils.getLong("show_time",0)
        if (getTodayTime() - showTime >= 1) {
            //每个自然日重置限制
            adConfigMap.map {
                it.value.innerAdList.forEach {
                    SPStaticUtils.put("${it.ad_local}_show",0)
                }
            }
        }
    }


    fun loadAdmobInstance(vararg adTypeTag: String) {
        if (!this::adConfigMap.isInitialized) {
            Log.d(TAG, "广告配置没初始化好")
            return
        }
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
                    skipTag.addIfAbsent(adWrapper.place.getPlaceString())
                }
            },
            failCallBack = { code, msg, adWrapper ->
                Log.d(TAG, "${adWrapper.type} 请求失败 code:$code msg:$msg id:${adWrapper.getAdSourceId()} ")
                adWrapper.adLoading = false
                if (adWrapper.type == AD_TYPE_START) skipTag.add(AD_TYPE_START)
            },
            showCallBack = { adWrapper ->
                val scene = getAdConfigByScene( nowShowAdScene?:"",adWrapper.type)

                //记录这个位置的展示次数
                if (scene != null && nowShowAdScene != null) {
                    scene.apply {
                        this.nowShowCount = SPStaticUtils.getInt("${nowShowAdScene}_show",0) + 1
                        SPStaticUtils.put("${nowShowAdScene}_show",this.nowShowCount)

                        SPStaticUtils.put("show_time",System.currentTimeMillis())
                    }
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

        skipTag.clear()
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
        adPlaceTag: AdPlaceTag,
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
                    Log.d(TAG, "${adPlaceTag.getPlaceString()}广告位关闭，不请求")
                }
            }

        }

        smallAdPool.find { it.type == adType }?.let { adWrapper->
            //找到配置中的广告位
            adWrapper.innerAdList.find { it.ad_local == adScene }.let {
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
        emptyAction: (() -> Unit)? = null,
        finishLoadAction: ((Boolean) -> Unit) ?= null
    ) {

        if (!this::adConfigMap.isInitialized || !AppUtils.isAppForeground()) {
            closeAction.invoke()
            return
        }

        nowShowAdScene = adScene
        //检查这个场景 限制条件权限 true达到限制
        val showBool = checkAdSceneShow(adScene,adType)
        if (showBool) {
            Log.d(TAG, "展示限制:${adScene}")
            closeAction.invoke()
            return
        }
        val clickBool = checkAdSceneClick(adScene, adType)
        if (clickBool) {
            Log.d(TAG, "点击限制:${adScene}")
            closeAction.invoke()
            return
        }
        //广告场景开关
        val sceneBool = getAdConfigByScene(adScene,adType)?.showBtn ?: true
        if (sceneBool.not()) {
            closeAction.invoke()
            return
        }

        closeBlock = closeAction
        if (fullAdPool.none { it.type == adType }) {
            //广告池里 没有这个类型
            adConfigMap[adType]?.let {
                if (it.openBtn) {
                    loadAdmobInstance(adType)
                } else {
                    Log.d(TAG, "${adType}广告位关闭，不请求")
                    closeAction.invoke()
                    return
                }
            }
            if (emptyAction == null) {
                closeAction.invoke()
            }else{
                this.finishLoadBlock = finishLoadAction
                emptyAction.invoke()
            }

            return
        }

        fullAdPool.find { it.type == adType }?.let { adWrapper->
            //找到配置中的广告位
            adWrapper.innerAdList.find { it.ad_local == adScene }.let {
                fullAdPool.remove(adWrapper)
                adWrapper.showFullAd(activity)
            }

            return
        }
        closeAction.invoke()
    }

    //检查展示限制
    private fun checkAdSceneShow(adScene: String, adType: String): Boolean {
        val limitShowCount = getAdConfigByScene(adScene,adType)?.showCount?:10
        val nowShowCount = SPStaticUtils.getInt("${adScene}_show", 0)

        return nowShowCount >= limitShowCount
    }

    //检查点击限制
    private fun checkAdSceneClick(adScene: String, adType: String): Boolean {
        val limitShowCount = getAdConfigByScene(adScene,adType)?.clickCount?:10
        val nowShowCount = SPStaticUtils.getInt("${adScene}_click", 0)

        return nowShowCount >= limitShowCount
    }

    //获取广告场景配置
    private fun getAdConfigByScene(adScene: String, adType: String): InnerAd? {
        return adConfigMap[adType]?.innerAdList?.find { it.ad_local == adScene }
    }


}