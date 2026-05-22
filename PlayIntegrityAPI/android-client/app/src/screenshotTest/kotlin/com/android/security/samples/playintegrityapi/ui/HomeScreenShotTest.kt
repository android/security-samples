package com.android.security.samples.playintegrityapi.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest

class HomeScreenScreenshotTest {

    @PreviewTest
    @Preview(showBackground = true, name = "HomeScreen - Light Mode")
    @Composable
    fun HomeScreenLightPreviewTest() {
        HomeScreenPreview()
    }

    @PreviewTest
    @Preview(showBackground = true, name = "UseCaseCard - Light Mode")
    @Composable
    fun UseCaseCardLightPreviewTest() {
        UseCaseCardPreview()
    }
}