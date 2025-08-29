package com.app.videobox.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.app.videobox.GUIDER
import com.app.videobox.R
import com.app.videobox.ad.AdManager
import com.app.videobox.ad.NativeAdsView
import com.app.videobox.ad.base.AD_TYPE_INT
import com.app.videobox.ui.base.BaseActivity
import com.app.videobox.ui.widgets.GradientButton
import com.app.videobox.utils.EventReportUtils
import com.blankj.utilcode.util.SPStaticUtils
import kotlinx.coroutines.launch

class GuiderActivity: BaseActivity() {

    companion object{
        fun start(context: Context, extras: Bundle? = null) {
            val intent = Intent(context, GuiderActivity::class.java)
            extras?.let { intent.putExtras(it) }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BackHandler {  }
            GuiderPage(
                onFinish = {
                    AdManager.getFullAdFromPool(
                        this,
                        adScene = "i_guider",
                        adType = AD_TYPE_INT,
                        closeAction = {
                            MainActivity.start(this)
                        }
                    )
                    SPStaticUtils.put(GUIDER,false)
                    EventReportUtils.reportTDParams(
                        "guide_click",
                        mutableMapOf("action" to "next"),
                        desc = "引导页面点击进入主页")

                }
            )
        }

    }

    @Composable
    fun GuiderPage(
        onNext:()-> Unit= {},
        onFinish:()-> Unit = {}
    ){
        val pagerState = rememberPagerState(pageCount = { 3 })
        val coroutineScope = rememberCoroutineScope()
        // 监听当前页面变化
        val currentPage by remember { derivedStateOf { pagerState.currentPage } }
        LaunchedEffect(currentPage) {
            EventReportUtils.reportTDParams(
                "guide_show",
                params = mutableMapOf("guide_show_type" to currentPage),
                desc = "引导页面展示->${currentPage}")
        }
        Box(Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()){
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            )
            {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
                { page->

                    when (page){
                        0->{
                            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally)
                            {


                                Box(Modifier.fillMaxWidth().weight(1f)){
                                    val lottie by rememberLottieComposition(LottieCompositionSpec.Asset("guider_1.json"))
                                    LottieAnimation(
                                        composition = lottie,
                                        iterations = LottieConstants.IterateForever,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(
                                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(85.dp)
                                            .background(brush = Brush.verticalGradient(
                                                listOf(Color(0x001C1D1E),Color(0xFF1C1D1E))
                                            ))
                                    )
                                }
                                Spacer(Modifier.height(20.dp))

                                Text(
                                    text = stringResource(R.string.popular_short_dramas_library),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = Color.White,
                                )
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    text = stringResource(R.string.discover_trending_short_dramas_updated_daily_for_endless_excitement),
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(27.dp))
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    )
                                    {
                                        Spacer(Modifier
                                            .size(21.dp, 3.dp)
                                            .background(
                                                Color.White,
                                                shape = RoundedCornerShape(3.dp)
                                            ))
                                        Spacer(Modifier
                                            .size(21.dp, 3.dp)
                                            .background(
                                                Color.White.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(3.dp)
                                            ))
                                        Spacer(Modifier
                                            .size(21.dp, 3.dp)
                                            .background(
                                                Color.White.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(3.dp)
                                            ))

                                    }

                                    GradientButton(
                                        text = stringResource(R.string.next),
                                        onClick = {
                                            coroutineScope.launch{
                                                onNext.invoke()
                                                pagerState.animateScrollToPage((currentPage + 1))
                                            }
                                        },
                                        modifier = Modifier.size(100.dp,39.dp)
                                    )
                                }
                            }

                        }
                        1->{
                            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally)
                            {
                                Box(Modifier.fillMaxWidth().weight(1f)){
                                    val lottie by rememberLottieComposition(LottieCompositionSpec.Asset("guider_2.json"))
                                    LottieAnimation(
                                        composition = lottie,
                                        iterations = LottieConstants.IterateForever,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(
                                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(85.dp)
                                            .background(brush = Brush.verticalGradient(
                                                listOf(Color(0x001C1D1E),Color(0xFF1C1D1E))
                                            ))
                                    )
                                }
                                Spacer(Modifier.height(20.dp))

                                Text(
                                    text = stringResource(R.string.fast_ad_free_downloads),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = Color.White,
                                )
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    text = stringResource(R.string.download_episodes_effortlessly_with_zero_ads_and_full_hd_quality),
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(27.dp))
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    )
                                    {
                                        Spacer(Modifier
                                            .size(21.dp, 3.dp)
                                            .background(
                                                Color.White.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(3.dp)
                                            ))
                                        Spacer(Modifier
                                            .size(21.dp, 3.dp)
                                            .background(
                                                Color.White,
                                                shape = RoundedCornerShape(3.dp)
                                            ))
                                        Spacer(Modifier
                                            .size(21.dp, 3.dp)
                                            .background(
                                                Color.White.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(3.dp)
                                            ))

                                    }

                                    GradientButton(
                                        text = stringResource(R.string.next),
                                        onClick = {
                                            coroutineScope.launch{
                                                onNext.invoke()
                                                pagerState.animateScrollToPage((currentPage + 1))
                                            }
                                        },
                                        modifier = Modifier.size(100.dp,39.dp)
                                    )
                                }
                            }

                        }
                        2->{
                            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally)
                            {
                                Box(Modifier.fillMaxWidth().weight(1f)){
                                    val lottie by rememberLottieComposition(LottieCompositionSpec.Asset("guider_3.json"))
                                    LottieAnimation(
                                        composition = lottie,
                                        iterations = LottieConstants.IterateForever,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.FillWidth
                                    )
                                    Spacer(
                                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(85.dp)
                                            .background(brush = Brush.verticalGradient(
                                                listOf(Color(0x001C1D1E),Color(0xFF1C1D1E))
                                            ))
                                    )
                                }
                                Spacer(Modifier.height(20.dp))

                                Text(
                                    text = stringResource(R.string.watch_offline_save_locally),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = Color.White,
                                )
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    text = stringResource(R.string.enjoy_your_favorite_dramas_anytime_anywhere_even_without_internet),
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(27.dp))
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    )
                                    {
                                        Spacer(Modifier
                                            .size(21.dp, 3.dp)
                                            .background(
                                                Color.White.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(3.dp)
                                            ))
                                        Spacer(Modifier
                                            .size(21.dp, 3.dp)
                                            .background(
                                                Color.White.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(3.dp)
                                            ))
                                        Spacer(Modifier
                                            .size(21.dp, 3.dp)
                                            .background(
                                                Color.White,
                                                shape = RoundedCornerShape(3.dp)
                                            ))

                                    }

                                    GradientButton(
                                        text = stringResource(R.string.ok),
                                        onClick = {
                                            onFinish.invoke()
                                        },
                                        modifier = Modifier.size(100.dp,39.dp)
                                    )
                                }
                            }

                        }
                    }

                }

                Spacer(Modifier.height(20.dp))
                NativeAdsView(
                    modifier = Modifier
                        .fillMaxWidth(1f),
                    adScene = "n_guider"
                )
            }

        }

    }
}