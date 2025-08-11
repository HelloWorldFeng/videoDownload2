package com.app.videobox.ad.adLoaders


import android.util.Log
import com.app.videobox.ad.base.BaseAd
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.appopen.AppOpenAd
import com.app.videobox.App

class OpenAdmobAdLoader: BaseAd() {
    override fun loadingAd(id: String,type:String) {
        val request = AdManagerAdRequest.Builder().build()
        AppOpenAd.load(
            App.appContext(), id, request,
            object : AppOpenAd.AppOpenAdLoadCallback() {

                override fun onAdLoaded(ad: AppOpenAd) {
                    setFullScreenCallBack(ad)
                    getAdCallBackInstance().loadSuccess(ad)

                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.d("AdLog", "${loadAdError.message}")
                    getAdCallBackInstance().loadFail(loadAdError.code,loadAdError.message)
                }
            }
        )
    }

    private fun setFullScreenCallBack(ad: AppOpenAd) {
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                getAdCallBackInstance().onClose()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                getAdCallBackInstance().onClose()
            }

            override fun onAdShowedFullScreenContent() {
                getAdCallBackInstance().onShow()
            }

            override fun onAdClicked() {
                getAdCallBackInstance().onClick()
            }
        }
    }

}