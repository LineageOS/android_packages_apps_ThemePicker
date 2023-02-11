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
import com.android.customization.picker.clock.shared.ClockSize
import com.android.customization.picker.clock.shared.model.ClockMetadataModel
import com.android.systemui.plugins.ClockMetadata
import com.android.systemui.shared.clocks.ClockRegistry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Implementation of [ClockPickerRepository], using [ClockRegistry]. */
class ClockPickerRepositoryImpl(
    private val registry: ClockRegistry,
    private val scope: CoroutineScope,
    private val backgroundDispatcher: CoroutineDispatcher,
) : ClockPickerRepository {

    override val allClocks: Array<ClockMetadataModel> =
        registry
            .getClocks()
            .filter { "NOT_IN_USE" !in it.clockId }
            .map { it.toModel(null) }
            .toTypedArray()

    /** The currently-selected clock. */
    override val selectedClock: Flow<ClockMetadataModel> =
        callbackFlow {
                suspend fun send() {
                    val currentClockId =
                        withContext(backgroundDispatcher) { registry.currentClockId }
                    val model =
                        registry
                            .getClocks()
                            .find { clockMetadata -> clockMetadata.clockId == currentClockId }
                            ?.toModel(registry.seedColor)
                    if (model == null) {
                        Log.w(
                            TAG,
                            "Clock with ID \"$currentClockId\" not found!",
                        )
                    }
                    trySend(model)
                }

                val listener = ClockRegistry.ClockChangeListener { scope.launch { send() } }
                registry.registerClockChangeListener(listener)
                send()
                awaitClose { registry.unregisterClockChangeListener(listener) }
            }
            .mapNotNull { it }

    override fun setSelectedClock(clockId: String) {
        registry.currentClockId = clockId
    }

    override fun setClockColor(color: Int?) {
        registry.seedColor = color
    }

    // TODO(b/262924055): Use the shared system UI component to query the clock size
    private val _selectedClockSize = MutableStateFlow(ClockSize.DYNAMIC)
    override val selectedClockSize: Flow<ClockSize> = _selectedClockSize.asStateFlow()

    override fun setClockSize(size: ClockSize) {
        _selectedClockSize.value = size
    }

    private fun ClockMetadata.toModel(color: Int?): ClockMetadataModel {
        return ClockMetadataModel(clockId = clockId, name = name, color = color)
    }

    companion object {
        private const val TAG = "ClockPickerRepositoryImpl"
    }
}
