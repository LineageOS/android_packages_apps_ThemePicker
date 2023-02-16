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
package com.android.customization.picker.clock.data.repository

import com.android.customization.picker.clock.shared.ClockSize
import com.android.customization.picker.clock.shared.model.ClockMetadataModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine

class FakeClockPickerRepository : ClockPickerRepository {

    override val allClocks: Array<ClockMetadataModel> = fakeClocks

    private val _selectedClockId = MutableStateFlow(fakeClocks[0].clockId)
    private val _clockColor = MutableStateFlow<Int?>(null)
    override val selectedClock: Flow<ClockMetadataModel> =
        combine(_selectedClockId, _clockColor) { selectedClockId, clockColor ->
            val selectedClock = allClocks.find { clock -> clock.clockId == selectedClockId }
            checkNotNull(selectedClock)
            ClockMetadataModel(selectedClock.clockId, selectedClock.name, clockColor)
        }

    private val _selectedClockSize = MutableStateFlow(ClockSize.SMALL)
    override val selectedClockSize: Flow<ClockSize> = _selectedClockSize.asStateFlow()

    override fun setSelectedClock(clockId: String) {
        _selectedClockId.value = clockId
    }

    override fun setClockColor(color: Int?) {
        _clockColor.value = color
    }

    override suspend fun setClockSize(size: ClockSize) {
        _selectedClockSize.value = size
    }

    companion object {
        val fakeClocks =
            arrayOf(
                ClockMetadataModel("clock0", "clock0", null),
                ClockMetadataModel("clock1", "clock1", null),
                ClockMetadataModel("clock2", "clock2", null),
                ClockMetadataModel("clock3", "clock3", null),
            )
    }
}
