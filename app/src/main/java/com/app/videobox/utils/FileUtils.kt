package com.app.videobox.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import com.app.videobox.manager.FileManager
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.SPStaticUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.XXPermissions
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File

object FileUtils {

    data class RenameFile(var oldName: String, var newName: String)


    var deleteSet = SPStaticUtils.getStringSet("deleteSet").toMutableSet()
    var renameSet = mutableListOf<RenameFile>()

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

        // 选择条件：可以根据需要添加更多 MIME 类型  "mp4", "mkv", "avi", "mov", "wmv"
        val selectionMimeType = "${MediaStore.Video.Media.MIME_TYPE} IN (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
        val selectionArgs = arrayOf(
            "video/mp4",         // MP4
            "audio/mp4",        // M4A
            "video/x-msvideo",  // AVI
            "video/x-matroska", // MKV
            "video/x-flv",      // FLV
            "video/x-ms-wmv",   // WMV
            "video/quicktime",   // MOV
            "video/3gpp",       // 3GP
            "video/mp2t",       // TS
            "video/webm",       // WEBM
            "video/mpeg"        // MPG
        )

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
                    val fileSize = file.length()
                    val createTimeStamp = file.lastModified()
                    if (file.exists()) {
                        val playTime = getVideoDuration(file.absoluteFile)
                        val fileInfo = FileManager.FileInfo(
                            file = file,
                            sizeKB = fileSize,
                            creationTimestamp = createTimeStamp,
                            parentDir = dirName,
                            titleName = fileName,
                            playTime = playTime
                        )
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

    fun requestFilePermission(context: Activity, doNotAsk:()->Unit={}, hasPermission:()->Unit= {}) {
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
                    doNotAsk.invoke()
                }
            })
    }

    fun checkFilePermission(context: Context): Boolean {
        return XXPermissions.isGranted(context,Manifest.permission.READ_MEDIA_VIDEO,Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    fun renameFile(newName: String, oldFile: FileManager.FileInfo) {
        val newNames = newName + ".${oldFile.file.name.split(".").last()}"

        val result = FileUtils.rename(oldFile.file,newNames)
        FileManager.scanFileResultState.find { it == oldFile }?.let {fileInfo->

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val re = RenameFile(oldName = fileInfo.titleName, newName = newNames)
                val bean = renameSet.find { it.oldName == fileInfo.titleName }
                if (bean == null) {
                    renameSet.add(re)
                }else{
                    bean.newName = newNames
                }
                Gson().toJson(renameSet).let {
                    SPStaticUtils.put("rename",it)
                }
                fileInfo.titleName = newNames
                return
            }

            fileInfo.titleName = newNames
            val newFile = File(fileInfo.file.parent + File.separator + newNames)
            fileInfo.file= newFile
        }


        Log.d("TAG", "renameFile:${result} ")
    }


    fun deleteFile(context: Context, fileInfo: FileManager.FileInfo) {
        fileInfo.file.delete()
        FileManager.scanFileResultState.remove(fileInfo)
        deleteSet.add(fileInfo.titleName)
        SPStaticUtils.put("deleteSet", deleteSet)
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

    /**
     * 获取随机文件大小
     * @return 返回20MB-30MB之间的随机Long值（单位：字节）
     */
    fun getRandomFileSize(): Long {
        // 20MB = 20 * 1024 * 1024 = 20971520 字节
        // 30MB = 30 * 1024 * 1024 = 31457280 字节
        val minSize = 20L * 1024 * 1024 // 20MB
        val maxSize = 30L * 1024 * 1024 // 30MB
        
        // 生成20MB到30MB之间的随机值
        return (minSize..maxSize).random()
    }
}