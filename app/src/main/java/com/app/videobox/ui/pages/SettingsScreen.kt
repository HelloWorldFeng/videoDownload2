package com.app.videobox.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 设置页面 - 基于Figma设计稿实现
 * 展示应用设置选项和用户信息
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    // 设置项数据
    val settingsItems = remember {
        listOf(
            SettingsItem(
                id = "language",
                title = "Language",
                icon = Icons.Default.Settings,
                hasArrow = true
            ),
            SettingsItem(
                id = "rate",
                title = "Rate us",
                icon = Icons.Default.Settings,
                hasArrow = true
            ),
            SettingsItem(
                id = "share",
                title = "Share the app",
                icon = Icons.Default.Settings,
                hasArrow = true
            ),
            SettingsItem(
                id = "terms",
                title = "Terms of Service",
                icon = Icons.Default.Settings,
                hasArrow = true
            ),
            SettingsItem(
                id = "privacy",
                title = "Privacy policy",
                icon = Icons.Default.Settings,
                hasArrow = true
            ),
            SettingsItem(
                id = "feedback",
                title = "Feedback",
                icon = Icons.Default.Settings,
                hasArrow = true
            ),
            SettingsItem(
                id = "version",
                title = "Version update",
                icon = Icons.Default.Settings,
                hasArrow = true,
                subtitle = "V1.0"
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 状态栏区域
        StatusBarSection()
        
        // 用户信息区域
        UserInfoSection()
        
        // 设置选项列表
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(settingsItems) { item ->
                SettingsItemCard(item = item)
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 时间显示
        Text(
            text = "9:41",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black
        )
        
        // 右侧状态图标
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                modifier = Modifier.size(16.dp),
                tint = Color.Black
            )
            // 电池
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "电池",
                modifier = Modifier.size(20.dp),
                tint = Color.Black
            )
        }
    }
}

/**
 * 用户信息区域组件
 */
@Composable
private fun UserInfoSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 用户头像
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFF6B6B).copy(alpha = 0.6f),
                            Color(0xFF4ECDC4).copy(alpha = 0.4f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // 应用图标占位
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "应用图标",
                    modifier = Modifier.size(30.dp),
                    tint = Color(0xFF333333)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 应用名称
        Text(
            text = "Stream Box",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // 版本信息
        Text(
            text = "V1.0",
            fontSize = 14.sp,
            color = Color.Gray
        )
    }
}

/**
 * 设置项卡片组件
 */
@Composable
private fun SettingsItemCard(item: SettingsItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // TODO: 处理设置项点击事件
                when (item.id) {
                    "language" -> { /* 打开语言设置 */ }
                    "rate" -> { /* 打开应用评分 */ }
                    "share" -> { /* 分享应用 */ }
                    "terms" -> { /* 打开服务条款 */ }
                    "privacy" -> { /* 打开隐私政策 */ }
                    "feedback" -> { /* 打开反馈页面 */ }
                    "version" -> { /* 检查版本更新 */ }
                }
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8F9FA)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 设置项图标
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFF666666)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 设置项文本
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                
                item.subtitle?.let { subtitle ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            
            // 箭头图标
            if (item.hasArrow) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "进入",
                    modifier = Modifier.size(20.dp),
                    tint = Color.Gray
                )
            }
        }
    }
}

/**
 * 设置项数据类
 */
data class SettingsItem(
    val id: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val hasArrow: Boolean = false,
    val subtitle: String? = null
)