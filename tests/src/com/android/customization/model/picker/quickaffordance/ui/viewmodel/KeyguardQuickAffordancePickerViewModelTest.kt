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

package com.android.customization.model.picker.quickaffordance.ui.viewmodel

import android.content.Context
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.android.customization.picker.quickaffordance.data.repository.KeyguardQuickAffordancePickerRepository
import com.android.customization.picker.quickaffordance.domain.interactor.KeyguardQuickAffordancePickerInteractor
import com.android.customization.picker.quickaffordance.domain.interactor.KeyguardQuickAffordanceSnapshotRestorer
import com.android.customization.picker.quickaffordance.ui.viewmodel.KeyguardQuickAffordancePickerViewModel
import com.android.customization.picker.quickaffordance.ui.viewmodel.KeyguardQuickAffordanceSlotViewModel
import com.android.customization.picker.quickaffordance.ui.viewmodel.KeyguardQuickAffordanceSummaryViewModel
import com.android.customization.picker.quickaffordance.ui.viewmodel.KeyguardQuickAffordanceViewModel
import com.android.systemui.shared.customization.data.content.CustomizationProviderClient
import com.android.systemui.shared.customization.data.content.FakeCustomizationProviderClient
import com.android.systemui.shared.keyguard.shared.model.KeyguardQuickAffordanceSlots
import com.android.wallpaper.module.InjectorProvider
import com.android.wallpaper.picker.undo.data.repository.UndoRepository
import com.android.wallpaper.picker.undo.domain.interactor.UndoInteractor
import com.android.wallpaper.testing.FAKE_RESTORERS
import com.android.wallpaper.testing.TestCurrentWallpaperInfoFactory
import com.android.wallpaper.testing.TestInjector
import com.android.wallpaper.testing.collectLastValue
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalCoroutinesApi::class)
@SmallTest
@RunWith(JUnit4::class)
class KeyguardQuickAffordancePickerViewModelTest {

    private lateinit var underTest: KeyguardQuickAffordancePickerViewModel

    private lateinit var context: Context
    private lateinit var testScope: TestScope
    private lateinit var client: FakeCustomizationProviderClient
    private lateinit var quickAffordanceInteractor: KeyguardQuickAffordancePickerInteractor

    @Before
    fun setUp() {
        InjectorProvider.setInjector(TestInjector())
        context = InstrumentationRegistry.getInstrumentation().targetContext
        val testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        Dispatchers.setMain(testDispatcher)
        client = FakeCustomizationProviderClient()

        quickAffordanceInteractor =
            KeyguardQuickAffordancePickerInteractor(
                repository =
                    KeyguardQuickAffordancePickerRepository(
                        client = client,
                        backgroundDispatcher = testDispatcher,
                    ),
                client = client,
                snapshotRestorer = {
                    KeyguardQuickAffordanceSnapshotRestorer(
                            interactor = quickAffordanceInteractor,
                            client = client,
                        )
                        .apply { runBlocking { setUpSnapshotRestorer {} } }
                },
            )
        val undoInteractor =
            UndoInteractor(
                scope = testScope.backgroundScope,
                repository = UndoRepository(),
                restorerByOwnerId = FAKE_RESTORERS,
            )
        underTest =
            KeyguardQuickAffordancePickerViewModel.Factory(
                    context = context,
                    quickAffordanceInteractor = quickAffordanceInteractor,
                    undoInteractor = undoInteractor,
                    wallpaperInfoFactory = TestCurrentWallpaperInfoFactory(context),
                )
                .create(KeyguardQuickAffordancePickerViewModel::class.java)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Select an affordance for each side`() =
        testScope.runTest {
            val slots = collectLastValue(underTest.slots)
            val quickAffordances = collectLastValue(underTest.quickAffordances)

            // Initially, the first slot is selected with the "none" affordance selected.
            assertPickerUiState(
                slots = slots(),
                affordances = quickAffordances(),
                selectedSlotText = "Left button",
                selectedAffordanceText = "None",
            )
            assertPreviewUiState(
                slots = slots(),
                expectedAffordanceNameBySlotId =
                    mapOf(
                        KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_START to null,
                        KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_END to null,
                    ),
            )

            // Select "affordance 1" for the first slot.
            quickAffordances()?.get(1)?.onClicked?.invoke()
            assertPickerUiState(
                slots = slots(),
                affordances = quickAffordances(),
                selectedSlotText = "Left button",
                selectedAffordanceText = FakeCustomizationProviderClient.AFFORDANCE_1,
            )
            assertPreviewUiState(
                slots = slots(),
                expectedAffordanceNameBySlotId =
                    mapOf(
                        KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_START to
                            FakeCustomizationProviderClient.AFFORDANCE_1,
                        KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_END to null,
                    ),
            )

            // Select an affordance for the second slot.
            // First, switch to the second slot:
            slots()?.get(KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_END)?.onClicked?.invoke()
            // Second, select the "affordance 3" affordance:
            quickAffordances()?.get(3)?.onClicked?.invoke()
            assertPickerUiState(
                slots = slots(),
                affordances = quickAffordances(),
                selectedSlotText = "Right button",
                selectedAffordanceText = FakeCustomizationProviderClient.AFFORDANCE_3,
            )
            assertPreviewUiState(
                slots = slots(),
                expectedAffordanceNameBySlotId =
                    mapOf(
                        KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_START to
                            FakeCustomizationProviderClient.AFFORDANCE_1,
                        KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_END to
                            FakeCustomizationProviderClient.AFFORDANCE_3,
                    ),
            )

            // Select a different affordance for the second slot.
            quickAffordances()?.get(2)?.onClicked?.invoke()
            assertPickerUiState(
                slots = slots(),
                affordances = quickAffordances(),
                selectedSlotText = "Right button",
                selectedAffordanceText = FakeCustomizationProviderClient.AFFORDANCE_2,
            )
            assertPreviewUiState(
                slots = slots(),
                expectedAffordanceNameBySlotId =
                    mapOf(
                        KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_START to
                            FakeCustomizationProviderClient.AFFORDANCE_1,
                        KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_END to
                            FakeCustomizationProviderClient.AFFORDANCE_2,
                    ),
            )
        }

    @Test
    fun `Unselect - AKA selecting the none affordance - on one side`() =
        testScope.runTest {
            val slots = collectLastValue(underTest.slots)
            val quickAffordances = collectLastValue(underTest.quickAffordances)

            // Select "affordance 1" for the first slot.
            quickAffordances()?.get(1)?.onClicked?.invoke()
            // Select an affordance for the second slot.
            // First, switch to the second slot:
            slots()?.get(KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_END)?.onClicked?.invoke()
            // Second, select the "affordance 3" affordance:
            quickAffordances()?.get(3)?.onClicked?.invoke()

            // Switch back to the first slot:
            slots()?.get(KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_START)?.onClicked?.invoke()
            // Select the "none" affordance, which is always in position 0:
            quickAffordances()?.get(0)?.onClicked?.invoke()

            assertPickerUiState(
                slots = slots(),
                affordances = quickAffordances(),
                selectedSlotText = "Left button",
                selectedAffordanceText = "None",
            )
            assertPreviewUiState(
                slots = slots(),
                expectedAffordanceNameBySlotId =
                    mapOf(
                        KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_START to null,
                        KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_END to
                            FakeCustomizationProviderClient.AFFORDANCE_3,
                    ),
            )
        }

    @Test
    fun `Show enablement dialog when selecting a disabled affordance`() =
        testScope.runTest {
            val slots = collectLastValue(underTest.slots)
            val quickAffordances = collectLastValue(underTest.quickAffordances)
            val dialog = collectLastValue(underTest.dialog)

            val enablementInstructions = listOf("instruction1", "instruction2")
            val enablementActionText = "enablementActionText"
            val packageName = "packageName"
            val action = "action"
            val enablementActionComponentName = "$packageName/$action"
            // Lets add a disabled affordance to the picker:
            val affordanceIndex =
                client.addAffordance(
                    CustomizationProviderClient.Affordance(
                        id = "disabled",
                        name = "disabled",
                        iconResourceId = 1,
                        isEnabled = false,
                        enablementInstructions = enablementInstructions,
                        enablementActionText = enablementActionText,
                        enablementActionComponentName = enablementActionComponentName,
                    )
                )

            // Lets try to select that disabled affordance:
            quickAffordances()?.get(affordanceIndex + 1)?.onClicked?.invoke()

            // We expect there to be a dialog that should be shown:
            assertThat(dialog()?.icon).isEqualTo(FakeCustomizationProviderClient.ICON_1)
            assertThat(dialog()?.instructions).isEqualTo(enablementInstructions)
            assertThat(dialog()?.actionText).isEqualTo(enablementActionText)
            assertThat(dialog()?.intent?.`package`).isEqualTo(packageName)
            assertThat(dialog()?.intent?.action).isEqualTo(action)

            // Once we report that the dialog has been dismissed by the user, we expect there to be
            // no
            // dialog to be shown:
            underTest.onDialogDismissed()
            assertThat(dialog()).isNull()
        }

    @Test
    fun `summary - affordance selected in both bottom-start and bottom-end`() =
        testScope.runTest {
            val slots = collectLastValue(underTest.slots)
            val quickAffordances = collectLastValue(underTest.quickAffordances)
            val summary = collectLastValue(underTest.summary)

            // Select "affordance 1" for the first slot.
            quickAffordances()?.get(1)?.onClicked?.invoke()
            // Select an affordance for the second slot.
            // First, switch to the second slot:
            slots()?.get(KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_END)?.onClicked?.invoke()
            // Second, select the "affordance 3" affordance:
            quickAffordances()?.get(3)?.onClicked?.invoke()

            assertThat(summary())
                .isEqualTo(
                    KeyguardQuickAffordanceSummaryViewModel(
                        description =
                            "${FakeCustomizationProviderClient.AFFORDANCE_1}," +
                                " ${FakeCustomizationProviderClient.AFFORDANCE_3}",
                        icon1 = FakeCustomizationProviderClient.ICON_1,
                        icon2 = FakeCustomizationProviderClient.ICON_3,
                    )
                )
        }

    @Test
    fun `summary - affordance selected only on bottom-start`() =
        testScope.runTest {
            val slots = collectLastValue(underTest.slots)
            val quickAffordances = collectLastValue(underTest.quickAffordances)
            val summary = collectLastValue(underTest.summary)

            // Select "affordance 1" for the first slot.
            quickAffordances()?.get(1)?.onClicked?.invoke()

            assertThat(summary())
                .isEqualTo(
                    KeyguardQuickAffordanceSummaryViewModel(
                        description = FakeCustomizationProviderClient.AFFORDANCE_1,
                        icon1 = FakeCustomizationProviderClient.ICON_1,
                        icon2 = null,
                    )
                )
        }

    @Test
    fun `summary - affordance selected only on bottom-end`() =
        testScope.runTest {
            val slots = collectLastValue(underTest.slots)
            val quickAffordances = collectLastValue(underTest.quickAffordances)
            val summary = collectLastValue(underTest.summary)

            // Select an affordance for the second slot.
            // First, switch to the second slot:
            slots()?.get(KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_END)?.onClicked?.invoke()
            // Second, select the "affordance 3" affordance:
            quickAffordances()?.get(3)?.onClicked?.invoke()

            assertThat(summary())
                .isEqualTo(
                    KeyguardQuickAffordanceSummaryViewModel(
                        description = FakeCustomizationProviderClient.AFFORDANCE_3,
                        icon1 = null,
                        icon2 = FakeCustomizationProviderClient.ICON_3,
                    )
                )
        }

    @Test
    fun `summary - no affordances selected`() =
        testScope.runTest {
            val slots = collectLastValue(underTest.slots)
            val quickAffordances = collectLastValue(underTest.quickAffordances)
            val summary = collectLastValue(underTest.summary)

            assertThat(summary()?.description).isEqualTo("None")
            assertThat(summary()?.icon1).isNotNull()
            assertThat(summary()?.icon2).isNull()
        }

    /**
     * Asserts the entire picker UI state is what is expected. This includes the slot tabs and the
     * affordance list.
     *
     * @param slots The observed slot view-models, keyed by slot ID
     * @param affordances The observed affordances
     * @param selectedSlotText The text of the slot that's expected to be selected
     * @param selectedAffordanceText The text of the affordance that's expected to be selected
     */
    private fun assertPickerUiState(
        slots: Map<String, KeyguardQuickAffordanceSlotViewModel>?,
        affordances: List<KeyguardQuickAffordanceViewModel>?,
        selectedSlotText: String,
        selectedAffordanceText: String,
    ) {
        assertSlotTabUiState(
            slots = slots,
            slotId = KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_START,
            isSelected = "Left button" == selectedSlotText,
        )
        assertSlotTabUiState(
            slots = slots,
            slotId = KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_END,
            isSelected = "Right button" == selectedSlotText,
        )

        var foundSelectedAffordance = false
        assertThat(affordances).isNotNull()
        affordances?.forEach { affordance ->
            val nameMatchesSelectedName = affordance.contentDescription == selectedAffordanceText
            assertWithMessage(
                    "Expected affordance with name \"${affordance.contentDescription}\" to have" +
                        " isSelected=$nameMatchesSelectedName but it was ${affordance.isSelected}"
                )
                .that(affordance.isSelected)
                .isEqualTo(nameMatchesSelectedName)
            foundSelectedAffordance = foundSelectedAffordance || nameMatchesSelectedName
        }
        assertWithMessage("No affordance is selected!").that(foundSelectedAffordance).isTrue()
    }

    /**
     * Asserts that a slot tab has the correct UI state.
     *
     * @param slots The observed slot view-models, keyed by slot ID
     * @param slotId the ID of the slot to assert
     * @param isSelected Whether that slot should be selected
     */
    private fun assertSlotTabUiState(
        slots: Map<String, KeyguardQuickAffordanceSlotViewModel>?,
        slotId: String,
        isSelected: Boolean,
    ) {
        val viewModel = slots?.get(slotId) ?: error("No slot with ID \"$slotId\"!")
        assertThat(viewModel.isSelected).isEqualTo(isSelected)
    }

    /**
     * Asserts the UI state of the preview.
     *
     * @param slots The observed slot view-models, keyed by slot ID
     * @param expectedAffordanceNameBySlotId The expected name of the selected affordance for each
     * slot ID or `null` if it's expected for there to be no affordance for that slot in the preview
     */
    private fun assertPreviewUiState(
        slots: Map<String, KeyguardQuickAffordanceSlotViewModel>?,
        expectedAffordanceNameBySlotId: Map<String, String?>,
    ) {
        assertThat(slots).isNotNull()
        slots?.forEach { (slotId, slotViewModel) ->
            val expectedAffordanceName = expectedAffordanceNameBySlotId[slotId]
            val actualAffordanceName =
                slotViewModel.selectedQuickAffordances.firstOrNull()?.contentDescription
            assertWithMessage(
                    "At slotId=\"$slotId\", expected affordance=\"$expectedAffordanceName\" but" +
                        " was \"$actualAffordanceName\"!"
                )
                .that(actualAffordanceName)
                .isEqualTo(expectedAffordanceName)
        }
    }
}
