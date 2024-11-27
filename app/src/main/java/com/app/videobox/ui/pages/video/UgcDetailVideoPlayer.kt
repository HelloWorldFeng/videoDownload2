package com.app.videobox.ui.pages.video

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.res.Configuration
import android.media.metrics.Event
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.app.videobox.R
import com.shuyu.gsyvideoplayer.utils.GSYVideoType
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer

class UgcDetailVideoPlayer : StandardGSYVideoPlayer {

    private var brightProgress:ProgressBar?=null
    private var speedIv:TextView?= findViewById(R.id.speed_iv)
    private var bottomLayout:ConstraintLayout = findViewById(R.id.all_widget)
    private var centerLayout:LinearLayout = findViewById(R.id.center_layout)
    private var addSecond:ImageView = findViewById(R.id.add_second)
    private var reduceSecond:ImageView = findViewById(R.id.reduce_second)
    private var ratioBtn:ImageView = findViewById(R.id.ratio_btn)
    private var fullBtn:ImageView = findViewById(R.id.full_btn)
    private var lockBtn:ImageView = findViewById(R.id.lock_iv)
    private var startBtn:ImageView = findViewById(R.id.start_btn)
    private var currentSpeed:SpeedUnit = SpeedUnit.One
    private var currentType:Int = 0
    private var mLockCurIv:Boolean = false

    enum class SpeedUnit{
        Half,One,OneHalf,Two,Three
    }


    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    override fun getLayoutId(): Int {
        return R.layout.layout_ugc_detail_video_player
    }

    init {
        speedIv?.setOnClickListener {
            changeSpeedPlay()
        }
        lockBtn.setOnClickListener {
            myLockTouchLogic()
        }
        fullBtn.setOnClickListener {
            if (currentType == 0) {
                currentType = 1
                GSYVideoType.setShowType(GSYVideoType.SCREEN_MATCH_FULL)
            }else if (currentType == 1) {
                currentType = 0
                GSYVideoType.setShowType(GSYVideoType.SCREEN_TYPE_DEFAULT)
            }
            changeTextureViewShowType()
            fullBtn.requestLayout()
        }
        addSecond.setOnClickListener {
            val currentPosition = this.currentPositionWhenPlaying // 获取当前播放位置
            val newPosition = currentPosition + 10 * 1000 // 计算新的播放位置
            this.seekTo(newPosition) // 设置新的播放位置
        }
        reduceSecond.setOnClickListener {
            val currentPosition = this.currentPositionWhenPlaying // 获取当前播放位置
            val newPosition = currentPosition - 10 * 1000 // 计算新的播放位置
            this.seekTo(newPosition) // 设置新的播放位置
        }
        startBtn.setOnClickListener {
            if (mCurrentState == CURRENT_STATE_PLAYING) {
                onVideoPause()
            }
            else if (mCurrentState == CURRENT_STATE_AUTO_COMPLETE) {
                onVideoReset()
                startPlayLogic()
            } else {
                onVideoResume()
            }
        }
    }

    override fun updateStartImage() {
        super.updateStartImage()

        if (mCurrentState == CURRENT_STATE_PLAYING) {
            startBtn.setImageResource(R.drawable.icon_pause)
        } else if (mCurrentState == CURRENT_STATE_ERROR) {
            startBtn.setImageResource(com.shuyu.gsyvideoplayer.R.drawable.video_click_error_selector)
        } else {
            startBtn.setImageResource(R.drawable.icon_start)
        }
    }

    private fun changeSpeedPlay() {

        when (currentSpeed) {
            SpeedUnit.Half -> {
                setSpeedPlaying(1f,true)
                currentSpeed = SpeedUnit.One
                speedIv?.setText("1X")
            }
            SpeedUnit.One -> {
                setSpeedPlaying(1.5f,true)
                currentSpeed = SpeedUnit.OneHalf
                speedIv?.setText("1.5X")
            }
            SpeedUnit.OneHalf -> {
                setSpeedPlaying(2f,true)
                currentSpeed = SpeedUnit.Two
                speedIv?.setText("2X")
            }
            SpeedUnit.Two -> {
                setSpeedPlaying(3f,true)
                currentSpeed = SpeedUnit.Three
                speedIv?.setText("3X")
            }
            SpeedUnit.Three -> {
                setSpeedPlaying(0.5f,true)
                currentSpeed = SpeedUnit.Half
                speedIv?.setText("0.5X")
            }
        }

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
        return ratioBtn
    }



    override fun hideAllWidget() {
        super.hideAllWidget()
        bottomLayout.visibility = View.GONE
        centerLayout.visibility = View.GONE
    }

    override fun changeUiToPlayingClear() {
        super.changeUiToPlayingClear()
        bottomLayout.visibility = View.GONE
        centerLayout.visibility = View.GONE
    }

    override fun changeUiToPlayingShow() {
        super.changeUiToPlayingShow()
        bottomLayout.visibility = View.VISIBLE
        centerLayout.visibility = View.VISIBLE
    }

    override fun changeUiToPauseShow() {
        super.changeUiToPauseShow()
        Log.d("TAG", "changeUiToPauseShow: ")
        bottomLayout.visibility = View.VISIBLE
        centerLayout.visibility = View.VISIBLE
    }

    override fun changeUiToPauseClear() {
        super.changeUiToPauseClear()
        bottomLayout.visibility = View.GONE
        centerLayout.visibility = View.GONE
    }

    override fun changeUiToCompleteShow() {
        super.changeUiToCompleteShow()
        centerLayout.visibility = View.VISIBLE
        bottomLayout.visibility = View.VISIBLE
    }

    private fun myLockTouchLogic() {
        if (mLockCurIv) {
            lockBtn.setImageResource(R.drawable.icon_unlock)
            mLockCurIv = false
        } else {
            lockBtn.setImageResource(R.drawable.icon_lock)
            mLockCurIv = true
            hideAllWidget()
        }
    }

    override fun onClickUiToggle(e:MotionEvent) {
        if (mLockCurIv) {
            return
        }
        super.onClickUiToggle(e)
    }

    override fun touchDoubleUp(e:MotionEvent) {
        if (mLockCurIv) {
            return
        }
        super.touchDoubleUp(e)
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        Log.d("TAG", "onConfigurationChanged: ")
    }
}