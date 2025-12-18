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

package com.android.customization.model.font

import android.app.Activity
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.android.customization.picker.font.FontSectionView
import com.android.customization.picker.font.ui.view.FontSectionScreen
import com.android.customization.picker.font.ui.viewmodel.FontPickerViewModel
import com.android.themepicker.R
import com.android.wallpaper.model.CustomizationSectionController
import com.google.android.flexbox.FlexboxLayout

class FontSectionController(
    private val viewModelFactory: FontPickerViewModel.Factory,
    private val lifecycleOwner: ViewModelStoreOwner
) : CustomizationSectionController<FontSectionView> {

    override fun isAvailable(context: Context): Boolean = true

    override fun createView(context: Context): FontSectionView {
        val view = LayoutInflater.from(context).inflate(
            R.layout.font_section_view,
            null
        ) as FontSectionView

        // Layout parameters for the entry button
        val lp = FlexboxLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lp.flexBasisPercent = 1.0f
        view.layoutParams = lp

        // Set the text to the current font
        val descriptionView = view.findViewById<TextView>(R.id.font_section_description)
        val viewModel = viewModelFactory.create(FontPickerViewModel::class.java)
        val currentOptions = viewModel.fontOptions.value
        if (currentOptions.isNotEmpty()) {
             val active = viewModel.selectedOption.value ?: currentOptions[0]
             descriptionView.text = active.title
        }

        view.setOnClickListener {
            if (context is Activity) {
                showFloatingSheet(context, viewModel)
            }
        }

        return view
    }

    private fun showFloatingSheet(activity: Activity, viewModel: FontPickerViewModel) {
        val rootView = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)

        // Container
        val sheetContainer = ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.01f))
                        .clickable(
                            interactionSource = androidx.compose.runtime.remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            (parent as? ViewGroup)?.removeView(this)
                        },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    // The Actual Sheet
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .navigationBarsPadding()
                            .padding(bottom = 16.dp)
                            .clickable(enabled = false) {},
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        // Render the compact row
                        FontSectionScreen(
                            viewModel = viewModel,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                }
            }
        }

        // Add to the activity root.
        rootView.addView(sheetContainer, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))
    }
}
