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

package com.android.security.samples.playintegrityapi.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val PiaShapes = Shapes(
    // Used for minimal components like Snack bars, small badges, or Text Field drop-downs
    extraSmall = RoundedCornerShape(4.dp),

    // Used for standard TextFields, Chips, and Tooltips
    small = RoundedCornerShape(8.dp),

    // Used for Buttons, standard Cards, and small Dialogs
    medium = RoundedCornerShape(12.dp),

    // Used for prominent surface areas like Navigation Drawers, large Cards, or medium Modals
    large = RoundedCornerShape(16.dp),

    // Used for massive surface transitions like Bottom Sheets or large Dialogs
    extraLarge = RoundedCornerShape(24.dp)
)