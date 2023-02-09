/*
 * Copyright (C) 2023 The Android Open Source Project
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
 *
 */

package com.android.customization.picker.color.ui.binder

import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.customization.picker.color.ui.viewmodel.ColorOptionViewModel
import com.android.customization.picker.color.ui.viewmodel.ColorPickerViewModel
import com.android.wallpaper.R
import kotlinx.coroutines.launch

object ColorSectionViewBinder {

    /**
     * Binds view with view-model for color picker section. The view should include a linear layout
     * with id [R.id.color_section_option_container]
     */
    @JvmStatic
    fun bind(
        view: View,
        viewModel: ColorPickerViewModel,
        lifecycleOwner: LifecycleOwner,
        navigationOnClick: (View) -> Unit,
        isConnectedHorizontallyToOtherSections: Boolean = false,
    ) {
        val optionContainer: LinearLayout =
            view.requireViewById(R.id.color_section_option_container)
        val moreColorsButton: View = view.requireViewById(R.id.more_colors)
        if (isConnectedHorizontallyToOtherSections) {
            moreColorsButton.isVisible = true
            moreColorsButton.setOnClickListener(navigationOnClick)
        } else {
            moreColorsButton.isVisible = false
        }
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.colorSectionOptions.collect { colorOptions ->
                        setOptions(
                            options = colorOptions,
                            view = optionContainer,
                            addOverflowOption = !isConnectedHorizontallyToOtherSections,
                            overflowOnClick = navigationOnClick
                        )
                    }
                }
            }
        }
    }

    fun setOptions(
        options: List<ColorOptionViewModel>,
        view: LinearLayout,
        addOverflowOption: Boolean = false,
        overflowOnClick: (View) -> Unit = {}
    ) {
        view.removeAllViews()
        // Color option slot size is the minimum between the color option size and the view column
        // count. When having an overflow option, a slot is reserved for the overflow option.
        val colorOptionSlotSize =
            (if (addOverflowOption) {
                    minOf(view.weightSum.toInt() - 1, options.size)
                } else {
                    minOf(view.weightSum.toInt(), options.size)
                })
                .let { if (it < 0) 0 else it }
        options.subList(0, colorOptionSlotSize).forEach { item ->
            val itemView =
                LayoutInflater.from(view.context)
                    .inflate(R.layout.color_option_no_background, view, false)

            val color0View: ImageView = itemView.requireViewById(R.id.color_preview_0)
            val color1View: ImageView = itemView.requireViewById(R.id.color_preview_1)
            val color2View: ImageView = itemView.requireViewById(R.id.color_preview_2)
            val color3View: ImageView = itemView.requireViewById(R.id.color_preview_3)
            color0View.drawable.colorFilter = BlendModeColorFilter(item.color0, BlendMode.SRC)
            color1View.drawable.colorFilter = BlendModeColorFilter(item.color1, BlendMode.SRC)
            color2View.drawable.colorFilter = BlendModeColorFilter(item.color2, BlendMode.SRC)
            color3View.drawable.colorFilter = BlendModeColorFilter(item.color3, BlendMode.SRC)

            val optionSelectedView = itemView.findViewById<ImageView>(R.id.option_selected)
            optionSelectedView.isVisible = item.isSelected

            itemView.setOnClickListener(
                if (item.onClick != null) {
                    View.OnClickListener { item.onClick.invoke() }
                } else {
                    null
                }
            )
            view.addView(itemView)
        }
        // add overflow option
        if (addOverflowOption) {
            val itemView =
                LayoutInflater.from(view.context)
                    .inflate(R.layout.color_option_overflow_no_background, view, false)
            itemView.setOnClickListener(overflowOnClick)
            view.addView(itemView)
        }
    }
}
