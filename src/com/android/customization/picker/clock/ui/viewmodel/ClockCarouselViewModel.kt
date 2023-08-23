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

import android.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.customization.picker.clock.domain.interactor.ClockPickerInteractor
import com.android.customization.picker.clock.shared.ClockSize
import com.android.wallpaper.R
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Clock carousel view model that provides data for the carousel of clock previews. When there is
 * only one item, we should show a single clock preview instead of a carousel.
 */
class ClockCarouselViewModel(
    private val interactor: ClockPickerInteractor,
    private val backgroundDispatcher: CoroutineDispatcher,
) : ViewModel() {
    @OptIn(ExperimentalCoroutinesApi::class)
    val allClocks: StateFlow<List<ClockCarouselItemViewModel>> =
        interactor.allClocks
            .mapLatest { allClocks ->
                // Delay to avoid the case that the full list of clocks is not initiated.
                delay(CLOCKS_EVENT_UPDATE_DELAY_MILLIS)
                allClocks.map { ClockCarouselItemViewModel(it.clockId, it.isSelected) }
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val selectedClockSize: Flow<ClockSize> = interactor.selectedClockSize

    val seedColor: Flow<Int?> = interactor.seedColor

    fun getClockCardColorResId(isDarkThemeEnabled: Boolean): Flow<Int> {
        return interactor.seedColor.map {
            it.let { seedColor ->
                // if seedColor is null, default clock color is selected
                if (seedColor == null) {
                    if (isDarkThemeEnabled) {
                        // In dark mode, use darkest surface container color
                        R.color.system_surface_container_high
                    } else {
                        // In light mode, use lightest surface container color
                        R.color.system_surface_bright
                    }
                } else {
                    val luminance = Color.luminance(seedColor)
                    if (isDarkThemeEnabled) {
                        if (luminance <= CARD_COLOR_CHANGE_LUMINANCE_THRESHOLD_DARK_THEME) {
                            R.color.system_surface_bright
                        } else {
                            R.color.system_surface_container_high
                        }
                    } else {
                        if (luminance <= CARD_COLOR_CHANGE_LUMINANCE_THRESHOLD_LIGHT_THEME) {
                            R.color.system_surface_bright
                        } else {
                            R.color.system_surface_container_highest
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedIndex: Flow<Int> =
        allClocks
            .flatMapLatest { allClockIds ->
                interactor.selectedClockId.map { selectedClockId ->
                    val index = allClockIds.indexOfFirst { it.clockId == selectedClockId }
                    /** Making sure there is no active [setSelectedClockJob] */
                    val isSetClockIdJobActive = setSelectedClockJob?.isActive == true
                    if (index >= 0 && !isSetClockIdJobActive) {
                        index
                    } else {
                        null
                    }
                }
            }
            .mapNotNull { it }

    private var setSelectedClockJob: Job? = null
    fun setSelectedClock(clockId: String) {
        setSelectedClockJob?.cancel()
        setSelectedClockJob =
            viewModelScope.launch(backgroundDispatcher) { interactor.setSelectedClock(clockId) }
    }

    class Factory(
        private val interactor: ClockPickerInteractor,
        private val backgroundDispatcher: CoroutineDispatcher,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ClockCarouselViewModel(
                interactor = interactor,
                backgroundDispatcher = backgroundDispatcher,
            )
                as T
        }
    }

    companion object {
        const val CLOCKS_EVENT_UPDATE_DELAY_MILLIS: Long = 100
        const val CARD_COLOR_CHANGE_LUMINANCE_THRESHOLD_LIGHT_THEME: Float = 0.85f
        const val CARD_COLOR_CHANGE_LUMINANCE_THRESHOLD_DARK_THEME: Float = 0.03f
    }
}
