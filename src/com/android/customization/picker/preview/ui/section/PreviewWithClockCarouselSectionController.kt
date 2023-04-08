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

package com.android.customization.picker.preview.ui.section

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.android.customization.picker.clock.ui.binder.ClockCarouselViewBinder
import com.android.customization.picker.clock.ui.fragment.ClockSettingsFragment
import com.android.customization.picker.clock.ui.view.ClockCarouselView
import com.android.customization.picker.clock.ui.view.ClockViewFactory
import com.android.customization.picker.clock.ui.viewmodel.ClockCarouselViewModel
import com.android.wallpaper.R
import com.android.wallpaper.model.CustomizationSectionController.CustomizationSectionNavigationController
import com.android.wallpaper.model.WallpaperColorsViewModel
import com.android.wallpaper.model.WallpaperPreviewNavigator
import com.android.wallpaper.module.CurrentWallpaperInfoFactory
import com.android.wallpaper.module.CustomizationSections
import com.android.wallpaper.picker.customization.domain.interactor.WallpaperInteractor
import com.android.wallpaper.picker.customization.ui.section.ScreenPreviewSectionController
import com.android.wallpaper.picker.customization.ui.section.ScreenPreviewView
import com.android.wallpaper.util.DisplayUtils
import kotlinx.coroutines.launch

/** Controls the screen preview section. */
class PreviewWithClockCarouselSectionController(
    activity: Activity,
    private val lifecycleOwner: LifecycleOwner,
    private val screen: CustomizationSections.Screen,
    wallpaperInfoFactory: CurrentWallpaperInfoFactory,
    colorViewModel: WallpaperColorsViewModel,
    displayUtils: DisplayUtils,
    private val clockCarouselViewModel: ClockCarouselViewModel,
    private val clockViewFactory: ClockViewFactory,
    wallpaperPreviewNavigator: WallpaperPreviewNavigator,
    private val navigationController: CustomizationSectionNavigationController,
    wallpaperInteractor: WallpaperInteractor,
) :
    ScreenPreviewSectionController(
        activity,
        lifecycleOwner,
        screen,
        wallpaperInfoFactory,
        colorViewModel,
        displayUtils,
        wallpaperPreviewNavigator,
        wallpaperInteractor,
    ) {

    private var clockColorAndSizeButton: View? = null

    override val hideLockScreenClockPreview = true

    override fun createView(context: Context): ScreenPreviewView {
        val view = super.createView(context)
        if (screen == CustomizationSections.Screen.LOCK_SCREEN) {
            val clockColorAndSizeButtonStub: ViewStub =
                view.requireViewById(R.id.clock_color_and_size_button)
            clockColorAndSizeButtonStub.layoutResource = R.layout.clock_color_and_size_button
            clockColorAndSizeButton = clockColorAndSizeButtonStub.inflate() as View
            clockColorAndSizeButton?.setOnClickListener {
                navigationController.navigateTo(ClockSettingsFragment())
            }

            val carouselViewStub: ViewStub = view.requireViewById(R.id.clock_carousel_view_stub)
            carouselViewStub.layoutResource = R.layout.clock_carousel_view
            val carouselView = carouselViewStub.inflate() as ClockCarouselView

            // TODO (b/270716937) We should handle the single clock case in the clock carousel
            // itself
            val singleClockViewStub: ViewStub = view.requireViewById(R.id.single_clock_view_stub)
            singleClockViewStub.layoutResource = R.layout.single_clock_view
            val singleClockView = singleClockViewStub.inflate() as ViewGroup
            lifecycleOwner.lifecycleScope.launch {
                ClockCarouselViewBinder.bind(
                    carouselView = carouselView,
                    singleClockView = singleClockView,
                    viewModel = clockCarouselViewModel,
                    clockViewFactory = clockViewFactory,
                    lifecycleOwner = lifecycleOwner,
                )
            }
        }

        return view
    }
}
