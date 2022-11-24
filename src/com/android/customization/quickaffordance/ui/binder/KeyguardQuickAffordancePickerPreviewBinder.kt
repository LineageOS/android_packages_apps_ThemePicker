/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.customization.quickaffordance.ui.binder

import android.view.View
import android.widget.ImageView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.customization.quickaffordance.ui.viewmodel.KeyguardQuickAffordancePickerViewModel
import com.android.systemui.shared.keyguard.shared.model.KeyguardQuickAffordanceSlots
import com.android.wallpaper.R
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

object KeyguardQuickAffordancePickerPreviewBinder {

    /** Binds view with view-model for a lock screen quick affordance preview experience. */
    @JvmStatic
    fun bind(
        view: View,
        viewModel: KeyguardQuickAffordancePickerViewModel,
        lifecycleOwner: LifecycleOwner,
    ) {
        val startView: ImageView = view.requireViewById(R.id.start_affordance)
        val endView: ImageView = view.requireViewById(R.id.end_affordance)
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    updateView(
                        view = startView,
                        viewModel = viewModel,
                        slotId = KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_START,
                    )
                }

                launch {
                    updateView(
                        view = endView,
                        viewModel = viewModel,
                        slotId = KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_END,
                    )
                }
            }
        }
    }

    private suspend fun updateView(
        view: ImageView,
        viewModel: KeyguardQuickAffordancePickerViewModel,
        slotId: String,
    ) {
        viewModel.slots
            .mapNotNull { slotById -> slotById[slotId] }
            .map { slot -> slot.selectedQuickAffordances.firstOrNull() }
            .collect { affordance ->
                view.setImageDrawable(affordance?.icon)
                view.contentDescription = affordance?.contentDescription
            }
    }
}
