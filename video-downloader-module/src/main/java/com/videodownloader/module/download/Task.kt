package com.videodownloader.module.download

import com.videodownloader.module.api.VideoInfo
import android.annotation.SuppressLint
import com.videodownloader.module.download.Task.VideoType
import kotlinx.coroutines.Job
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * 视频下载任务数据模型
 * 
 * 核心功能：
 * - 定义下载任务的基本信息和状态
 * - 支持MP4和M3U8两种视频格式
 * - 提供任务状态管理和生命周期控制
 * - 支持任务序列化和持久化存储
 * 
 * @param url 视频下载链接，作为任务的唯一标识基础
 * @param type 视频类型，支持MP4直链和M3U8流媒体
 * @param id 任务唯一标识符，由URL和类型组合生成
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Task(
    val url: String,
    val type: VideoType = VideoType.MP4,
    val id: String = generateTaskId(url, type),
) : Comparable<Task> {

    /** 任务创建时间戳，用于排序和管理 */
    val timeCreated: Long = System.currentTimeMillis()

    /**
     * 任务比较规则：按创建时间排序，先创建的任务优先级更高
     */
    override fun compareTo(other: Task): Int {
        return timeCreated.compareTo(other.timeCreated)
    }

    /**
     * 视频类型枚举
     * 
     * 支持的视频格式：
     * - MP4: 直链下载，单文件格式
     * - M3U8: 流媒体下载，需要下载分片并合并
     */
    @Serializable
    sealed interface VideoType {
        /** MP4直链视频格式 */
        @Serializable 
        data object MP4 : VideoType
        
        /** M3U8流媒体视频格式 */
        @Serializable 
        data object M3U8 : VideoType
    }

    /**
     * 任务状态数据类
     * 
     * 包含任务的完整状态信息：
     * @param downloadState 下载状态（空闲、运行中、已完成等）
     * @param videoInfo 视频基本信息（标题、时长、大小等）
     * @param viewState 界面显示状态（用于UI展示）
     */
    @SuppressLint("UnsafeOptInUsageError")
    @Serializable
    data class State(
        val downloadState: DownloadState,
        val videoInfo: VideoInfo,
        val viewState: ViewState,
    )

    /**
     * 下载状态密封接口
     * 
     * 定义任务在下载生命周期中的各种状态：
     * - 支持状态比较和排序
     * - 提供可取消和可重启的状态接口
     * - 确保状态转换的类型安全
     */
    @Serializable
    sealed interface DownloadState : Comparable<DownloadState> {

        /**
         * 可取消状态接口
         * 适用于正在运行的任务，可以被用户主动取消
         */
        interface Cancelable {
            val job: Job              // 协程任务句柄
            val taskId: String        // 任务标识符
            val action: RestartableAction  // 可重启的操作类型
        }

        /**
         * 可重启状态接口
         * 适用于已取消或失败的任务，可以被重新启动
         */
        interface Restartable {
            val action: RestartableAction  // 重启操作类型
        }

        /** 空闲状态：任务已创建但未开始下载 */
        @Serializable 
        data object Idle : DownloadState

        /**
         * 运行中状态：任务正在下载
         * 
         * @param job 协程任务句柄，用于取消操作
         * @param taskId 任务标识符
         * @param progress 下载进度（0.0-100.0），-1表示未知进度
         * @param progressText 进度描述文本
         * @param speed 下载速度描述
         */
        @Serializable
        data class Running(
            @Transient override val job: Job = Job(),
            override val taskId: String,
            val progress: Float = PROGRESS_INDETERMINATE,
            val progressText: String = "",
            val speed: String = "",
        ) : DownloadState, Cancelable {
            override val action: RestartableAction = RestartableAction.Download
        }

        /**
         * 已取消状态：任务被用户主动取消
         * 
         * @param action 取消前的操作类型
         * @param progress 取消时的进度（可选）
         */
        @Serializable
        data class Canceled(
            override val action: RestartableAction, 
            val progress: Float? = null
        ) : DownloadState, Restartable

        /**
         * 错误状态：任务下载失败
         * 
         * @param throwable 错误异常信息
         * @param action 失败前的操作类型
         */
        @Serializable
        data class Error(
            @Transient val throwable: Throwable = Throwable(),
            override val action: RestartableAction,
        ) : DownloadState, Restartable

        /**
         * 已完成状态：任务下载成功
         * 
         * @param filePath 下载文件的完整路径
         */
        @Serializable 
        data class Completed(val filePath: String?) : DownloadState

        /**
         * 状态优先级比较
         * 运行中 > 空闲 > 已取消 > 错误 > 已完成
         */
        override fun compareTo(other: DownloadState): Int {
            return ordinal - other.ordinal
        }

        /**
         * 状态优先级序号
         * 数字越小优先级越高
         */
        private val ordinal: Int
            get() = when (this) {
                is Running -> 0      // 最高优先级：正在运行
                Idle -> 1           // 次高优先级：等待运行
                is Canceled -> 2    // 中等优先级：已取消
                is Error -> 3       // 较低优先级：错误状态
                is Completed -> 4   // 最低优先级：已完成
            }
    }

    /**
     * 可重启操作类型
     * 定义任务可以重新执行的操作类型
     */
    @Serializable
    sealed interface RestartableAction {
        /** 下载操作：重新开始下载任务 */
        @Serializable 
        data object Download : RestartableAction
    }

    /**
     * 界面显示状态
     * 
     * 用于UI层展示的视频信息：
     * @param url 视频链接
     * @param title 视频标题
     * @param uploader 上传者名称
     * @param extractorKey 提取器标识
     * @param duration 视频时长（秒）
     * @param fileSizeApprox 文件大小估算值（字节）
     * @param thumbnailUrl 缩略图链接
     */
    @SuppressLint("UnsafeOptInUsageError")
    @Serializable
    data class ViewState(
        val url: String = "",
        val title: String = "",
        val uploader: String = "",
        val extractorKey: String = "",
        val duration: Float = 0f,
        val fileSizeApprox: Double = 0.0,
        val thumbnailUrl: String? = null,
    )

    companion object {
        /** 未知进度标识 */
        private const val PROGRESS_INDETERMINATE = -1f
        
        /**
         * 生成任务唯一标识符
         * 
         * @param url 视频链接
         * @param type 视频类型
         * @return 格式为 "url_type" 的唯一标识符
         */
        private fun generateTaskId(url: String, type: VideoType): String {
            val typeId = when (type) {
                VideoType.MP4 -> "mp4"
                VideoType.M3U8 -> "m3u8"
            }
            return "${url}_${typeId}"
        }
    }
}
