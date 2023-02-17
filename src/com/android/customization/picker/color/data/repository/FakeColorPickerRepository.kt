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
package com.android.customization.picker.color.data.repository

import android.content.Context
import android.graphics.Color
import com.android.customization.model.color.ColorBundle
import com.android.customization.model.color.ColorSeedOption
import com.android.customization.picker.color.shared.model.ColorOptionModel
import com.android.customization.picker.color.shared.model.ColorType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeColorPickerRepository(context: Context) : ColorPickerRepository {
    override val activeColorOption: StateFlow<ColorOptionModel?> =
        MutableStateFlow<ColorOptionModel?>(null)

    private val colorSeedOption0: ColorSeedOption =
        ColorSeedOption.Builder()
            .setLightColors(
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.TRANSPARENT,
                    Color.TRANSPARENT,
                    Color.TRANSPARENT
                )
            )
            .setDarkColors(
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.TRANSPARENT,
                    Color.TRANSPARENT,
                    Color.TRANSPARENT
                )
            )
            .setIndex(0)
            .build()
    private val colorSeedOption1: ColorSeedOption =
        ColorSeedOption.Builder()
            .setLightColors(
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.TRANSPARENT,
                    Color.TRANSPARENT,
                    Color.TRANSPARENT
                )
            )
            .setDarkColors(
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.TRANSPARENT,
                    Color.TRANSPARENT,
                    Color.TRANSPARENT
                )
            )
            .setIndex(1)
            .build()
    private val colorSeedOption2: ColorSeedOption =
        ColorSeedOption.Builder()
            .setLightColors(
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.TRANSPARENT,
                    Color.TRANSPARENT,
                    Color.TRANSPARENT
                )
            )
            .setDarkColors(
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.TRANSPARENT,
                    Color.TRANSPARENT,
                    Color.TRANSPARENT
                )
            )
            .setIndex(2)
            .build()
    private val colorSeedOption3: ColorSeedOption =
        ColorSeedOption.Builder()
            .setLightColors(
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.TRANSPARENT,
                    Color.TRANSPARENT,
                    Color.TRANSPARENT
                )
            )
            .setDarkColors(
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.TRANSPARENT,
                    Color.TRANSPARENT,
                    Color.TRANSPARENT
                )
            )
            .setIndex(3)
            .build()
    private val colorBundle0: ColorBundle = ColorBundle.Builder().setIndex(0).build(context)
    private val colorBundle1: ColorBundle = ColorBundle.Builder().setIndex(1).build(context)
    private val colorBundle2: ColorBundle = ColorBundle.Builder().setIndex(2).build(context)
    private val colorBundle3: ColorBundle = ColorBundle.Builder().setIndex(3).build(context)

    private val _colorOptions =
        MutableStateFlow(
            mapOf(
                ColorType.WALLPAPER_COLOR to
                    listOf(
                        ColorOptionModel(colorOption = colorSeedOption0, isSelected = true),
                        ColorOptionModel(colorOption = colorSeedOption1, isSelected = false),
                        ColorOptionModel(colorOption = colorSeedOption2, isSelected = false),
                        ColorOptionModel(colorOption = colorSeedOption3, isSelected = false)
                    ),
                ColorType.BASIC_COLOR to
                    listOf(
                        ColorOptionModel(colorOption = colorBundle0, isSelected = false),
                        ColorOptionModel(colorOption = colorBundle1, isSelected = false),
                        ColorOptionModel(colorOption = colorBundle2, isSelected = false),
                        ColorOptionModel(colorOption = colorBundle3, isSelected = false)
                    )
            )
        )
    override val colorOptions: StateFlow<Map<ColorType, List<ColorOptionModel>>> =
        _colorOptions.asStateFlow()

    override fun select(colorOptionModel: ColorOptionModel) {
        val colorOptions = _colorOptions.value
        val wallpaperColorOptions = colorOptions[ColorType.WALLPAPER_COLOR]!!
        val newWallpaperColorOptions = buildList {
            wallpaperColorOptions.forEach { option ->
                add(
                    ColorOptionModel(
                        colorOption = option.colorOption,
                        isSelected = option.testEquals(colorOptionModel),
                    )
                )
            }
        }
        val basicColorOptions = colorOptions[ColorType.BASIC_COLOR]!!
        val newBasicColorOptions = buildList {
            basicColorOptions.forEach { option ->
                add(
                    ColorOptionModel(
                        colorOption = option.colorOption,
                        isSelected = option.testEquals(colorOptionModel),
                    )
                )
            }
        }
        _colorOptions.value =
            mapOf(
                ColorType.WALLPAPER_COLOR to newWallpaperColorOptions,
                ColorType.BASIC_COLOR to newBasicColorOptions
            )
    }

    private fun ColorOptionModel.testEquals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        return if (other is ColorOptionModel) {
            val thisColorOptionIsWallpaperColor = this.colorOption is ColorSeedOption
            val otherColorOptionIsWallpaperColor = other.colorOption is ColorSeedOption
            (thisColorOptionIsWallpaperColor == otherColorOptionIsWallpaperColor) &&
                (this.colorOption.index == other.colorOption.index)
        } else {
            false
        }
    }
}
