package com.app.videobox.ad

import android.app.Activity
import com.app.videobox.BuildConfig
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

object UmpHelper {

    private lateinit var consentInformation: ConsentInformation

    fun requestUmp(context: Activity, block: () -> Unit = {}) {
        val debugSettings = ConsentDebugSettings.Builder(context)
            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            .addTestDeviceHashedId("B589F61669214E3E49ABF1B1166919C4")
            .build()

        val params = ConsentRequestParameters.Builder()
//            .setConsentDebugSettings(debugSettings)
            .build()

        consentInformation = UserMessagingPlatform.getConsentInformation(context)
        if (BuildConfig.DEBUG) {
            consentInformation.reset()
        }

        consentInformation.requestConsentInfoUpdate(context, params, {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                context
            ) { loadAndShowError ->
                block()
            }

        }, { requestConsentError ->
            block()
        })
    }
}