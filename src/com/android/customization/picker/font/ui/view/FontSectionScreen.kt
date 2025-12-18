/*
 * Copyright (C) The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.customization.picker.font.ui.view

import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Typeface
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.customization.model.font.FontOption
import com.android.customization.picker.font.ui.viewmodel.FontPickerViewModel

@Composable
fun FontSectionScreen(
    viewModel: FontPickerViewModel,
    isDark: Boolean = isSystemInDarkTheme(),
    modifier: Modifier = Modifier,
) {
    val options by viewModel.fontOptions.collectAsState()
    val selectedOption by viewModel.selectedOption.collectAsState()
    val context = LocalContext.current

    val colorScheme =
        remember(isDark) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (isDark) darkColorScheme() else lightColorScheme()
            }
        }

    MaterialTheme(colorScheme = colorScheme) {
        Column(
            modifier = modifier.fillMaxWidth().wrapContentHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ABC/123 preview
            val previewFont =
                remember(selectedOption) {
                    selectedOption?.headlineFont?.let {
                        FontFamily(Typeface(it))
                    } ?: FontFamily.Default
                }

            // Preview Box
            Surface(
                modifier = Modifier.fillMaxWidth().height(120.dp).padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 2.dp,
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "ABC • abc • 123",
                        style =
                            MaterialTheme.typography.headlineLarge.copy(
                                fontFamily = previewFont,
                                fontSize = 32.sp,
                            ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(options) { option ->
                    val isSelected = option == selectedOption
                    FontOptionItem(
                        option = option,
                        isSelected = isSelected,
                        onClick = { viewModel.selectFont(option) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FontOptionItem(option: FontOption, isSelected: Boolean, onClick: () -> Unit) {
    val borderColor =
        if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant

    val borderWidth = if (isSelected) 2.dp else 1.dp

    val containerColor = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent

    val textColor =
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    val fontFamily =
        remember(option) {
            option.headlineFont?.let {
                FontFamily(Typeface(it))
            } ?: FontFamily.Default
        }

    // Aa
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp).clickable(onClick = onClick),
    ) {
        Surface(
            modifier =
                Modifier.size(80.dp).border(borderWidth, borderColor, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = containerColor,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Aa",
                    style = MaterialTheme.typography.headlineMedium.copy(fontFamily = fontFamily),
                    color = textColor,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Scroll on longer strings
        Text(
            text = option.title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().basicMarquee(),
        )
    }
}
