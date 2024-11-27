package com.app.videobox.ui.pages.video

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.app.videobox.App
import com.app.videobox.R
import com.shuyu.gsyvideoplayer.builder.GSYVideoOptionBuilder
import com.shuyu.gsyvideoplayer.listener.GSYSampleCallBack
import com.shuyu.gsyvideoplayer.utils.GSYVideoType
import com.shuyu.gsyvideoplayer.utils.OrientationUtils
import kotlinx.coroutines.flow.MutableStateFlow


class VideoPlayActivity : AppCompatActivity() {

    private val focusChangeFlow= MutableStateFlow(false)
    private lateinit var detailPlayer: UgcDetailVideoPlayer
    private val isPlay = false
    private var isPause = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_player)
        //允许window 的内容可以上移到刘海屏状态栏
        if (window != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val lp = window.attributes
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = lp
            window.navigationBarColor = Color.TRANSPARENT
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    )
        }


        detailPlayer = findViewById<View>(R.id.detail_player) as UgcDetailVideoPlayer

        //增加title
        detailPlayer.titleTextView.visibility = View.VISIBLE
//        detailPlayer.fullscreenButton.visibility = View.VISIBLE


        val title = intent.getStringExtra("title")
        val url =  intent.getStringExtra("video_url")

        val gsyVideoOption = GSYVideoOptionBuilder()
        gsyVideoOption
            .setIsTouchWiget(true)
            .setRotateViewAuto(false)
            .setLockLand(true)
            .setAutoFullWithSize(false)
            .setShowFullAnimation(false)
            .setNeedLockFull(true)
            .setUrl(url)
            .setCacheWithPlay(false)
            .setVideoTitle(title) ///不需要旋转
            .setNeedOrientationUtils(false)
            .setSeekRatio(1f)

            .setVideoAllCallBack(object : GSYSampleCallBack() {
                override fun onPrepared(url: String, vararg objects: Any) {
                }

                override fun onQuitFullscreen(url: String, vararg objects: Any) {
                }
            }).setLockClickListener { view, lock ->

            }
            .build(detailPlayer)

        detailPlayer.startPlayLogic()

        detailPlayer.backButton.setOnClickListener {
            finish()
        }

        detailPlayer.getFullButton().setOnClickListener { //直接横屏
            if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                return@setOnClickListener
            }
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }


    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        focusChangeFlow.value = hasFocus
    }
    override fun onPause() {
        detailPlayer.currentPlayer.onVideoPause()
        super.onPause()
        isPause = true
    }

    override fun onResume() {
        detailPlayer.currentPlayer.onVideoResume(false)
        super.onResume()
        isPause = false
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isPlay) {
            detailPlayer.currentPlayer.release()
        }
//        if (orientationUtils != null)
//            orientationUtils.releaseListener();
    }

    /**
     * orientationUtils 和  detailPlayer.onConfigurationChanged 方法是用于触发屏幕旋转的
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        //如果旋转了就全屏
//        if (isPlay && !isPause) {
//            detailPlayer.onConfigurationChanged(this, newConfig, orientationUtils, true, true);
//        }
    }

}