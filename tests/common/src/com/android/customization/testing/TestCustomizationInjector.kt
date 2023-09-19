package com.android.customization.testing

import android.content.Context
import android.content.res.Resources
import androidx.activity.ComponentActivity
import androidx.fragment.app.FragmentActivity
import com.android.customization.model.theme.OverlayManagerCompat
import com.android.customization.model.theme.ThemeBundleProvider
import com.android.customization.model.theme.ThemeManager
import com.android.customization.module.CustomizationInjector
import com.android.customization.module.CustomizationPreferences
import com.android.customization.module.ThemesUserEventLogger
import com.android.customization.picker.clock.domain.interactor.ClockPickerInteractor
import com.android.customization.picker.clock.ui.view.ClockViewFactory
import com.android.customization.picker.clock.ui.viewmodel.ClockCarouselViewModel
import com.android.customization.picker.clock.ui.viewmodel.ClockSectionViewModel
import com.android.customization.picker.clock.ui.viewmodel.ClockSettingsViewModel
import com.android.customization.picker.clock.utils.ClockDescriptionUtils
import com.android.customization.picker.color.domain.interactor.ColorPickerInteractor
import com.android.customization.picker.color.ui.viewmodel.ColorPickerViewModel
import com.android.customization.picker.quickaffordance.domain.interactor.KeyguardQuickAffordancePickerInteractor
import com.android.systemui.shared.clocks.ClockRegistry
import com.android.wallpaper.model.WallpaperColorsRepository
import com.android.wallpaper.module.UserEventLogger
import com.android.wallpaper.testing.TestInjector
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class TestCustomizationInjector @Inject constructor() : TestInjector(), CustomizationInjector {
    private var customizationPrefs: CustomizationPreferences? = null
    private var themeManager: ThemeManager? = null
    private var themesUserEventLogger: ThemesUserEventLogger? = null

    /////////////////
    // CustomizationInjector implementations
    /////////////////

    override fun getCustomizationPreferences(context: Context): CustomizationPreferences {
        return customizationPrefs
            ?: TestDefaultCustomizationPreferences(context).also { customizationPrefs = it }
    }

    override fun getThemeManager(
        provider: ThemeBundleProvider,
        activity: FragmentActivity,
        overlayManagerCompat: OverlayManagerCompat,
        logger: ThemesUserEventLogger,
    ): ThemeManager {
        return themeManager
            ?: TestThemeManager(provider, activity, overlayManagerCompat, logger).also {
                themeManager = it
            }
    }

    override fun getKeyguardQuickAffordancePickerInteractor(
        context: Context
    ): KeyguardQuickAffordancePickerInteractor {
        throw UnsupportedOperationException("not implemented")
    }

    override fun getClockRegistry(context: Context): ClockRegistry? {
        throw UnsupportedOperationException("not implemented")
    }

    override fun getClockPickerInteractor(context: Context): ClockPickerInteractor {
        throw UnsupportedOperationException("not implemented")
    }

    override fun getClockSectionViewModel(context: Context): ClockSectionViewModel {
        throw UnsupportedOperationException("not implemented")
    }

    override fun getColorPickerInteractor(
        context: Context,
        wallpaperColorsRepository: WallpaperColorsRepository,
    ): ColorPickerInteractor {
        throw UnsupportedOperationException("not implemented")
    }

    override fun getColorPickerViewModelFactory(
        context: Context,
        wallpaperColorsRepository: WallpaperColorsRepository,
    ): ColorPickerViewModel.Factory {
        throw UnsupportedOperationException("not implemented")
    }

    override fun getClockCarouselViewModelFactory(
        interactor: ClockPickerInteractor
    ): ClockCarouselViewModel.Factory {
        throw UnsupportedOperationException("not implemented")
    }

    override fun getClockViewFactory(activity: ComponentActivity): ClockViewFactory {
        throw UnsupportedOperationException("not implemented")
    }

    override fun getClockSettingsViewModelFactory(
        context: Context,
        wallpaperColorsRepository: WallpaperColorsRepository,
        clockViewFactory: ClockViewFactory,
    ): ClockSettingsViewModel.Factory {
        throw UnsupportedOperationException("not implemented")
    }

    override fun getClockDescriptionUtils(resources: Resources): ClockDescriptionUtils {
        throw UnsupportedOperationException("not implemented")
    }

    /////////////////
    // TestInjector overrides
    /////////////////

    override fun getUserEventLogger(context: Context): UserEventLogger {
        return themesUserEventLogger
            ?: TestThemesUserEventLogger().also { themesUserEventLogger = it }
    }
}
