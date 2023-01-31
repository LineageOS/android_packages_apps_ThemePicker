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
package com.android.customization.picker.color.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.android.customization.module.ThemePickerInjector
import com.android.customization.picker.color.ui.binder.ColorPickerBinder
import com.android.wallpaper.R
import com.android.wallpaper.model.WallpaperColorsViewModel
import com.android.wallpaper.module.InjectorProvider
import com.android.wallpaper.picker.AppbarFragment

class ColorPickerFragment : AppbarFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view =
            inflater.inflate(
                R.layout.fragment_color_picker,
                container,
                false,
            )
        setUpToolbar(view)
        val injector = InjectorProvider.getInjector() as ThemePickerInjector
        val wcViewModel = ViewModelProvider(requireActivity())[WallpaperColorsViewModel::class.java]
        ColorPickerBinder.bind(
            view = view,
            viewModel =
                ViewModelProvider(
                        requireActivity(),
                        injector.getColorPickerViewModelFactory(
                            context = requireContext(),
                            wallpaperColorsViewModel = wcViewModel,
                        ),
                    )
                    .get(),
            lifecycleOwner = this,
        )
        return view
    }

    override fun getDefaultTitle(): CharSequence {
        return requireContext().getString(R.string.color_picker_title)
    }
}
