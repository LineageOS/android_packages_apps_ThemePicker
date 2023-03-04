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
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.customization.model.color.ColorBundle
import com.android.customization.model.color.ColorSeedOption
import com.android.customization.picker.clock.domain.interactor.ClockPickerInteractor
import com.android.customization.picker.clock.shared.ClockSize
import com.android.customization.picker.clock.shared.model.ClockMetadataModel
import com.android.customization.picker.color.domain.interactor.ColorPickerInteractor
import com.android.customization.picker.color.shared.model.ColorType
import com.android.customization.picker.color.ui.viewmodel.ColorOptionViewModel
import com.android.wallpaper.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** View model for the clock settings screen. */
class ClockSettingsViewModel
private constructor(
    context: Context,
    private val clockPickerInteractor: ClockPickerInteractor,
    private val colorPickerInteractor: ColorPickerInteractor,
) : ViewModel() {

    enum class Tab {
        COLOR,
        SIZE,
    }

    val selectedColor: StateFlow<Int?> =
        clockPickerInteractor.selectedColor.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val sliderProgressColorTone = MutableStateFlow(ClockMetadataModel.DEFAULT_COLOR_TONE)
    val isSliderEnabled: Flow<Boolean> =
        clockPickerInteractor.selectedColor.map { it != null }.distinctUntilChanged()
    val sliderProgress: Flow<Int> = merge(clockPickerInteractor.colorTone, sliderProgressColorTone)

    private val _seedColor: MutableStateFlow<Int?> = MutableStateFlow(null)
    val seedColor: Flow<Int?> = merge(clockPickerInteractor.seedColor, _seedColor)

    /**
     * The slider color tone updates are quick. Do not set color tone and the blended color to the
     * settings until [onSliderProgressStop] is called. Update to a locally cached temporary
     * [sliderProgressColorTone] and [_seedColor] instead.
     */
    fun onSliderProgressChanged(progress: Int) {
        sliderProgressColorTone.value = progress
        val color = selectedColor.value
        if (color != null) {
            _seedColor.value = blendColorWithTone(color, progress)
        }
    }

    fun onSliderProgressStop(progress: Int) {
        val color = selectedColor.value ?: return
        clockPickerInteractor.setClockColor(
            selectedColor = color,
            colorTone = progress,
            seedColor = blendColorWithTone(color = color, colorTone = progress)
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val colorOptions: StateFlow<List<ColorOptionViewModel>> =
        combine(colorPickerInteractor.colorOptions, clockPickerInteractor.selectedColor, ::Pair)
            .mapLatest { (colorOptions, selectedColor) ->
                // Use mapLatest and delay(100) here to prevent too many selectedClockColor update
                // events from ClockRegistry upstream, caused by sliding the saturation level bar.
                delay(COLOR_OPTIONS_EVENT_UPDATE_DELAY_MILLIS)
                buildList {
                    val defaultThemeColorOptionViewModel =
                        (colorOptions[ColorType.WALLPAPER_COLOR]
                                ?.find { it.isSelected }
                                ?.colorOption as? ColorSeedOption)
                            ?.toColorOptionViewModel(
                                context,
                                selectedColor,
                            )
                            ?: (colorOptions[ColorType.BASIC_COLOR]
                                    ?.find { it.isSelected }
                                    ?.colorOption as? ColorBundle)
                                ?.toColorOptionViewModel(
                                    context,
                                    selectedColor,
                                )
                    if (defaultThemeColorOptionViewModel != null) {
                        add(defaultThemeColorOptionViewModel)
                    }

                    val selectedColorPosition =
                        if (selectedColor != null) {
                            COLOR_SET_LIST.indexOf(selectedColor)
                        } else {
                            -1
                        }

                    COLOR_SET_LIST.forEachIndexed { index, color ->
                        val isSelected = selectedColorPosition == index
                        val colorTone = ClockMetadataModel.DEFAULT_COLOR_TONE
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
                                        {
                                            clockPickerInteractor.setClockColor(
                                                selectedColor = color,
                                                colorTone = colorTone,
                                                seedColor =
                                                    blendColorWithTone(
                                                        color = color,
                                                        colorTone = colorTone,
                                                    ),
                                            )
                                        }
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

    private fun ColorSeedOption.toColorOptionViewModel(
        context: Context,
        selectedColor: Int?,
    ): ColorOptionViewModel {
        val colors = previewInfo.resolveColors(context.resources)
        return ColorOptionViewModel(
            color0 = colors[0],
            color1 = colors[1],
            color2 = colors[2],
            color3 = colors[3],
            contentDescription = getContentDescription(context).toString(),
            title = context.getString(R.string.default_theme_title),
            isSelected = selectedColor == null,
            onClick =
                if (selectedColor == null) {
                    null
                } else {
                    {
                        clockPickerInteractor.setClockColor(
                            selectedColor = null,
                            colorTone = ClockMetadataModel.DEFAULT_COLOR_TONE,
                            seedColor = null,
                        )
                    }
                },
        )
    }

    private fun ColorBundle.toColorOptionViewModel(
        context: Context,
        selectedColor: Int?
    ): ColorOptionViewModel {
        val primaryColor = previewInfo.resolvePrimaryColor(context.resources)
        val secondaryColor = previewInfo.resolveSecondaryColor(context.resources)
        return ColorOptionViewModel(
            color0 = primaryColor,
            color1 = secondaryColor,
            color2 = primaryColor,
            color3 = secondaryColor,
            contentDescription = getContentDescription(context).toString(),
            title = context.getString(R.string.default_theme_title),
            isSelected = selectedColor == null,
            onClick =
                if (selectedColor == null) {
                    null
                } else {
                    {
                        clockPickerInteractor.setClockColor(
                            selectedColor = null,
                            colorTone = ClockMetadataModel.DEFAULT_COLOR_TONE,
                            seedColor = null,
                        )
                    }
                },
        )
    }

    val selectedClockSize: Flow<ClockSize> = clockPickerInteractor.selectedClockSize

    fun setClockSize(size: ClockSize) {
        viewModelScope.launch { clockPickerInteractor.setClockSize(size) }
    }

    private val _selectedTabPosition = MutableStateFlow(Tab.COLOR)
    val selectedTab: StateFlow<Tab> = _selectedTabPosition.asStateFlow()
    val tabs: Flow<List<ClockSettingsTabViewModel>> =
        selectedTab.map {
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
        val COLOR_SET_LIST =
            listOf(
                -786432,
                -3648768,
                -9273600,
                -16739840,
                -9420289,
                -6465837,
                -5032719,
            )

        fun blendColorWithTone(color: Int, colorTone: Int): Int {
            val helperColorLab: DoubleArray by lazy { DoubleArray(3) }
            ColorUtils.colorToLAB(color, helperColorLab)
            return ColorUtils.LABToColor(
                colorTone.toDouble(),
                helperColorLab[1],
                helperColorLab[2],
            )
        }

        const val COLOR_OPTIONS_EVENT_UPDATE_DELAY_MILLIS: Long = 100
    }

    class Factory(
        private val context: Context,
        private val clockPickerInteractor: ClockPickerInteractor,
        private val colorPickerInteractor: ColorPickerInteractor,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ClockSettingsViewModel(
                context = context,
                clockPickerInteractor = clockPickerInteractor,
                colorPickerInteractor = colorPickerInteractor,
            )
                as T
        }
    }
}
