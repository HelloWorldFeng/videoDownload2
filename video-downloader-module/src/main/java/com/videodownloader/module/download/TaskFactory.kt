package com.videodownloader.module.download

import VideoInfo
import androidx.annotation.CheckResult
import com.videodownloader.module.download.Task.DownloadState.Idle

object TaskFactory {
    @CheckResult
    fun createMP4Task(
        videoInfo: VideoInfo,
        newTitle: String = "",
    ): TaskWithState {
        // 使用提供的标题或原始标题
        val finalTitle = if (newTitle.isNotEmpty()) newTitle else videoInfo.title

        // 创建 MP4 类型的任务
        val taskMP4 = Task(
            url = videoInfo.url,
            type = Task.TypeInfo.MP4
        )
        
        // 创建视图状态
        val viewState = Task.ViewState(
            url = videoInfo.url,
            title = finalTitle,
            uploader = "", // VideoInfo 中没有 uploader 字段
            duration = videoInfo.duration,
            fileSizeApprox = 0.0, // VideoInfo 中没有文件大小字段
            thumbnailUrl = videoInfo.thumbnail
        )
        
        // 创建任务状态
        val state = Task.State(
            downloadState = Idle,
            videoInfo = videoInfo,
            viewState = viewState
        )

        return TaskWithState(taskMP4, state)
    }

    @CheckResult
    fun createM3U8Task(
        videoInfo: VideoInfo,
        newTitle: String = "",
    ): TaskWithState {
        // 使用提供的标题或原始标题
        val finalTitle = if (newTitle.isNotEmpty()) newTitle else videoInfo.title

        // 创建 M3U8 类型的任务
        val taskM3U8 = Task(
            url = videoInfo.url,
            type = Task.TypeInfo.M3U8
        )


        // 创建视图状态
        val viewState = Task.ViewState(
            url = videoInfo.url,
            title = finalTitle,
            uploader = "", // VideoInfo 中没有 uploader 字段
            duration = videoInfo.duration,
            fileSizeApprox = 0.0, // VideoInfo 中没有文件大小字段
            thumbnailUrl = videoInfo.thumbnail
        )

        // 创建任务状态
        val state = Task.State(
            downloadState = Idle,
            videoInfo = videoInfo,
            viewState = viewState
        )

        return TaskWithState(taskM3U8, state)
    }


    data class TaskWithState(val task: Task, val state: Task.State)
}
