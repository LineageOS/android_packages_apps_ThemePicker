/*
 * Copyright (C) 2025 The Android Open Source Project
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
 */

package com.android.wallpaper.customization.ui.viewmodel

import android.content.Context
import android.stats.style.StyleEnums.APP_ICON_STYLE_THEMED
import android.stats.style.StyleEnums.APP_ICON_STYLE_UNSPECIFIED
import androidx.test.filters.SmallTest
import com.android.customization.model.grid.FakeShapeGridManager
import com.android.customization.module.logging.TestThemesUserEventLogger
import com.android.customization.picker.grid.data.repository.ShapeRepository
import com.android.customization.picker.grid.ui.viewmodel.ShapeIconViewModel
import com.android.customization.picker.icon.data.GlobalIconShapeManager
import com.android.customization.picker.icon.data.repository.FakeIconStyleRepository
import com.android.customization.picker.icon.domain.interactor.AppIconInteractor
import com.android.customization.picker.icon.shared.model.ThemePickerIconStyle
import com.android.themepicker.R
import com.android.wallpaper.picker.common.text.ui.viewmodel.Text
import com.android.wallpaper.picker.option.ui.viewmodel.OptionItemViewModel2
import com.android.wallpaper.testing.collectLastValue
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@OptIn(ExperimentalCoroutinesApi::class)
@SmallTest
@RunWith(RobolectricTestRunner::class)
class AppIconPickerViewModelTest {

    @get:Rule var hiltRule = HiltAndroidRule(this)
    @Inject lateinit var testScope: TestScope
    @Inject lateinit var interactor: AppIconInteractor
    @Inject lateinit var iconStyleRepository: FakeIconStyleRepository
    @Inject lateinit var shapeManager: FakeShapeGridManager
    @Inject lateinit var shapeRepository: ShapeRepository
    @Inject @ApplicationContext lateinit var appContext: Context
    @Inject lateinit var logger: TestThemesUserEventLogger

    private lateinit var underTest: AppIconPickerViewModel

    @Before
    fun setUp() {
        hiltRule.inject()
        // Robolectric does not provide a functional system OverlayManager. Existing ViewModel
        // behavior tests do not need to create an FRRO, so keep the global projection disabled.
        appContext
            .getSharedPreferences("global_icon_shape", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("enabled", false)
            .commit()
        underTest =
            AppIconPickerViewModel(
                appContext,
                interactor,
                GlobalIconShapeManager(appContext, shapeManager, Dispatchers.Unconfined),
                logger,
                testScope.backgroundScope,
            )
        shapeManager.setShapeOptions(FakeShapeGridManager.DEFAULT_SHAPE_OPTION_LIST)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun selectedShape() =
        testScope.runTest {
            val selectedShapeKey = collectLastValue(underTest.selectedShape)

            assertThat(selectedShapeKey()?.key?.value).isEqualTo("arch")
        }

    @Test
    fun shapeOptions() =
        testScope.runTest {
            val shapeOptions = collectLastValue(underTest.shapeOptions)

            for (i in 0 until FakeShapeGridManager.DEFAULT_SHAPE_OPTION_LIST.size) {
                val (expectedKey, expectedPath, expectedTitle) =
                    with(FakeShapeGridManager.DEFAULT_SHAPE_OPTION_LIST[i]) {
                        arrayOf(key, path, title)
                    }
                assertShapeItem(
                    optionItem = shapeOptions()?.get(i),
                    key = FakeShapeGridManager.DEFAULT_SHAPE_OPTION_LIST[i].key,
                    payload = ShapeIconViewModel(expectedKey, expectedPath),
                    text = Text.Loaded(expectedTitle),
                    isTextUserVisible = true,
                    isSelected = expectedKey == "arch",
                    isEnabled = true,
                )
            }
        }

    @Test
    fun shapeOptions_whenClickOnCircleOption() =
        testScope.runTest {
            val shapeOptions = collectLastValue(underTest.shapeOptions)
            val previewingShapeKey = collectLastValue(underTest.previewingShapeKey)
            val circleOption = shapeOptions()?.firstOrNull { it.key.value == "circle" }
            val onCircleOptionClicked = circleOption?.onClicked?.let { collectLastValue(it) }
            checkNotNull(onCircleOptionClicked)

            onCircleOptionClicked()?.invoke()

            assertThat(previewingShapeKey()).isEqualTo("circle")
            for (i in 0 until FakeShapeGridManager.DEFAULT_SHAPE_OPTION_LIST.size) {
                val expectedKey = FakeShapeGridManager.DEFAULT_SHAPE_OPTION_LIST[i].key
                val expectedPath = FakeShapeGridManager.DEFAULT_SHAPE_OPTION_LIST[i].path
                val expectedTitle = FakeShapeGridManager.DEFAULT_SHAPE_OPTION_LIST[i].title
                assertShapeItem(
                    optionItem = shapeOptions()?.get(i),
                    key = expectedKey,
                    payload = ShapeIconViewModel(expectedKey, expectedPath),
                    text = Text.Loaded(expectedTitle),
                    isTextUserVisible = true,
                    isSelected = expectedKey == "circle",
                    isEnabled = true,
                )
            }
        }

    @Test
    fun shapeAndThemedIconOnApply_shouldBeNonnull_whenClickOnCircleOption() =
        testScope.runTest {
            val shapeOptions = collectLastValue(underTest.shapeOptions)
            val circleOption = shapeOptions()?.firstOrNull { it.key.value == "circle" }
            val onCircleOptionClicked = circleOption?.onClicked?.let { collectLastValue(it) }
            val onApply = collectLastValue(underTest.shapeAndThemedIconOnApply)
            checkNotNull(onCircleOptionClicked)

            assertThat(onApply()).isNull()

            onCircleOptionClicked()?.invoke()

            assertThat(onApply()).isNotNull()
        }

    @Test
    fun iconStyleAndShapeOnApply_shouldBeNonnull_whenClickOnCircleOption() =
        testScope.runTest {
            val shapeOptions = collectLastValue(underTest.shapeOptions)
            val circleOption = shapeOptions()?.firstOrNull { it.key.value == "circle" }
            val onCircleOptionClicked = circleOption?.onClicked?.let { collectLastValue(it) }
            val onApply = collectLastValue(underTest.iconStyleAndShapeOnApply)
            checkNotNull(onCircleOptionClicked)

            assertThat(onApply()).isNull()

            onCircleOptionClicked()?.invoke()

            assertThat(onApply()).isNotNull()
        }

    @Test
    fun isThemeIconEnabled_shouldBeFalseByDefault() =
        testScope.runTest {
            val isThemeIconEnabled = collectLastValue(underTest.isThemedIconEnabled)

            assertThat(isThemeIconEnabled()).isFalse()
        }

    @Test
    fun previewingIsThemeIconEnabled_shouldBeFalseByDefault() =
        testScope.runTest {
            val previewingIsThemeIconEnabled =
                collectLastValue(underTest.previewingIsThemeIconEnabled)

            assertThat(previewingIsThemeIconEnabled()).isFalse()
        }

    @Test
    fun previewingIsThemeIconEnabled_shouldBeTrue_whenToggle() =
        testScope.runTest {
            val toggleThemedIcon = collectLastValue(underTest.toggleThemedIcon)
            val previewingIsThemeIconEnabled =
                collectLastValue(underTest.previewingIsThemeIconEnabled)

            assertThat(previewingIsThemeIconEnabled()).isFalse()

            toggleThemedIcon()?.invoke()

            assertThat(previewingIsThemeIconEnabled()).isTrue()
        }

    @Test
    fun globalIconShapeEnabled_shouldDefaultToTrue() =
        testScope.runTest {
            appContext
                .getSharedPreferences("global_icon_shape", Context.MODE_PRIVATE)
                .edit()
                .remove("enabled")
                .commit()
            val manager =
                GlobalIconShapeManager(appContext, shapeManager, Dispatchers.Unconfined)
            val isEnabled = collectLastValue(manager.isEnabled)

            assertThat(isEnabled()).isTrue()
        }

    @Test
    fun previewingGlobalIconShapeEnabled_shouldReflectDisabledPreference() =
        testScope.runTest {
            val isEnabled = collectLastValue(underTest.previewingGlobalIconShapeEnabled)

            assertThat(isEnabled()).isFalse()
        }

    @Test
    fun previewingGlobalIconShapeEnabled_shouldToggleFromDisabledPreference() =
        testScope.runTest {
            val toggle = collectLastValue(underTest.toggleGlobalIconShape)
            val isEnabled = collectLastValue(underTest.previewingGlobalIconShapeEnabled)

            toggle()?.invoke()

            assertThat(isEnabled()).isTrue()
        }

    @Test
    fun shapeAndThemedIconOnApply_shouldBeNonnull_whenToggle() =
        testScope.runTest {
            val toggleThemedIcon = collectLastValue(underTest.toggleThemedIcon)
            val onApply = collectLastValue(underTest.shapeAndThemedIconOnApply)

            assertThat(onApply()).isNull()

            toggleThemedIcon()?.invoke()

            assertThat(onApply()).isNotNull()
        }

    @Test
    fun previewingIconStyle_shouldBeDefault() =
        testScope.runTest {
            val previewingIconStyle = collectLastValue(underTest.previewingIconStyle)

            assertThat(previewingIconStyle()).isEqualTo(ThemePickerIconStyle.DEFAULT)
        }

    @Test
    fun iconStyleAndShapeOnApply_shouldBeNonnull_whenIconStyleSelected() =
        testScope.runTest {
            val styleOptions = collectLastValue(underTest.styleOptions)
            val onMinimalOptionClick =
                styleOptions()?.get(1)?.onClicked?.let { collectLastValue(it) }
            checkNotNull(onMinimalOptionClick)
            val onApply = collectLastValue(underTest.iconStyleAndShapeOnApply)

            assertThat(onApply()).isNull()

            onMinimalOptionClick()?.invoke()

            assertThat(onApply()).isNotNull()
        }

    @Test
    fun iconStyleAndShapeOnApply_completesOnSuccess() =
        testScope.runTest {
            val styleOptions = collectLastValue(underTest.styleOptions)
            val onMinimalOptionClick =
                styleOptions()?.get(1)?.onClicked?.let { collectLastValue(it) }
            checkNotNull(onMinimalOptionClick)
            val onApply = collectLastValue(underTest.iconStyleAndShapeOnApply)
            val selectedIconStyle = collectLastValue(underTest.selectedIconStyle)
            assertThat(selectedIconStyle()).isEqualTo(ThemePickerIconStyle.DEFAULT)

            onMinimalOptionClick()?.invoke()
            onApply()?.invoke()

            assertThat(selectedIconStyle()).isEqualTo(ThemePickerIconStyle.MONOCHROME)
        }

    @Test
    fun iconStyleAndShapeOnApply_completesOnFailure() =
        testScope.runTest {
            val styleOptions = collectLastValue(underTest.styleOptions)
            val onMinimalOptionClick =
                styleOptions()?.get(1)?.onClicked?.let { collectLastValue(it) }
            checkNotNull(onMinimalOptionClick)
            val onApply = collectLastValue(underTest.iconStyleAndShapeOnApply)
            val selectedIconStyle = collectLastValue(underTest.selectedIconStyle)
            assertThat(selectedIconStyle()).isEqualTo(ThemePickerIconStyle.DEFAULT)
            iconStyleRepository.shouldApplySuccessfully = false

            onMinimalOptionClick()?.invoke()
            onApply()?.invoke()

            assertThat(selectedIconStyle()).isEqualTo(ThemePickerIconStyle.DEFAULT)
        }

    @Test
    fun iconStyleAndShapeOnApply_completesOnTimeOut() =
        testScope.runTest {
            val styleOptions = collectLastValue(underTest.styleOptions)
            val onMinimalOptionClick =
                styleOptions()?.get(1)?.onClicked?.let { collectLastValue(it) }
            checkNotNull(onMinimalOptionClick)
            val onApply = collectLastValue(underTest.iconStyleAndShapeOnApply)
            val selectedIconStyle = collectLastValue(underTest.selectedIconStyle)
            assertThat(selectedIconStyle()).isEqualTo(ThemePickerIconStyle.DEFAULT)
            iconStyleRepository.shouldUpdateSuccessfully = false

            onMinimalOptionClick()?.invoke()
            val job = testScope.launch { onApply()?.invoke() }

            assertThat(job.isActive).isTrue()
            advanceTimeBy(AppIconPickerViewModel.ICON_UPDATE_TIMEOUT)
            runCurrent()

            assertThat(job.isActive).isFalse()
        }

    @Test
    fun iconStyleAndShapeOnApply_logs_default() =
        testScope.runTest {
            val styleOptions = collectLastValue(underTest.styleOptions)
            val onMinimalOptionClick =
                styleOptions()?.get(1)?.onClicked?.let { collectLastValue(it) }
            checkNotNull(onMinimalOptionClick)
            val onDefaultOptionClick =
                styleOptions()?.get(0)?.onClicked?.let { collectLastValue(it) }
            checkNotNull(onDefaultOptionClick)
            val onApply = collectLastValue(underTest.iconStyleAndShapeOnApply)
            val selectedIconStyle = collectLastValue(underTest.selectedIconStyle)
            // Apply monochrome option first to enable applying default option
            onMinimalOptionClick()?.invoke()
            onApply()?.invoke()
            assertThat(selectedIconStyle()).isEqualTo(ThemePickerIconStyle.MONOCHROME)

            onDefaultOptionClick()?.invoke()
            onApply()?.invoke()

            assertThat(logger.iconStyle).isEqualTo(APP_ICON_STYLE_UNSPECIFIED)
        }

    @Test
    fun iconStyleAndShapeOnApply_logs_monochrome() =
        testScope.runTest {
            val styleOptions = collectLastValue(underTest.styleOptions)
            val onMinimalOptionClick =
                styleOptions()?.get(1)?.onClicked?.let { collectLastValue(it) }
            checkNotNull(onMinimalOptionClick)
            val onApply = collectLastValue(underTest.iconStyleAndShapeOnApply)
            val selectedIconStyle = collectLastValue(underTest.selectedIconStyle)
            assertThat(selectedIconStyle()).isEqualTo(ThemePickerIconStyle.DEFAULT)

            onMinimalOptionClick()?.invoke()
            onApply()?.invoke()

            assertThat(logger.iconStyle).isEqualTo(APP_ICON_STYLE_THEMED)
        }

    @Test
    fun selectedShapeOption_shouldUpdate_afterOnApply() =
        testScope.runTest {
            val selectedShapeOption = collectLastValue(underTest.selectedShape)
            val optionItems = collectLastValue(underTest.shapeOptions)
            val onApply = collectLastValue(underTest.shapeAndThemedIconOnApply)
            val on4SidedCookieOptionClick =
                optionItems()?.get(FakeShapeGridManager.FOUR_SIDED_COOKIE_IDX)?.onClicked?.let {
                    collectLastValue(it)
                }
            checkNotNull(on4SidedCookieOptionClick)

            on4SidedCookieOptionClick()?.invoke()
            onApply()?.invoke()

            assertShapeItem(
                optionItem = selectedShapeOption(),
                key = FakeShapeGridManager.FOUR_SIDED_COOKIE_KEY,
                payload =
                    ShapeIconViewModel(
                        FakeShapeGridManager.FOUR_SIDED_COOKIE_KEY,
                        FakeShapeGridManager.FOUR_SIDED_COOKIE_PATH,
                    ),
                text = Text.Loaded(FakeShapeGridManager.FOUR_SIDED_COOKIE_TITLE),
                isTextUserVisible = true,
                isSelected = true,
                isEnabled = true,
            )
        }

    @Test
    fun isThemedIconEnabled_shouldUpdate_afterOnApply() =
        testScope.runTest {
            val isEnabled = collectLastValue(underTest.isThemedIconEnabled)
            val toggleThemedIcon = collectLastValue(underTest.toggleThemedIcon)
            val onApply = collectLastValue(underTest.shapeAndThemedIconOnApply)
            assertThat(isEnabled()).isFalse()

            toggleThemedIcon()?.invoke()
            onApply()?.invoke()

            assertThat(isEnabled()).isTrue()

            toggleThemedIcon()?.invoke()
            onApply()?.invoke()

            assertThat(isEnabled()).isFalse()
        }

    @Test
    fun shapeAndThemedIcon_shouldUpdate_afterOnApply() =
        testScope.runTest {
            val selectedShapeOption = collectLastValue(underTest.selectedShape)
            val optionItems = collectLastValue(underTest.shapeOptions)
            val onApply = collectLastValue(underTest.shapeAndThemedIconOnApply)
            val on4SidedCookieOptionClick =
                optionItems()?.get(FakeShapeGridManager.FOUR_SIDED_COOKIE_IDX)?.onClicked?.let {
                    collectLastValue(it)
                }
            checkNotNull(on4SidedCookieOptionClick)
            val isEnabled = collectLastValue(underTest.isThemedIconEnabled)
            val toggleThemedIcon = collectLastValue(underTest.toggleThemedIcon)
            assertThat(isEnabled()).isFalse()

            on4SidedCookieOptionClick()?.invoke()
            toggleThemedIcon()?.invoke()
            onApply()?.invoke()

            assertShapeItem(
                optionItem = selectedShapeOption(),
                key = FakeShapeGridManager.FOUR_SIDED_COOKIE_KEY,
                payload =
                    ShapeIconViewModel(
                        FakeShapeGridManager.FOUR_SIDED_COOKIE_KEY,
                        FakeShapeGridManager.FOUR_SIDED_COOKIE_PATH,
                    ),
                text = Text.Loaded(FakeShapeGridManager.FOUR_SIDED_COOKIE_TITLE),
                isTextUserVisible = true,
                isSelected = true,
                isEnabled = true,
            )
            assertThat(isEnabled()).isTrue()
        }

    @Test
    fun tabs_shapeAndStyleAvailable() =
        testScope.runTest {
            val tabs = collectLastValue(underTest.tabs)

            val resultTabs = checkNotNull(tabs())
            assertThat(resultTabs).hasSize(2)
            assertThat(resultTabs[0].isSelected).isTrue()
            assertThat(resultTabs[1].isSelected).isFalse()
        }

    @Test
    fun tabs_styleNotAvailable() =
        testScope.runTest {
            val tabs = collectLastValue(underTest.tabs)
            iconStyleRepository.setIsCustomizationAvailable(false)

            val resultTabs = checkNotNull(tabs())
            assertThat(resultTabs).hasSize(1)
            assertThat(resultTabs[0].isSelected).isTrue()
            assertThat(resultTabs[0].text).isEqualTo(appContext.getString(R.string.app_icons_shape))
        }

    @Test
    fun tabs_shapeNotAvailable() =
        testScope.runTest {
            val tabs = collectLastValue(underTest.tabs)
            shapeManager.setShapeOptions(emptyList())

            val resultTabs = checkNotNull(tabs())
            assertThat(resultTabs).hasSize(1)
            assertThat(resultTabs[0].isSelected).isTrue()
            assertThat(resultTabs[0].text).isEqualTo(appContext.getString(R.string.app_icons_style))
        }

    @Test
    fun shapeAndThemedIconSummary_shouldUpdate_afterOnApply() =
        testScope.runTest {
            val summary = collectLastValue(underTest.shapeAndThemedIconSummary)
            val optionItems = collectLastValue(underTest.shapeOptions)
            val onApply = collectLastValue(underTest.shapeAndThemedIconOnApply)
            val on4SidedCookieOptionClick =
                optionItems()?.get(FakeShapeGridManager.FOUR_SIDED_COOKIE_IDX)?.onClicked?.let {
                    collectLastValue(it)
                }
            checkNotNull(on4SidedCookieOptionClick)
            val isEnabled = collectLastValue(underTest.isThemedIconEnabled)
            val toggleThemedIcon = collectLastValue(underTest.toggleThemedIcon)
            assertThat(isEnabled()).isFalse()

            on4SidedCookieOptionClick()?.invoke()
            toggleThemedIcon()?.invoke()
            onApply()?.invoke()

            val currentSummary = summary()
            assertThat(currentSummary?.iconShape)
                .isEqualTo(
                    ShapeIconViewModel(
                        FakeShapeGridManager.FOUR_SIDED_COOKIE_KEY,
                        FakeShapeGridManager.FOUR_SIDED_COOKIE_PATH,
                    )
                )
            assertThat(currentSummary?.description?.asString(appContext)).matches(".+,.+")
            assertThat(currentSummary?.isThemed).isEqualTo(true)
        }

    @Test
    fun shapeAndThemedIconSummary_shouldOnlyShowTheme_ifNoShapes() =
        testScope.runTest {
            shapeManager.setShapeOptions(emptyList())
            interactor.applyShape("")
            val summary = collectLastValue(underTest.shapeAndThemedIconSummary)
            val currentSummary = summary()
            assertThat(currentSummary?.description?.asString(appContext)).doesNotMatch(".+,.+")
        }

    @Test
    fun shapeAndThemedIconSummary_shouldOnlyShowTheme_ifOnlyOneShape() =
        testScope.runTest {
            shapeManager.setShapeOptions(shapeManager.getShapeOptions().subList(0, 1))
            interactor.applyShape("")
            val summary = collectLastValue(underTest.shapeAndThemedIconSummary)
            val currentSummary = summary()
            assertThat(currentSummary?.description?.asString(appContext)).doesNotMatch(".+,.+")
        }

    @Test
    fun iconStyleAndShapeSummary_shouldUpdate_afterOnApply() =
        testScope.runTest {
            val summary = collectLastValue(underTest.iconStyleAndShapeSummary)
            val shapeOptions = collectLastValue(underTest.shapeOptions)
            val onApply = collectLastValue(underTest.iconStyleAndShapeOnApply)
            val on4SidedCookieOptionClick =
                shapeOptions()?.get(FakeShapeGridManager.FOUR_SIDED_COOKIE_IDX)?.onClicked?.let {
                    collectLastValue(it)
                }
            checkNotNull(on4SidedCookieOptionClick)
            val styleOptions = collectLastValue(underTest.styleOptions)
            val onMinimalOptionClick =
                styleOptions()?.get(1)?.onClicked?.let { collectLastValue(it) }
            checkNotNull(onMinimalOptionClick)

            on4SidedCookieOptionClick()?.invoke()
            onMinimalOptionClick()?.invoke()
            onApply()?.invoke()

            val currentSummary = summary()
            assertThat(currentSummary?.iconShape)
                .isEqualTo(
                    ShapeIconViewModel(
                        FakeShapeGridManager.FOUR_SIDED_COOKIE_KEY,
                        FakeShapeGridManager.FOUR_SIDED_COOKIE_PATH,
                    )
                )
            assertThat(currentSummary?.description?.asString(appContext)).matches(".+,.+")
            assertThat(currentSummary?.isThemed).isEqualTo(true)
        }

    @Test
    fun iconStyleAndShapeSummary_shouldOnlyShowTheme_ifNoShapes() =
        testScope.runTest {
            shapeManager.setShapeOptions(emptyList())
            interactor.applyShape("")
            val summary = collectLastValue(underTest.iconStyleAndShapeSummary)
            val currentSummary = summary()
            assertThat(currentSummary?.description?.asString(appContext)).doesNotMatch(".*,.*")
        }

    @Test
    fun iconStyleAndShapeSummary_shouldOnlyShowTheme_ifOnlyOneShape() =
        testScope.runTest {
            shapeManager.setShapeOptions(shapeManager.getShapeOptions().subList(0, 1))
            interactor.applyShape("")
            val summary = collectLastValue(underTest.iconStyleAndShapeSummary)
            val currentSummary = summary()
            assertThat(currentSummary?.description?.asString(appContext)).doesNotMatch(".*,.*")
        }

    @Test
    fun iconStyleAndShapeSummary_shouldOnlyShowShape_ifNoTheme() =
        testScope.runTest {
            iconStyleRepository.setIsCustomizationAvailable(false)
            val summary = collectLastValue(underTest.iconStyleAndShapeSummary)
            val currentSummary = summary()
            assertThat(currentSummary?.description?.asString(appContext)).doesNotMatch(".*,.*")
        }

    @Test
    fun shapeOptionsAvailable_isTrueOnlyIfMoreThanOneOption() =
        testScope.runTest {
            val isAvailable = collectLastValue(underTest.isShapeOptionsAvailable)

            // setUp fills the shape options with DEFAULT_SHAPE_OPTION_LIST which has 5 items
            assertThat(isAvailable()).isEqualTo(true)

            shapeManager.setShapeOptions(shapeManager.getShapeOptions().subList(0, 1))
            interactor.applyShape("")
            assertThat(isAvailable()).isEqualTo(false)

            shapeManager.setShapeOptions(emptyList())
            interactor.applyShape("")
            assertThat(isAvailable()).isEqualTo(false)
        }

    private fun TestScope.assertShapeItem(
        optionItem: OptionItemViewModel2<ShapeIconViewModel>?,
        key: String,
        payload: ShapeIconViewModel?,
        text: Text,
        isTextUserVisible: Boolean,
        isSelected: Boolean,
        isEnabled: Boolean,
    ) {
        checkNotNull(optionItem)
        assertThat(collectLastValue(optionItem.key)()).isEqualTo(key)
        assertThat(optionItem.text).isEqualTo(text)
        assertThat(optionItem.payload).isEqualTo(payload)
        assertThat(optionItem.isTextUserVisible).isEqualTo(isTextUserVisible)
        assertThat(collectLastValue(optionItem.isSelected)()).isEqualTo(isSelected)
        assertThat(optionItem.isEnabled).isEqualTo(isEnabled)
    }
}
