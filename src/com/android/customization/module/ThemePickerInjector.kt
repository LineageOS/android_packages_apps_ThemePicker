/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.customization.module

import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.android.customization.model.mode.DarkModeSnapshotRestorer
import com.android.customization.model.theme.OverlayManagerCompat
import com.android.customization.model.theme.ThemeBundleProvider
import com.android.customization.model.theme.ThemeManager
import com.android.customization.model.themedicon.ThemedIconSwitchProvider
import com.android.customization.model.themedicon.data.repository.ThemeIconRepository
import com.android.customization.model.themedicon.domain.interactor.ThemedIconInteractor
import com.android.customization.model.themedicon.domain.interactor.ThemedIconSnapshotRestorer
import com.android.customization.picker.clock.data.repository.ClockPickerRepositoryImpl
import com.android.customization.picker.clock.data.repository.ClockRegistryProvider
import com.android.customization.picker.clock.domain.interactor.ClockPickerInteractor
import com.android.customization.picker.clock.ui.view.ClockViewFactory
import com.android.customization.picker.clock.ui.viewmodel.ClockCarouselViewModel
import com.android.customization.picker.clock.ui.viewmodel.ClockSectionViewModel
import com.android.customization.picker.clock.ui.viewmodel.ClockSettingsViewModel
import com.android.customization.picker.color.data.repository.ColorPickerRepositoryImpl
import com.android.customization.picker.color.domain.interactor.ColorPickerInteractor
import com.android.customization.picker.color.ui.viewmodel.ColorPickerViewModel
import com.android.customization.picker.notifications.data.repository.NotificationsRepository
import com.android.customization.picker.notifications.domain.interactor.NotificationsInteractor
import com.android.customization.picker.notifications.domain.interactor.NotificationsSnapshotRestorer
import com.android.customization.picker.notifications.ui.viewmodel.NotificationSectionViewModel
import com.android.customization.picker.preview.ui.section.PreviewWithClockCarouselSectionController.ClockCarouselViewModelProvider
import com.android.customization.picker.preview.ui.section.PreviewWithClockCarouselSectionController.ClockViewFactoryProvider
import com.android.customization.picker.quickaffordance.data.repository.KeyguardQuickAffordancePickerRepository
import com.android.customization.picker.quickaffordance.domain.interactor.KeyguardQuickAffordancePickerInteractor
import com.android.customization.picker.quickaffordance.domain.interactor.KeyguardQuickAffordanceSnapshotRestorer
import com.android.customization.picker.quickaffordance.ui.viewmodel.KeyguardQuickAffordancePickerViewModel
import com.android.systemui.shared.clocks.ClockRegistry
import com.android.systemui.shared.customization.data.content.CustomizationProviderClient
import com.android.systemui.shared.customization.data.content.CustomizationProviderClientImpl
import com.android.wallpaper.model.LiveWallpaperInfo
import com.android.wallpaper.model.WallpaperColorsViewModel
import com.android.wallpaper.model.WallpaperInfo
import com.android.wallpaper.module.CustomizationSections
import com.android.wallpaper.module.FragmentFactory
import com.android.wallpaper.module.UserEventLogger
import com.android.wallpaper.module.WallpaperPicker2Injector
import com.android.wallpaper.module.WallpaperPreferences
import com.android.wallpaper.picker.CustomizationPickerActivity
import com.android.wallpaper.picker.ImagePreviewFragment
import com.android.wallpaper.picker.LivePreviewFragment
import com.android.wallpaper.picker.PreviewFragment
import com.android.wallpaper.picker.undo.domain.interactor.SnapshotRestorer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope

open class ThemePickerInjector : WallpaperPicker2Injector(), CustomizationInjector {
    private var customizationSections: CustomizationSections? = null
    private var userEventLogger: UserEventLogger? = null
    private var prefs: WallpaperPreferences? = null
    private var keyguardQuickAffordancePickerInteractor: KeyguardQuickAffordancePickerInteractor? =
        null
    private var keyguardQuickAffordancePickerViewModelFactory:
        KeyguardQuickAffordancePickerViewModel.Factory? =
        null
    private var customizationProviderClient: CustomizationProviderClient? = null
    private var fragmentFactory: FragmentFactory? = null
    private var keyguardQuickAffordanceSnapshotRestorer: KeyguardQuickAffordanceSnapshotRestorer? =
        null
    private var notificationsSnapshotRestorer: NotificationsSnapshotRestorer? = null
    private var clockRegistryProvider: ClockRegistryProvider? = null
    private var clockPickerInteractor: ClockPickerInteractor? = null
    private var clockSectionViewModel: ClockSectionViewModel? = null
    private var clockCarouselViewModel: ClockCarouselViewModel? = null
    private var clockViewFactory: ClockViewFactory? = null
    private var notificationsInteractor: NotificationsInteractor? = null
    private var notificationSectionViewModelFactory: NotificationSectionViewModel.Factory? = null
    private var colorPickerInteractor: ColorPickerInteractor? = null
    private var colorPickerViewModelFactory: ColorPickerViewModel.Factory? = null
    private var darkModeSnapshotRestorer: DarkModeSnapshotRestorer? = null
    private var themedIconSnapshotRestorer: ThemedIconSnapshotRestorer? = null
    private var themedIconInteractor: ThemedIconInteractor? = null
    private var clockSettingsViewModelFactory: ClockSettingsViewModel.Factory? = null

    override fun getCustomizationSections(activity: ComponentActivity): CustomizationSections {
        return customizationSections
            ?: DefaultCustomizationSections(
                    getColorPickerViewModelFactory(
                        context = activity,
                        wallpaperColorsViewModel =
                            ViewModelProvider(activity)[WallpaperColorsViewModel::class.java],
                    ),
                    getKeyguardQuickAffordancePickerInteractor(activity),
                    getKeyguardQuickAffordancePickerViewModelFactory(activity),
                    NotificationSectionViewModel.Factory(
                        interactor = getNotificationsInteractor(activity),
                    ),
                    getFlags(),
                    getClockRegistryProvider(activity),
                    object : ClockCarouselViewModelProvider {
                        override fun get(registry: ClockRegistry): ClockCarouselViewModel {
                            return getClockCarouselViewModel(
                                context = activity,
                                clockRegistry = registry,
                            )
                        }
                    },
                    object : ClockViewFactoryProvider {
                        override fun get(registry: ClockRegistry): ClockViewFactory {
                            return getClockViewFactory(
                                activity = activity,
                                registry = registry,
                            )
                        }
                    },
                    getDarkModeSnapshotRestorer(activity),
                    getThemedIconSnapshotRestorer(activity),
                    getThemedIconInteractor(),
                )
                .also { customizationSections = it }
    }

    override fun getDeepLinkRedirectIntent(context: Context, uri: Uri): Intent {
        val intent = Intent()
        intent.setClass(context, CustomizationPickerActivity::class.java)
        intent.data = uri
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        return intent
    }

    override fun getDownloadableIntentAction(): String? {
        return null
    }

    override fun getPreviewFragment(
        context: Context,
        wallpaperInfo: WallpaperInfo,
        mode: Int,
        viewAsHome: Boolean,
        viewFullScreen: Boolean,
        testingModeEnabled: Boolean
    ): Fragment {
        return if (wallpaperInfo is LiveWallpaperInfo) LivePreviewFragment()
        else
            ImagePreviewFragment().apply {
                arguments =
                    Bundle().apply {
                        putParcelable(PreviewFragment.ARG_WALLPAPER, wallpaperInfo)
                        putInt(PreviewFragment.ARG_PREVIEW_MODE, mode)
                        putBoolean(PreviewFragment.ARG_VIEW_AS_HOME, viewAsHome)
                        putBoolean(PreviewFragment.ARG_FULL_SCREEN, viewFullScreen)
                        putBoolean(PreviewFragment.ARG_TESTING_MODE_ENABLED, testingModeEnabled)
                    }
            }
    }

    @Synchronized
    override fun getUserEventLogger(context: Context): ThemesUserEventLogger {
        return if (userEventLogger != null) userEventLogger as ThemesUserEventLogger
        else StatsLogUserEventLogger(context).also { userEventLogger = it }
    }

    @Synchronized
    override fun getPreferences(context: Context): WallpaperPreferences {
        return prefs
            ?: DefaultCustomizationPreferences(context.applicationContext).also { prefs = it }
    }

    override fun getFragmentFactory(): FragmentFactory? {
        return fragmentFactory ?: ThemePickerFragmentFactory().also { fragmentFactory }
    }

    override fun getSnapshotRestorers(context: Context): Map<Int, SnapshotRestorer> {
        return super<WallpaperPicker2Injector>.getSnapshotRestorers(context).toMutableMap().apply {
            this[KEY_QUICK_AFFORDANCE_SNAPSHOT_RESTORER] =
                getKeyguardQuickAffordanceSnapshotRestorer(context)
            this[KEY_WALLPAPER_SNAPSHOT_RESTORER] = getWallpaperSnapshotRestorer(context)
            this[KEY_NOTIFICATIONS_SNAPSHOT_RESTORER] = getNotificationsSnapshotRestorer(context)
            this[KEY_DARK_MODE_SNAPSHOT_RESTORER] = getDarkModeSnapshotRestorer(context)
            this[KEY_THEMED_ICON_SNAPSHOT_RESTORER] = getThemedIconSnapshotRestorer(context)
        }
    }

    override fun getCustomizationPreferences(context: Context): CustomizationPreferences {
        return getPreferences(context) as CustomizationPreferences
    }

    override fun getThemeManager(
        provider: ThemeBundleProvider,
        activity: FragmentActivity,
        overlayManagerCompat: OverlayManagerCompat,
        logger: ThemesUserEventLogger
    ): ThemeManager {
        return ThemeManager(provider, activity, overlayManagerCompat, logger)
    }

    override fun getKeyguardQuickAffordancePickerInteractor(
        context: Context
    ): KeyguardQuickAffordancePickerInteractor {
        return keyguardQuickAffordancePickerInteractor
            ?: getKeyguardQuickAffordancePickerInteractorImpl(context).also {
                keyguardQuickAffordancePickerInteractor = it
            }
    }

    fun getKeyguardQuickAffordancePickerViewModelFactory(
        context: Context
    ): KeyguardQuickAffordancePickerViewModel.Factory {
        return keyguardQuickAffordancePickerViewModelFactory
            ?: KeyguardQuickAffordancePickerViewModel.Factory(
                    context,
                    getKeyguardQuickAffordancePickerInteractor(context),
                    getCurrentWallpaperInfoFactory(context),
                ) { intent ->
                    context.startActivity(intent)
                }
                .also { keyguardQuickAffordancePickerViewModelFactory = it }
    }

    fun getNotificationSectionViewModelFactory(
        context: Context,
    ): NotificationSectionViewModel.Factory {
        return notificationSectionViewModelFactory
            ?: NotificationSectionViewModel.Factory(
                    interactor = getNotificationsInteractor(context),
                )
                .also { notificationSectionViewModelFactory = it }
    }

    private fun getKeyguardQuickAffordancePickerInteractorImpl(
        context: Context
    ): KeyguardQuickAffordancePickerInteractor {
        val client = getKeyguardQuickAffordancePickerProviderClient(context)
        return KeyguardQuickAffordancePickerInteractor(
            KeyguardQuickAffordancePickerRepository(client, Dispatchers.IO),
            client
        ) { getKeyguardQuickAffordanceSnapshotRestorer(context) }
    }

    protected fun getKeyguardQuickAffordancePickerProviderClient(
        context: Context
    ): CustomizationProviderClient {
        return customizationProviderClient
            ?: CustomizationProviderClientImpl(context, Dispatchers.IO).also {
                customizationProviderClient = it
            }
    }

    private fun getKeyguardQuickAffordanceSnapshotRestorer(
        context: Context
    ): KeyguardQuickAffordanceSnapshotRestorer {
        return keyguardQuickAffordanceSnapshotRestorer
            ?: KeyguardQuickAffordanceSnapshotRestorer(
                    getKeyguardQuickAffordancePickerInteractor(context),
                    getKeyguardQuickAffordancePickerProviderClient(context)
                )
                .also { keyguardQuickAffordanceSnapshotRestorer = it }
    }

    private fun getNotificationsSnapshotRestorer(context: Context): NotificationsSnapshotRestorer {
        return notificationsSnapshotRestorer
            ?: NotificationsSnapshotRestorer(
                    interactor =
                        getNotificationsInteractor(
                            context = context,
                        ),
                )
                .also { notificationsSnapshotRestorer = it }
    }

    override fun getClockRegistryProvider(context: Context): ClockRegistryProvider {
        return clockRegistryProvider
            ?: ClockRegistryProvider(
                    context = context,
                    coroutineScope = GlobalScope,
                    mainDispatcher = Dispatchers.Main,
                    backgroundDispatcher = Dispatchers.IO,
                )
                .also { clockRegistryProvider = it }
    }

    override fun getClockPickerInteractor(
        context: Context,
        clockRegistry: ClockRegistry,
    ): ClockPickerInteractor {
        return clockPickerInteractor
            ?: ClockPickerInteractor(
                    ClockPickerRepositoryImpl(
                        secureSettingsRepository = getSecureSettingsRepository(context),
                        registry = clockRegistry,
                        scope = GlobalScope,
                        backgroundDispatcher = Dispatchers.IO,
                    ),
                )
                .also { clockPickerInteractor = it }
    }

    override fun getClockSectionViewModel(
        context: Context,
        clockRegistry: ClockRegistry,
    ): ClockSectionViewModel {
        return clockSectionViewModel
            ?: ClockSectionViewModel(getClockPickerInteractor(context, clockRegistry)).also {
                clockSectionViewModel = it
            }
    }

    override fun getClockCarouselViewModel(
        context: Context,
        clockRegistry: ClockRegistry
    ): ClockCarouselViewModel {
        return clockCarouselViewModel
            ?: ClockCarouselViewModel(getClockPickerInteractor(context, clockRegistry)).also {
                clockCarouselViewModel = it
            }
    }

    override fun getClockViewFactory(
        activity: Activity,
        registry: ClockRegistry,
    ): ClockViewFactory {
        return clockViewFactory
            ?: ClockViewFactory(activity, registry).also { clockViewFactory = it }
    }

    protected fun getNotificationsInteractor(
        context: Context,
    ): NotificationsInteractor {
        return notificationsInteractor
            ?: NotificationsInteractor(
                    repository =
                        NotificationsRepository(
                            scope = GlobalScope,
                            backgroundDispatcher = Dispatchers.IO,
                            secureSettingsRepository = getSecureSettingsRepository(context),
                        ),
                    snapshotRestorer = { getNotificationsSnapshotRestorer(context) },
                )
                .also { notificationsInteractor = it }
    }

    override fun getColorPickerInteractor(
        context: Context,
        wallpaperColorsViewModel: WallpaperColorsViewModel,
    ): ColorPickerInteractor {
        return colorPickerInteractor
            ?: ColorPickerInteractor(ColorPickerRepositoryImpl(context, wallpaperColorsViewModel))
                .also { colorPickerInteractor = it }
    }

    override fun getColorPickerViewModelFactory(
        context: Context,
        wallpaperColorsViewModel: WallpaperColorsViewModel,
    ): ColorPickerViewModel.Factory {
        return colorPickerViewModelFactory
            ?: ColorPickerViewModel.Factory(
                    context,
                    getColorPickerInteractor(context, wallpaperColorsViewModel),
                )
                .also { colorPickerViewModelFactory = it }
    }

    fun getDarkModeSnapshotRestorer(
        context: Context,
    ): DarkModeSnapshotRestorer {
        return darkModeSnapshotRestorer
            ?: DarkModeSnapshotRestorer(
                    context = context,
                    manager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager,
                    backgroundDispatcher = Dispatchers.IO,
                )
                .also { darkModeSnapshotRestorer = it }
    }

    protected fun getThemedIconSnapshotRestorer(
        context: Context,
    ): ThemedIconSnapshotRestorer {
        val optionProvider = ThemedIconSwitchProvider.getInstance(context)
        return themedIconSnapshotRestorer
            ?: ThemedIconSnapshotRestorer(
                    isActivated = { optionProvider.isThemedIconEnabled },
                    setActivated = { isActivated ->
                        optionProvider.isThemedIconEnabled = isActivated
                    },
                    interactor = getThemedIconInteractor(),
                )
                .also { themedIconSnapshotRestorer = it }
    }

    protected fun getThemedIconInteractor(): ThemedIconInteractor {
        return themedIconInteractor
            ?: ThemedIconInteractor(
                    repository = ThemeIconRepository(),
                )
                .also { themedIconInteractor = it }
    }

    override fun getClockSettingsViewModelFactory(
        context: Context,
        registry: ClockRegistry,
    ): ClockSettingsViewModel.Factory {
        return clockSettingsViewModelFactory
            ?: ClockSettingsViewModel.Factory(
                    context,
                    getClockPickerInteractor(context, registry),
                )
                .also { clockSettingsViewModelFactory = it }
    }

    companion object {
        @JvmStatic
        private val KEY_QUICK_AFFORDANCE_SNAPSHOT_RESTORER =
            WallpaperPicker2Injector.MIN_SNAPSHOT_RESTORER_KEY
        @JvmStatic
        private val KEY_WALLPAPER_SNAPSHOT_RESTORER = KEY_QUICK_AFFORDANCE_SNAPSHOT_RESTORER + 1
        @JvmStatic
        private val KEY_NOTIFICATIONS_SNAPSHOT_RESTORER = KEY_WALLPAPER_SNAPSHOT_RESTORER + 1
        @JvmStatic
        private val KEY_DARK_MODE_SNAPSHOT_RESTORER = KEY_NOTIFICATIONS_SNAPSHOT_RESTORER + 1
        @JvmStatic
        private val KEY_THEMED_ICON_SNAPSHOT_RESTORER = KEY_DARK_MODE_SNAPSHOT_RESTORER + 1

        /**
         * When this injector is overridden, this is the minimal value that should be used by
         * restorers returns in [getSnapshotRestorers].
         */
        @JvmStatic protected val MIN_SNAPSHOT_RESTORER_KEY = KEY_THEMED_ICON_SNAPSHOT_RESTORER + 1
    }
}
