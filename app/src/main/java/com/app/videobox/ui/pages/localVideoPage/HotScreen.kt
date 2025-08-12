package com.app.videobox.ui.pages.localVideoPage

import android.app.Activity
import android.content.Context
import android.content.res.AssetManager
import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.lifecycleScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.app.videobox.R
import com.app.videobox.ad.AdManager
import com.app.videobox.ad.NativeAdsView
import com.app.videobox.ad.base.AdUnitWrapper
import com.app.videobox.manager.RemoteConfigManager
import com.app.videobox.ui.base.BaseActivity
import com.app.videobox.ui.pages.video.playerV2.VlcPlayActivity
import com.app.videobox.ui.widgets.CoilImage
import com.app.videobox.ui.widgets.TitleBar
import com.app.videobox.ui.widgets.singClick
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class HotScreen: Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current as Activity
        val list = remember {
            getVideoFilesFromAssets(context)
        }


        BackHandler {
            navigator.pop()
        }
        Column(
            Modifier.Companion
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            TitleBar(title = stringResource(R.string.hot_video)) {
                navigator.pop()
            }

            Spacer(modifier = Modifier.Companion.height(20.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(list) { file ->
                    Box(
                        modifier = Modifier.Companion
                            .size(109.dp, 162.dp)
                            .clip(shape = RoundedCornerShape(12.dp))
                    ) {
                        CoilImage(
                            modifier = Modifier.Companion
                                .singClick {
                                    VlcPlayActivity.start(
                                        context = context,
                                        videoUrl = file.absolutePath,
                                        title =  file.name
                                    )
                                }
                                .fillMaxSize(),
                            contentScale = ContentScale.Companion.FillBounds,
                            data = file.absolutePath,
                        )

                        Box(
                            modifier = Modifier.Companion
                                .padding(6.dp)
                                .align(Alignment.Companion.TopEnd)
                                .wrapContentSize()
                                .background(
                                    color = Color(0x52000000),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                                )
                        ) {
                            val sizeKb = Formatter.formatFileSize(context, file.length())
                            Text(
                                text = sizeKb,
                                fontSize = 12.sp,
                                color = Color.Companion.White,
                                modifier = Modifier.Companion.padding(3.dp)
                            )
                        }
                    }

                }
            }

            Spacer(modifier = Modifier.Companion.height(20.dp))
            Spacer(modifier = Modifier.Companion.weight(1f))

        }
    }




    fun copyAssetToFile(context: Context, assetFileName: String): File? {
        val assetManager: AssetManager = context.assets
        val outputFile = File(context.filesDir, assetFileName)

        return try {
            // 打开 assets 中的文件输入流
            val inputStream: InputStream = assetManager.open(assetFileName)
            // 创建输出流
            val outputStream = FileOutputStream(outputFile)

            // 复制文件内容
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            // 返回创建的 File 对象
            outputFile
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun getVideoFilesFromAssets(context: Context): List<File> {
        val assetManager: AssetManager = context.assets
        val mp4Files = mutableListOf<File>()

        try {
            // 列出 assets 目录中的所有文件
            val files = assetManager.list("") // 空字符串表示根目录
            // 过滤出 MP4 文件并复制到内部存储
            files?.filter { it.endsWith(".mp4") }?.forEach { fileName ->
                val file = copyAssetToFile(context, fileName)
                file?.let {
                    mp4Files.add(it)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return mp4Files
    }
}