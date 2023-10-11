/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.customization.picker.quickaffordance.data.repository

import com.android.customization.picker.quickaffordance.shared.model.KeyguardQuickAffordancePickerAffordanceModel as AffordanceModel
import com.android.customization.picker.quickaffordance.shared.model.KeyguardQuickAffordancePickerSelectionModel as SelectionModel
import com.android.customization.picker.quickaffordance.shared.model.KeyguardQuickAffordancePickerSlotModel as SlotModel
import com.android.systemui.shared.customization.data.content.CustomizationProviderClient as Client
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Abstracts access to application state related to functionality for selecting, picking, or setting
 * lock screen quick affordances.
 */
class KeyguardQuickAffordancePickerRepository(
    private val client: Client,
) {
    /** List of slots available on the device. */
    val slots: Flow<List<SlotModel>> =
        client.observeSlots().map { slots -> slots.map { slot -> slot.toModel() } }

    /** List of all available quick affordances. */
    val affordances: Flow<List<AffordanceModel>> =
        client.observeAffordances().map { affordances ->
            affordances.map { affordance -> affordance.toModel() }
        }

    /** List of slot-affordance pairs, modeling what the user has currently chosen for each slot. */
    val selections: Flow<List<SelectionModel>> =
        client.observeSelections().map { selections ->
            selections.map { selection -> selection.toModel() }
        }

    private fun Client.Slot.toModel(): SlotModel {
        return SlotModel(
            id = id,
            maxSelectedQuickAffordances = capacity,
        )
    }

    private fun Client.Affordance.toModel(): AffordanceModel {
        return AffordanceModel(
            id = id,
            name = name,
            iconResourceId = iconResourceId,
            isEnabled = isEnabled,
            enablementExplanation = enablementExplanation ?: "",
            enablementActionText = enablementActionText,
            enablementActionIntent = enablementActionIntent,
            configureIntent = configureIntent,
        )
    }

    private fun Client.Selection.toModel(): SelectionModel {
        return SelectionModel(
            slotId = slotId,
            affordanceId = affordanceId,
        )
    }
}
