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
package com.android.customization.picker.clock.ui.viewmodel

import android.annotation.ColorInt
import android.content.res.Resources
import android.graphics.Color
import androidx.core.content.res.getStringOrThrow
import com.android.wallpaper.R

/** The view model that defines custom clock colors. */
data class ClockColorViewModel(
    val colorId: String,
    val colorName: String?,
    @ColorInt val color: Int,
    private val colorToneMin: Double,
    private val colorToneMax: Double,
) {

    fun getColorTone(progress: Int): Double {
        return colorToneMin + (progress.toDouble() * (colorToneMax - colorToneMin)) / 100
    }

    companion object {
        const val DEFAULT_COLOR_TONE_MIN = 0
        const val DEFAULT_COLOR_TONE_MAX = 100

        fun getPresetColorMap(resources: Resources): Map<String, ClockColorViewModel> {
            val ids = resources.obtainTypedArray(R.array.clock_color_ids)
            val names = resources.obtainTypedArray(R.array.clock_color_names)
            val colors = resources.obtainTypedArray(R.array.clock_colors)
            val colorToneMinList = resources.obtainTypedArray(R.array.clock_color_tone_min)
            val colorToneMaxList = resources.obtainTypedArray(R.array.clock_color_tone_max)
            return buildList {
                    for (i in 0 until ids.length()) {
                        add(
                            ClockColorViewModel(
                                ids.getStringOrThrow(i),
                                names.getString(i),
                                colors.getColor(i, Color.TRANSPARENT),
                                colorToneMinList.getInt(i, DEFAULT_COLOR_TONE_MIN).toDouble(),
                                colorToneMaxList.getInt(i, DEFAULT_COLOR_TONE_MAX).toDouble(),
                            )
                        )
                    }
                }
                .associateBy { it.colorId }
                .also {
                    ids.recycle()
                    names.recycle()
                    colors.recycle()
                    colorToneMinList.recycle()
                    colorToneMaxList.recycle()
                }
        }
    }
}
