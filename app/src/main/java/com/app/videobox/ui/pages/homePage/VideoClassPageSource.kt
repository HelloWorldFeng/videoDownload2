package com.app.videobox.ui.pages.homePage

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.app.videobox.network.DataRepository
import com.app.videobox.network.model.MediaVideo

//获取视频分类
class VideoClassPageSource(private val categoryId: Int) : PagingSource<Int, MediaVideo>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaVideo> {
        return try {
            val page = params.key ?: 1
            val pageSize = params.loadSize.coerceAtMost(20)
            val response = DataRepository.getVideoList(page, pageSize, categoryId)
            if (response == null) {
                return LoadResult.Error(Exception("Network error"))
            }
            val videos = response.model ?: emptyList()
            val prevKey = if (page > 1) page - 1 else null
            val nextKey = if (videos.isNotEmpty() && videos.size >= pageSize) page + 1 else null
            LoadResult.Page(
                data = videos,
                prevKey = prevKey,
                nextKey = nextKey
            )
        } catch (e: Exception) {
            e.printStackTrace()
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, MediaVideo>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}