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
package com.android.customization.model.picker.color.ui.viewmodel

import android.content.Context
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.android.customization.picker.color.data.repository.FakeColorPickerRepository
import com.android.customization.picker.color.domain.interactor.ColorPickerInteractor
import com.android.customization.picker.color.shared.model.ColorType
import com.android.customization.picker.color.ui.viewmodel.ColorOptionViewModel
import com.android.customization.picker.color.ui.viewmodel.ColorPickerViewModel
import com.android.customization.picker.color.ui.viewmodel.ColorTypeViewModel
import com.android.wallpaper.testing.collectLastValue
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalCoroutinesApi::class)
@SmallTest
@RunWith(JUnit4::class)
class ColorPickerViewModelTest {
    private lateinit var underTest: ColorPickerViewModel

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext

        underTest =
            ColorPickerViewModel.Factory(
                    context = context,
                    interactor =
                        ColorPickerInteractor(
                            repository = FakeColorPickerRepository(context = context),
                        ),
                )
                .create(ColorPickerViewModel::class.java)
    }

    @Test
    fun `Select a color section color`() = runTest {
        val colorSectionOptions = collectLastValue(underTest.colorSectionOptions)

        assertColorOptionUiState(colorOptions = colorSectionOptions(), selectedColorOptionIndex = 0)

        colorSectionOptions()?.get(2)?.onClick?.invoke()
        assertColorOptionUiState(colorOptions = colorSectionOptions(), selectedColorOptionIndex = 2)

        colorSectionOptions()?.get(4)?.onClick?.invoke()
        assertColorOptionUiState(colorOptions = colorSectionOptions(), selectedColorOptionIndex = 4)
    }

    @Test
    fun `Select a preset color`() = runTest {
        val colorTypes = collectLastValue(underTest.colorTypes)
        val colorOptions = collectLastValue(underTest.colorOptions)

        // Initially, the wallpaper color tab should be selected
        assertPickerUiState(
            colorTypes = colorTypes(),
            colorOptions = colorOptions(),
            selectedColorTypeText = "Wallpaper colors",
            selectedColorOptionIndex = 0
        )

        // Select "Basic colors" tab
        colorTypes()?.get(ColorType.BASIC_COLOR)?.onClick?.invoke()
        assertPickerUiState(
            colorTypes = colorTypes(),
            colorOptions = colorOptions(),
            selectedColorTypeText = "Basic colors",
            selectedColorOptionIndex = -1
        )

        // Select a color option
        colorOptions()?.get(2)?.onClick?.invoke()

        // Check original option is no longer selected
        colorTypes()?.get(ColorType.WALLPAPER_COLOR)?.onClick?.invoke()
        assertPickerUiState(
            colorTypes = colorTypes(),
            colorOptions = colorOptions(),
            selectedColorTypeText = "Wallpaper colors",
            selectedColorOptionIndex = -1
        )

        // Check new option is selected
        colorTypes()?.get(ColorType.BASIC_COLOR)?.onClick?.invoke()
        assertPickerUiState(
            colorTypes = colorTypes(),
            colorOptions = colorOptions(),
            selectedColorTypeText = "Basic colors",
            selectedColorOptionIndex = 2
        )
    }

    /**
     * Asserts the entire picker UI state is what is expected. This includes the color type tabs and
     * the color options list.
     *
     * @param colorTypes The observed color type view-models, keyed by ColorType
     * @param colorOptions The observed color options
     * @param selectedColorTypeText The text of the color type that's expected to be selected
     * @param selectedColorOptionIndex The index of the color option that's expected to be selected,
     * -1 stands for no color option should be selected
     */
    private fun assertPickerUiState(
        colorTypes: Map<ColorType, ColorTypeViewModel>?,
        colorOptions: List<ColorOptionViewModel>?,
        selectedColorTypeText: String,
        selectedColorOptionIndex: Int,
    ) {
        assertColorTypeTabUiState(
            colorTypes = colorTypes,
            colorTypeId = ColorType.WALLPAPER_COLOR,
            isSelected = "Wallpaper colors" == selectedColorTypeText,
        )
        assertColorTypeTabUiState(
            colorTypes = colorTypes,
            colorTypeId = ColorType.BASIC_COLOR,
            isSelected = "Basic colors" == selectedColorTypeText,
        )
        assertColorOptionUiState(colorOptions, selectedColorOptionIndex)
    }

    /**
     * Asserts the picker section UI state is what is expected.
     *
     * @param colorOptions The observed color options
     * @param selectedColorOptionIndex The index of the color option that's expected to be selected,
     * -1 stands for no color option should be selected
     */
    private fun assertColorOptionUiState(
        colorOptions: List<ColorOptionViewModel>?,
        selectedColorOptionIndex: Int,
    ) {
        var foundSelectedColorOption = false
        assertThat(colorOptions).isNotNull()
        if (colorOptions != null) {
            for (i in colorOptions.indices) {
                val colorOptionHasSelectedIndex = i == selectedColorOptionIndex
                assertWithMessage(
                        "Expected color option with index \"${i}\" to have" +
                            " isSelected=$colorOptionHasSelectedIndex but it was" +
                            " ${colorOptions[i].isSelected}, num options: ${colorOptions.size}"
                    )
                    .that(colorOptions[i].isSelected)
                    .isEqualTo(colorOptionHasSelectedIndex)
                foundSelectedColorOption = foundSelectedColorOption || colorOptionHasSelectedIndex
            }
            if (selectedColorOptionIndex == -1) {
                assertWithMessage(
                        "Expected no color options to be selected, but a color option is" +
                            " selected"
                    )
                    .that(foundSelectedColorOption)
                    .isFalse()
            } else {
                assertWithMessage(
                        "Expected a color option to be selected, but no color option is" +
                            " selected"
                    )
                    .that(foundSelectedColorOption)
                    .isTrue()
            }
        }
    }

    /**
     * Asserts that a color type tab has the correct UI state.
     *
     * @param colorTypes The observed color type view-models, keyed by ColorType enum
     * @param colorTypeId the ID of the color type to assert
     * @param isSelected Whether that color type should be selected
     */
    private fun assertColorTypeTabUiState(
        colorTypes: Map<ColorType, ColorTypeViewModel>?,
        colorTypeId: ColorType,
        isSelected: Boolean,
    ) {
        val viewModel =
            colorTypes?.get(colorTypeId) ?: error("No color type with ID \"$colorTypeId\"!")
        assertThat(viewModel.isSelected).isEqualTo(isSelected)
    }
}
