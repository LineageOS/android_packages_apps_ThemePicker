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

package com.android.customization.picker.quickaffordance.ui.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.customization.picker.quickaffordance.domain.interactor.KeyguardQuickAffordancePickerInteractor
import com.android.systemui.shared.customization.data.content.CustomizationProviderContract
import com.android.systemui.shared.keyguard.shared.model.KeyguardQuickAffordanceSlots
import com.android.systemui.shared.quickaffordance.shared.model.KeyguardQuickAffordancePreviewConstants
import com.android.wallpaper.R
import com.android.wallpaper.module.CurrentWallpaperInfoFactory
import com.android.wallpaper.picker.customization.ui.viewmodel.ScreenPreviewViewModel
import com.android.wallpaper.picker.undo.domain.interactor.UndoInteractor
import com.android.wallpaper.picker.undo.ui.viewmodel.UndoViewModel
import com.android.wallpaper.util.PreviewUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/** Models UI state for a lock screen quick affordance picker experience. */
@OptIn(ExperimentalCoroutinesApi::class)
class KeyguardQuickAffordancePickerViewModel
private constructor(
    context: Context,
    private val quickAffordanceInteractor: KeyguardQuickAffordancePickerInteractor,
    undoInteractor: UndoInteractor,
    private val wallpaperInfoFactory: CurrentWallpaperInfoFactory,
) : ViewModel() {

    @SuppressLint("StaticFieldLeak") private val applicationContext = context.applicationContext

    val preview =
        ScreenPreviewViewModel(
            previewUtils =
                PreviewUtils(
                    context = applicationContext,
                    authority =
                        applicationContext.getString(
                            R.string.lock_screen_preview_provider_authority,
                        ),
                ),
            initialExtrasProvider = {
                Bundle().apply {
                    putString(
                        KeyguardQuickAffordancePreviewConstants.KEY_INITIALLY_SELECTED_SLOT_ID,
                        selectedSlotId.value,
                    )
                }
            },
            wallpaperInfoProvider = {
                suspendCancellableCoroutine { continuation ->
                    wallpaperInfoFactory.createCurrentWallpaperInfos(
                        { homeWallpaper, lockWallpaper, _ ->
                            continuation.resume(lockWallpaper ?: homeWallpaper, null)
                        },
                        /* forceRefresh= */ true,
                    )
                }
            },
        )

    val undo: UndoViewModel =
        UndoViewModel(
            interactor = undoInteractor,
        )

    private val _selectedSlotId = MutableStateFlow<String?>(null)
    val selectedSlotId: StateFlow<String?> = _selectedSlotId.asStateFlow()

    /** View-models for each slot, keyed by slot ID. */
    val slots: Flow<Map<String, KeyguardQuickAffordanceSlotViewModel>> =
        combine(
            quickAffordanceInteractor.slots,
            quickAffordanceInteractor.affordances,
            quickAffordanceInteractor.selections,
            selectedSlotId,
        ) { slots, affordances, selections, selectedSlotIdOrNull ->
            slots
                .mapIndexed { index, slot ->
                    val selectedAffordanceIds =
                        selections
                            .filter { selection -> selection.slotId == slot.id }
                            .map { selection -> selection.affordanceId }
                            .toSet()
                    val selectedAffordances =
                        affordances.filter { affordance ->
                            selectedAffordanceIds.contains(affordance.id)
                        }
                    val isSelected =
                        (selectedSlotIdOrNull == null && index == 0) ||
                            selectedSlotIdOrNull == slot.id
                    slot.id to
                        KeyguardQuickAffordanceSlotViewModel(
                            name = getSlotName(slot.id),
                            isSelected = isSelected,
                            selectedQuickAffordances =
                                selectedAffordances.map { affordanceModel ->
                                    KeyguardQuickAffordanceViewModel(
                                        icon = getAffordanceIcon(affordanceModel.iconResourceId),
                                        contentDescription = affordanceModel.name,
                                        isSelected = true,
                                        onClicked = null,
                                        isEnabled = affordanceModel.isEnabled,
                                    )
                                },
                            maxSelectedQuickAffordances = slot.maxSelectedQuickAffordances,
                            onClicked =
                                if (isSelected) {
                                    null
                                } else {
                                    { _selectedSlotId.tryEmit(slot.id) }
                                },
                        )
                }
                .toMap()
        }

    /** The list of all available quick affordances for the selected slot. */
    val quickAffordances: Flow<List<KeyguardQuickAffordanceViewModel>> =
        combine(
            quickAffordanceInteractor.slots,
            quickAffordanceInteractor.affordances,
            quickAffordanceInteractor.selections,
            selectedSlotId,
        ) { slots, affordances, selections, selectedSlotIdOrNull ->
            val selectedSlot =
                selectedSlotIdOrNull?.let { slots.find { slot -> slot.id == it } } ?: slots.first()
            val selectedAffordanceIds =
                selections
                    .filter { selection -> selection.slotId == selectedSlot.id }
                    .map { selection -> selection.affordanceId }
                    .toSet()
            listOf(
                none(
                    slotId = selectedSlot.id,
                    isSelected = selectedAffordanceIds.isEmpty(),
                )
            ) +
                affordances.map { affordance ->
                    val isSelected = selectedAffordanceIds.contains(affordance.id)
                    val affordanceIcon = getAffordanceIcon(affordance.iconResourceId)
                    KeyguardQuickAffordanceViewModel(
                        icon = affordanceIcon,
                        contentDescription = affordance.name,
                        isSelected = isSelected,
                        onClicked =
                            if (affordance.isEnabled) {
                                {
                                    viewModelScope.launch {
                                        if (isSelected) {
                                            quickAffordanceInteractor.unselect(
                                                slotId = selectedSlot.id,
                                                affordanceId = affordance.id,
                                            )
                                        } else {
                                            quickAffordanceInteractor.select(
                                                slotId = selectedSlot.id,
                                                affordanceId = affordance.id,
                                            )
                                        }
                                    }
                                }
                            } else {
                                {
                                    showEnablementDialog(
                                        icon = affordanceIcon,
                                        name = affordance.name,
                                        instructions = affordance.enablementInstructions,
                                        actionText = affordance.enablementActionText,
                                        actionComponentName =
                                            affordance.enablementActionComponentName,
                                    )
                                }
                            },
                        isEnabled = affordance.isEnabled,
                    )
                }
        }

    @SuppressLint("UseCompatLoadingForDrawables")
    val summary: Flow<KeyguardQuickAffordanceSummaryViewModel> =
        slots.map { slots ->
            val icon2 =
                slots[KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_END]
                    ?.selectedQuickAffordances
                    ?.firstOrNull()
                    ?.icon
            val icon1 =
                slots[KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_START]
                    ?.selectedQuickAffordances
                    ?.firstOrNull()
                    ?.icon

            KeyguardQuickAffordanceSummaryViewModel(
                description = toDescriptionText(context, slots),
                icon1 = icon1
                        ?: if (icon2 == null) {
                            context.getDrawable(R.drawable.link_off)
                        } else {
                            null
                        },
                icon2 = icon2,
            )
        }

    private val _dialog = MutableStateFlow<DialogViewModel?>(null)
    /**
     * The current dialog to show. If `null`, no dialog should be shown.
     *
     * When the dialog is dismissed, [onDialogDismissed] must be called.
     */
    val dialog: Flow<DialogViewModel?> = _dialog.asStateFlow()

    /** Notifies that the dialog has been dismissed in the UI. */
    fun onDialogDismissed() {
        _dialog.value = null
    }

    private fun showEnablementDialog(
        icon: Drawable,
        name: String,
        instructions: List<String>,
        actionText: String?,
        actionComponentName: String?,
    ) {
        _dialog.value =
            DialogViewModel(
                icon = icon,
                name = name,
                instructions = instructions,
                actionText = actionText
                        ?: applicationContext.getString(
                            R.string.keyguard_affordance_enablement_dialog_dismiss_button
                        ),
                intent = actionComponentName.toIntent(),
            )
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun none(
        slotId: String,
        isSelected: Boolean,
    ): KeyguardQuickAffordanceViewModel {
        return KeyguardQuickAffordanceViewModel.none(
            context = applicationContext,
            isSelected = isSelected,
            onSelected = {
                viewModelScope.launch { quickAffordanceInteractor.unselectAll(slotId) }
            },
        )
    }

    private fun getSlotName(slotId: String): String {
        return applicationContext.getString(
            when (slotId) {
                KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_START ->
                    R.string.keyguard_slot_name_bottom_start
                KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_END ->
                    R.string.keyguard_slot_name_bottom_end
                else -> error("No name for slot with ID of \"$slotId\"!")
            }
        )
    }

    private suspend fun getAffordanceIcon(@DrawableRes iconResourceId: Int): Drawable {
        return quickAffordanceInteractor.getAffordanceIcon(iconResourceId)
    }

    private fun String?.toIntent(): Intent? {
        if (isNullOrEmpty()) {
            return null
        }

        val splitUp =
            split(
                CustomizationProviderContract.LockScreenQuickAffordances.AffordanceTable
                    .COMPONENT_NAME_SEPARATOR
            )
        check(splitUp.size == 1 || splitUp.size == 2) {
            "Illegal component name \"$this\". Must be either just an action or a package and an" +
                " action separated by a" +
                " \"${CustomizationProviderContract.LockScreenQuickAffordances.AffordanceTable.COMPONENT_NAME_SEPARATOR}\"!"
        }

        return Intent(splitUp.last()).apply {
            if (splitUp.size > 1) {
                setPackage(splitUp[0])
            }
        }
    }

    /** Encapsulates a request to show a dialog. */
    data class DialogViewModel(
        /** An icon to show. */
        val icon: Drawable,

        /** Name of the affordance. */
        val name: String,

        /** The set of instructions to show below the header. */
        val instructions: List<String>,

        /** Label for the dialog button. */
        val actionText: String,

        /**
         * Optional [Intent] to use to start an activity when the dialog button is clicked. If
         * `null`, the dialog should be dismissed.
         */
        val intent: Intent?,
    )

    private fun toDescriptionText(
        context: Context,
        slots: Map<String, KeyguardQuickAffordanceSlotViewModel>,
    ): String {
        val bottomStartAffordanceName =
            slots[KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_START]
                ?.selectedQuickAffordances
                ?.firstOrNull()
                ?.contentDescription
        val bottomEndAffordanceName =
            slots[KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_END]
                ?.selectedQuickAffordances
                ?.firstOrNull()
                ?.contentDescription

        return when {
            !bottomStartAffordanceName.isNullOrEmpty() &&
                !bottomEndAffordanceName.isNullOrEmpty() -> {
                context.getString(
                    R.string.keyguard_quick_affordance_two_selected_template,
                    bottomStartAffordanceName,
                    bottomEndAffordanceName,
                )
            }
            !bottomStartAffordanceName.isNullOrEmpty() -> bottomStartAffordanceName
            !bottomEndAffordanceName.isNullOrEmpty() -> bottomEndAffordanceName
            else -> context.getString(R.string.keyguard_quick_affordance_none_selected)
        }
    }

    class Factory(
        private val context: Context,
        private val quickAffordanceInteractor: KeyguardQuickAffordancePickerInteractor,
        private val undoInteractor: UndoInteractor,
        private val wallpaperInfoFactory: CurrentWallpaperInfoFactory,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return KeyguardQuickAffordancePickerViewModel(
                context = context,
                quickAffordanceInteractor = quickAffordanceInteractor,
                undoInteractor = undoInteractor,
                wallpaperInfoFactory = wallpaperInfoFactory,
            )
                as T
        }
    }
}
