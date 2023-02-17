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
package com.android.customization.picker.clock.ui.view

import android.app.Activity
import android.view.View
import com.android.systemui.plugins.ClockController
import com.android.systemui.shared.clocks.ClockRegistry
import com.android.wallpaper.R
import com.android.wallpaper.util.ScreenSizeCalculator

class ClockViewFactory(
    private val activity: Activity,
    private val registry: ClockRegistry,
) {
    private val clockControllers: HashMap<String, ClockController> = HashMap()

    fun getView(clockId: String): View {
        return (clockControllers[clockId] ?: initClockController(clockId)).largeClock.view
    }

    private fun initClockController(clockId: String): ClockController {
        val controller =
            registry.createExampleClock(clockId).also { it?.initialize(activity.resources, 0f, 0f) }
        checkNotNull(controller)
        val screenSizeCalculator = ScreenSizeCalculator.getInstance()
        val screenSize = screenSizeCalculator.getScreenSize(activity.windowManager.defaultDisplay)
        val ratio =
            activity.resources.getDimensionPixelSize(R.dimen.screen_preview_height).toFloat() /
                screenSize.y.toFloat()
        controller.largeClock.events.onFontSettingChanged(
            activity.resources.getDimensionPixelSize(R.dimen.large_clock_text_size).toFloat() *
                ratio
        )
        clockControllers[clockId] = controller
        return controller
    }
}
