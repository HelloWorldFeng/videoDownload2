package com.app.videobox.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import android.util.Log
import com.app.videobox.manager.FileManager
import com.blankj.utilcode.util.FileUtils
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.XXPermissions
import java.io.File

object FileUtils {

    fun getVideoFiles(activity: Context):  MutableList<FileManager.FileInfo> {
        val videoList = mutableListOf<FileManager.FileInfo>()
        val cr = activity.contentResolver
        val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI // 使用视频的内容 URI
        val columns = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.DATA
        )

        // 选择条件：可以根据需要添加更多 MIME 类型
        val selectionMimeType = "${MediaStore.Video.Media.MIME_TYPE} IN (?, ?)"
        val selectionArgs = arrayOf("video/mp4", "video/x-m4v") // 常见的视频 MIME 类型

        val cursor: Cursor? = cr.query(uri, columns, selectionMimeType, selectionArgs, null)
        cursor?.use {
            if (it.count != 0) {
                it.moveToFirst()
                do {
                    val dataColumn = it.getColumnIndex(MediaStore.Video.Media.DATA)
                    val filePath = it.getString(dataColumn) // 文件路径
                    val fileName = getFileNameWithSuffix(filePath) // 文件名称
                    val dirName = getDirNameWithSuffix(filePath)

                    Log.d("Log", "fileName: $fileName, path: $filePath ,dir:${dirName}")


                    val file = File(filePath)
                    val fileSize = file.length() / 1024
                    val createTimeStamp = file.lastModified()
                    if (file.exists()) {
                        val playTime = getVideoDuration(file.absoluteFile)
                        val fileInfo = FileManager.FileInfo(file,fileSize,createTimeStamp,dirName,fileName,playTime)
                        videoList.add(fileInfo)
                    }
                } while (it.moveToNext())
            } else {
                Log.d("Log", "video count: ${it.count}")
            }
        }
        return videoList
    }

    fun getFileNameWithSuffix(path: String?): String {
        if (path.isNullOrEmpty()) {
            return ""
        }
        val start = path.lastIndexOf("/")
        return if (start != -1) {
            path.substring(start + 1)
        } else {
            ""
        }
    }

    fun getDirNameWithSuffix(path: String?): String {
        if (path.isNullOrEmpty()) {
            return ""
        }
        val start = path.split("/")
        return if (start.isNotEmpty()) {
            start[(start.size - 2)]
        } else {
            ""
        }
    }

    fun requestFilePermission(context: Activity, hasPermission:()->Unit= {}) {
        XXPermissions.with(context)
            .permission(
                arrayOf(Manifest.permission.READ_MEDIA_VIDEO,Manifest.permission.WRITE_EXTERNAL_STORAGE)
            ).request(object : OnPermissionCallback {
                override fun onGranted(p0: MutableList<String>, p1: Boolean) {
                    hasPermission.invoke()
                }

                override fun onDenied(
                    permissions: MutableList<String>,
                    doNotAskAgain: Boolean
                ) {
                    if (doNotAskAgain) {
                        // 如果是被永久拒绝就跳转到应用权限系统设置页面
                        XXPermissions.startPermissionActivity(context, permissions);
                    }
                }
            })
    }

    fun checkFilePermission(context: Activity): Boolean {
        return XXPermissions.isGranted(context,Manifest.permission.READ_MEDIA_VIDEO,Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    fun renameFile(newName: String, oldFile: FileManager.FileInfo) {
        val newNames = newName + ".${oldFile.file.name.split(".").last()}"
        FileUtils.rename(oldFile.file,newNames)
        FileManager.scanFileResultState.find { it == oldFile }?.let {
            it.titleName = newNames
            val newFile = File(it.file.parent + File.separator + newNames)
            it.file= newFile
        }

        Log.d("TAG", "renameFile: ")
    }

    fun getVideoDuration(file: File): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            // 设置数据源
            retriever.setDataSource(file.absolutePath)
            // 获取视频时长（单位：毫秒）
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            duration?.toLong() ?: 0L
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        } finally {
            retriever.release()
        }
    }
}