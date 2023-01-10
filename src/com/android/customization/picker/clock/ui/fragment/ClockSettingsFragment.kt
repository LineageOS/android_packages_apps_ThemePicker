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
package com.android.customization.picker.clock.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import androidx.lifecycle.lifecycleScope
import com.android.customization.module.ThemePickerInjector
import com.android.customization.picker.clock.ui.binder.ClockSettingsBinder
import com.android.customization.picker.clock.ui.viewmodel.ClockSettingsViewModel
import com.android.customization.picker.quickaffordance.ui.binder.KeyguardQuickAffordancePreviewBinder
import com.android.customization.picker.quickaffordance.ui.viewmodel.KeyguardQuickAffordancePickerViewModel
import com.android.wallpaper.R
import com.android.wallpaper.module.InjectorProvider
import com.android.wallpaper.picker.AppbarFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class ClockSettingsFragment : AppbarFragment() {
    companion object {
        const val DESTINATION_ID = "clock_settings"

        @JvmStatic
        fun newInstance(): ClockSettingsFragment {
            return ClockSettingsFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view =
            inflater.inflate(
                R.layout.fragment_clock_settings,
                container,
                false,
            )
        setUpToolbar(view)
        val injector = InjectorProvider.getInjector() as ThemePickerInjector

        // TODO(b/262924055): Modify to render the lockscreen properly
        val viewModel: KeyguardQuickAffordancePickerViewModel =
            ViewModelProvider(
                    requireActivity(),
                    injector.getKeyguardQuickAffordancePickerViewModelFactory(requireContext()),
                )
                .get()
        KeyguardQuickAffordancePreviewBinder.bind(
            activity = requireActivity(),
            previewView = view.requireViewById(R.id.preview),
            viewModel = viewModel,
            lifecycleOwner = this,
            offsetToStart =
                injector.getDisplayUtils(requireActivity()).isOnWallpaperDisplay(requireActivity())
        )

        lifecycleScope.launch {
            val clockRegistry =
                withContext(Dispatchers.IO) {
                    injector.getClockRegistryProvider(requireContext()).get()
                }
            ClockSettingsBinder.bind(
                view,
                ClockSettingsViewModel(
                    requireContext(),
                    injector.getClockPickerInteractor(requireContext(), clockRegistry)
                ),
                this@ClockSettingsFragment,
            )
        }

        return view
    }

    override fun getDefaultTitle(): CharSequence {
        return requireContext().getString(R.string.clock_settings_title)
    }
}
