package com.app.videobox.manager

import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import com.app.videobox.App
import com.app.videobox.ui.base.BaseActivity
import com.app.videobox.utils.FileUtils
import com.blankj.utilcode.util.SPStaticUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.Serializable

object FileManager {

    data class FileInfo(
        var file: File,
        val sizeKB: Long,
        val creationTimestamp: Long, // 创建时间戳
        val parentDir: String,
        var titleName: String = "",
        var playTime:Long
    ):Serializable

    var scanFileResultState: MutableList<FileInfo> = mutableListOf()
    var scanFileState = mutableStateOf(value = false)


    fun fetchPhoneVideo(context: BaseActivity) {
        if (FileUtils.checkFilePermission(context).not()) {
            return
        }

        context.lifecycleScope.launch(Dispatchers.IO) {
            val list = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                FileUtils.getVideoFiles(context)
            } else {
                scanForFiles()
            }
            scanFileResultState.clear()
            //不使用 所有文件访问权限为了过审 做伪逻辑操作
            //伪删除
            FileUtils.deleteSet.forEach { deleteName->
                list.find { it.titleName.equals(deleteName,true) }?.let {
                    list.remove(it)
                }
            }
            scanFileResultState.addAll(list)

            //伪重命名
            val rename = SPStaticUtils.getString("rename")
            if (rename.isNotEmpty()) {
                FileUtils.renameSet.clear()
                val type = object : TypeToken<List<FileUtils.RenameFile>>() {}.type
                val list = Gson().fromJson<List<FileUtils.RenameFile>>(rename,type)
                FileUtils.renameSet.addAll(list)

                list.forEach {  fileInfo->
                    scanFileResultState.find { it.titleName == fileInfo.oldName }?.titleName = fileInfo.newName
                }
            }

        }

    }

    suspend fun scanForFiles():  MutableList<FileInfo> = withContext(Dispatchers.IO){
        val list = mutableListOf<FileInfo>()
        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            val rootDir = Environment.getExternalStorageDirectory()
            Log.d("TempLog", "开始扫描:${rootDir}")
            scanFileState.value = true
            val startTime = System.currentTimeMillis()
            if (rootDir.isDirectory) {
                rootDir.listFiles()?.map {
                    async { scanDirectoryForFile(it, list) }
                }?.awaitAll()
            }
            scanFileState.value = false
            Log.d("TempLog", "扫描结束: ${System.currentTimeMillis() - startTime} ms")
        }
        return@withContext list
    }

    private fun scanDirectoryForFile(
        directory: File,
        list: MutableList<FileInfo>,
    ) {
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                // 递归扫描子目录
                scanDirectoryForFile(file, list)
            } else if (isVideoFile(file)) {
                addFileInfoToList(file, list)
            }
        }
    }

    private fun addFileInfoToList(file: File, list: MutableList<FileInfo>) {
        val sizeKB = file.length() / 1024
        val createStamp = file.lastModified()
        val fileName = FileUtils.getFileNameWithSuffix(file.absolutePath) // 文件名称
        val dirName = FileUtils.getDirNameWithSuffix(file.absolutePath)

        Log.d("Log", "fileName: $fileName, path: ${file.absolutePath}, dir: $dirName")
        val playTime = FileUtils.getVideoDuration(file.absoluteFile)
        val fileInfo = FileInfo(file, sizeKB, createStamp, dirName, fileName,playTime)
        list.add(fileInfo)
    }

    private fun isVideoFile(file: File): Boolean {
        val documentExtensions = listOf("mp4", "mkv", "avi", "mov", "wmv") // 添加更多视频格式
        return documentExtensions.any { file.extension.equals(it, ignoreCase = true) }
    }

    fun updateMediaStore(context: Context, file: File) {
        // 通知系统更新媒体库
        MediaScannerConnection.scanFile(
            context.applicationContext,
            arrayOf(file.absolutePath),
            null
        ) { _, uri ->
            // 在这里您可以获取到媒体库中的 URI，如果需要的话
            // 也可以通过广播通知其他应用有新文件添加到媒体库
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).apply {
                data = uri
            }
            context.sendBroadcast(mediaScanIntent)
        }
    }

    fun searchVideoList(searchText: String): MutableList<FileInfo> {
        val result = scanFileResultState.filter { it.titleName.contains(searchText,true) }.toMutableList()
        return result
    }


}

