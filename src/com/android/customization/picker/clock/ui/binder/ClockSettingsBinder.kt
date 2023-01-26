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
package com.android.customization.picker.clock.ui.binder

import android.view.View
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.customization.picker.clock.shared.ClockSize
import com.android.customization.picker.clock.ui.adapter.ClockSettingsTabAdapter
import com.android.customization.picker.clock.ui.view.ClockSizeRadioButtonGroup
import com.android.customization.picker.clock.ui.viewmodel.ClockSettingsViewModel
import com.android.customization.picker.common.ui.view.ItemSpacing
import com.android.wallpaper.R
import kotlinx.coroutines.launch

/** Bind between the clock settings screen and its view model. */
object ClockSettingsBinder {
    fun bind(
        view: View,
        viewModel: ClockSettingsViewModel,
        lifecycleOwner: LifecycleOwner,
    ) {
        val tabView: RecyclerView = view.requireViewById(R.id.tabs)
        val sizeOptions =
            view.requireViewById<ClockSizeRadioButtonGroup>(R.id.clock_size_radio_button_group)

        val tabAdapter = ClockSettingsTabAdapter()
        tabView.adapter = tabAdapter
        tabView.layoutManager = LinearLayoutManager(view.context, RecyclerView.HORIZONTAL, false)
        tabView.addItemDecoration(ItemSpacing(ItemSpacing.TAB_ITEM_SPACING_DP))

        sizeOptions.onRadioButtonClickListener =
            object : ClockSizeRadioButtonGroup.OnRadioButtonClickListener {
                override fun onClick(size: ClockSize) {
                    viewModel.setClockSize(size)
                }
            }

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.tabs.collect { tabAdapter.setItems(it) } }

                launch {
                    viewModel.selectedTabPosition.collect { tab ->
                        when (tab) {
                            ClockSettingsViewModel.Tab.COLOR -> {
                                sizeOptions.isInvisible = true
                            }
                            ClockSettingsViewModel.Tab.SIZE -> {
                                sizeOptions.isVisible = true
                            }
                        }
                    }
                }

                launch {
                    viewModel.selectedClockSize.collect { size ->
                        when (size) {
                            ClockSize.DYNAMIC -> {
                                sizeOptions.radioButtonDynamic.isChecked = true
                                sizeOptions.radioButtonLarge.isChecked = false
                            }
                            ClockSize.LARGE -> {
                                sizeOptions.radioButtonDynamic.isChecked = false
                                sizeOptions.radioButtonLarge.isChecked = true
                            }
                        }
                    }
                }
            }
        }
    }
}
