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
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.android.customization.model.theme.OverlayManagerCompat
import com.android.customization.model.theme.ThemeBundleProvider
import com.android.customization.model.theme.ThemeManager
import com.android.customization.picker.quickaffordance.data.repository.KeyguardQuickAffordancePickerRepository
import com.android.customization.picker.quickaffordance.domain.interactor.KeyguardQuickAffordancePickerInteractor
import com.android.customization.picker.quickaffordance.domain.interactor.KeyguardQuickAffordanceSnapshotRestorer
import com.android.customization.picker.quickaffordance.ui.viewmodel.KeyguardQuickAffordancePickerViewModel
import com.android.systemui.shared.customization.data.content.CustomizationProviderClient
import com.android.systemui.shared.customization.data.content.CustomizationProviderClientImpl
import com.android.wallpaper.model.LiveWallpaperInfo
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
import kotlinx.coroutines.Dispatchers.IO

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

    override fun getCustomizationSections(activity: Activity): CustomizationSections {
        return customizationSections
            ?: DefaultCustomizationSections(
                    getKeyguardQuickAffordancePickerInteractor(activity),
                    getKeyguardQuickAffordancePickerViewModelFactory(activity)
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
                    getUndoInteractor(context),
                    getCurrentWallpaperInfoFactory(context),
                )
                .also { keyguardQuickAffordancePickerViewModelFactory = it }
    }

    private fun getKeyguardQuickAffordancePickerInteractorImpl(
        context: Context
    ): KeyguardQuickAffordancePickerInteractor {
        val client = getKeyguardQuickAffordancePickerProviderClient(context)
        return KeyguardQuickAffordancePickerInteractor(
            KeyguardQuickAffordancePickerRepository(client, IO),
            client
        ) { getKeyguardQuickAffordanceSnapshotRestorer(context) }
    }

    protected fun getKeyguardQuickAffordancePickerProviderClient(
        context: Context
    ): CustomizationProviderClient {
        return customizationProviderClient
            ?: CustomizationProviderClientImpl(context, IO).also {
                customizationProviderClient = it
            }
    }

    protected fun getKeyguardQuickAffordanceSnapshotRestorer(
        context: Context
    ): KeyguardQuickAffordanceSnapshotRestorer {
        return keyguardQuickAffordanceSnapshotRestorer
            ?: KeyguardQuickAffordanceSnapshotRestorer(
                    getKeyguardQuickAffordancePickerInteractor(context),
                    getKeyguardQuickAffordancePickerProviderClient(context)
                )
                .also { keyguardQuickAffordanceSnapshotRestorer = it }
    }

    companion object {
        @JvmStatic
        private val KEY_QUICK_AFFORDANCE_SNAPSHOT_RESTORER =
            WallpaperPicker2Injector.MIN_SNAPSHOT_RESTORER_KEY

        /**
         * When this injector is overridden, this is the minimal value that should be used by
         * restorers returns in [getSnapshotRestorers].
         */
        @JvmStatic
        protected val MIN_SNAPSHOT_RESTORER_KEY = KEY_QUICK_AFFORDANCE_SNAPSHOT_RESTORER + 1
    }
}
