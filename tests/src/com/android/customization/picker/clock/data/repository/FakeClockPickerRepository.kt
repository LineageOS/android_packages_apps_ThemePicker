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

class FakeClockPickerRepository : ClockPickerRepository {

    override val allClocks: Array<ClockMetadataModel> = fakeClocks

    private val _selectedClock = MutableStateFlow(fakeClocks[0])
    override val selectedClock: Flow<ClockMetadataModel> = _selectedClock.asStateFlow()

    private val _selectedClockSize = MutableStateFlow(ClockSize.LARGE)
    override val selectedClockSize: Flow<ClockSize> = _selectedClockSize.asStateFlow()

    override fun setSelectedClock(clockId: String) {
        val clock = fakeClocks.find { it.clockId == clockId }
        checkNotNull(clock)
        _selectedClock.value = clock
    }

    override fun setClockSize(size: ClockSize) {
        _selectedClockSize.value = size
    }

    companion object {
        val fakeClocks =
            arrayOf(
                ClockMetadataModel("clock0", "clock0"),
                ClockMetadataModel("clock1", "clock1"),
                ClockMetadataModel("clock2", "clock2"),
                ClockMetadataModel("clock3", "clock3"),
            )
    }
}
