package com.app.videobox.ui.pages.video

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.app.videobox.R
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer

class UgcDetailVideoPlayer : StandardGSYVideoPlayer {

    private var seekRatioIv:TextView?= findViewById(R.id.seek_ratio_iv)
    private var brightProgress:ProgressBar?=null
    private var allWidget:ConstraintLayout = findViewById(R.id.all_widget)
    private var addSecond:TextView = findViewById(R.id.add_second)
    private var reduceSecond:TextView = findViewById(R.id.reduce_second)
    private var fullBtn:ImageView = findViewById(R.id.full_btn)


    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    override fun getLayoutId(): Int {
        return R.layout.layout_ugc_detail_video_player
    }

    init {
        seekRatioIv?.setOnClickListener {
            setSpeedPlaying(13f,true)

        }
        addSecond.setOnClickListener {
            val currentPosition = this.currentPositionWhenPlaying // 获取当前播放位置
            val newPosition = currentPosition + 5 * 1000 // 计算新的播放位置
            this.seekTo(newPosition) // 设置新的播放位置
        }
        reduceSecond.setOnClickListener {
            val currentPosition = this.currentPositionWhenPlaying // 获取当前播放位置
            val newPosition = currentPosition - 5 * 1000 // 计算新的播放位置
            this.seekTo(newPosition) // 设置新的播放位置
        }
    }

    override fun updateStartImage() {
        super.updateStartImage()
    }

    override fun getVolumeLayoutId(): Int {
        return R.layout.video_volume
    }

    override fun showVolumeDialog(deltaY: Float, volumePercent: Int) {
        Log.d("volume", "showVolumeDialog:${deltaY},${volumePercent} ")
        super.showVolumeDialog(deltaY, volumePercent)
    }

    override fun getVolumeProgressId(): Int {
        return super.getVolumeProgressId()
    }

    @SuppressLint("WrongConstant")
    override fun showBrightnessDialog(percent: Float) {
        if (this.mBrightnessDialog == null) {
            val localView = LayoutInflater.from(this.activityContext).inflate(this.brightnessLayoutId, null as ViewGroup?)
            if (localView.findViewById<View>(this.brightnessTextId) is ProgressBar) {
                brightProgress = localView.findViewById<View>(this.brightnessTextId) as ProgressBar
            }

            this.mBrightnessDialog = Dialog(this.activityContext, com.shuyu.gsyvideoplayer.R.style.video_style_dialog_progress)
            mBrightnessDialog.setContentView(localView)
            mBrightnessDialog.window!!.addFlags(8)
            mBrightnessDialog.window!!.addFlags(32)
            mBrightnessDialog.window!!.addFlags(16)
            mBrightnessDialog.window!!.decorView.systemUiVisibility = 2
            mBrightnessDialog.window!!.setLayout(-2, -2)
            val localLayoutParams = mBrightnessDialog.window!!.attributes
            localLayoutParams.gravity = 8388661
            localLayoutParams.width = this.width
            localLayoutParams.height = this.height
            val location = IntArray(2)
            this.getLocationOnScreen(location)
            localLayoutParams.x = location[0]
            localLayoutParams.y = location[1]
            mBrightnessDialog.window!!.attributes = localLayoutParams
        }

        if (!mBrightnessDialog.isShowing) {
            mBrightnessDialog.show()
        }

        if (this.brightProgress != null) {
            brightProgress?.setProgress((percent * 100.0f).toInt())
        }
        Log.d("speed", "showBrightnessDialog:${speed} ")
    }

    override fun getBrightnessTextId(): Int {
        return R.id.bright_progressbar
    }

    override fun getBrightnessLayoutId(): Int {
        return R.layout.bright_layout
    }


    fun getFullButton(): ImageView {
        return fullBtn
    }

    override fun lockTouchLogic() {
        super.lockTouchLogic()
    }

    override fun hideAllWidget() {
        super.hideAllWidget()
        allWidget.visibility = View.GONE
    }

    override fun changeUiToPlayingClear() {
        super.changeUiToPlayingClear()
        allWidget.visibility = View.GONE
    }

    override fun changeUiToPlayingShow() {
        super.changeUiToPlayingShow()
        allWidget.visibility = View.VISIBLE
    }

}