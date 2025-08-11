package com.app.videobox.ad.callback

interface AdCallBack {

    fun onShow()
    fun onClose()
    fun onClick()

    fun loadSuccess(adInstance: Any)
    fun loadFail(code: Int, msg: String)


}