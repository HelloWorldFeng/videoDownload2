package com.app.videobox.ui.pages.homePage

import android.app.Activity
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
import kotlinx.coroutines.launch
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.app.videobox.BuildConfig
import com.app.videobox.R
import com.app.videobox.ad.AdManager
import com.app.videobox.ad.NativeAdsView
import com.app.videobox.ad.base.AD_TYPE_INT
import com.app.videobox.ext.openGooglePlayStore
import com.app.videobox.ext.safeStartActivity
import com.app.videobox.ext.shareApp
import com.app.videobox.ext.urlInBrowser
import com.app.videobox.network.DataRepository
import com.app.videobox.network.model.MediaClass
import com.app.videobox.network.model.WebsiteItem
import com.app.videobox.ui.LanguageActivity
import com.app.videobox.ui.pages.FeedbackScreen
import com.app.videobox.ui.pages.localVideoPage.FolderScreen
import com.app.videobox.ui.pages.videoDownloadPage.DownloadListScreen
import com.app.videobox.ui.pages.webViewPage.WebViewActivity
import com.app.videobox.ui.widgets.AsyncImageImpl
import com.app.videobox.ui.widgets.CoilImage
import com.app.videobox.ui.widgets.NavBarV3
import com.app.videobox.ui.widgets.StateAsyncImageImpl
import com.app.videobox.ui.widgets.TextTitle
import com.app.videobox.ui.widgets.singClick
import com.app.videobox.utils.EventReportUtils
import com.blankj.utilcode.util.SPStaticUtils

/**
 * 新主页 - 基于Figma设计稿实现
 * 包含状态栏、搜索框、分类标签和视频网格
 * 
 * 注意：Screen接口需要支持序列化，因此不能在类级别声明Compose状态
 */
class NewHomeScreen : Screen {
    companion object {
        private const val SELECT_VIDEO = 0
        private const val SELECT_HOME = 1
        private const val SELECT_DOWNLOAD = 2
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current as Activity
        // 将状态管理移到Content方法内部，避免序列化问题
        var mSelectIndex by remember { mutableIntStateOf(value = SELECT_HOME) }
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        
        BackHandler {  }
        
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = true,
            drawerContent = {
                SettingDrawerContent(
                    drawerState = drawerState,
                    scope = scope
                )
            }
        ) {
            Box(Modifier.fillMaxSize()) {
                HomeScreen(
                    onMenuClick = {
                        scope.launch {
                            drawerState.open()
                        }
                    }
                )

                if (mSelectIndex == SELECT_DOWNLOAD){
                    DownloadListScreen()
                }

                if (mSelectIndex == SELECT_VIDEO){
                    // TODO: 实现视频页面内容
                    FolderScreen(
                        onMenuClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        }
                    )
                }
                
                NavBarV3(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 30.dp),
                    defaultIndex = SELECT_HOME,
                    onClickHome = {
                        mSelectIndex = SELECT_HOME
                    },
                    onClickDownload = {
                        AdManager.getFullAdFromPool(
                            context,
                            adType = AD_TYPE_INT,
                            adScene = "i_home_download",
                            closeAction = {
                                mSelectIndex = SELECT_DOWNLOAD
                            })

                    },
                    onClickVideo = {
                        AdManager.getFullAdFromPool(
                            context,
                            adType = AD_TYPE_INT,
                            adScene = "i_home_video",
                            closeAction = {
                                mSelectIndex = SELECT_VIDEO
                            })

                    })
            }
        }
    }


}

@Composable
fun HomeScreen(onMenuClick: () -> Unit = {}){
    val navigator = LocalNavigator.currentOrThrow
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) {
        if (SPStaticUtils.getBoolean("browser_show",true)){
            SPStaticUtils.put("browser_show",false)
            EventReportUtils.reportTDParams("browser_show",
                params = mutableMapOf(
                    "type" to "first"
                ), desc = "应用内浏览器展示")
        }else{
            EventReportUtils.reportTDParams("browser_show",
                params = mutableMapOf(
                    "type" to "nofirst"
                ), desc = "应用内浏览器展示")
        }
    }

    Box(Modifier.fillMaxSize()){
        AsyncImageImpl(
            modifier = Modifier.fillMaxWidth(),
            model = R.drawable.bg_comm
        )
        Column(modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .singClick {
                focusManager.clearFocus()
                keyboardController?.hide()
            })
        {
            // 状态栏区域
            StatusBarSection(onMenuClick = onMenuClick)

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
                Spacer(modifier = Modifier.height(10.dp))
                NativeAdsView(
                    modifier = Modifier
                        .padding(vertical = 15.dp)
                        .fillMaxWidth(1f),
                    adScene = "n_home"
                )

                // 热门推荐展示区域
                PopularVideoSection(navigator)
            }
        }
    }

}

@Composable
fun PopularVideoSection(navigator: Navigator) {
    // 获取分类数据
    val videoClasses by DataRepository.videoClassFlow.collectAsStateWithLifecycle()
    
    // 只有当分类数据可用时才展示分类列表
    if (videoClasses.isNotEmpty()) {
        Column(
            modifier = Modifier
                .padding(bottom = 90.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 遍历所有分类，为每个分类创建独立的视频列表
            videoClasses.forEach { category ->
                CategoryVideoSection(
                    category = category,
                    navigator = navigator
                )
            }
        }
    } else {
        // 显示加载状态
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.loading_video_categories),
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

/**
 * 单个分类的视频展示组件
 * 包含分类标题和水平滑动的视频列表
 */
@Composable
private fun CategoryVideoSection(
    category: MediaClass,
    navigator: Navigator
) {
    val context = LocalContext.current as Activity
    // 为每个分类创建独立的分页器
    val pager = remember(category.id) {
        Pager(
            config = PagingConfig(
                pageSize = 10,
                enablePlaceholders = false,
                prefetchDistance = 3
            ),
            pagingSourceFactory = { VideoClassPageSource(category.id) }
        )
    }
    val lazyPagingItems = pager.flow.collectAsLazyPagingItems()
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 分类标题和描述
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(
                    Modifier
                        .padding(end = 4.dp)
                        .size(4.dp, 14.dp)
                        .background(color = Color(0xFFFF5C7F), shape = RoundedCornerShape(3.dp))
                )
                Text(
                    text = category.categoryName.replaceFirstChar { it.uppercase() },
                    color = Color.White,
                    fontSize = 16.sp,
                    style = MaterialTheme.typography.headlineSmall
                )
            }

        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 水平滑动的视频列表
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(lazyPagingItems.itemCount) { index ->
                lazyPagingItems[index]?.let { video ->
                    HorizontalVideoCard(
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

                            EventReportUtils.reportTDParams("browser_click",
                                params = mutableMapOf(
                                    "action" to "recommend",
                                    "webname" to video.videoURL
                                ), desc = "应用内浏览器点击")
                        }
                    )
                }
            }
            
            // 如果还有更多数据可以加载，显示加载更多指示器
            if (lazyPagingItems.itemCount > 0) {
                item {
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.more),
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * 水平滑动列表中的视频卡片组件
 * 采用竖向布局，适合水平滑动展示
 */
@Composable
private fun HorizontalVideoCard(
    video: com.app.videobox.network.model.MediaVideo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(102.dp)
            .height(161.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // 视频缩略图
            StateAsyncImageImpl(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(8.dp)),
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
                                .size(28.dp),
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
                                .size(28.dp),
                            model = R.drawable.icon_video_place,
                            contentDescription = null
                        )
                    }
                }
            )

            // 视频标题
            Text(
                modifier = Modifier.align(Alignment.BottomCenter),
                text = video.title,
                color = Color.White,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * 状态栏组件 - 模拟iPhone状态栏
 */
@Composable
fun StatusBarSection(onMenuClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
    ){
        AsyncImageImpl(
            modifier = Modifier
                .padding(start = 15.dp)
                .align(Alignment.CenterStart)
                .size(24.dp)
                .clickable { onMenuClick() },
            model = R.drawable.icon_menu
        )

        Text(text = stringResource(R.string.app_name), color = Color.White,
            modifier = Modifier.align(Alignment.Center))

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
                        text = stringResource(R.string.search_or_type_url),
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
                modifier = Modifier.size(50.dp).singClick{
                    focusManager.clearFocus()
                    handleSearch(searchText, context, navigator)
                },
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
    val context = LocalContext.current as Activity
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
                    AdManager.getFullAdFromPool(
                        context,
                        adType = AD_TYPE_INT,
                        adScene = "i_web_click",
                        closeAction = {
                            // 点击网站图标跳转到WebView页面
                            WebViewActivity.start(context = context,website.url)
                        })
                    EventReportUtils.reportTDParams("browser_click",
                        params = mutableMapOf(
                            "action" to "web",
                            "webname" to website.url
                        ), desc = "应用内浏览器点击")
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
        StateAsyncImageImpl(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape),
            model = website.icon,
            contentScale = ContentScale.FillBounds,
            onLoadingComposable = {
                AsyncImageImpl(
                    modifier = Modifier.fillMaxSize(),
                    model = R.drawable.icon_url_place,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds
                )
            },
            onErrorComposable = {
                AsyncImageImpl(
                    modifier = Modifier.fillMaxSize(),
                    model = R.drawable.icon_url_place,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds
                )
            }
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
        ToastUtils.showShort(context.getString(R.string.please_enter_the_search_content_or_url_link))
        return
    }
    EventReportUtils.reportTDParams("browser_click",
        params = mutableMapOf(
            "action" to "search",
            "webname" to searchText
        ), desc = "应用内浏览器点击")
    //(WebView浏览)
    if (searchText.startsWith("http")) {
        WebViewActivity.start(context,searchText)
    }else{
        WebViewActivity.start(context,"https://google.com/search?q=${searchText}")
    }

}

/**
 * 抽屉内容组件
 */
@Composable
private fun SettingDrawerContent(
    drawerState: DrawerState,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.currentOrThrow
    ModalDrawerSheet(
        modifier = Modifier.width(314.dp),
        drawerContainerColor = Color(0xFF212121)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 抽屉头部
            DrawerHeader()
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 抽屉菜单项
            DrawerMenuItem(
                icon = R.drawable.icon_share,
                title = stringResource(R.string.share_the_app),
                onClick = {
                    context.shareApp()
                    scope.launch { drawerState.close() }
                }
            )
            
            DrawerMenuItem(
                icon = R.drawable.icon_language,
                title = stringResource(R.string.language),
                onClick = {
                    context.safeStartActivity(LanguageActivity::class.java)
                    scope.launch { drawerState.close() }
                }
            )
            
            DrawerMenuItem(
                icon = R.drawable.icon_rate,
                title = stringResource(R.string.rate_us),
                onClick = {
                    context.openGooglePlayStore()
                    scope.launch { drawerState.close() }
                }
            )
            
            DrawerMenuItem(
                icon = R.drawable.icon_terms,
                title = stringResource(R.string.terms_of_service),
                onClick = {
                    context.urlInBrowser(BuildConfig.termUrl)
                    scope.launch { drawerState.close() }
                }
            )
            
            DrawerMenuItem(
                icon = R.drawable.icon_version,
                title = stringResource(R.string.version_update),
                onClick = {
                    context.openGooglePlayStore()
                    scope.launch { drawerState.close() }
                }
            )

            DrawerMenuItem(
                icon = R.drawable.icon_privacy,
                title = stringResource(R.string.privacy_policy),
                onClick = {
                    context.urlInBrowser(BuildConfig.privacyUrl)
                    scope.launch { drawerState.close() }
                }
            )

            DrawerMenuItem(
                icon = R.drawable.icon_feed,
                title = stringResource(R.string.feedback),
                onClick = {
                    navigator.push(FeedbackScreen())
                    scope.launch { drawerState.close() }
                }
            )
        }
    }
}

/**
 * 抽屉头部组件
 */
@Composable
private fun DrawerHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 应用图标
        AsyncImageImpl(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape),
            model = R.mipmap.icon_logo // 可以替换为应用图标
        )
        
        Spacer(modifier = Modifier.height(17.dp))
        
        // 应用名称
        Text(
            text = stringResource(R.string.app_name),
            color = Color.White,
            fontSize = 22.sp,
            style = MaterialTheme.typography.headlineSmall
        )
        
        Spacer(modifier = Modifier.height(4.dp))
    }
}

/**
 * 抽屉菜单项组件
 */
@Composable
private fun DrawerMenuItem(
    icon: Any,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImageImpl(
            modifier = Modifier.size(27.dp),
            model = icon
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = title,
            color = Color.White,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.weight(1f))

        AsyncImageImpl(
            modifier = Modifier.size(16.dp),
            model = R.drawable.icon_arrow_right
        )
    }
}
