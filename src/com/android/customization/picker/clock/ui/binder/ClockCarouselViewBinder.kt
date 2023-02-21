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
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.customization.picker.clock.ui.view.ClockCarouselView
import com.android.customization.picker.clock.ui.viewmodel.ClockCarouselViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

object ClockCarouselViewBinder {
    /**
     * The binding is used by the view where there is an action executed from another view, e.g.
     * toggling show/hide of the view that the binder is holding.
     */
    interface Binding {
        fun show()
        fun hide()
    }

    @JvmStatic
    fun bind(
        view: ClockCarouselView,
        viewModel: ClockCarouselViewModel,
        clockViewFactory: (clockId: String) -> View,
        lifecycleOwner: LifecycleOwner,
    ): Binding {
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.allClockIds.collect { allClockIds ->
                        view.setUpClockCarouselView(
                            clockIds = allClockIds,
                            onGetClockPreview = clockViewFactory,
                            onClockSelected = { clockId -> viewModel.setSelectedClock(clockId) },
                        )
                    }
                }
                launch {
                    viewModel.selectedIndex.collect { selectedIndex ->
                        view.setSelectedClockIndex(selectedIndex)
                    }
                }
            }
        }
        return object : Binding {
            override fun show() {
                view.isVisible = true
            }

            override fun hide() {
                view.isVisible = false
            }
        }
    }
}
