package com.app.videobox.ad

interface AdCallBack {

    fun loadSuccess(adInstance: Any)
    fun loadFail(code: Int, msg: String)
    fun onShow()
    fun onClose()
    fun onClick()

}