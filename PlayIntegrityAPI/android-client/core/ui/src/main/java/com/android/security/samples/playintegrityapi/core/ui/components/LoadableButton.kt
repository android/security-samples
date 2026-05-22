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

package com.android.security.samples.playintegrityapi.core.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.security.samples.playintegrityapi.core.ui.theme.PiaSampleTheme

/**
 * A Material Design 3 [Button] that handles asynchronous loading states.
 *
 * When [isLoading] is true, the [content] is temporarily hidden and replaced with a
 * [CircularProgressIndicator]. The button also automatically ignores clicks and adjusts its
 * visual disabled state based on the reason it is disabled:
 * - If disabled due to [isLoading], it retains its custom [containerColor] but is slightly faded.
 * - If disabled due to [isEnabled] being false, it uses the standard grey disabled styling.
 *
 * @param onClick Called when the user clicks the button. Clicks are ignored if [isEnabled]
 * is false or if [isLoading] is true.
 * @param isLoading Whether the button is currently performing an asynchronous operation.
 * @param modifier The [Modifier] to be applied to this button.
 * @param isEnabled Controls the manually enabled state of the button. Note that the button
 * will be functionally disabled if [isLoading] is true, regardless of this parameter.
 * @param containerColor The background color of the button. Defaults to primary.
 * @param animateContent Whether to smoothly crossfade and animate size changes between the
 * standard [content] and the loading indicator. Defaults to true. Set to false for instant state changes.
 * @param shape Defines the button's shape.
 * @param content The content to be displayed inside the button when [isLoading] is false.
 */
@Composable
fun LoadableButton(
    onClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    animateContent: Boolean = true,
    shape: Shape = MaterialTheme.shapes.medium,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        enabled = isEnabled && !isLoading,
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            disabledContainerColor = if (isLoading) {
                containerColor.copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            },
            disabledContentColor = if (isLoading) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            }
        )
    ) {
        if (animateContent) {
            AnimatedContent(
                targetState = isLoading, transitionSpec = {
                    fadeIn(animationSpec = tween()) togetherWith fadeOut(animationSpec = tween())
                }, label = "button_loading_animation"
            ) { targetIsLoading ->
                if (targetIsLoading) {
                    ButtonLoadingIndicator()
                } else {
                    content()
                }
            }
        } else {
            if (isLoading) {
                ButtonLoadingIndicator()
            } else {
                content()
            }
        }
    }
}

/**
 * An internal helper to ensure the loading indicator remains visually consistent
 * whether it is animated or instantly swapped.
 */
@Composable
private fun ButtonLoadingIndicator() {
    CircularProgressIndicator(
        modifier = Modifier.size(24.dp),
        color = MaterialTheme.colorScheme.onPrimary,
        strokeWidth = 2.dp
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, name = "Loadable Button States")
@Composable
private fun LoadableButtonPreview() {
    PiaSampleTheme(dynamicColor = false) {
        Column(
            modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // State 1: Default / Active
            LoadableButton(
                onClick = {}, isLoading = false, modifier = Modifier.fillMaxWidth()
            ) {
                Text("Submit (Idle)")
            }

            // State 2: Loading (Animated Default)
            LoadableButton(
                onClick = {}, isLoading = true, modifier = Modifier.fillMaxWidth()
            ) {
                Text("Streaming Example")
            }

            // State 3: Custom Color
            LoadableButton(
                onClick = {},
                isLoading = false,
                containerColor = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Stop (Error Color)")
            }

            // State 4: Disabled
            LoadableButton(
                onClick = {},
                isLoading = false,
                isEnabled = false,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Submit (Disabled)")
            }
        }
    }
}