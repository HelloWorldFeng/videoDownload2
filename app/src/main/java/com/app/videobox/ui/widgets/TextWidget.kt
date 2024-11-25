package com.app.videobox.ui.widgets


import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit

@Composable
fun TextTitle(
    text: String,
    fontSize: TextUnit = TextUnit.Unspecified,
    color: Color = Color.Unspecified,
    modifier:Modifier = Modifier
) {

    Text(text = text, fontSize = fontSize, color = color, modifier = modifier)
}