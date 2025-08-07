package com.videodownloader.module

import com.blankj.utilcode.util.SPStaticUtils
import com.videodownloader.module.download.Task
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json {
    ignoreUnknownKeys = true
    allowStructuredMapKeys = true
}
private val TASK_LIST = "TASK_LIST"

fun encodeTaskListBackup(map: Map<Task, Task.State>) =
    runCatching {
        json.encodeToString<Map<Task, Task.State>>(map)
    }
        .onSuccess {
            SPStaticUtils.put(TASK_LIST,it)
        }
        .onFailure { it.printStackTrace() }

fun decodeTaskListBackup(): Map<Task, Task.State> =
    runCatching {
        SPStaticUtils.getString(TASK_LIST)?.let {
            json.decodeFromString<Map<Task, Task.State>>(it)
        }
    }
        .onFailure { it.printStackTrace() }
        .getOrNull() ?: emptyMap()