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
package com.android.customization.picker.clock.ui.viewmodel

import android.content.Context
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.customization.picker.clock.domain.interactor.ClockPickerInteractor
import com.android.customization.picker.clock.shared.ClockSize
import com.android.customization.picker.color.ui.viewmodel.ColorOptionViewModel
import com.android.wallpaper.R
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** View model for the clock settings screen. */
class ClockSettingsViewModel
private constructor(context: Context, private val interactor: ClockPickerInteractor) : ViewModel() {

    enum class Tab {
        COLOR,
        SIZE,
    }

    private val helperColorHsl: FloatArray by lazy { FloatArray(3) }

    /**
     * Saturation level of the current selected color. Note that this can be null if the selected
     * color is null, which means that the clock color respects the system theme color. In this
     * case, the saturation level is no longer needed since we do not allow changing saturation
     * level of the system theme color.
     */
    private val saturationLevel: Flow<Float?> =
        interactor.selectedClockColor
            .map { selectedColor ->
                if (selectedColor == null) {
                    null
                } else {
                    ColorUtils.colorToHSL(selectedColor, helperColorHsl)
                    helperColorHsl[1]
                }
            }
            .shareIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(),
                replay = 1,
            )

    /**
     * When the selected clock color is null, it means that the clock will respect the system theme
     * color. And we no longer need the slider, which determines the saturation level of the clock's
     * overridden color.
     */
    val isSliderEnabled: Flow<Boolean> = saturationLevel.map { it != null }

    /**
     * Slide progress from 0 to 100. Note that this can be null if the selected color is null, which
     * means that the clock color respects the system theme color. In this case, the saturation
     * level is no longer needed since we do not allow changing saturation level of the system theme
     * color.
     */
    val sliderProgress: Flow<Int?> =
        saturationLevel.map { saturation -> saturation?.let { (it * 100).roundToInt() } }

    fun onSliderProgressChanged(progress: Int) {
        val saturation = progress / 100f
        val selectedOption = colorOptions.value.find { option -> option.isSelected }
        selectedOption?.let { option ->
            ColorUtils.colorToHSL(option.color0, helperColorHsl)
            helperColorHsl[1] = saturation
            interactor.setClockColor(ColorUtils.HSLToColor(helperColorHsl))
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val colorOptions: StateFlow<List<ColorOptionViewModel>> =
        interactor.selectedClockColor
            .mapLatest { selectedColor ->
                // Use mapLatest and delay(100) here to prevent too many selectedClockColor update
                // events from ClockRegistry upstream, caused by sliding the saturation level bar.
                delay(COLOR_OPTIONS_EVENT_UPDATE_DELAY_MILLIS)
                buildList {
                    // TODO (b/241966062) Change design of the placeholder for default theme color
                    add(
                        ColorOptionViewModel(
                            color0 = Color.TRANSPARENT,
                            color1 = Color.TRANSPARENT,
                            color2 = Color.TRANSPARENT,
                            color3 = Color.TRANSPARENT,
                            contentDescription =
                                context.getString(
                                    R.string.content_description_color_option,
                                ),
                            isSelected = selectedColor == null,
                            onClick =
                                if (selectedColor == null) {
                                    null
                                } else {
                                    { interactor.setClockColor(null) }
                                },
                        )
                    )

                    if (selectedColor != null) {
                        ColorUtils.colorToHSL(selectedColor, helperColorHsl)
                    }

                    val selectedColorPosition =
                        if (selectedColor != null) {
                            getSelectedColorPosition(helperColorHsl)
                        } else {
                            -1
                        }

                    COLOR_LIST_HSL.forEachIndexed { index, colorHSL ->
                        val color = ColorUtils.HSLToColor(colorHSL)
                        val isSelected = selectedColorPosition == index
                        val colorToSet: Int by lazy {
                            val saturation =
                                if (selectedColor != null) {
                                    helperColorHsl[1]
                                } else {
                                    colorHSL[1]
                                }
                            ColorUtils.HSLToColor(
                                listOf(
                                        colorHSL[0],
                                        saturation,
                                        colorHSL[2],
                                    )
                                    .toFloatArray()
                            )
                        }
                        add(
                            ColorOptionViewModel(
                                color0 = color,
                                color1 = color,
                                color2 = color,
                                color3 = color,
                                contentDescription =
                                    context.getString(
                                        R.string.content_description_color_option,
                                        index,
                                    ),
                                isSelected = isSelected,
                                onClick =
                                    if (isSelected) {
                                        null
                                    } else {
                                        { interactor.setClockColor(colorToSet) }
                                    },
                            )
                        )
                    }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = emptyList(),
            )

    val selectedClockSize: Flow<ClockSize> = interactor.selectedClockSize

    fun setClockSize(size: ClockSize) {
        viewModelScope.launch { interactor.setClockSize(size) }
    }

    private val _selectedTabPosition = MutableStateFlow(Tab.COLOR)
    val selectedTabPosition: StateFlow<Tab> = _selectedTabPosition.asStateFlow()
    val tabs: Flow<List<ClockSettingsTabViewModel>> =
        selectedTabPosition.map {
            listOf(
                ClockSettingsTabViewModel(
                    name = context.resources.getString(R.string.clock_color),
                    isSelected = it == Tab.COLOR,
                    onClicked =
                        if (it == Tab.COLOR) {
                            null
                        } else {
                            { _selectedTabPosition.tryEmit(Tab.COLOR) }
                        }
                ),
                ClockSettingsTabViewModel(
                    name = context.resources.getString(R.string.clock_size),
                    isSelected = it == Tab.SIZE,
                    onClicked =
                        if (it == Tab.SIZE) {
                            null
                        } else {
                            { _selectedTabPosition.tryEmit(Tab.SIZE) }
                        }
                ),
            )
        }

    companion object {
        // TODO (b/241966062) The color integers here are temporary for dev purposes. We need to
        //                    finalize the overridden colors.
        val COLOR_LIST_HSL =
            listOf(
                arrayOf(225f, 0.65f, 0.74f).toFloatArray(),
                arrayOf(30f, 0.65f, 0.74f).toFloatArray(),
                arrayOf(249f, 0.65f, 0.74f).toFloatArray(),
                arrayOf(144f, 0.65f, 0.74f).toFloatArray(),
            )

        const val COLOR_OPTIONS_EVENT_UPDATE_DELAY_MILLIS: Long = 100

        fun getSelectedColorPosition(selectedColorHsl: FloatArray): Int {
            return COLOR_LIST_HSL.withIndex().minBy { abs(it.value[0] - selectedColorHsl[0]) }.index
        }
    }

    class Factory(
        private val context: Context,
        private val interactor: ClockPickerInteractor,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ClockSettingsViewModel(
                context = context,
                interactor = interactor,
            )
                as T
        }
    }
}
