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
import android.view.ViewStub
import androidx.core.view.isGone
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.android.customization.picker.clock.data.repository.ClockRegistryProvider
import com.android.customization.picker.clock.ui.binder.ClockCarouselViewBinder
import com.android.customization.picker.clock.ui.view.ClockCarouselView
import com.android.customization.picker.clock.ui.view.ClockViewFactory
import com.android.customization.picker.clock.ui.viewmodel.ClockCarouselViewModel
import com.android.systemui.shared.clocks.ClockRegistry
import com.android.wallpaper.R
import com.android.wallpaper.model.CustomizationSectionController
import com.android.wallpaper.model.WallpaperColorsViewModel
import com.android.wallpaper.module.CurrentWallpaperInfoFactory
import com.android.wallpaper.module.CustomizationSections
import com.android.wallpaper.picker.customization.ui.section.ScreenPreviewSectionController
import com.android.wallpaper.picker.customization.ui.section.ScreenPreviewView
import com.android.wallpaper.util.DisplayUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Controls the screen preview section. */
@OptIn(ExperimentalCoroutinesApi::class)
class PreviewWithClockCarouselSectionController(
    activity: Activity,
    private val lifecycleOwner: LifecycleOwner,
    private val initialScreen: CustomizationSections.Screen,
    wallpaperInfoFactory: CurrentWallpaperInfoFactory,
    colorViewModel: WallpaperColorsViewModel,
    displayUtils: DisplayUtils,
    private val clockRegistryProvider: ClockRegistryProvider,
    private val clockCarouselViewModelProvider: ClockCarouselViewModelProvider,
    private val clockViewFactoryProvider: ClockViewFactoryProvider,
    navigator: CustomizationSectionController.CustomizationSectionNavigationController,
) :
    ScreenPreviewSectionController(
        activity,
        lifecycleOwner,
        initialScreen,
        wallpaperInfoFactory,
        colorViewModel,
        displayUtils,
        navigator,
    ) {

    private var clockCarouselBinding: ClockCarouselViewBinder.Binding? = null

    override val hideLockScreenClockPreview = true

    override fun createView(context: Context): ScreenPreviewView {
        val view = super.createView(context)
        val carouselViewStub: ViewStub = view.requireViewById(R.id.clock_carousel_view_stub)
        carouselViewStub.layoutResource = R.layout.clock_carousel_view
        val carouselView: ClockCarouselView = carouselViewStub.inflate() as ClockCarouselView
        carouselView.isGone = true
        lifecycleOwner.lifecycleScope.launch {
            val registry = withContext(Dispatchers.IO) { clockRegistryProvider.get() }
            val clockViewFactory = clockViewFactoryProvider.get(registry)
            clockCarouselBinding =
                ClockCarouselViewBinder.bind(
                    view = carouselView,
                    viewModel = clockCarouselViewModelProvider.get(registry),
                    clockViewFactory = { clockId -> clockViewFactory.getView(clockId) },
                    lifecycleOwner = lifecycleOwner,
                )
            onScreenSwitched(
                isOnLockScreen = initialScreen == CustomizationSections.Screen.LOCK_SCREEN
            )
        }
        return view
    }

    override fun onScreenSwitched(isOnLockScreen: Boolean) {
        super.onScreenSwitched(isOnLockScreen)
        if (isOnLockScreen) {
            clockCarouselBinding?.show()
        } else {
            clockCarouselBinding?.hide()
        }
    }

    interface ClockCarouselViewModelProvider {
        fun get(registry: ClockRegistry): ClockCarouselViewModel
    }

    interface ClockViewFactoryProvider {
        fun get(registry: ClockRegistry): ClockViewFactory
    }
}
