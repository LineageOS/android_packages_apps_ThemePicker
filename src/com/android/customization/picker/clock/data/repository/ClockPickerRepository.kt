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

import android.util.Log
import com.android.customization.picker.clock.shared.model.ClockMetadataModel
import com.android.systemui.plugins.ClockMetadata
import com.android.systemui.shared.clocks.ClockRegistry
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Repository for accessing application clock settings, as well as selecting and configuring custom
 * clocks.
 */
class ClockPickerRepository(registry: ClockRegistry) {

    /** The currently-selected clock. */
    val selectedClock: Flow<ClockMetadataModel?> = callbackFlow {
        fun send() {
            val model =
                registry
                    .getClocks()
                    .find { clockMetadata -> clockMetadata.clockId == registry.currentClockId }
                    ?.toModel()
            if (model == null) {
                Log.e(TAG, "Currently selected clock ID is not one of the available clocks.")
            }
            trySend(model)
        }

        val listener = ClockRegistry.ClockChangeListener { send() }
        registry.registerClockChangeListener(listener)
        send()
        awaitClose { registry.unregisterClockChangeListener(listener) }
    }

    private fun ClockMetadata.toModel(): ClockMetadataModel {
        return ClockMetadataModel(clockId = clockId, name = name)
    }

    companion object {
        private const val TAG = "ClockPickerRepository"
    }
}
