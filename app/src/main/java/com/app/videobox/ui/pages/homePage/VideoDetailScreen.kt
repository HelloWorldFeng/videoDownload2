package com.app.videobox.ui.pages.homePage

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.app.videobox.R
import com.app.videobox.ad.AdManager
import com.app.videobox.ad.NativeAdsView
import com.app.videobox.ad.base.AD_TYPE_INT
import com.app.videobox.ui.base.BaseActivity
import com.app.videobox.ui.pages.webViewPage.WebViewActivity
import com.app.videobox.ui.widgets.AsyncImageImpl
import com.app.videobox.ui.widgets.StateAsyncImageImpl

/**
 * 视频详情页面 - 展示特定分类的所有视频
 * 使用两列网格布局展示视频列表
 * 
 * @param categoryId 视频分类ID
 * @param categoryName 视频分类名称
 */
class VideoDetailActivity() : BaseActivity() {

    companion object{
        fun start(context: Context, categoryId: Int,categoryName: String) {
            val intent = Intent(context, VideoDetailActivity::class.java).apply {
                putExtra("categoryId",categoryId)
                putExtra("categoryName",categoryName)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val categoryId = intent.getIntExtra("categoryId",0)
        val categoryName = intent.getStringExtra("categoryName")?:"title"

        setContent {
            Content(categoryId,categoryName)
        }
    }

    @Composable
    fun Content(categoryId: Int, categoryName: String) {
        val context = LocalContext.current as Activity
        
        // 创建分页器获取视频数据
        val pager = remember(categoryId) {
            Pager(
                config = PagingConfig(
                    pageSize = 20,
                    enablePlaceholders = false,
                    prefetchDistance = 5
                ),
                pagingSourceFactory = { VideoClassPageSource(categoryId) }
            )
        }
        val lazyPagingItems = pager.flow.collectAsLazyPagingItems()
        
        Box(modifier = Modifier.fillMaxSize()) {
            // 背景图片
            AsyncImageImpl(
                modifier = Modifier.fillMaxWidth(),
                model = R.drawable.bg_comm
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // 顶部导航栏
                TopNavigationBar(
                    title = categoryName,
                    onBackClick = {
                        finish()
                    }
                )
                Spacer(Modifier.height(10.dp))
                NativeAdsView(
                    modifier = Modifier
                        .fillMaxWidth(1f),
                    adScene = "n_recommend_more"
                )
                Spacer(Modifier.height(10.dp))
                
                // 根据数据状态显示不同的内容
                when {
                    // 数据加载中或为空时显示占位卡片
                    lazyPagingItems.itemCount == 0 -> {
                        // 数据为空时展示8个EmptyCard视图作为占位符
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            // 显示8个空白占位卡片，提供良好的加载体验
                            items(8) { index ->
                                EmptyCard()
                            }
                        }
                    }
                    // 有数据时显示实际的视频列表
                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            items(lazyPagingItems.itemCount) { index ->
                                lazyPagingItems[index]?.let { video ->
                                    GridVideoCard(
                                        video = video,
                                        onClick = {
                                            AdManager.getFullAdFromPool(
                                                context,
                                                adType = AD_TYPE_INT,
                                                adScene = "i_recommend_click",
                                                closeAction = {
                                                    // 点击视频跳转到WebView页面播放
                                                    WebViewActivity.start(context = context,video.videoURL)
                                                })
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 顶部导航栏组件
 * 包含返回按钮和标题
 */
@Composable
private fun TopNavigationBar(
    title: String,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
    ) {
        // 返回按钮
        AsyncImageImpl(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(24.dp)
                .clickable { onBackClick() },
            model = R.drawable.icon_back_1
        )
        
        // 标题
        Text(
            text = title.replaceFirstChar { it.uppercase() },
            color = Color.White,
            fontSize = 18.sp,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.align(Alignment.Center),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EmptyCard(){
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.632f)
            .clickable {  },
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 1f)
        ),
        shape = RoundedCornerShape(12.dp)
    )
    {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // 视频缩略图
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = Color(0xFF2E2F30),
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                AsyncImageImpl(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(32.dp),
                    model = R.drawable.icon_video_place,
                    contentDescription = null,
                    contentScale = ContentScale.Inside
                )
            }


        }
    }
}

/**
 * 网格布局中的视频卡片组件
 * 复用HorizontalVideoCard的样式，但调整为适合网格布局的尺寸
 */
@Composable
private fun GridVideoCard(
    video: com.app.videobox.network.model.MediaVideo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.632f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 1f)
        ),
        shape = RoundedCornerShape(12.dp)
    )
    {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // 视频缩略图
            StateAsyncImageImpl(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(12.dp)),
                model = video.imageURL,
                contentScale = ContentScale.Crop,
                onLoadingComposable = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                color = Color(0xFF2E2F30),
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        AsyncImageImpl(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(32.dp),
                            model = R.drawable.icon_video_place,
                            contentDescription = null,
                            contentScale = ContentScale.Inside
                        )
                    }
                },
                onErrorComposable = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                color = Color(0xFF2E2F30),
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        AsyncImageImpl(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(32.dp),
                            model = R.drawable.icon_video_place,
                            contentDescription = null
                        )
                    }
                }
            )
            
            // 视频标题背景渐变
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        ),
                        shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                    )
            )
            
            // 视频标题
            Text(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                text = video.title,
                color = Color.White,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}