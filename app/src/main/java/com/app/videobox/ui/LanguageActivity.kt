package com.app.videobox.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeGesturesPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.app.videobox.R
import com.app.videobox.ui.base.BaseActivity
import com.app.videobox.ui.theme.gradientColor
import com.app.videobox.ui.widgets.TextTitle
import com.app.videobox.ui.widgets.singClick
import com.app.videobox.utils.LanguageUtils
import com.blankj.utilcode.util.SPStaticUtils

class LanguageActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Content()
        }
    }

    @Composable
    private fun Content() {
        val context = (LocalContext.current as Activity)
        val selectLanguageIndex = remember {
            mutableStateOf(SPStaticUtils.getInt("selectLanguageIndex",0))
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color.Black)
                .safeGesturesPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextTitle(text = stringResource(R.string.language), fontSize = 27.sp, color = Color.White)
            Spacer(modifier = Modifier.height(20.dp))
            LanguageUtils.list.forEachIndexed { index, ls ->
                val isCheck = selectLanguageIndex.value == index
                val checkColor = if (isCheck) {
                    Color(0xFFFF792E)
                }else{
                    Color.White
                }
                val checkBg = if (isCheck) {
                    Modifier.background(color = Color(0xFF2E2F30), shape = RoundedCornerShape(28.dp))
                }else{
                    Modifier
                }
                Box(modifier = Modifier
                    .singClick {
                        selectLanguageIndex.value = index
                    }
                    .padding(bottom = 20.dp)
                    .fillMaxWidth(0.9f)
                    .height(56.dp)
                    .then(checkBg)
                ) {
                    Text(text = ls.name, fontSize = 16.sp,color = checkColor, modifier = Modifier.align(
                        Alignment.Center))
                }

            }

            Box(
                modifier = Modifier
                    .singClick {
                        SPStaticUtils.put("chooseLanguage", false)
                        SPStaticUtils.put("selectLanguageIndex", selectLanguageIndex.value)
                        LanguageUtils.setAppLanguage(context)
                        reStartActivity(context)

                    }
                    .fillMaxWidth(0.9f)
                    .height(57.dp)
                    .background(brush = gradientColor, shape = RoundedCornerShape(38.dp))
            ) {
                Text(
                    text = stringResource(R.string.next), color = Color.White, fontSize = 18.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }

    private fun reStartActivity(context: Activity) {
        val intent = Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
    }

    override fun onBackPressed() {
        if (SPStaticUtils.getBoolean("chooseLanguage", true)) {
            return
        }
        super.onBackPressed()
    }
}