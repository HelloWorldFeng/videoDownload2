package com.app.videobox.manager

import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.util.Log
import cafe.adriel.voyager.core.model.ScreenModelStore.getOrPut
import com.app.videobox.utils.FileUtils
import java.io.File
import java.io.Serializable

object FileManager {

    data class FileInfo(
        var file: File,
        val sizeKB: Long,
        val creationTimestamp: Long, // 创建时间戳
        val parentDir: String,
        var titleName: String = "",
        var playTime:Long,
    ):Serializable

    var scanFileResultState: MutableList<FileInfo> = mutableListOf()

    fun getScanFileDir(): MutableMap<String, MutableList<FileInfo>> {
        val videoMap = mutableMapOf<String,MutableList<FileInfo>>()
        scanFileResultState.forEach {
            videoMap.getOrPut(it.parentDir){ mutableListOf() }.add(it) // 仅添加存在的文件
        }
        return videoMap
    }

    fun fetchPhoneVideo(context: Context) {
        scanFileResultState.clear()
        val list = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            FileUtils.getVideoFiles(context)
        } else {
            scanForFiles()
        }

        scanFileResultState.addAll(list)
    }

    fun scanForFiles():  MutableList<FileInfo> {
        val list = mutableListOf<FileInfo>()
        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            val rootDir = Environment.getExternalStorageDirectory()
            Log.d("TempLog", "开始扫描")
            val startTime = System.currentTimeMillis()
            scanDirectoryForFile(rootDir, list)
            Log.d("TempLog", "扫描结束: ${System.currentTimeMillis() - startTime} ms")
        }
        return list
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



}

