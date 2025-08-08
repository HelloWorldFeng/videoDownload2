package com.app.videobox.ui.pages.homePage

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.app.videobox.ui.base.BaseActivity
import com.app.videobox.ui.pages.webViewPage.WebViewScreen
import com.blankj.utilcode.util.ToastUtils
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.compose.collectAsLazyPagingItems
import com.app.videobox.R
import com.app.videobox.network.DataRepository
import com.app.videobox.network.model.WebsiteItem
import com.app.videobox.ui.pages.FolderScreen
import com.app.videobox.ui.pages.videoDownloadPage.DownloadListScreen
import com.app.videobox.ui.widgets.AsyncImageImpl
import com.app.videobox.ui.widgets.NavBarV2

/**
 * 新主页 - 基于Figma设计稿实现
 * 包含状态栏、搜索框、分类标签和视频网格
 */
class NewHomeScreen : Screen {
    var mSelectIndex = mutableIntStateOf(value = 0)

    private val SELECT_VIDEO = 0
    private val SELECT_HOME = 1
    private val SELECT_DOWNLOAD = 2

    @Composable
    override fun Content() {
        BackHandler {  }
        Box(Modifier.fillMaxSize()) {
            HomeScreen()

            if (mSelectIndex.intValue == SELECT_DOWNLOAD){
                DownloadListScreen()
            }

            if (mSelectIndex.intValue == SELECT_VIDEO){

            }
            NavBarV2(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 30.dp),
                defaultIndex = SELECT_HOME,
                onClickHome = {
                    mSelectIndex.intValue = SELECT_HOME
                },
                onClickDownload = {
                    mSelectIndex.intValue = SELECT_DOWNLOAD
                },
                onClickVideo = {
                    mSelectIndex.intValue = SELECT_VIDEO
                })
        }


    }


}

@Composable
fun HomeScreen(){
    val navigator = LocalNavigator.currentOrThrow

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding())
    {
        // 状态栏区域
        StatusBarSection()

        // 主要内容区域
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // 搜索框
            SearchSection()


            Spacer(modifier = Modifier.height(20.dp))

            // 热门网站展示区域 - 两页轮播
            PopularWebsitesSection(navigator)
            Spacer(modifier = Modifier.height(20.dp))
            // 热门推荐展示区域
            PopularVideoSection(navigator)
        }
    }
}

@Composable
fun PopularVideoSection(navigator: Navigator) {
    // 获取分类数据

    // 根据分类数据获取分类下的视频数据流
    val pager = remember {
        Pager(
            config = PagingConfig(pageSize = 10),
            pagingSourceFactory = { VideoClassPageSource() }
        )
    }
    val lazyPagingItems = pager.flow.collectAsLazyPagingItems()

}

/**
 * 状态栏组件 - 模拟iPhone状态栏
 */
@Composable
private fun StatusBarSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 21.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row {
            AsyncImageImpl(
                modifier = Modifier.size(24.dp),
                model = R.drawable.icon_menu
            )
            Spacer(Modifier.weight(1f))
            Text(text = stringResource(R.string.app_name), color = Color.White)
            Spacer(Modifier.weight(1f))
        }
    }
}

/**
 * 搜索框组件
 */
@Composable
private fun SearchSection() {
    var searchText by remember { mutableStateOf("") }
    val context = LocalContext.current as BaseActivity
    val navigator = LocalNavigator.currentOrThrow
    val focusManager = LocalFocusManager.current
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(14.dp))
            AsyncImageImpl(
                modifier = Modifier.size(18.dp),
                model = R.drawable.icon_google
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = {
                    Text(
                        text = "Search or type URL",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus()
                        handleSearch(searchText, context, navigator)
                    }
                ),
                singleLine = true
            )

            AsyncImageImpl(
                modifier = Modifier.size(50.dp),
                model = R.drawable.icon_search
            )
        }
    }
}

/**
 * 热门网站展示组件 - 两页轮播展示
 * 第一页展示前4个网站，第二页展示后4个网站
 */
@Composable
private fun PopularWebsitesSection(navigator: Navigator) {
    // 生成热门网站假数据
    val webListState by DataRepository.webUrlFlow.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState(pageCount = { 2 })

    webListState?.runCatching {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // 网站轮播区域
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> {
                        // 第一页：展示前4个网站
                        WebsiteGridRow(
                            websites = webListState!!.urlList.take(4),
                            navigator = navigator
                        )
                    }
                    1 -> {
                        // 第二页：展示后4个网站
                        WebsiteGridRow(
                            websites = webListState!!.urlList.drop(4).take(4),
                            navigator = navigator
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            // 页面指示器
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(2) { index ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (pagerState.currentPage == index)
                                    Color(0xFFFF5C7F)
                                else
                                    Color.White.copy(alpha = 0.3f)
                            )
                    )
                }
            }
        }
    }

}

/**
 * 网站网格行组件 - 显示4个网站图标
 */
@Composable
private fun WebsiteGridRow(
    websites: List<WebsiteItem>,
    navigator: Navigator
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        websites.forEach { website ->
            WebsiteItemCard(
                website = website,
                onClick = {
                    // 点击网站图标跳转到WebView页面
                    navigator.push(WebViewScreen(website.url))
                }
            )
        }
        
        // 如果网站数量不足4个，用空白填充
        repeat(4 - websites.size) {
            Spacer(modifier = Modifier.width(45.dp))
        }
    }
}

/**
 * 单个网站卡片组件
 */
@Composable
private fun WebsiteItemCard(
    website: WebsiteItem,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(45.dp)
            .clickable { onClick() }
    ) {
        // 使用AsyncImageImpl显示网站图标
        AsyncImageImpl(
            modifier = Modifier.size(42.dp).clip(CircleShape),
            model = website.icon,
            contentScale = ContentScale.FillBounds,
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 网站名称
        Text(
            text = website.name,
            fontSize = 12.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1
        )
    }
}





/**
 * 处理搜索功能
 */
private fun handleSearch(
    searchText: String,
    context: BaseActivity,
    navigator: Navigator
) {
    if (searchText.isBlank()) {
        ToastUtils.showShort("请输入搜索内容或URL链接")
        return
    }
    //(WebView浏览)
    if (searchText.startsWith("http")) {
        navigator.push(WebViewScreen(searchText))
    }else{
        navigator.push(WebViewScreen("https://google.com/search?q=${searchText}"))
    }

}
