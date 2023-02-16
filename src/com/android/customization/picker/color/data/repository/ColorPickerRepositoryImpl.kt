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

import android.app.WallpaperColors
import android.content.Context
import android.util.Log
import com.android.customization.model.CustomizationManager
import com.android.customization.model.color.ColorBundle
import com.android.customization.model.color.ColorCustomizationManager
import com.android.customization.model.color.ColorOption
import com.android.customization.model.color.ColorSeedOption
import com.android.customization.model.theme.OverlayManagerCompat
import com.android.customization.picker.color.shared.model.ColorOptionModel
import com.android.customization.picker.color.shared.model.ColorType
import com.android.wallpaper.model.WallpaperColorsViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine

// TODO (b/262924623): refactor to remove dependency on ColorCustomizationManager & ColorOption
// TODO (b/268203200): Create test for ColorPickerRepositoryImpl
class ColorPickerRepositoryImpl(
    context: Context,
    wallpaperColorsViewModel: WallpaperColorsViewModel,
) : ColorPickerRepository {

    private val homeWallpaperColors: StateFlow<WallpaperColors?> =
        wallpaperColorsViewModel.homeWallpaperColors
    private val lockWallpaperColors: StateFlow<WallpaperColors?> =
        wallpaperColorsViewModel.lockWallpaperColors
    private val colorManager: ColorCustomizationManager =
        ColorCustomizationManager.getInstance(context, OverlayManagerCompat(context))

    private val _activeColorOption = MutableStateFlow<ColorOptionModel?>(null)
    override val activeColorOption: StateFlow<ColorOptionModel?> = _activeColorOption.asStateFlow()

    override val colorOptions: Flow<Map<ColorType, List<ColorOptionModel>>> =
        combine(activeColorOption, homeWallpaperColors, lockWallpaperColors) {
                activeOption,
                homeColors,
                lockColors ->
                Triple(activeOption, homeColors, lockColors)
            }
            .map { (activeOption, homeColors, lockColors) ->
                suspendCancellableCoroutine { continuation ->
                    colorManager.setWallpaperColors(homeColors, lockColors)
                    colorManager.fetchOptions(
                        object : CustomizationManager.OptionsFetchedListener<ColorOption?> {
                            override fun onOptionsLoaded(options: MutableList<ColorOption?>?) {
                                val wallpaperColorOptions: MutableList<ColorOptionModel> =
                                    mutableListOf()
                                val presetColorOptions: MutableList<ColorOptionModel> =
                                    mutableListOf()
                                options?.forEach { option ->
                                    when (option) {
                                        is ColorSeedOption ->
                                            wallpaperColorOptions.add(option.toModel(activeOption))
                                        is ColorBundle ->
                                            presetColorOptions.add(option.toModel(activeOption))
                                    }
                                }
                                continuation.resumeWith(
                                    Result.success(
                                        mapOf(
                                            ColorType.WALLPAPER_COLOR to wallpaperColorOptions,
                                            ColorType.BASIC_COLOR to presetColorOptions
                                        )
                                    )
                                )
                            }

                            override fun onError(throwable: Throwable?) {
                                Log.e(TAG, "Error loading theme bundles", throwable)
                                continuation.resumeWith(
                                    Result.failure(
                                        throwable ?: Throwable("Error loading theme bundles")
                                    )
                                )
                            }
                        },
                        /* reload= */ false
                    )
                }
            }

    override fun select(colorOptionModel: ColorOptionModel) {
        _activeColorOption.value = colorOptionModel
        val colorOption: ColorOption = colorOptionModel.colorOption
        colorManager.apply(
            colorOption,
            object : CustomizationManager.Callback {
                override fun onSuccess() {
                    _activeColorOption.value = null
                }

                override fun onError(throwable: Throwable?) {
                    _activeColorOption.value = null
                    Log.w(TAG, "Apply theme with error", throwable)
                }
            }
        )
    }

    private fun ColorOption.toModel(activeColorOption: ColorOptionModel?): ColorOptionModel {
        return ColorOptionModel(
            colorOption = this,
            isSelected =
                if (activeColorOption != null) {
                    isEquivalent(activeColorOption.colorOption)
                } else {
                    isActive(colorManager)
                },
        )
    }

    companion object {
        private const val TAG = "ColorPickerRepositoryImpl"
    }
}
