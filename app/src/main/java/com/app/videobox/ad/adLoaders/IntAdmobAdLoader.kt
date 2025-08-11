package com.app.videobox.ad.adLoaders


import com.app.videobox.ad.base.BaseAd
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.app.videobox.App

class IntAdmobAdLoader : BaseAd() {

    override fun loadingAd(id: String,  type: String) {
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(App.appContext(),id, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                getAdCallBackInstance().loadFail(adError.code,adError.message)
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                setFullScreenContentCallback(interstitialAd)
                getAdCallBackInstance().loadSuccess(interstitialAd)
            }
        })
    }

    private fun setFullScreenContentCallback(interstitialAd: InterstitialAd) {
        interstitialAd.fullScreenContentCallback = object: FullScreenContentCallback() {
            override fun onAdClicked() {
                getAdCallBackInstance().onClick()
            }

            override fun onAdDismissedFullScreenContent() {
                getAdCallBackInstance().onClose()
            }

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                getAdCallBackInstance().onClose()
            }

            override fun onAdImpression() {
                getAdCallBackInstance().onShow()
            }

            override fun onAdShowedFullScreenContent() {

            }
        }

    }

}