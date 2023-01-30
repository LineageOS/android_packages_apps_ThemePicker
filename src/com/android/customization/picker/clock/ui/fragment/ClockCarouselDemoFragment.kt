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
import androidx.lifecycle.lifecycleScope
import com.android.customization.module.ThemePickerInjector
import com.android.customization.picker.clock.ui.binder.ClockCarouselViewBinder
import com.android.customization.picker.clock.ui.view.ClockCarouselView
import com.android.customization.picker.clock.ui.viewmodel.ClockCarouselViewModel
import com.android.wallpaper.R
import com.android.wallpaper.module.InjectorProvider
import com.android.wallpaper.picker.AppbarFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClockCarouselDemoFragment : AppbarFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val injector = InjectorProvider.getInjector() as ThemePickerInjector
        val view = inflater.inflate(R.layout.fragment_clock_carousel_demo, container, false)
        setUpToolbar(view)
        val carouselView = view.requireViewById<ClockCarouselView>(R.id.image_carousel_view)
        lifecycleScope.launch {
            val registry =
                withContext(Dispatchers.IO) {
                    injector.getClockRegistryProvider(requireContext()).get()
                }
            ClockCarouselViewBinder.bind(
                view = carouselView,
                viewModel =
                    ClockCarouselViewModel(
                        injector.getClockPickerInteractor(requireContext(), registry)
                    ),
                clockViewFactory = { clockId ->
                    registry.createExampleClock(clockId)?.largeClock?.view!!
                },
                lifecycleOwner = this@ClockCarouselDemoFragment,
            )
        }

        return view
    }

    override fun getDefaultTitle(): CharSequence {
        return "Clock H-scroll Demo"
    }
}
