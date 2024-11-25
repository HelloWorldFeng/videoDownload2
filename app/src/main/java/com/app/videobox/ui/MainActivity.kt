package com.app.videobox.ui

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.app.videobox.R
import com.app.videobox.ui.base.BaseActivity
import com.app.videobox.manager.FileManager
import com.app.videobox.manager.FileManager.fetchPhoneVideo
import com.app.videobox.ui.pages.FolderScreen
import com.app.videobox.ui.pages.LocalVideoScreen
import com.app.videobox.ui.pages.SettingScreen
import com.app.videobox.ui.widgets.CoilImage
import com.app.videobox.ui.widgets.TextTitle
import com.app.videobox.ui.widgets.singClick
import com.app.videobox.utils.FileUtils
import kotlinx.coroutines.launch

class MainActivity : BaseActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BackHandler {}
            Navigator(HomeScreen())
        }


    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            FileUtils.requestFilePermission(this@MainActivity){
                fetchPhoneVideo(this@MainActivity)
            }
        }
    }



}

class HomeScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current as BaseActivity
        CoilImage(
            modifier = Modifier.fillMaxWidth(), data = R.drawable.bg_home_top,
            contentScale = ContentScale.FillWidth
        )
        Column(modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally){
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)) {
                TextTitle(
                    text = stringResource(id = R.string.app_name),
                    color = Color.White,
                    fontSize = 27.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
                CoilImage(
                    modifier = Modifier
                        .padding(end = 14.dp)
                        .align(Alignment.CenterEnd)
                        .size(24.dp)
                        .singClick {
                            navigator.push(SettingScreen())
                        },
                    data = R.drawable.icon_setting
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            SearchBarView()
            Spacer(modifier = Modifier.height(22.dp))
            FoldView(){
                if (FileUtils.checkFilePermission(context).not()) {
                    FileUtils.requestFilePermission(context){
                        fetchPhoneVideo(context)
                    }
                }

                navigator.push(FolderScreen())
            }
            Spacer(modifier = Modifier.height(10.dp))

            Row(Modifier.fillMaxWidth(0.9f), horizontalArrangement = Arrangement.SpaceAround) {
                HomeItemView("Local Video",
                    R.drawable.icon_local_logo,
                    modifier = Modifier
                        .weight(1f)
                        .background(color = Color.White, shape = RoundedCornerShape(14.dp))
                        .singClick {
                            navigator.push(LocalVideoScreen(FileManager.scanFileResultState))

                        })
                Spacer(modifier = Modifier.width(17.dp))
                HomeItemView("Hot Video",
                    R.drawable.icon_hot,
                    modifier = Modifier
                        .weight(1f)
                        .background(color = Color.White, shape = RoundedCornerShape(14.dp))
                        .singClick {

                        })
            }

        }
    }

    @Composable
    private fun HomeItemView(text:String,icon:Any,modifier: Modifier) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            CoilImage(
                modifier = Modifier
                    .size(141.dp)
                    .aspectRatio(1f), data = icon
            )
            Spacer(modifier = Modifier.height(21.dp))
            TextTitle(text = text, fontSize = 22.sp)
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    @Composable
    private fun FoldView(click:()->Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(120.dp)
                .background(color = Color.White, shape = RoundedCornerShape(14.dp))
                .singClick {
                    click.invoke()
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(35.dp))
            CoilImage(
                modifier = Modifier.size(63.dp, 45.dp),
                data = R.drawable.icon_folder
            )
            Spacer(modifier = Modifier.weight(1f))
            TextTitle(text = "Folder", fontSize = 22.sp)
            Spacer(modifier = Modifier.width(50.dp))
        }
    }

    @Composable
    private fun SearchBarView() {
        Box(
            Modifier
                .fillMaxWidth(0.9f)
                .background(color = Color.White, shape = RoundedCornerShape(20.dp))) {
            CoilImage(modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(50.dp), data = R.drawable.icon_search)
        }
    }
}



