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