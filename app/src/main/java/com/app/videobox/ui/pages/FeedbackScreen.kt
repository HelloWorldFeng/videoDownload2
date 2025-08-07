package com.app.videobox.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 反馈页面 - 基于Figma设计稿实现
 * 用户可以在此页面提交反馈意见
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    onBackClick: () -> Unit = {}
) {
    // 反馈文本状态
    var feedbackText by remember { mutableStateOf("") }
    val maxCharacters = 600
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 状态栏区域
        StatusBarSection()
        
        // 导航栏区域
        NavigationBarSection(onBackClick = onBackClick)
        
        // 页面标题
        PageTitleSection()
        
        // 反馈输入区域
        FeedbackInputSection(
            feedbackText = feedbackText,
            onTextChange = { newText ->
                if (newText.length <= maxCharacters) {
                    feedbackText = newText
                }
            },
            currentCharacters = feedbackText.length,
            maxCharacters = maxCharacters
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // 提交按钮
        SubmitButtonSection(
            onSubmitClick = {
                // TODO: 处理提交反馈逻辑
                // 可以在这里添加提交反馈到服务器的逻辑
            },
            isEnabled = feedbackText.isNotBlank()
        )
        
        Spacer(modifier = Modifier.height(40.dp))
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
            .height(48.dp)
            .padding(horizontal = 21.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 时间显示
        Text(
            text = "9:41",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black,
            textAlign = TextAlign.Center
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
 * 导航栏组件 - 包含返回按钮
 */
@Composable
private fun NavigationBarSection(
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(Color(0xFFD8D8D8))
    ) {
        // 返回按钮
        Box(
            modifier = Modifier
                .padding(start = 10.dp)
                .size(24.dp)
                .align(Alignment.CenterStart)
                .clickable { onBackClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "返回",
                modifier = Modifier.size(20.dp),
                tint = Color.Black
            )
        }
    }
}

/**
 * 页面标题组件
 */
@Composable
private fun PageTitleSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Feedback",
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 反馈输入区域组件
 */
@Composable
private fun FeedbackInputSection(
    feedbackText: String,
    onTextChange: (String) -> Unit,
    currentCharacters: Int,
    maxCharacters: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
    ) {
        // 输入框
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(224.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF2E2F30))
                .padding(21.dp)
        ) {
            Column {
                // 占位符文本或用户输入
                if (feedbackText.isEmpty()) {
                    Text(
                        text = "Please enter the feedback you would like to submit",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF898989).copy(alpha = 0.97f),
                        lineHeight = 16.sp
                    )
                }
                
                // 文本输入框
                BasicTextField(
                    value = feedbackText,
                    onValueChange = onTextChange,
                    textStyle = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        lineHeight = 16.sp
                    ),
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // 字符计数器
            Text(
                text = "$currentCharacters/$maxCharacters",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF898989).copy(alpha = 0.97f),
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

/**
 * 提交按钮组件
 */
@Composable
private fun SubmitButtonSection(
    onSubmitClick: () -> Unit,
    isEnabled: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 52.dp),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = onSubmitClick,
            enabled = isEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(57.dp),
            shape = RoundedCornerShape(28.5.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = if (isEnabled) {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFF7B29),
                                    Color(0xFFFF5A83)
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.Gray.copy(alpha = 0.3f),
                                    Color.Gray.copy(alpha = 0.3f)
                                )
                            )
                        },
                        shape = RoundedCornerShape(28.5.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Submit",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}