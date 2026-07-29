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
import android.stats.style.StyleEnums.APP_ICON_STYLE_UNSPECIFIED
import android.util.Log
import android.widget.Toast
import com.android.customization.model.grid.ShapeOptionModel
import com.android.customization.module.logging.ThemesUserEventLogger
import com.android.customization.picker.grid.ui.viewmodel.ShapeIconViewModel
import com.android.customization.picker.icon.data.GlobalIconShapeManager
import com.android.customization.picker.icon.domain.interactor.AppIconInteractor
import com.android.customization.picker.icon.shared.model.IconStyle
import com.android.customization.picker.icon.shared.model.IconStyleModel
import com.android.themepicker.R
import com.android.wallpaper.picker.common.icon.ui.viewmodel.Icon
import com.android.wallpaper.picker.common.text.ui.viewmodel.Text
import com.android.wallpaper.picker.customization.ui.viewmodel.FloatingToolbarTabViewModel
import com.android.wallpaper.picker.option.ui.viewmodel.OptionItemViewModel2
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class AppIconPickerViewModel
@AssistedInject
constructor(
    @ApplicationContext private val applicationContext: Context,
    interactor: AppIconInteractor,
    private val globalIconShapeManager: GlobalIconShapeManager,
    private val logger: ThemesUserEventLogger,
    @Assisted private val viewModelScope: CoroutineScope,
) {
    //// Shape

    // The currently-set system shape option
    val selectedShape =
        interactor.selectedShapeOption
            .filterNotNull()
            .map { toShapeOptionItemViewModel(it) }
            .shareIn(scope = viewModelScope, started = SharingStarted.Lazily, replay = 1)
    private val overridingShapeKey = MutableStateFlow<String?>(null)
    // If the overriding key is null, use the currently-set system shape option
    val previewingShapeKey =
        combine(overridingShapeKey, selectedShape) { overridingShapeOptionKey, selectedShape ->
            overridingShapeOptionKey ?: selectedShape.key.value
        }

    val shapeOptions: Flow<List<OptionItemViewModel2<ShapeIconViewModel>>> =
        interactor.shapeOptions
            .filterNotNull()
            .map { shapeOptions -> shapeOptions.map { toShapeOptionItemViewModel(it) } }
            .shareIn(scope = viewModelScope, started = SharingStarted.Lazily, replay = 1)

    val isShapeOptionsAvailable: Flow<Boolean> =
        interactor.isShapeOptionsAvailable.shareIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            replay = 1,
        )

    //// Themed icons
    val isThemedIconAvailable =
        interactor.isThemedIconAvailable.shareIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            replay = 1,
        )

    private val overridingIsThemedIconEnabled = MutableStateFlow<Boolean?>(null)
    val isThemedIconEnabled =
        interactor.isThemedIconEnabled.shareIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            replay = 1,
        )
    val previewingIsThemeIconEnabled =
        combine(overridingIsThemedIconEnabled, isThemedIconEnabled) {
            overridingIsThemeIconEnabled,
            isThemeIconEnabled ->
            overridingIsThemeIconEnabled ?: isThemeIconEnabled
        }
    val toggleThemedIcon: Flow<suspend () -> Unit> =
        previewingIsThemeIconEnabled.map {
            {
                val newValue = !it
                overridingIsThemedIconEnabled.value = newValue
            }
        }

    //// Global icon shape
    private val overridingGlobalIconShapeEnabled = MutableStateFlow<Boolean?>(null)
    val isGlobalIconShapeEnabled =
        globalIconShapeManager.isEnabled.shareIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            replay = 1,
        )
    val previewingGlobalIconShapeEnabled =
        combine(overridingGlobalIconShapeEnabled, isGlobalIconShapeEnabled) {
            overridingGlobalIconShapeEnabled,
            isGlobalIconShapeEnabled ->
            overridingGlobalIconShapeEnabled ?: isGlobalIconShapeEnabled
        }
    val toggleGlobalIconShape: Flow<suspend () -> Unit> =
        previewingGlobalIconShapeEnabled.map {
            {
                overridingGlobalIconShapeEnabled.value = !it
            }
        }

    //// Style
    val selectedIconStyle = interactor.selectedIconStyle
    private val overridingIconStyle: MutableStateFlow<IconStyle?> = MutableStateFlow(null)
    val previewingIconStyle =
        combine(selectedIconStyle, overridingIconStyle) { selected, overriding ->
            overriding ?: selected
        }
    private val iconStylesModels =
        interactor.iconStyleModels.shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            replay = 1,
        )
    val previewingIconStyleModel =
        combine(previewingIconStyle, iconStylesModels) { previewingIconStyle, iconStylesModels ->
            iconStylesModels.find { it.iconStyle == previewingIconStyle }
        }
    val styleOptions: Flow<List<OptionItemViewModel2<IconStyleModel>>> =
        iconStylesModels.map {
            List(size = it.size, init = { index -> toStyleOptionItemViewModel(it[index]) })
        }
    val isIconStyleAvailable = iconStylesModels.map { it.size > 1 }

    enum class Tab {
        STYLE,
        SHAPE,
    }

    private val _selectedTab = MutableStateFlow<Tab?>(null)
    val selectedTab =
        combine(isIconStyleAvailable, isShapeOptionsAvailable, _selectedTab) {
            isIconStyleAvailable,
            isShapeOptionsAvailable,
            selectedTab ->
            selectedTab
                ?: if (isIconStyleAvailable) {
                    Tab.STYLE
                } else if (isShapeOptionsAvailable) {
                    Tab.SHAPE
                } else {
                    null
                }
        }

    val tabs: Flow<List<FloatingToolbarTabViewModel>> =
        combine(isIconStyleAvailable, isShapeOptionsAvailable, selectedTab) {
            isIconStyleAvailable,
            isShapeOptionsAvailable,
            selectedTab ->
            buildList {
                if (isIconStyleAvailable) {
                    val isSelected = (selectedTab == Tab.STYLE)
                    add(
                        FloatingToolbarTabViewModel(
                            icon =
                                Icon.Resource(
                                    res = R.drawable.ic_style,
                                    contentDescription = Text.Resource(R.string.app_icons_style),
                                ),
                            text =
                                Text.Resource(R.string.app_icons_style)
                                    .asString(applicationContext),
                            isSelected = isSelected,
                            onClick =
                                if (isSelected) {
                                    null
                                } else {
                                    { _selectedTab.value = Tab.STYLE }
                                },
                        )
                    )
                }
                if (isShapeOptionsAvailable) {
                    val isSelected = (selectedTab == Tab.SHAPE)
                    add(
                        FloatingToolbarTabViewModel(
                            icon =
                                Icon.Resource(
                                    res = R.drawable.ic_shapes,
                                    contentDescription = Text.Resource(R.string.app_icons_shape),
                                ),
                            text =
                                Text.Resource(R.string.app_icons_shape)
                                    .asString(applicationContext),
                            isSelected = isSelected,
                            onClick =
                                if (isSelected) {
                                    null
                                } else {
                                    { _selectedTab.value = Tab.SHAPE }
                                },
                        )
                    )
                }
            }
        }

    val shapeAndThemedIconSummary: Flow<AppIconPickerSummaryViewModel> =
        combine(selectedShape, isThemedIconEnabled, isShapeOptionsAvailable) {
            selectedShape,
            isThemedIconEnabled,
            isShapeOptionsAvailable ->
            val selectedShapeString =
                if (isShapeOptionsAvailable) selectedShape.text.asString(applicationContext) else ""
            val appIconThemeString =
                if (isThemedIconEnabled) {
                    applicationContext.getString(R.string.app_icons_theme_themed)
                } else {
                    applicationContext.getString(R.string.app_icons_theme_default)
                }
            AppIconPickerSummaryViewModel(
                description =
                    Text.Loaded(
                        if (selectedShapeString.isEmpty()) {
                            appIconThemeString.replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(Locale.getDefault())
                                else it.toString()
                            }
                        } else {
                            applicationContext.getString(
                                R.string.app_icons_description,
                                selectedShapeString,
                                appIconThemeString,
                            )
                        }
                    ),
                iconShape = selectedShape.payload,
                icon = null,
                isThemed = isThemedIconEnabled,
            )
        }

    val iconStyleAndShapeSummary: Flow<AppIconPickerSummaryViewModel> =
        combine(
            selectedShape,
            selectedIconStyle,
            iconStylesModels,
            isIconStyleAvailable,
            isShapeOptionsAvailable,
        ) {
            selectedShape,
            selectedIconStyle,
            iconStylesModels,
            isIconStyleAvailable,
            isShapeOptionsAvailable ->
            val selectedShapeString =
                if (isShapeOptionsAvailable) selectedShape.text.asString(applicationContext) else ""
            val selectedIconStyleModel = iconStylesModels.find { it.iconStyle == selectedIconStyle }
            val appIconThemeString =
                if (isIconStyleAvailable)
                    selectedIconStyleModel?.nameResId?.let { applicationContext.getString(it) }
                else null
            AppIconPickerSummaryViewModel(
                description =
                    Text.Loaded(
                        if (
                            selectedShapeString.isNotEmpty() && !appIconThemeString.isNullOrEmpty()
                        ) {
                            // Show theme and shape, comma separated
                            applicationContext.getString(
                                R.string.app_icons_description,
                                appIconThemeString,
                                selectedShapeString,
                            )
                        } else if (!appIconThemeString.isNullOrEmpty()) {
                            appIconThemeString
                        } else {
                            selectedShapeString
                        }
                    ),
                iconShape = selectedShape.payload,
                icon = selectedIconStyleModel?.icon,
                isThemed = selectedIconStyleModel?.isThemedIcon ?: false,
            )
        }

    val shapeAndThemedIconOnApply: Flow<(suspend () -> Unit)?> =
        combine(
            overridingShapeKey,
            selectedShape,
            overridingIsThemedIconEnabled,
            isThemedIconEnabled,
            combine(overridingGlobalIconShapeEnabled, isGlobalIconShapeEnabled, ::Pair),
        ) {
            overridingShapeKey,
            selectedShape,
            overridingIsThemedIconEnabled,
            currentIsThemedIconEnabled,
            globalIconShapeState ->
            val (overridingGlobalIconShapeEnabled, currentGlobalIconShapeEnabled) =
                globalIconShapeState
            val shapeNeedsUpdate =
                overridingShapeKey != null && overridingShapeKey != selectedShape.key.value
            val themedIconNeedsUpdate =
                overridingIsThemedIconEnabled != null &&
                    overridingIsThemedIconEnabled != currentIsThemedIconEnabled
            val globalIconShapeNeedsUpdate =
                overridingGlobalIconShapeEnabled != null &&
                    overridingGlobalIconShapeEnabled != currentGlobalIconShapeEnabled
            if (shapeNeedsUpdate || themedIconNeedsUpdate || globalIconShapeNeedsUpdate) {
                {
                    val targetShapeKey = overridingShapeKey ?: selectedShape.key.value
                    val targetGlobalIconShapeEnabled =
                        overridingGlobalIconShapeEnabled ?: currentGlobalIconShapeEnabled
                    val globalIconShapeApplied =
                        if (
                        globalIconShapeNeedsUpdate ||
                            (shapeNeedsUpdate && targetGlobalIconShapeEnabled)
                    ) {
                        val result =
                            globalIconShapeManager.setEnabled(
                                targetGlobalIconShapeEnabled,
                                targetShapeKey,
                            )
                        if (result.isFailure) {
                            showGlobalIconShapeFailure(result.exceptionOrNull())
                        }
                        result.isSuccess
                    } else {
                        true
                    }
                    if (globalIconShapeApplied && shapeNeedsUpdate) {
                        overridingShapeKey?.let {
                            interactor.applyShape(it)
                            logger.logShapeApplied(it)
                        }
                    }
                    if (globalIconShapeApplied && themedIconNeedsUpdate) {
                        coroutineScope {
                            launch {
                                overridingIsThemedIconEnabled?.let {
                                    interactor.applyThemedIconEnabled(it)
                                }
                            }
                            isThemedIconEnabled.drop(1).take(1).collect {
                                return@collect
                            }
                            overridingIsThemedIconEnabled?.let { logger.logThemedIconApplied(it) }
                        }
                    }
                }
            } else {
                null
            }
        }

    val iconStyleAndShapeOnApply: Flow<(suspend () -> Unit)?> =
        combine(
            overridingShapeKey,
            selectedShape,
            overridingIconStyle,
            selectedIconStyle,
            combine(overridingGlobalIconShapeEnabled, isGlobalIconShapeEnabled, ::Pair),
        ) {
            overridingShapeKey,
            selectedShape,
            overridingIconStyle,
            currentIconStyle,
            globalIconShapeState ->
            val (overridingGlobalIconShapeEnabled, currentGlobalIconShapeEnabled) =
                globalIconShapeState
            val shapeNeedsUpdate =
                overridingShapeKey != null && overridingShapeKey != selectedShape.key.value
            val styleNeedsUpdate =
                overridingIconStyle != null && overridingIconStyle != currentIconStyle
            val globalIconShapeNeedsUpdate =
                overridingGlobalIconShapeEnabled != null &&
                    overridingGlobalIconShapeEnabled != currentGlobalIconShapeEnabled
            if (shapeNeedsUpdate || styleNeedsUpdate || globalIconShapeNeedsUpdate) {
                {
                    val targetShapeKey = overridingShapeKey ?: selectedShape.key.value
                    val targetGlobalIconShapeEnabled =
                        overridingGlobalIconShapeEnabled ?: currentGlobalIconShapeEnabled
                    val globalIconShapeApplied =
                        if (
                        globalIconShapeNeedsUpdate ||
                            (shapeNeedsUpdate && targetGlobalIconShapeEnabled)
                    ) {
                        val result =
                            globalIconShapeManager.setEnabled(
                                targetGlobalIconShapeEnabled,
                                targetShapeKey,
                            )
                        if (result.isFailure) {
                            showGlobalIconShapeFailure(result.exceptionOrNull())
                        }
                        result.isSuccess
                    } else {
                        true
                    }
                    if (globalIconShapeApplied && shapeNeedsUpdate) {
                        overridingShapeKey?.let {
                            interactor.applyShape(it)
                            logger.logShapeApplied(it)
                        }
                    }
                    if (globalIconShapeApplied && styleNeedsUpdate) {
                        coroutineScope {
                            val waitForUpdate = launch {
                                try {
                                    withTimeout(ICON_UPDATE_TIMEOUT) {
                                        // Drop the first emitted icon style since it is the current
                                        // selection. The next emitted icon style signals an update.
                                        selectedIconStyle.drop(1).take(1).collect {
                                            return@collect
                                        }
                                    }
                                } catch (e: TimeoutCancellationException) {
                                    Log.w(TAG, "Timed out waiting for icon update", e)
                                }
                            }
                            launch {
                                val success =
                                    overridingIconStyle?.let { interactor.applyIconStyle(it) }
                                if (success == true) {
                                    logger.logIconStyleApplied(
                                        overridingIconStyle.loggingId ?: APP_ICON_STYLE_UNSPECIFIED
                                    )
                                } else {
                                    Log.w(TAG, "Apply unsuccessful, no data was updated")
                                    waitForUpdate.cancel()
                                }
                            }
                            try {
                                waitForUpdate.join()
                            } catch (_: CancellationException) {}
                        }
                    }
                }
            } else {
                null
            }
        }

    fun resetPreview() {
        overridingShapeKey.value = null
        overridingIsThemedIconEnabled.value = null
        overridingGlobalIconShapeEnabled.value = null
    }

    fun resetPreview2() {
        overridingShapeKey.value = null
        overridingIconStyle.value = null
        overridingGlobalIconShapeEnabled.value = null
    }

    private fun showGlobalIconShapeFailure(throwable: Throwable?) {
        Log.e(TAG, "Failed to apply global icon shape", throwable)
        Toast.makeText(
                applicationContext,
                R.string.global_icon_shape_apply_failed,
                Toast.LENGTH_LONG,
            )
            .show()
    }

    private fun toShapeOptionItemViewModel(
        option: ShapeOptionModel
    ): OptionItemViewModel2<ShapeIconViewModel> {
        val isSelected =
            previewingShapeKey
                .map { it == option.key }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Lazily,
                    initialValue = false,
                )

        return OptionItemViewModel2(
            key = MutableStateFlow(option.key),
            payload = ShapeIconViewModel(option.key, option.path),
            text = Text.Loaded(option.title),
            isSelected = isSelected,
            onClicked =
                isSelected.map {
                    if (!it) {
                        { overridingShapeKey.value = option.key }
                    } else {
                        null
                    }
                },
        )
    }

    private fun toStyleOptionItemViewModel(
        iconStyleModel: IconStyleModel
    ): OptionItemViewModel2<IconStyleModel> {
        val isSelected =
            previewingIconStyle
                .map { it == iconStyleModel.iconStyle }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Lazily,
                    initialValue = false,
                )
        val text = Text.Resource(iconStyleModel.nameResId)
        return OptionItemViewModel2(
            key = MutableStateFlow(text.asString(applicationContext)),
            payload = iconStyleModel,
            text = text,
            isSelected = isSelected,
            onClicked =
                if (iconStyleModel.isExternalLink) {
                    // A button is not selectable.
                    flowOf(null)
                } else {
                    isSelected.map {
                        if (!it) {
                            { overridingIconStyle.value = iconStyleModel.iconStyle }
                        } else {
                            null
                        }
                    }
                },
            skipOnClickBinding = iconStyleModel.isExternalLink,
            // Icon styles have custom color bindings, if any, and don't need the default binding.
            skipForegroundColorBinding = true,
        )
    }

    @ViewModelScoped
    @AssistedFactory
    interface Factory {
        fun create(viewModelScope: CoroutineScope): AppIconPickerViewModel
    }

    companion object {
        const val TAG = "AppIconPickerViewModel"
        val ICON_UPDATE_TIMEOUT = 1000.milliseconds
    }
}
