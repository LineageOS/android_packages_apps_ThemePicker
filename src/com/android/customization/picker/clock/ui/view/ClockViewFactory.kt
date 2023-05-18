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
import android.graphics.Rect
import android.util.TypedValue
import android.view.View
import androidx.annotation.ColorInt
import androidx.lifecycle.LifecycleOwner
import com.android.systemui.plugins.ClockController
import com.android.systemui.plugins.WeatherData
import com.android.systemui.shared.clocks.ClockRegistry
import com.android.wallpaper.R
import com.android.wallpaper.util.ScreenSizeCalculator
import com.android.wallpaper.util.TimeUtils.TimeTicker
import java.util.concurrent.ConcurrentHashMap

class ClockViewFactory(
    private val activity: Activity,
    private val registry: ClockRegistry,
) {
    private val timeTickListeners: ConcurrentHashMap<Int, TimeTicker> = ConcurrentHashMap()
    private val clockControllers: HashMap<String, ClockController> = HashMap()

    fun getController(clockId: String): ClockController {
        return clockControllers[clockId] ?: initClockController(clockId)
    }

    fun getView(clockId: String): View {
        return getController(clockId).largeClock.view
    }

    fun updateColorForAllClocks(@ColorInt seedColor: Int?) {
        clockControllers.values.forEach { it.events.onSeedColorChanged(seedColor = seedColor) }
    }

    fun updateColor(clockId: String, @ColorInt seedColor: Int?) {
        return (clockControllers[clockId] ?: initClockController(clockId))
            .events
            .onSeedColorChanged(seedColor)
    }

    fun updateTimeFormat(clockId: String) {
        getController(clockId)
            .events
            .onTimeFormatChanged(android.text.format.DateFormat.is24HourFormat(activity))
    }

    fun registerTimeTicker(owner: LifecycleOwner) {
        val hashCode = owner.hashCode()
        if (timeTickListeners.keys.contains(hashCode)) {
            return
        }

        timeTickListeners[hashCode] =
            TimeTicker.registerNewReceiver(activity.applicationContext) { onTimeTick() }
    }

    private fun onTimeTick() {
        clockControllers.values.forEach { it.largeClock.events.onTimeTick() }
    }

    fun unregisterTimeTicker(owner: LifecycleOwner) {
        val hashCode = owner.hashCode()
        timeTickListeners[hashCode]?.let {
            activity.applicationContext.unregisterReceiver(it)
            timeTickListeners.remove(hashCode)
        }
    }

    private fun initClockController(clockId: String): ClockController {
        val controller =
            registry.createExampleClock(clockId).also { it?.initialize(activity.resources, 0f, 0f) }
        checkNotNull(controller)

        // Configure light/dark theme
        val isLightTheme = TypedValue()
        activity.theme.resolveAttribute(android.R.attr.isLightTheme, isLightTheme, true)
        val isRegionDark = isLightTheme.data == 0
        controller.largeClock.events.onRegionDarknessChanged(isRegionDark)
        // Configure font size
        controller.largeClock.events.onFontSettingChanged(
            activity.resources.getDimensionPixelSize(R.dimen.large_clock_text_size).toFloat()
        )
        controller.largeClock.events.onTargetRegionChanged(getLargeClockRegion())
        // Use placeholder for weather clock preview in picker
        controller.events.onWeatherDataChanged(
            WeatherData(
                description = DESCRIPTION_PLACEHODLER,
                state = WEATHERICON_PLACEHOLDER,
                temperature = TEMPERATURE_PLACEHOLDER,
                useCelsius = USE_CELSIUS_PLACEHODLER,
            )
        )
        clockControllers[clockId] = controller
        return controller
    }

    /**
     * Simulate the function of getLargeClockRegion in KeyguardClockSwitch so that we can get a
     * proper region corresponding to lock screen in picker and for onTargetRegionChanged to scale
     * and position the clock view
     */
    fun getLargeClockRegion(): Rect {
        val screenSizeCalculator = ScreenSizeCalculator.getInstance()
        val screenSize = screenSizeCalculator.getScreenSize(activity.windowManager.defaultDisplay)
        val largeClockTopMargin =
            activity.resources.getDimensionPixelSize(R.dimen.keyguard_large_clock_top_margin)
        val targetHeight =
            activity.resources.getDimensionPixelSize(R.dimen.large_clock_text_size) * 2
        val top = (screenSize.y / 2 - targetHeight / 2 + largeClockTopMargin / 2)
        return Rect(0, top, screenSize.x, (top + targetHeight))
    }

    companion object {
        val DESCRIPTION_PLACEHODLER = ""
        val TEMPERATURE_PLACEHOLDER = 58
        val WEATHERICON_PLACEHOLDER = WeatherData.WeatherStateIcon.MOSTLY_SUNNY
        val USE_CELSIUS_PLACEHODLER = false
    }
}
