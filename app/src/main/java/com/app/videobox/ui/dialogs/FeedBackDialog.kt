package com.app.videobox.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.app.videobox.R
import com.app.videobox.ui.widgets.AsyncImageImpl
import com.app.videobox.ui.widgets.singClick
import com.app.videobox.utils.EventReportUtils

@Composable
fun FeedBackDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit = {} // 确认按钮回调，传递选中的反馈类型
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        LaunchedEffect(Unit) {
            EventReportUtils.reportTDParams(
                "score_show",
                mutableMapOf("rate_type" to "bad"),
                desc = "评分弹窗")
        }

        Column(
            Modifier
                .width(325.dp,)
                .wrapContentHeight()
                .background(color = Color(0xFF2E2F30), shape = RoundedCornerShape(24.dp))
                .padding(horizontal = 24.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImageImpl(
                modifier = Modifier
                    .align(Alignment.End)
                    .size(24.dp)
                    .singClick{
                        onDismissRequest.invoke()
                    },
                model = R.drawable.icon_close,
            )

            val lottie by rememberLottieComposition(LottieCompositionSpec.Asset("bad_feedback.json"))
            LottieAnimation(
                composition = lottie,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.sorry_to_hear_you_didn_t_have_a_great_experience),
                fontSize = 16.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 单选区域 - 反馈问题类型选择
            SingleCheckBox(
                onConfirm = { selectedFeedback ->
                    EventReportUtils.reportTDParams(
                        "score_click",
                        mutableMapOf(
                            "rate_type" to "bad",
                            "action" to selectedFeedback
                        ),
                        desc = "评分弹窗")

                    onConfirm(selectedFeedback)
                    onDismissRequest()
                }
            )
        }
    }
}

/**
 * 单选反馈问题类型组件
 * 提供四种反馈选项：Bugs、Too many ads、Unable to download videos、Other
 * 支持单选逻辑，选中状态会改变背景颜色和文字颜色
 * @param onConfirm 确认按钮点击回调，传递选中的反馈类型
 */
@Composable
private fun SingleCheckBox(
    onConfirm: (String) -> Unit
) {
    // 反馈选项枚举
    val feedbackOptions = listOf(
        "Bugs",
        "Too many ads", 
        "Unable to download videos",
        "Other"
    )
    
    // 当前选中的选项索引，-1表示未选中任何选项
    var selectedIndex by remember { mutableStateOf(-1) }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 第一行：Bugs 和 Too many ads
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Bugs 选项
            FeedbackOptionItem(
                text = feedbackOptions[0],
                isSelected = selectedIndex == 0,
                onClick = { selectedIndex = if (selectedIndex == 0) -1 else 0 },
                modifier = Modifier.weight(1f)
            )
            
            // Too many ads 选项
            FeedbackOptionItem(
                text = feedbackOptions[1],
                isSelected = selectedIndex == 1,
                onClick = { selectedIndex = if (selectedIndex == 1) -1 else 1 },
                modifier = Modifier.weight(1f)
            )
        }
        
        // 第二行：Unable to download videos
        FeedbackOptionItem(
            text = feedbackOptions[2],
            isSelected = selectedIndex == 2,
            onClick = { selectedIndex = if (selectedIndex == 2) -1 else 2 },
            modifier = Modifier.fillMaxWidth()
        )
        
        // 第三行：Other
        FeedbackOptionItem(
            text = feedbackOptions[3],
            isSelected = selectedIndex == 3,
            onClick = { selectedIndex = if (selectedIndex == 3) -1 else 3 },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 确认按钮 - 根据选中状态动态改变样式和可点击性
        ConfirmButton(
            isEnabled = selectedIndex != -1, // 有选中项时才启用
            selectedFeedback = if (selectedIndex != -1) feedbackOptions[selectedIndex] else "",
            onConfirm = onConfirm
        )
    }
}

/**
 * 确认按钮组件
 * 根据是否有选中项动态改变样式和可点击性
 * @param isEnabled 是否启用按钮（有选中项时为true）
 * @param selectedFeedback 选中的反馈类型
 * @param onConfirm 确认按钮点击回调
 */
@Composable
private fun ConfirmButton(
    isEnabled: Boolean,
    selectedFeedback: String,
    onConfirm: (String) -> Unit
) {
    // 根据启用状态设置渐变色
    val gradientColors = if (isEnabled) {
        listOf(Color(0xFFFF5A83), Color(0xFFFF7B29)) // 启用时的鲜艳渐变
    } else {
        listOf(
            Color(0xFFFF5A83).copy(alpha = 0.4f), 
            Color(0xFFFF7B29).copy(alpha = 0.4f)
        ) // 禁用时的半透明渐变
    }
    
    // 文字颜色根据启用状态调整
    val textColor = if (isEnabled) {
        Color.White
    } else {
        Color.White.copy(alpha = 0.6f)
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(
                brush = Brush.horizontalGradient(gradientColors),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(enabled = isEnabled) {
                if (isEnabled) {
                    onConfirm(selectedFeedback)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.ok),
            fontSize = 16.sp,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 单个反馈选项组件
 * @param text 选项文本
 * @param isSelected 是否被选中
 * @param onClick 点击回调
 * @param modifier 修饰符
 */
@Composable
private fun FeedbackOptionItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 根据选中状态动态设置背景和文字颜色
    val backgroundColor = if (isSelected) {
        Color.White // 选中时白色背景
    } else {
        Color(0xFF515253) // 未选中时灰色背景
    }
    
    val textColor = if (isSelected) {
        Color.Black // 选中时黑色文字
    } else {
        Color.White // 未选中时白色文字
    }
    
    Box(
        modifier = modifier
            .height(48.dp)
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(24.dp)
            )
            .clickable { onClick() } // 添加点击事件
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            color = textColor,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}