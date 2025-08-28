package com.app.videobox.ui.pages.homePage

import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {

    var mSelectIndex = mutableIntStateOf(value = 1)
}