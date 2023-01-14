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
 */
package com.android.customization.picker.color

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import com.android.customization.model.color.ColorCustomizationManager
import com.android.customization.model.color.ColorOption
import com.android.wallpaper.R
import com.android.wallpaper.picker.SectionView

/**
 * The class inherits from {@link SectionView} as the view representing the color section of the
 * customization picker. It displays a list of color options and an overflow option.
 */
class ColorSectionView2(context: Context, attrs: AttributeSet?) : SectionView(context, attrs) {

    private val items = mutableListOf<ColorOption>()
    private var onClick: ((ColorOption) -> Unit)? = null
    private var overflowOnClick: (() -> Unit)? = null

    // TODO (b/262924623): make adjustments for large screen
    fun setItems(items: List<ColorOption>, manager: ColorCustomizationManager) {
        this.items.clear()
        this.items.addAll(items)
        val optionContainer = findViewById<LinearLayout>(R.id.color_section_option_container)
        optionContainer.removeAllViews()
        // Last color option is either the last index of the items list, or the second last index
        // of column count. Save the last column of the option container for the overflow option
        val lastOptionIndex = minOf(optionContainer.weightSum.toInt() - 2, items.size - 1)
        if (items.isNotEmpty()) {
            for (position in 0..lastOptionIndex) {
                val item = items[position]
                val itemView =
                    LayoutInflater.from(context)
                        .inflate(R.layout.color_option_section, optionContainer, false)
                item.bindThumbnailTile(itemView.findViewById(R.id.option_tile))
                if (item.isActive(manager)) {
                    val optionSelectedView = itemView.findViewById<ImageView>(R.id.option_selected)
                    optionSelectedView.visibility = VISIBLE
                }
                itemView.setOnClickListener { onClick?.invoke(item) }
                optionContainer.addView(itemView)
            }
        }
        // add overflow option
        val itemView =
            LayoutInflater.from(context)
                .inflate(R.layout.color_option_section_overflow, optionContainer, false)
        itemView.setOnClickListener { overflowOnClick?.invoke() }
        optionContainer.addView(itemView)
    }

    /** Sets the on click callback for a color option. */
    fun setColorOptionOnClick(onClick: (ColorOption) -> Unit) {
        this.onClick = onClick
        if (items.isNotEmpty()) {
            val optionContainer = findViewById<LinearLayout>(R.id.color_section_option_container)
            val lastOptionIndex = minOf(optionContainer.childCount - 2, items.size - 1)
            for (position in 0..lastOptionIndex) {
                val item = items[position]
                val itemView = optionContainer.getChildAt(position)
                itemView.setOnClickListener { onClick.invoke(item) }
            }
        }
    }

    /** Sets the on click callback for the overflow option. */
    fun setOverflowOnClick(onClick: () -> Unit) {
        this.overflowOnClick = onClick
        val optionContainer = findViewById<LinearLayout>(R.id.color_section_option_container)
        if (optionContainer.childCount > 0) {
            val itemView = optionContainer.getChildAt(optionContainer.childCount - 1)
            itemView.setOnClickListener { onClick.invoke() }
        }
    }
}
