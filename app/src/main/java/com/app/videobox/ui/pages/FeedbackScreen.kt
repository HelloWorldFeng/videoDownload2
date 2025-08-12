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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.app.videobox.R
import com.app.videobox.network.DataRepository
import com.app.videobox.ui.widgets.GradientButton
import com.app.videobox.ui.widgets.singClick
import com.blankj.utilcode.util.ToastUtils
import kotlinx.coroutines.launch

class FeedbackScreen : Screen{
    @Composable
    override fun Content() {
        FeedbackContent()
    }
}
/**
 * 反馈页面 - 基于Figma设计稿实现
 * 用户可以在此页面提交反馈意见
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackContent(
    onBackClick: () -> Unit = {}
) {
    // 反馈文本状态
    val context = LocalContext.current
    var feedbackText by remember { mutableStateOf("") }
    val maxCharacters = 600
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val navigator = LocalNavigator.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .singClick {
                focusManager.clearFocus()
                keyboardController?.hide()
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // 页面标题
        PageTitleSection()
        
        // 反馈输入区域
        FeedbackInputSection(
            feedbackText = feedbackText,
            onTextChange = { newText ->
                if (newText.isEmpty()) {
                    return@FeedbackInputSection
                }
                feedbackText = newText
            },
            currentCharacters = feedbackText.length,
            maxCharacters = maxCharacters
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // 提交按钮
        GradientButton(
            onClick = {
                // TODO: 处理提交反馈逻辑
                // 可以在这里添加提交反馈到服务器的逻辑
                coroutineScope.launch {
                    ToastUtils.showLong(context.getString(R.string.submit_success))
                    DataRepository.feedbackApi(feedbackText)
                    navigator?.pop()
                }
            },
            text = stringResource(R.string.submit)
        )
        
        Spacer(modifier = Modifier.height(40.dp))
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