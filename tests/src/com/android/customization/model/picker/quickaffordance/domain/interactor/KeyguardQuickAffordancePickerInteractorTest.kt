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

package com.android.customization.model.picker.quickaffordance.domain.interactor

import androidx.test.filters.SmallTest
import com.android.customization.picker.quickaffordance.data.repository.KeyguardQuickAffordancePickerRepository
import com.android.customization.picker.quickaffordance.domain.interactor.KeyguardQuickAffordancePickerInteractor
import com.android.customization.picker.quickaffordance.shared.model.KeyguardQuickAffordancePickerSelectionModel
import com.android.systemui.shared.keyguard.shared.model.KeyguardQuickAffordanceSlots
import com.android.systemui.shared.quickaffordance.data.content.FakeKeyguardQuickAffordanceProviderClient
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
class KeyguardQuickAffordancePickerInteractorTest {

    private lateinit var underTest: KeyguardQuickAffordancePickerInteractor

    private lateinit var testScope: TestScope
    private lateinit var client: FakeKeyguardQuickAffordanceProviderClient

    @Before
    fun setUp() {
        val coroutineDispatcher = UnconfinedTestDispatcher()
        testScope = TestScope(coroutineDispatcher)
        Dispatchers.setMain(coroutineDispatcher)
        client = FakeKeyguardQuickAffordanceProviderClient()
        underTest =
            KeyguardQuickAffordancePickerInteractor(
                repository =
                    KeyguardQuickAffordancePickerRepository(
                        client = client,
                        backgroundDispatcher = coroutineDispatcher,
                    ),
                client = client,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun select() =
        testScope.runTest {
            val selections = mutableListOf<List<KeyguardQuickAffordancePickerSelectionModel>>()
            val job = launch { underTest.selections.toList(selections) }

            underTest.select(
                slotId = KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_START,
                affordanceId = FakeKeyguardQuickAffordanceProviderClient.AFFORDANCE_1,
            )
            assertThat(selections.last())
                .isEqualTo(
                    listOf(
                        KeyguardQuickAffordancePickerSelectionModel(
                            slotId = KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_START,
                            affordanceId = FakeKeyguardQuickAffordanceProviderClient.AFFORDANCE_1,
                        ),
                    )
                )

            underTest.select(
                slotId = KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_START,
                affordanceId = FakeKeyguardQuickAffordanceProviderClient.AFFORDANCE_2,
            )
            assertThat(selections.last())
                .isEqualTo(
                    listOf(
                        KeyguardQuickAffordancePickerSelectionModel(
                            slotId = KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_START,
                            affordanceId = FakeKeyguardQuickAffordanceProviderClient.AFFORDANCE_2,
                        ),
                    )
                )

            job.cancel()
        }

    @Test
    fun unselect() =
        testScope.runTest {
            val selections = mutableListOf<List<KeyguardQuickAffordancePickerSelectionModel>>()
            val job = launch { underTest.selections.toList(selections) }
            underTest.select(
                slotId = KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_START,
                affordanceId = FakeKeyguardQuickAffordanceProviderClient.AFFORDANCE_1,
            )

            underTest.unselect(
                slotId = KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_START,
                affordanceId = FakeKeyguardQuickAffordanceProviderClient.AFFORDANCE_1,
            )

            assertThat(selections.last()).isEmpty()

            job.cancel()
        }

    @Test
    fun unselectAll() =
        testScope.runTest {
            client.setSlotCapacity(KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_END, 3)
            val selections = mutableListOf<List<KeyguardQuickAffordancePickerSelectionModel>>()
            val job = launch { underTest.selections.toList(selections) }
            underTest.select(
                slotId = KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_END,
                affordanceId = FakeKeyguardQuickAffordanceProviderClient.AFFORDANCE_1,
            )
            underTest.select(
                slotId = KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_END,
                affordanceId = FakeKeyguardQuickAffordanceProviderClient.AFFORDANCE_2,
            )
            underTest.select(
                slotId = KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_END,
                affordanceId = FakeKeyguardQuickAffordanceProviderClient.AFFORDANCE_3,
            )

            underTest.unselectAll(
                slotId = KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_END,
            )

            assertThat(selections.last()).isEmpty()

            job.cancel()
        }
}
