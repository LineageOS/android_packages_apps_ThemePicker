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

import android.graphics.Rect
import android.view.View
import androidx.core.view.ViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.customization.picker.color.ui.adapter.ColorOptionAdapter
import com.android.customization.picker.color.ui.adapter.ColorTypeTabAdapter
import com.android.customization.picker.color.ui.viewmodel.ColorPickerViewModel
import com.android.wallpaper.R
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

object ColorPickerBinder {

    /**
     * Binds view with view-model for a color picker experience. The view should include a Recycler
     * View for color type tabs with id [R.id.color_type_tabs] and a Recycler View for color options
     * with id [R.id.color_options]
     */
    @JvmStatic
    fun bind(
        view: View,
        viewModel: ColorPickerViewModel,
        lifecycleOwner: LifecycleOwner,
    ) {
        val colorTypeTabView: RecyclerView = view.requireViewById(R.id.color_type_tabs)
        val colorOptionContainerView: RecyclerView = view.requireViewById(R.id.color_options)

        val colorTypeTabAdapter = ColorTypeTabAdapter()
        colorTypeTabView.adapter = colorTypeTabAdapter
        colorTypeTabView.layoutManager =
            LinearLayoutManager(view.context, RecyclerView.HORIZONTAL, false)
        colorTypeTabView.addItemDecoration(ItemSpacing())
        val colorOptionAdapter = ColorOptionAdapter()
        colorOptionContainerView.adapter = colorOptionAdapter
        colorOptionContainerView.layoutManager =
            LinearLayoutManager(view.context, RecyclerView.HORIZONTAL, false)
        colorOptionContainerView.addItemDecoration(ItemSpacing())

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.colorTypes
                        .map { colorTypeById -> colorTypeById.values }
                        .collect { colorTypes -> colorTypeTabAdapter.setItems(colorTypes.toList()) }
                }

                launch {
                    viewModel.colorOptions.collect { colorOptions ->
                        colorOptionAdapter.setItems(colorOptions)
                    }
                }
            }
        }
    }

    // TODO (b/262924623): Remove function and use common ItemSpacing after ag/20929223 is merged
    private class ItemSpacing : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(outRect: Rect, itemPosition: Int, parent: RecyclerView) {
            val addSpacingToStart = itemPosition > 0
            val addSpacingToEnd = itemPosition < (parent.adapter?.itemCount ?: 0) - 1
            val isRtl = parent.layoutManager?.layoutDirection == ViewCompat.LAYOUT_DIRECTION_RTL
            val density = parent.context.resources.displayMetrics.density
            if (!isRtl) {
                outRect.left = if (addSpacingToStart) ITEM_SPACING_DP.toPx(density) else 0
                outRect.right = if (addSpacingToEnd) ITEM_SPACING_DP.toPx(density) else 0
            } else {
                outRect.left = if (addSpacingToEnd) ITEM_SPACING_DP.toPx(density) else 0
                outRect.right = if (addSpacingToStart) ITEM_SPACING_DP.toPx(density) else 0
            }
        }

        private fun Int.toPx(density: Float): Int {
            return (this * density).toInt()
        }

        companion object {
            private const val ITEM_SPACING_DP = 8
        }
    }
}
