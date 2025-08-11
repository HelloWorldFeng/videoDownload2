package com.app.videobox.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetDefaults
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color

val ThemeBgColor = Color(0xFF2E2F30)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModalBottomSheetV3(
    modifier: Modifier = Modifier,
    sheetState: SheetState =
        with(LocalDensity.current) {
            SheetState(
                initialValue = SheetValue.Expanded,
                skipPartiallyExpanded = true,
                density = this,
                confirmValueChange = { true },
                skipHiddenState = false
            )
        },
    onDismissRequest: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(horizontal = 28.dp),
    properties: ModalBottomSheetProperties = ModalBottomSheetDefaults.properties,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        properties = properties,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier.background(Color(0xFF2E2F30))
                .padding(contentPadding)
                .fillMaxWidth()
        ) {
            content()
        }
    }
}