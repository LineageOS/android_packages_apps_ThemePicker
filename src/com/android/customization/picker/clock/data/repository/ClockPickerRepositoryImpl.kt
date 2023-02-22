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

import android.provider.Settings
import com.android.customization.picker.clock.shared.ClockSize
import com.android.customization.picker.clock.shared.model.ClockMetadataModel
import com.android.systemui.plugins.ClockMetadata
import com.android.systemui.shared.clocks.ClockRegistry
import com.android.wallpaper.settings.data.repository.SecureSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.shareIn

/** Implementation of [ClockPickerRepository], using [ClockRegistry]. */
class ClockPickerRepositoryImpl(
    private val secureSettingsRepository: SecureSettingsRepository,
    private val registry: ClockRegistry,
    scope: CoroutineScope,
) : ClockPickerRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override val allClocks: Flow<List<ClockMetadataModel>> =
        callbackFlow {
                fun send() {
                    val allClocks =
                        registry
                            .getClocks()
                            .filter { "NOT_IN_USE" !in it.clockId }
                            .map { it.toModel(null) }
                    trySend(allClocks)
                }

                val listener =
                    object : ClockRegistry.ClockChangeListener {
                        override fun onAvailableClocksChanged() {
                            send()
                        }
                    }
                registry.registerClockChangeListener(listener)
                send()
                awaitClose { registry.unregisterClockChangeListener(listener) }
            }
            .mapLatest { allClocks ->
                // Loading list of clock plugins can cause many consecutive calls of
                // onAvailableClocksChanged(). We only care about the final fully-initiated clock
                // list. Delay to avoid unnecessary too many emits.
                delay(100)
                allClocks
            }

    /** The currently-selected clock. */
    override val selectedClock: Flow<ClockMetadataModel> =
        callbackFlow {
                fun send() {
                    val currentClockId = registry.currentClockId
                    // It is possible that the model can be null since the full clock list is not
                    // initiated.
                    val model =
                        registry
                            .getClocks()
                            .find { clockMetadata -> clockMetadata.clockId == currentClockId }
                            ?.toModel(registry.seedColor)
                    trySend(model)
                }

                val listener =
                    object : ClockRegistry.ClockChangeListener {
                        override fun onCurrentClockChanged() {
                            send()
                        }

                        override fun onAvailableClocksChanged() {
                            send()
                        }
                    }
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

    override val selectedClockSize: SharedFlow<ClockSize> =
        secureSettingsRepository
            .intSetting(
                name = Settings.Secure.LOCKSCREEN_USE_DOUBLE_LINE_CLOCK,
            )
            .map { setting -> setting == 1 }
            .map { isDynamic -> if (isDynamic) ClockSize.DYNAMIC else ClockSize.SMALL }
            .shareIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(),
                replay = 1,
            )

    override suspend fun setClockSize(size: ClockSize) {
        secureSettingsRepository.set(
            name = Settings.Secure.LOCKSCREEN_USE_DOUBLE_LINE_CLOCK,
            value = if (size == ClockSize.DYNAMIC) 1 else 0,
        )
    }

    private fun ClockMetadata.toModel(color: Int?): ClockMetadataModel {
        return ClockMetadataModel(clockId = clockId, name = name, color = color)
    }
}
