// Copyright 2026 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

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