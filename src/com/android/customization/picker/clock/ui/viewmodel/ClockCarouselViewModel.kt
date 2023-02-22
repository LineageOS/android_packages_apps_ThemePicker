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

import com.android.customization.picker.clock.domain.interactor.ClockPickerInteractor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull

class ClockCarouselViewModel(
    private val interactor: ClockPickerInteractor,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    val allClockIds: Flow<List<String>> =
        interactor.allClocks.mapLatest { clockArray ->
            // Delay to avoid the case that the full list of clocks is not initiated.
            delay(CLOCKS_EVENT_UPDATE_DELAY_MILLIS)
            clockArray.map { it.clockId }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedIndex: Flow<Int> =
        allClockIds
            .flatMapLatest { allClockIds ->
                interactor.selectedClock.map { selectedClock ->
                    val index = allClockIds.indexOf(selectedClock.clockId)
                    if (index >= 0) {
                        index
                    } else {
                        null
                    }
                }
            }
            .mapNotNull { it }

    fun setSelectedClock(clockId: String) {
        interactor.setSelectedClock(clockId)
    }

    companion object {
        const val CLOCKS_EVENT_UPDATE_DELAY_MILLIS: Long = 100
    }
}
