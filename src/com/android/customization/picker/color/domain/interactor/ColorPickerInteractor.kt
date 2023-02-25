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
package com.android.customization.picker.color.domain.interactor

import androidx.annotation.VisibleForTesting
import com.android.customization.picker.color.data.repository.ColorPickerRepository
import com.android.customization.picker.color.shared.model.ColorOptionModel
import javax.inject.Provider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

/** Single entry-point for all application state and business logic related to system color. */
class ColorPickerInteractor(
    private val repository: ColorPickerRepository,
    private val snapshotRestorer: Provider<ColorPickerSnapshotRestorer>,
) {
    /**
     * The newly selected color option for overwriting the current active option during an
     * optimistic update, the value is set to null when update fails
     */
    @VisibleForTesting private val activeColorOption = MutableStateFlow<ColorOptionModel?>(null)

    /** List of wallpaper and preset color options on the device, categorized by Color Type */
    val colorOptions =
        combine(repository.colorOptions, activeColorOption) { colorOptions, activeOption ->
            colorOptions
                .map { colorTypeEntry ->
                    colorTypeEntry.key to
                        colorTypeEntry.value.map { colorOptionModel ->
                            val isSelected =
                                if (activeOption != null) {
                                    colorOptionModel.colorOption.isEquivalent(
                                        activeOption.colorOption
                                    )
                                } else {
                                    colorOptionModel.isSelected
                                }
                            ColorOptionModel(
                                colorOption = colorOptionModel.colorOption,
                                isSelected = isSelected
                            )
                        }
                }
                .toMap()
        }

    suspend fun select(colorOptionModel: ColorOptionModel) {
        activeColorOption.value = colorOptionModel
        try {
            repository.select(colorOptionModel)
            snapshotRestorer.get().storeSnapshot(colorOptionModel)
        } catch (e: Exception) {
            activeColorOption.value = null
        }
    }

    fun getCurrentColorOption(): ColorOptionModel = repository.getCurrentColorOption()
}
