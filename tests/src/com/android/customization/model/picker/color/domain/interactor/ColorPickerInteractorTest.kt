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
package com.android.customization.model.picker.color.domain.interactor

import android.content.Context
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.android.customization.picker.color.data.repository.FakeColorPickerRepository
import com.android.customization.picker.color.domain.interactor.ColorPickerInteractor
import com.android.customization.picker.color.shared.model.ColorType
import com.android.wallpaper.testing.collectLastValue
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalCoroutinesApi::class)
@SmallTest
@RunWith(JUnit4::class)
class ColorPickerInteractorTest {
    private lateinit var underTest: ColorPickerInteractor

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        underTest =
            ColorPickerInteractor(
                repository = FakeColorPickerRepository(context = context),
            )
    }

    @Test
    fun select() = runTest {
        val colorOptions = collectLastValue(underTest.colorOptions)

        val wallpaperColorOptionModelBefore = colorOptions()?.get(ColorType.WALLPAPER_COLOR)?.get(2)
        assertThat(wallpaperColorOptionModelBefore?.isSelected).isFalse()

        wallpaperColorOptionModelBefore?.let { underTest.select(colorOptionModel = it) }
        val wallpaperColorOptionModelAfter = colorOptions()?.get(ColorType.WALLPAPER_COLOR)?.get(2)
        assertThat(wallpaperColorOptionModelAfter?.isSelected).isTrue()

        val presetColorOptionModelBefore = colorOptions()?.get(ColorType.BASIC_COLOR)?.get(1)
        assertThat(presetColorOptionModelBefore?.isSelected).isFalse()

        presetColorOptionModelBefore?.let { underTest.select(colorOptionModel = it) }
        val presetColorOptionModelAfter = colorOptions()?.get(ColorType.BASIC_COLOR)?.get(1)
        assertThat(presetColorOptionModelAfter?.isSelected).isTrue()
    }
}
