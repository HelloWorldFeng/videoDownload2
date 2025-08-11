package com.app.videobox.ui.pages.videoDownloadPage

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import com.videodownloader.module.api.*
import com.videodownloader.module.download.Task

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File

/**
 * 下载列表页面ViewModel
 * 遵循标准MVI架构模式，管理下载任务状态和用户交互
 * 
 * 主要职责：
 * 1. 管理多选状态和选中项列表
 * 2. 处理用户交互事件（Intent）
 * 3. 维护UI状态（State）
 * 4. 提供副作用处理（Effect）
 */
class DownloadListViewModel : ViewModel() {
    
    companion object {
        private const val TAG = "DownloadListViewModel"
    }
    
    // ==================== MVI State 定义 ====================
    
    /**
     * UI状态数据类
     * 包含页面所有状态信息
     */
    data class UiState(
        val isSelectModeEnabled: Boolean = false,           // 是否启用多选模式
        val selectedTasks: Set<Task> = emptySet(),          // 已选中的任务列表
        val isLoading: Boolean = false,                     // 是否正在加载
        val errorMessage: String? = null                    // 错误信息
    ) {
        /**
         * 计算属性：是否有选中项
         */
        val hasSelectedItems: Boolean get() = selectedTasks.isNotEmpty()
        
        /**
         * 计算属性：选中项数量
         */
        val selectedCount: Int get() = selectedTasks.size
    }
    
    /**
     * 任务操作类型
     * 定义所有可能的任务操作
     */
    sealed class TaskAction {
        data class OpenFile(val filePath: String?) : TaskAction()  // 打开文件
        object Cancel : TaskAction()                               // 取消下载
        object Delete : TaskAction()                               // 删除任务
        object Resume : TaskAction()                               // 恢复下载
    }

    /**
     * 用户交互意图
     * 定义所有可能的用户操作
     */
    sealed class Intent {
        // 多选相关操作
        object EnableSelectMode : Intent()                  // 启用多选模式
        object DisableSelectMode : Intent()                 // 禁用多选模式
        object ClearSelection : Intent()                    // 清空选择
        data class ToggleTaskSelection(val task: Task) : Intent()  // 切换任务选择状态
        data class SelectTask(val task: Task) : Intent()           // 选择任务
        data class UnselectTask(val task: Task) : Intent()         // 取消选择任务
        data class SelectAllTasks(val tasks: List<Task>) : Intent() // 全选任务
        object ToggleSelectAll : Intent()                   // 切换全选状态
        
        // 任务操作
        data class ExecuteTaskAction(val task: Task, val action: TaskAction) : Intent()  // 执行任务操作
        data class ExecuteBatchAction(val action: TaskAction) : Intent()                 // 批量执行操作

    }
    
    /**
     * 副作用事件
     * 定义需要UI响应的一次性事件
     */
    sealed class Effect {
        data class ShowToast(val message: String) : Effect()       // 显示Toast消息
        data class NavigateToPlayer(val filePath: String) : Effect() // 导航到播放器
        data class ShowError(val error: String) : Effect()         // 显示错误信息
        object ScrollToTop : Effect()                              // 滚动到顶部
    }
    
    // ==================== 状态管理 ====================
    
    /**
     * 内部可变状态
     */
    private val _uiState = MutableStateFlow(UiState())
    
    /**
     * 对外暴露的只读状态
     */
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    /**
     * 副作用事件流
     */
    private val _effects = MutableSharedFlow<Effect>()
    val effects: SharedFlow<Effect> = _effects.asSharedFlow()
    
    // ==================== 公共方法 ====================
    
    /**
     * 处理用户意图
     * 统一的事件处理入口
     * 
     * @param intent 用户意图
     */
    fun handleIntent(intent: Intent) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "处理用户意图: $intent")
                when (intent) {
                    // 多选模式管理
                    is Intent.EnableSelectMode -> enableSelectMode()
                    is Intent.DisableSelectMode -> disableSelectMode()
                    is Intent.ClearSelection -> clearSelection()
                    is Intent.ToggleTaskSelection -> toggleTaskSelection(intent.task)
                    is Intent.SelectTask -> selectTask(intent.task)
                    is Intent.UnselectTask -> unselectTask(intent.task)
                    is Intent.SelectAllTasks -> selectAllTasks(intent.tasks)
                    is Intent.ToggleSelectAll -> toggleSelectAll()
                    
                    // 任务操作
                    is Intent.ExecuteTaskAction -> executeTaskAction(intent.task, intent.action)
                    is Intent.ExecuteBatchAction -> executeBatchAction(intent.action)

                }
            } catch (e: Exception) {
                Log.e(TAG, "处理意图时发生错误: ${e.message}", e)
                handleError(e)
            }
        }
    }
    
    /**
     * 检查任务是否被选中
     * 
     * @param task 要检查的任务
     * @return 是否被选中
     */
    fun isTaskSelected(task: Task): Boolean {
        return _uiState.value.selectedTasks.contains(task)
    }
    
    /**
     * 获取当前选中的任务列表
     * 
     * @return 选中的任务列表
     */
    fun getSelectedTasks(): Set<Task> {
        return _uiState.value.selectedTasks
    }
    
    /**
     * 检查是否全选状态
     * 
     * @param totalTasks 总任务列表
     * @return 是否全选
     */
    fun isAllSelected(totalTasks: List<Task>): Boolean {
        val selectedTasks = _uiState.value.selectedTasks
        return totalTasks.isNotEmpty() && selectedTasks.size == totalTasks.size && 
               totalTasks.all { selectedTasks.contains(it) }
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 启用多选模式
     */
    private suspend fun enableSelectMode() {
        Log.d(TAG, "启用多选模式")
        updateState { 
            copy(isSelectModeEnabled = true) 
        }
    }
    
    /**
     * 禁用多选模式并清空选择
     */
    private suspend fun disableSelectMode() {
        Log.d(TAG, "禁用多选模式")
        updateState { 
            copy(
                isSelectModeEnabled = false,
                selectedTasks = emptySet()
            ) 
        }
    }
    
    /**
     * 清空所有选择
     */
    private suspend fun clearSelection() {
        Log.d(TAG, "清空选择")
        updateState { 
            copy(selectedTasks = emptySet()) 
        }
    }
    
    /**
     * 切换任务选择状态
     * 
     * @param task 要切换的任务
     */
    private suspend fun toggleTaskSelection(task: Task) {
        val currentState = _uiState.value
        val newSelectedTasks = if (currentState.selectedTasks.contains(task)) {
            Log.d(TAG, "取消选择任务: ${task.id}")
            currentState.selectedTasks - task
        } else {
            Log.d(TAG, "选择任务: ${task.id}")
            currentState.selectedTasks + task
        }
        
        updateState { 
            copy(selectedTasks = newSelectedTasks) 
        }
    }
    
    /**
     * 选择任务
     * 
     * @param task 要选择的任务
     */
    private suspend fun selectTask(task: Task) {
        Log.d(TAG, "选择任务: ${task.id}")
        updateState { 
            copy(selectedTasks = selectedTasks + task) 
        }
    }
    
    /**
     * 取消选择任务
     * 
     * @param task 要取消选择的任务
     */
    private suspend fun unselectTask(task: Task) {
        Log.d(TAG, "取消选择任务: ${task.id}")
        updateState { 
            copy(selectedTasks = selectedTasks - task) 
        }
    }
    
    /**
     * 全选任务
     * 
     * @param tasks 要全选的任务列表
     */
    private suspend fun selectAllTasks(tasks: List<Task>) {
        Log.d(TAG, "全选任务: 共${tasks.size}个任务")
        updateState { 
            copy(selectedTasks = tasks.toSet()) 
        }
    }
    
    /**
     * 切换全选状态
     * 需要从外部传入当前任务列表来判断全选状态
     * 这个方法主要用于UI层直接调用，不需要传参
     */
    private suspend fun toggleSelectAll() {
        Log.d(TAG, "切换全选状态")
        // 这个方法需要配合UI层使用，UI层需要判断当前状态并调用相应的方法
        clearSelection()
    }
    
    /**
     * 执行单个任务操作
     * 
     * @param task 目标任务
     * @param action 要执行的操作
     */
    private suspend fun executeTaskAction(task: Task, action: TaskAction) {
        Log.d(TAG, "执行任务操作: task=${task.id}, action=$action")
        
        try {
            when (action) {
                is TaskAction.OpenFile -> {
                    action.filePath?.let { filePath ->
                        _effects.emit(Effect.NavigateToPlayer(filePath))
                    }
                }
                is TaskAction.Cancel -> {
                    Log.d(TAG, "取消任务操作将由外部处理: $action")
                }
                is TaskAction.Delete -> {
                    Log.d(TAG, "删除任务操作将由外部处理: $action")
                }
                is TaskAction.Resume -> {
                    Log.d(TAG, "恢复任务操作将由外部处理: $action")
                }

            }
        } catch (e: Exception) {
            Log.e(TAG, "执行任务操作失败: ${e.message}", e)
            _effects.emit(Effect.ShowError("操作失败: ${e.message}"))
        }
    }
    
    /**
     * 执行批量操作
     * 
     * @param action 要执行的操作
     */
    private suspend fun executeBatchAction(action: TaskAction) {
        val selectedTasks = _uiState.value.selectedTasks
        Log.d(TAG, "执行批量操作: action=$action, 选中任务数=${selectedTasks.size}")
        
        if (selectedTasks.isEmpty()) {
            _effects.emit(Effect.ShowToast("请先选择要操作的任务"))
            return
        }
        
        try {
            // 批量操作完成后禁用多选模式
            when (action) {
                is TaskAction.Delete -> {
                    _effects.emit(Effect.ShowToast("已删除 ${selectedTasks.size} 个任务"))
                    disableSelectMode()
                }
                is TaskAction.Cancel -> {
                    _effects.emit(Effect.ShowToast("已取消 ${selectedTasks.size} 个任务"))
                    disableSelectMode()
                }
                is TaskAction.Resume -> {
                    _effects.emit(Effect.ShowToast("已恢复 ${selectedTasks.size} 个任务"))
                    disableSelectMode()
                }
                else -> {
                    Log.d(TAG, "批量操作将由外部处理: $action")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "执行批量操作失败: ${e.message}", e)
            _effects.emit(Effect.ShowError("批量操作失败: ${e.message}"))
        }
    }
    
    /**
     * 刷新任务列表
     */
    private suspend fun refreshTasks() {
        Log.d(TAG, "刷新任务列表")
        updateState { copy(isLoading = true) }
        
        try {
            // 这里可以添加刷新逻辑
            delay(500) // 模拟刷新延迟
            updateState { copy(isLoading = false) }
            _effects.emit(Effect.ShowToast("刷新完成"))
        } catch (e: Exception) {
            Log.e(TAG, "刷新任务列表失败: ${e.message}", e)
            updateState { copy(isLoading = false) }
            _effects.emit(Effect.ShowError("刷新失败: ${e.message}"))
        }
    }
    
    /**
     * 处理错误
     * 
     * @param error 错误信息
     */
    private suspend fun handleError(error: Throwable) {
        Log.e(TAG, "处理错误: ${error.message}", error)
        val errorMessage = error.message ?: "未知错误"
        updateState { copy(errorMessage = errorMessage) }
        _effects.emit(Effect.ShowError(errorMessage))
    }
    
    /**
     * 更新状态的辅助方法
     * 
     * @param update 状态更新函数
     */
    private suspend fun updateState(update: UiState.() -> UiState) {
        _uiState.value = _uiState.value.update()
    }
    
    // ==================== 生命周期管理 ====================
    
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel被清理")
    }
}