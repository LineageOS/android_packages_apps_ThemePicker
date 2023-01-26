package com.android.customization.picker.clock.ui.viewmodel

import android.content.Context
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.android.customization.picker.clock.data.repository.FakeClockPickerRepository
import com.android.customization.picker.clock.domain.interactor.ClockPickerInteractor
import com.android.customization.picker.clock.shared.ClockSize
import com.android.wallpaper.testing.collectLastValue
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
class ClockSettingsViewModelTest {

    private lateinit var underTest: ClockSettingsViewModel

    private lateinit var context: Context

    @Before
    fun setUp() {
        val testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        context = InstrumentationRegistry.getInstrumentation().targetContext
        underTest =
            ClockSettingsViewModel(context, ClockPickerInteractor(FakeClockPickerRepository()))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun setClockSize() = runTest {
        val observedClockSize = collectLastValue(underTest.selectedClockSize)
        underTest.setClockSize(ClockSize.DYNAMIC)
        assertThat(observedClockSize()).isEqualTo(ClockSize.DYNAMIC)

        underTest.setClockSize(ClockSize.LARGE)
        assertThat(observedClockSize()).isEqualTo(ClockSize.LARGE)
    }

    @Test
    fun `Click on a picker tab`() = runTest {
        val tabs = collectLastValue(underTest.tabs)
        assertThat(tabs()?.get(0)?.name).isEqualTo("Color")
        assertThat(tabs()?.get(0)?.isSelected).isTrue()
        assertThat(tabs()?.get(1)?.name).isEqualTo("Size")
        assertThat(tabs()?.get(1)?.isSelected).isFalse()

        tabs()?.get(1)?.onClicked?.invoke()
        assertThat(tabs()?.get(0)?.isSelected).isFalse()
        assertThat(tabs()?.get(1)?.isSelected).isTrue()
    }
}
