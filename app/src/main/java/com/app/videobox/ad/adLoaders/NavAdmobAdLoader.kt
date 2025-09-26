package com.app.videobox.ad.adLoaders

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.app.videobox.R
import com.app.videobox.ad.base.BaseAd
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.app.videobox.App
import com.google.android.gms.ads.nativead.MediaView

class NavAdmobAdLoader : BaseAd() {
    override fun loadingAd(id: String,  type: String) {
        val loader = AdLoader.Builder(App.appContext(), id)
            .forNativeAd { nativeAd ->
                getAdCallBackInstance().loadSuccess(nativeAd)
            }
            .withAdListener(object : AdListener() {
                override fun onAdClicked() {
                    super.onAdClicked()
                    getAdCallBackInstance().onClick()
                }

                override fun onAdFailedToLoad(p0: LoadAdError) {
                    super.onAdFailedToLoad(p0)
                    getAdCallBackInstance().loadFail(p0.code, p0.message)
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                    getAdCallBackInstance().onShow()
                }
            })
        loader.build().loadAd(AdRequest.Builder().build())
    }

    companion object {

        fun fillNavMaterial(context: Context, viewGroup: ViewGroup, nativeAd: NativeAd,isBigStyle: Boolean) {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val adView = if (isBigStyle){
                inflater.inflate(R.layout.nav_big_layout, null) as NativeAdView
            }else{
                inflater.inflate(R.layout.nav_layout, null) as NativeAdView
            }
            val adTitle = adView.findViewById<TextView>(R.id.ad_title)
            val adContent = adView.findViewById<TextView>(R.id.ad_content)
            val adIcon = adView.findViewById<ImageView>(R.id.ad_icon)
            val install = adView.findViewById<TextView>(R.id.ad_call)

            if (isBigStyle) {
                val adMedia = adView.findViewById<MediaView>(R.id.mediaView)
                adMedia.setImageScaleType(ImageView.ScaleType.CENTER_CROP)
                nativeAd.mediaContent?.let { adMedia.mediaContent = it }

                adMedia.setImageScaleType(ImageView.ScaleType.FIT_XY)
                nativeAd.mediaContent?.let {
                    adMedia.mediaContent = it
                    if (it.hasVideoContent()) {
                        val layoutParams = adMedia.layoutParams as ConstraintLayout.LayoutParams
                        // 设置宽高比例，替换 ratio 为您想要设置的具体比例值
                        layoutParams.dimensionRatio = it.aspectRatio.toString() // 替换 "16:9" 为您想要的宽高比例
                        // 应用布局参数的更改
                        adMedia.layoutParams = layoutParams
                    }

                }
                adView.mediaView= adMedia
            }

            adContent.text = nativeAd.body
            adTitle.text = nativeAd.headline
            install.text = nativeAd.callToAction
            nativeAd.icon?.let {
                adIcon.setImageDrawable(it.drawable)
                adIcon.setBackgroundColor(Color.TRANSPARENT)
            }

            adView.headlineView = adTitle
            adView.bodyView = adContent
            adView.iconView = adIcon
            adView.callToActionView = install

            adView.setNativeAd(nativeAd)
            viewGroup.removeAllViews()
            viewGroup.addView(adView)
        }
    }
}