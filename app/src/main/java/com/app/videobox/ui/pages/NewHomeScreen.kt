package com.app.videobox.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults

/**
 * 新主页 - 基于Figma设计稿实现
 * 包含状态栏、搜索框、分类标签和视频网格
 */
class NewHomeScreen : Screen {
    
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current as BaseActivity
        
        // 分类状态
        var selectedCategory by remember { mutableStateOf("Short Play") }
        val categories = listOf("Short Play", "Short Video", "Racing Car")
        
        // 底部导航状态
        var selectedBottomTab by remember { mutableStateOf(0) }
        
        // 模拟视频数据
        val videoItems = remember {
            generateSampleVideoItems()
        }
        
        Scaffold(
            bottomBar = {
                BottomNavigationBar(
                    selectedTab = selectedBottomTab,
                    onTabSelected = { index ->
                        selectedBottomTab = index
                        when (index) {
                            0 -> {
                                // 首页 - 当前页面，无需跳转
                            }
                            1 -> {
                                // 跳转到下载列表
                                navigator.push(DownloadListScreenWrapper())
                            }
                            2 -> {
                                // 跳转到下载测试页面
//                                navigator.push(DownloadTestScreenWrapper())
                            }
                        }
                    }
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1A1A1A),
                                Color(0xFF000000)
                            )
                        )
                    )
                    .padding(paddingValues)
            ) {
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
                    
                    Spacer(modifier = Modifier.height(30.dp))
                    
                    // Stream Box 标题
                    Text(
                        text = "Stream Box",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(30.dp))
                    
                    // 分类标签
                    CategoryTabsSection(
                        categories = categories,
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it }
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // 视频网格
                    VideoGridSection(
                        videoItems = videoItems.filter { it.category == selectedCategory },
                        onVideoClick = { videoItem ->
                            // TODO: 处理视频点击事件
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // More 按钮
                    MoreButtonSection()
                    
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

/**
 * 状态栏组件 - 模拟iPhone状态栏
 */
@Composable
private fun StatusBarSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 21.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 时间显示
        Text(
            text = "9:41",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black
        )
        
        // 右侧状态图标
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 信号强度
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "信号",
                modifier = Modifier.size(16.dp),
                tint = Color.Black
            )
            // WiFi
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "WiFi",
                modifier = Modifier.size(15.dp),
                tint = Color.Black
            )
            // 电池
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "电池",
                modifier = Modifier.size(24.dp),
                tint = Color.Black
            )
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
            .background(Color.White)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "搜索",
                modifier = Modifier
                    .size(20.dp)
                    .clickable {
                        focusManager.clearFocus()
                        handleSearch(searchText, context, navigator)
                    },
                tint = Color.Gray
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
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
        }
    }
}

/**
 * 分类标签组件
 */
@Composable
private fun CategoryTabsSection(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        categories.forEach { category ->
            val isSelected = category == selectedCategory
            
            Text(
                text = category,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = Color.White,
                modifier = Modifier
                    .clickable { onCategorySelected(category) }
                    .padding(vertical = 8.dp, horizontal = 16.dp)
            )
        }
    }
    
    // 选中指示器
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        categories.forEach { category ->
            val isSelected = category == selectedCategory
            
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(3.dp)
                    .background(
                        color = if (isSelected) Color.White else Color.Transparent,
                        shape = RoundedCornerShape(1.5.dp)
                    )
            )
        }
    }
}

/**
 * 视频网格组件
 */
@Composable
private fun VideoGridSection(
    videoItems: List<VideoItem>,
    onVideoClick: (VideoItem) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.height(400.dp) // 固定高度以避免滚动冲突
    ) {
        items(videoItems) { videoItem ->
            VideoItemCard(
                videoItem = videoItem,
                onClick = { onVideoClick(videoItem) }
            )
        }
    }
}

/**
 * 视频卡片组件
 */
@Composable
private fun VideoItemCard(
    videoItem: VideoItem,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(0.75f)
            .clip(RoundedCornerShape(11.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = videoItem.gradientColors
                )
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // 播放按钮
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "播放",
                modifier = Modifier.size(20.dp),
                tint = Color.White
            )
        }
    }
}

/**
 * More 按钮组件
 */
@Composable
private fun MoreButtonSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        repeat(3) {
            Text(
                text = "More",
                fontSize = 14.sp,
                color = Color.White,
                modifier = Modifier
                    .clickable {
                        // TODO: 处理More按钮点击
                    }
                    .padding(8.dp)
            )
        }
    }
}

/**
 * 视频项数据类
 */
data class VideoItem(
    val id: String,
    val title: String,
    val category: String,
    val gradientColors: List<Color>
)

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



/**
 * 底部导航栏组件
 * @param selectedTab 当前选中的标签索引
 * @param onTabSelected 标签选择回调
 */
@Composable
private fun BottomNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFF1A1A1A),
        contentColor = Color.White,
        modifier = Modifier.height(80.dp)
    ) {
        // 首页按钮
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "首页",
                    tint = if (selectedTab == 0) Color(0xFF6366F1) else Color.Gray
                )
            },
            label = {
                Text(
                    text = "首页",
                    color = if (selectedTab == 0) Color(0xFF6366F1) else Color.Gray,
                    fontSize = 12.sp
                )
            },
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF6366F1),
                selectedTextColor = Color(0xFF6366F1),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = Color.Transparent
            )
        )
        
        // 下载列表按钮
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = "下载列表",
                    tint = if (selectedTab == 1) Color(0xFF6366F1) else Color.Gray
                )
            },
            label = {
                Text(
                    text = "下载列表",
                    color = if (selectedTab == 1) Color(0xFF6366F1) else Color.Gray,
                    fontSize = 12.sp
                )
            },
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF6366F1),
                selectedTextColor = Color(0xFF6366F1),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = Color.Transparent
            )
        )
        
        // 下载测试按钮
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "下载测试",
                    tint = if (selectedTab == 2) Color(0xFF6366F1) else Color.Gray
                )
            },
            label = {
                Text(
                    text = "下载测试",
                    color = if (selectedTab == 2) Color(0xFF6366F1) else Color.Gray,
                    fontSize = 12.sp
                )
            },
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF6366F1),
                selectedTextColor = Color(0xFF6366F1),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = Color.Transparent
            )
        )
    }
}

/**
 * 生成示例视频数据
 */
private fun generateSampleVideoItems(): List<VideoItem> {
    val gradientSets = listOf(
        listOf(Color(0xFFFF6B6B), Color(0xFFFF8E53)),
        listOf(Color(0xFF4ECDC4), Color(0xFF44A08D)),
        listOf(Color(0xFF667eea), Color(0xFF764ba2)),
        listOf(Color(0xFFf093fb), Color(0xFFf5576c)),
        listOf(Color(0xFF4facfe), Color(0xFF00f2fe)),
        listOf(Color(0xFF43e97b), Color(0xFF38f9d7)),
        listOf(Color(0xFFfa709a), Color(0xFFfee140)),
        listOf(Color(0xFFa8edea), Color(0xFFfed6e3)),
        listOf(Color(0xFFffecd2), Color(0xFFfcb69f))
    )
    
    val categories = listOf("Short Play", "Short Video", "Racing Car")
    val items = mutableListOf<VideoItem>()
    
    categories.forEach { category ->
        repeat(9) { index ->
            items.add(
                VideoItem(
                    id = "${category}_$index",
                    title = "$category Video ${index + 1}",
                    category = category,
                    gradientColors = gradientSets[index % gradientSets.size]
                )
            )
        }
    }
    
    return items
}