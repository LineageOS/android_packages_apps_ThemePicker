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
import android.util.Log
import com.android.customization.picker.clock.shared.ClockSize
import com.android.customization.picker.clock.shared.model.ClockMetadataModel
import com.android.systemui.plugins.ClockMetadata
import com.android.systemui.shared.clocks.ClockRegistry
import com.android.wallpaper.settings.data.repository.SecureSettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Implementation of [ClockPickerRepository], using [ClockRegistry]. */
class ClockPickerRepositoryImpl(
    private val secureSettingsRepository: SecureSettingsRepository,
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

                val listener =
                    object : ClockRegistry.ClockChangeListener {
                        override fun onCurrentClockChanged() {
                            scope.launch { send() }
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
        withContext(backgroundDispatcher) {
            secureSettingsRepository.set(
                name = Settings.Secure.LOCKSCREEN_USE_DOUBLE_LINE_CLOCK,
                value = if (size == ClockSize.DYNAMIC) 1 else 0,
            )
        }
    }

    private fun ClockMetadata.toModel(color: Int?): ClockMetadataModel {
        return ClockMetadataModel(clockId = clockId, name = name, color = color)
    }

    companion object {
        private const val TAG = "ClockPickerRepositoryImpl"
    }
}
