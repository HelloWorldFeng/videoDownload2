package com.app.videobox.ui.pages

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.app.videobox.R
import com.app.videobox.manager.FileManager
import com.app.videobox.ui.widgets.CoilImage
import com.app.videobox.ui.widgets.TitleBar
import com.app.videobox.ui.widgets.singClick

class FolderScreen:Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val dataList = remember {
            FileManager.getScanFileDir().toList()
        }
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally) {
            TitleBar(title = stringResource(R.string.file_director), onBack = {
                navigator.pop()
            })
            if (dataList.isEmpty()) {
                Spacer(modifier = Modifier.weight(1f))
                CoilImage(
                    modifier = Modifier.size(66.dp, 74.dp),
                    data = R.drawable.ic_launcher_background
                )
                Spacer(modifier = Modifier.height(28.dp))
                Text(text = stringResource(R.string.no_content_at_the_moment), fontSize = 16.sp,color = Color.White)
                Spacer(modifier = Modifier.weight(1f))
            }else{
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    items(dataList) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .singClick {
                                    navigator.push(LocalVideoScreen(it.second))
                                }
                                .padding(bottom = 16.dp)
                                .fillMaxWidth(0.9f)
                                .height(100.dp)
                                .background(
                                    color = Color(0xFF2E2F30),
                                    shape = RoundedCornerShape(14.dp)
                                )
                        ) {
                            Spacer(modifier = Modifier.width(28.dp))
                            CoilImage(modifier = Modifier.size(55.dp, 39.dp), data = R.drawable.ic_launcher_background)
                            Spacer(modifier = Modifier.width(15.dp))
                            Text(text = it.first, fontSize = 17.sp,color = Color.White)
                        }
                    }
                }
            }

        }
    }
}