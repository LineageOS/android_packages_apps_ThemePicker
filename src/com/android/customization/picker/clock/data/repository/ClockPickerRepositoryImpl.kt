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
package com.android.customization.picker.clock.data.repository

import com.android.customization.picker.clock.shared.ClockSize
import com.android.customization.picker.clock.shared.model.ClockMetadataModel
import com.android.systemui.plugins.ClockMetadata
import com.android.systemui.shared.clocks.ClockRegistry
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow

/** Implementation of [ClockPickerRepository], using [ClockRegistry]. */
class ClockPickerRepositoryImpl(private val registry: ClockRegistry) : ClockPickerRepository {

    override val allClocks: Array<ClockMetadataModel> =
        registry
            .getClocks()
            .filter { "NOT_IN_USE" !in it.clockId }
            .map { it.toModel() }
            .toTypedArray()

    /** The currently-selected clock. */
    override val selectedClock: Flow<ClockMetadataModel> = callbackFlow {
        fun send() {
            val model =
                registry
                    .getClocks()
                    .find { clockMetadata -> clockMetadata.clockId == registry.currentClockId }
                    ?.toModel()
            checkNotNull(model)
            trySend(model)
        }

        val listener = ClockRegistry.ClockChangeListener { send() }
        registry.registerClockChangeListener(listener)
        send()
        awaitClose { registry.unregisterClockChangeListener(listener) }
    }

    override fun setSelectedClock(clockId: String) {
        registry.currentClockId = clockId
    }

    // TODO(b/262924055): Use the shared system UI component to query the clock size
    private val _selectedClockSize = MutableStateFlow(ClockSize.DYNAMIC)
    override val selectedClockSize: Flow<ClockSize> = _selectedClockSize.asStateFlow()

    override fun setClockSize(size: ClockSize) {
        _selectedClockSize.value = size
    }

    private fun ClockMetadata.toModel(): ClockMetadataModel {
        return ClockMetadataModel(clockId = clockId, name = name)
    }
}
