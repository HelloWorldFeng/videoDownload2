package com.app.videobox.ui.pages.homePage

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.videobox.App
import com.app.videobox.network.DataRepository
import com.app.videobox.network.model.HomeUrlModel
import com.app.videobox.network.model.MediaClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 主页ViewModel - 负责管理主页相关的数据状态和业务逻辑
 * 包含热门网站数据、视频分类数据的获取和状态管理
 */
class HomeViewModel : ViewModel() {

    // 热门网站数据状态流
    private val _webUrlState = MutableStateFlow<HomeUrlModel?>(null)
    val webUrlState: StateFlow<HomeUrlModel?> = _webUrlState.asStateFlow()

    // 视频分类数据状态流
    private val _videoClassState = MutableStateFlow<List<MediaClass>>(emptyList())
    val videoClassState: StateFlow<List<MediaClass>> = _videoClassState.asStateFlow()

    // 数据加载状态
    private val _isLoadingWebUrls = MutableStateFlow(false)
    val isLoadingWebUrls: StateFlow<Boolean> = _isLoadingWebUrls.asStateFlow()

    private val _isLoadingVideoClass = MutableStateFlow(false)
    val isLoadingVideoClass: StateFlow<Boolean> = _isLoadingVideoClass.asStateFlow()

    init {
        // ViewModel初始化时开始监听DataRepository的数据流
        observeDataRepositoryFlows()
        // 初始化数据加载
        initializeData()
    }

    /**
     * 监听DataRepository的数据流变化
     * 将DataRepository的数据同步到ViewModel的状态中
     */
    private fun observeDataRepositoryFlows() {
        viewModelScope.launch {
            // 监听热门网站数据流
            DataRepository.webUrlFlow.collect { webUrlModel ->
                _webUrlState.value = webUrlModel
                _isLoadingWebUrls.value = false
                Log.d("HomeViewModel", "热门网站数据已更新: ${webUrlModel?.urlList?.size ?: 0} 个网站")
            }
        }

        viewModelScope.launch {
            // 监听视频分类数据流
            DataRepository.videoClassFlow.collect { videoClasses ->
                _videoClassState.value = videoClasses
                _isLoadingVideoClass.value = false
                Log.d("HomeViewModel", "视频分类数据已更新: ${videoClasses.size} 个分类")
            }
        }
    }

    /**
     * 初始化数据加载
     * 检查数据是否为空，如果为空则触发数据获取
     */
    fun initializeData() {
        // 检查并获取热门网站数据
        if (DataRepository.webUrlFlow.value == null) {
            fetchWebUrls()
        }

        // 检查并获取视频分类数据
        if (DataRepository.videoClassFlow.value.isNullOrEmpty()) {
            fetchVideoClasses()
        }
    }

    /**
     * 获取热门网站数据
     * 设置加载状态并调用DataRepository获取数据
     */
    fun fetchWebUrls() {
        if (_isLoadingWebUrls.value) {
            Log.d("HomeViewModel", "热门网站数据正在加载中，跳过重复请求")
            return
        }

        _isLoadingWebUrls.value = true
        Log.d("HomeViewModel", "开始获取热门网站数据")
        DataRepository.fetchWebUrlList()
    }

    /**
     * 获取视频分类数据
     * 设置加载状态并调用DataRepository获取数据
     */
    fun fetchVideoClasses() {
        if (_isLoadingVideoClass.value) {
            Log.d("HomeViewModel", "视频分类数据正在加载中，跳过重复请求")
            return
        }

        _isLoadingVideoClass.value = true
        Log.d("HomeViewModel", "开始获取视频分类数据")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                DataRepository.getVideoClass()
            } catch (e: Exception) {
                Log.e("HomeViewModel", "获取视频分类数据失败: ${e.message}", e)
                _isLoadingVideoClass.value = false
            }
        }
    }

    /**
     * 刷新所有数据
     * 强制重新获取热门网站和视频分类数据
     */
    fun refreshAllData() {
        Log.d("HomeViewModel", "刷新所有数据")
        fetchWebUrls()
        fetchVideoClasses()
    }

    /**
     * 检查数据是否需要刷新
     * 在页面重新获得焦点时调用，确保数据是最新的
     */
    fun checkAndRefreshDataIfNeeded() {
        // 检查热门网站数据是否为空，如果为空则重新获取
        if (DataRepository.webUrlFlow.value == null && !_isLoadingWebUrls.value) {
            Log.d("HomeViewModel", "热门网站数据为空，重新获取")
            fetchWebUrls()
        }

        // 检查视频分类数据是否为空，如果为空则重新获取
        if (DataRepository.videoClassFlow.value.isNullOrEmpty() && !_isLoadingVideoClass.value) {
            Log.d("HomeViewModel", "视频分类数据为空，重新获取")
            fetchVideoClasses()
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("HomeViewModel", "HomeViewModel已清理")
    }
}