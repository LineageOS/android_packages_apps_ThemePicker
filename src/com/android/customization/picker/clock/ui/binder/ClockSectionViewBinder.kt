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

package com.android.customization.picker.clock.ui.binder

import android.view.View
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.android.customization.picker.clock.ui.viewmodel.ClockSectionViewModel
import com.android.wallpaper.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

object ClockSectionViewBinder {
    fun bind(
        view: View,
        viewModel: ClockSectionViewModel,
        lifecycleOwner: LifecycleOwner,
        onClicked: () -> Unit,
    ) {
        view.setOnClickListener { onClicked() }

        val selectedClockTextView: TextView = view.requireViewById(R.id.selected_clock_text)

        lifecycleOwner.lifecycleScope.launch {
            viewModel.selectedClockName
                .flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                .collectLatest { selectedClockName ->
                    selectedClockTextView.text = selectedClockName
                }
        }
    }
}
