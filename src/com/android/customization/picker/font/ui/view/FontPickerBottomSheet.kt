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

import android.app.Dialog
import android.content.res.Configuration
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.android.customization.picker.font.ui.viewmodel.FontPickerViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class FontPickerBottomSheet(private val viewModelFactory: FontPickerViewModel.Factory) :
    BottomSheetDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        dialog.setOnShowListener {
            val window = dialog.window
            if (window != null) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                window.setDimAmount(0f)

                // Edge-to-edge
                WindowCompat.setDecorFitsSystemWindows(window, false)
                window.navigationBarColor = AndroidColor.TRANSPARENT
            }

            val bottomSheet =
                dialog.findViewById<FrameLayout>(
                    com.google.android.material.R.id.design_bottom_sheet
                )
            if (bottomSheet != null) {
                bottomSheet.setBackgroundColor(AndroidColor.TRANSPARENT)
                BottomSheetBehavior.from(bottomSheet).apply {
                    state = BottomSheetBehavior.STATE_EXPANDED
                    skipCollapsed = true
                    isFitToContents = true
                }
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val context = LocalContext.current
                val uiMode =
                    context.applicationContext.resources.configuration.uiMode and
                        Configuration.UI_MODE_NIGHT_MASK
                val isSystemDark = uiMode == Configuration.UI_MODE_NIGHT_YES

                val colorScheme =
                    if (isSystemDark) {
                        dynamicDarkColorScheme(context)
                    } else {
                        dynamicLightColorScheme(context)
                    }

                val bgColor = if (isSystemDark) Color(0xFF1C1C1C) else Color(0xFFF0F0F0)

                MaterialTheme(colorScheme = colorScheme) {
                    Surface(
                        modifier =
                            Modifier.fillMaxWidth()
                                .wrapContentHeight()
                                .navigationBarsPadding()
                                .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                        color = bgColor,
                        tonalElevation = 8.dp,
                    ) {
                        val viewModel =
                            ViewModelProvider(this@FontPickerBottomSheet, viewModelFactory)[
                                FontPickerViewModel::class.java]

                        LaunchedEffect(Unit) {
                            viewModel.applyEvent.collect {
                                dismiss()
                                requireActivity().recreate()
                            }
                        }

                        // Padding
                        FontSectionScreen(
                            viewModel = viewModel,
                            modifier = Modifier.padding(vertical = 24.dp),
                        )
                    }
                }
            }
        }
    }
}
