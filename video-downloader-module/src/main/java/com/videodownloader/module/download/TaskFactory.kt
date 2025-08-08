package com.videodownloader.module.download

import com.videodownloader.module.api.VideoInfo
import androidx.annotation.CheckResult
import com.videodownloader.module.download.Task.DownloadState.Idle

/**
 * 下载任务工厂类
 * 
 * 职责：
 * - 根据视频信息创建不同类型的下载任务
 * - 统一任务创建逻辑，确保数据一致性
 * - 提供类型安全的任务构建方法
 * 
 * 设计原则：
 * - 使用工厂模式统一任务创建流程
 * - 支持自定义标题覆盖原始标题
 * - 自动根据视频类型创建对应任务
 * - 确保任务状态初始化的一致性
 */
object TaskFactory {

    /**
     * 创建MP4视频下载任务
     * 
     * 适用场景：
     * - 普通MP4格式视频下载
     * - 单文件视频资源
     * - 支持断点续传的视频文件
     * 
     * @param videoInfo 视频基础信息
     * @param newTitle 自定义标题（可选），为空时使用原始标题
     * @return 包含任务和状态的完整对象
     */
    @CheckResult
    fun createMP4Task(
        videoInfo: VideoInfo,
        newTitle: String = "",
    ): TaskWithState {
        // 标题处理：优先使用自定义标题，否则使用原始标题
        val finalTitle = newTitle.takeIf { it.isNotEmpty() } ?: videoInfo.title

        // 创建MP4类型的下载任务
        val task = Task(
            url = videoInfo.url,
            type = Task.VideoType.MP4
        )
        
        // 构建视图状态对象，用于UI展示
        val viewState = createViewState(videoInfo, finalTitle)
        
        // 创建初始任务状态（空闲状态）
        val state = Task.State(
            downloadState = Idle,
            videoInfo = videoInfo,
            viewState = viewState
        )

        return TaskWithState(task, state)
    }

    /**
     * 创建M3U8视频下载任务
     * 
     * 适用场景：
     * - HLS流媒体视频下载
     * - 分片视频资源合并
     * - 直播回放视频下载
     * 
     * @param videoInfo 视频基础信息
     * @param newTitle 自定义标题（可选），为空时使用原始标题
     * @return 包含任务和状态的完整对象
     */
    @CheckResult
    fun createM3U8Task(
        videoInfo: VideoInfo,
        newTitle: String = "",
    ): TaskWithState {
        // 标题处理：优先使用自定义标题，否则使用原始标题
        val finalTitle = newTitle.takeIf { it.isNotEmpty() } ?: videoInfo.title

        // 创建M3U8类型的下载任务
        val task = Task(
            url = videoInfo.url,
            type = Task.VideoType.M3U8
        )

        // 构建视图状态对象，用于UI展示
        val viewState = createViewState(videoInfo, finalTitle)

        // 创建初始任务状态（空闲状态）
        val state = Task.State(
            downloadState = Idle,
            videoInfo = videoInfo,
            viewState = viewState
        )

        return TaskWithState(task, state)
    }

    /**
     * 根据视频类型自动创建对应任务
     * 
     * 智能判断逻辑：
     * - URL包含".m3u8"则创建M3U8任务
     * - 其他情况创建MP4任务
     * 
     * @param videoInfo 视频基础信息
     * @param newTitle 自定义标题（可选）
     * @return 对应类型的任务对象
     */
    @CheckResult
    fun createTaskByType(
        videoInfo: VideoInfo,
        newTitle: String = ""
    ): TaskWithState {
        return if (videoInfo.url.contains(".m3u8", ignoreCase = true)) {
            createM3U8Task(videoInfo, newTitle)
        } else {
            createMP4Task(videoInfo, newTitle)
        }
    }

    /**
     * 创建视图状态对象
     * 
     * 统一视图状态构建逻辑：
     * - 处理视频信息到UI展示的数据转换
     * - 确保UI展示数据的完整性和一致性
     * 
     * @param videoInfo 原始视频信息
     * @param title 处理后的标题
     * @return 视图状态对象
     */
    private fun createViewState(videoInfo: VideoInfo, title: String): Task.ViewState {
        return Task.ViewState(
            url = videoInfo.url,
            title = title,
            uploader = "", // VideoInfo中暂无上传者信息，保留扩展性
            duration = videoInfo.duration,
            fileSizeApprox = videoInfo.size.toDouble(),
            thumbnailUrl = videoInfo.thumbnail
        )
    }

    /**
     * 任务与状态的组合数据类
     * 
     * 设计目的：
     * - 将任务对象和状态对象组合返回
     * - 确保任务创建时状态的完整性
     * - 便于后续的状态管理和更新
     * 
     * @property task 下载任务对象
     * @property state 任务状态对象
     */
    data class TaskWithState(
        /** 下载任务核心对象 */
        val task: Task,
        /** 任务状态管理对象 */
        val state: Task.State
    )
}
