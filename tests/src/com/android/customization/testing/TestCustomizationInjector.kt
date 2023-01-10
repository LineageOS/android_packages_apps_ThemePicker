package com.android.customization.testing

import android.content.Context
import android.os.Handler
import android.os.UserHandle
import android.view.LayoutInflater
import androidx.fragment.app.FragmentActivity
import com.android.customization.model.theme.OverlayManagerCompat
import com.android.customization.model.theme.ThemeBundleProvider
import com.android.customization.model.theme.ThemeManager
import com.android.customization.module.CustomizationInjector
import com.android.customization.module.CustomizationPreferences
import com.android.customization.module.ThemesUserEventLogger
import com.android.customization.picker.quickaffordance.data.repository.KeyguardQuickAffordancePickerRepository
import com.android.customization.picker.quickaffordance.domain.interactor.KeyguardQuickAffordancePickerInteractor
import com.android.customization.picker.quickaffordance.domain.interactor.KeyguardQuickAffordanceSnapshotRestorer
import com.android.systemui.plugins.PluginManager
import com.android.systemui.shared.clocks.ClockRegistry
import com.android.systemui.shared.clocks.DefaultClockProvider
import com.android.systemui.shared.customization.data.content.CustomizationProviderClient
import com.android.systemui.shared.customization.data.content.CustomizationProviderClientImpl
import com.android.wallpaper.config.BaseFlags
import com.android.wallpaper.module.DrawableLayerResolver
import com.android.wallpaper.module.PackageStatusNotifier
import com.android.wallpaper.module.UserEventLogger
import com.android.wallpaper.picker.undo.domain.interactor.SnapshotRestorer
import com.android.wallpaper.testing.TestInjector
import java.util.HashMap
import kotlinx.coroutines.Dispatchers.IO

/** Test implementation of the dependency injector. */
class TestCustomizationInjector : TestInjector(), CustomizationInjector {
    private var customizationPreferences: CustomizationPreferences? = null
    private var themeManager: ThemeManager? = null
    private var packageStatusNotifier: PackageStatusNotifier? = null
    private var drawableLayerResolver: DrawableLayerResolver? = null
    private var userEventLogger: UserEventLogger? = null
    private var keyguardQuickAffordancePickerInteractor: KeyguardQuickAffordancePickerInteractor? =
        null
    private var flags: BaseFlags? = null
    private var customizationProviderClient: CustomizationProviderClient? = null
    private var keyguardQuickAffordanceSnapshotRestorer: KeyguardQuickAffordanceSnapshotRestorer? =
        null
    private var clockRegistry: ClockRegistry? = null
    private var pluginManager: PluginManager? = null

    override fun getCustomizationPreferences(context: Context): CustomizationPreferences {
        return customizationPreferences
            ?: TestDefaultCustomizationPreferences(context).also { customizationPreferences = it }
    }

    override fun getThemeManager(
        provider: ThemeBundleProvider,
        activity: FragmentActivity,
        overlayManagerCompat: OverlayManagerCompat,
        logger: ThemesUserEventLogger
    ): ThemeManager {
        return themeManager
            ?: TestThemeManager(provider, activity, overlayManagerCompat, logger).also {
                themeManager = it
            }
    }

    override fun getPackageStatusNotifier(context: Context): PackageStatusNotifier {
        return packageStatusNotifier
            ?: TestPackageStatusNotifier().also {
                packageStatusNotifier = TestPackageStatusNotifier()
            }
    }

    override fun getDrawableLayerResolver(): DrawableLayerResolver {
        return drawableLayerResolver
            ?: TestDrawableLayerResolver().also { drawableLayerResolver = it }
    }

    override fun getUserEventLogger(context: Context): UserEventLogger {
        return userEventLogger ?: TestThemesUserEventLogger().also { userEventLogger = it }
    }

    override fun getKeyguardQuickAffordancePickerInteractor(
        context: Context
    ): KeyguardQuickAffordancePickerInteractor {
        return keyguardQuickAffordancePickerInteractor
            ?: createCustomizationProviderClient(context).also {
                keyguardQuickAffordancePickerInteractor = it
            }
    }

    private fun createCustomizationProviderClient(
        context: Context
    ): KeyguardQuickAffordancePickerInteractor {
        val client: CustomizationProviderClient = CustomizationProviderClientImpl(context, IO)
        return KeyguardQuickAffordancePickerInteractor(
            KeyguardQuickAffordancePickerRepository(client, IO),
            client
        ) { getKeyguardQuickAffordanceSnapshotRestorer(context) }
    }

    override fun getFlags(): BaseFlags {
        return flags ?: object : BaseFlags() {}.also { flags = it }
    }

    override fun getSnapshotRestorers(context: Context): Map<Int, SnapshotRestorer> {
        val restorers: MutableMap<Int, SnapshotRestorer> = HashMap()
        restorers[KEY_QUICK_AFFORDANCE_SNAPSHOT_RESTORER] =
            getKeyguardQuickAffordanceSnapshotRestorer(context)
        return restorers
    }

    /** Returns the [CustomizationProviderClient]. */
    private fun getKeyguardQuickAffordancePickerProviderClient(
        context: Context
    ): CustomizationProviderClient {
        return customizationProviderClient
            ?: CustomizationProviderClientImpl(context, IO).also {
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

    override fun getClockRegistry(context: Context): ClockRegistry {
        return clockRegistry
            ?: ClockRegistry(
                    context,
                    getPluginManager(context),
                    Handler.getMain(),
                    isEnabled = true,
                    userHandle = UserHandle.USER_SYSTEM,
                    DefaultClockProvider(context, LayoutInflater.from(context), context.resources)
                )
                .also { clockRegistry = it }
    }

    override fun getPluginManager(context: Context): PluginManager {
        return pluginManager ?: TestPluginManager().also { pluginManager = it }
    }

    companion object {
        private const val KEY_QUICK_AFFORDANCE_SNAPSHOT_RESTORER = 1
    }
}
