package com.videodownloader.module.download

import VideoInfo
import android.annotation.SuppressLint
import com.videodownloader.module.download.Task.TypeInfo
import kotlinx.coroutines.Job
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

private val TypeInfo.id: String
    get() =
        when (this) {
            TypeInfo.MP4 -> "mp4"
            TypeInfo.M3U8 -> "m3u8"
        }


private fun makeId(url: String, type: TypeInfo): String =
    "${url}_${type.id}"

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Task(
    val url: String,
    val type: TypeInfo = TypeInfo.MP4,
    val id: String = makeId(url, type),
) : Comparable<Task>
{

    val timeCreated: Long = System.currentTimeMillis()

    override fun compareTo(other: Task): Int {
        return timeCreated.compareTo(other.timeCreated)
    }

    @Serializable
    sealed interface TypeInfo {
        @Serializable data object MP4 : TypeInfo
        
        @Serializable data object M3U8 : TypeInfo
    }

    @SuppressLint("UnsafeOptInUsageError")
    @Serializable
    data class State(
        val downloadState: DownloadState,
        val videoInfo: VideoInfo,
        val viewState: ViewState,
    )

    @Serializable
    sealed interface DownloadState : Comparable<DownloadState> {

        interface Cancelable {
            val job: Job
            val taskId: String
            val action: RestartableAction
        }

        interface Restartable {
            val action: RestartableAction
        }

        @Serializable data object Idle : DownloadState


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

        @Serializable
        data class Canceled(override val action: RestartableAction, val progress: Float? = null) :
            DownloadState, Restartable

        @Serializable
        data class Error(
            @Transient val throwable: Throwable = Throwable(),
            override val action: RestartableAction,
        ) : DownloadState, Restartable

        @Serializable data class Completed(val filePath: String?) : DownloadState

        override fun compareTo(other: DownloadState): Int {
            return ordinal - other.ordinal
        }

        private val ordinal: Int
            get() =
                when (this) {
                    is Canceled -> 4
                    is Error -> 5
                    is Completed -> 6
                    Idle -> 3
                    is Running -> 0
                }
    }


    @Serializable
    sealed interface RestartableAction {

        @Serializable data object Download : RestartableAction
    }

    @SuppressLint("UnsafeOptInUsageError")
    @Serializable
    data class ViewState(
        val url: String = "",
        val title: String = "",
        val uploader: String = "",
        val extractorKey: String = "",
        val duration: Int = 0,
        val fileSizeApprox: Double = .0,
        val thumbnailUrl: String? = null,
    )

    companion object {
        private const val PROGRESS_INDETERMINATE = -1f
    }
}
