package com.app.videobox.ad

import android.app.Activity
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

object UmpHelper {

    private lateinit var consentInformation: ConsentInformation

    fun requestUmp(context: Activity, block: () -> Unit = {}) {
//        val debugSettings = ConsentDebugSettings.Builder(context)
//            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
//            .addTestDeviceHashedId("B3EEABB8EE11C2BE770B684D95219ECB")
//            .build()

        val params = ConsentRequestParameters.Builder()
//            .setConsentDebugSettings(debugSettings)
            .build()

        consentInformation = UserMessagingPlatform.getConsentInformation(context)
        consentInformation.reset()

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