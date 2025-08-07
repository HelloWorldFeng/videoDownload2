package com.nexus.core.media.processor.storage

import android.content.Context
import com.nexus.core.media.processor.config.ProcessorConfiguration
import com.nexus.core.media.processor.model.ProcessingTask
import com.nexus.core.media.processor.model.ProcessingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * 数据存储处理器
 * 负责管理任务数据的存储和检索
 */
class DataStorageHandler(
    private val context: Context,
    private val configuration: ProcessorConfiguration
) {
    private val taskCache = ConcurrentHashMap<String, ProcessingTask>()
    private val storageDirectory: File by lazy {
        File(configuration.outputDirectory).apply {
            if (!exists()) mkdirs()
        }
    }
    
    /**
     * 保存任务数据
     */
    suspend fun saveTask(task: ProcessingTask) = withContext(Dispatchers.IO) {
        try {
            taskCache[task.taskId] = task
            // 这里可以添加持久化存储逻辑，如保存到数据库或文件
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 获取任务数据
     */
    suspend fun getTask(taskId: String): ProcessingTask? = withContext(Dispatchers.IO) {
        return@withContext taskCache[taskId]
    }
    
    /**
     * 获取所有任务
     */
    suspend fun getAllTasks(): List<ProcessingTask> = withContext(Dispatchers.IO) {
        return@withContext taskCache.values.toList()
    }
    
    /**
     * 根据状态获取任务
     */
    suspend fun getTasksByStatus(status: ProcessingStatus): List<ProcessingTask> = withContext(Dispatchers.IO) {
        return@withContext taskCache.values.filter { it.status == status }
    }
    
    /**
     * 更新任务状态
     */
    suspend fun updateTaskStatus(taskId: String, status: ProcessingStatus) = withContext(Dispatchers.IO) {
        taskCache[taskId]?.let { task ->
            val updatedTask = task.copy(status = status)
            taskCache[taskId] = updatedTask
        }
    }
    
    /**
     * 删除任务数据
     */
    suspend fun deleteTask(taskId: String) = withContext(Dispatchers.IO) {
        taskCache.remove(taskId)
        // 这里可以添加从持久化存储中删除的逻辑
    }
    
    /**
     * 清理所有数据
     */
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        taskCache.clear()
        // 这里可以添加清理持久化存储的逻辑
    }
    

    
    /**
     * 获取任务文件路径
     */
    fun getTaskFilePath(taskId: String, fileName: String): String {
        val taskDir = File(storageDirectory, taskId)
        if (!taskDir.exists()) taskDir.mkdirs()
        return File(taskDir, fileName).absolutePath
    }
    
    /**
     * 检查存储空间
     */
    fun checkStorageSpace(): Long {
        return storageDirectory.usableSpace
    }
    
    /**
     * 清理临时文件
     */
    suspend fun cleanupTempFiles() = withContext(Dispatchers.IO) {
        try {
            val tempDir = File(storageDirectory, "temp")
            if (tempDir.exists()) {
                tempDir.listFiles()?.forEach { file ->
                    if (file.isFile && System.currentTimeMillis() - file.lastModified() > 24 * 60 * 60 * 1000) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}