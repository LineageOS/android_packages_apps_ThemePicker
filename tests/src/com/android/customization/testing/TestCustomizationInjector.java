package com.android.customization.testing;

import android.content.Context;

import androidx.fragment.app.FragmentActivity;

import com.android.customization.model.theme.OverlayManagerCompat;
import com.android.customization.model.theme.ThemeBundleProvider;
import com.android.customization.model.theme.ThemeManager;
import com.android.customization.module.CustomizationInjector;
import com.android.customization.module.CustomizationPreferences;
import com.android.customization.module.ThemesUserEventLogger;
import com.android.customization.picker.quickaffordance.data.repository.KeyguardQuickAffordancePickerRepository;
import com.android.customization.picker.quickaffordance.domain.interactor.KeyguardQuickAffordancePickerInteractor;
import com.android.customization.picker.quickaffordance.domain.interactor.KeyguardQuickAffordanceSnapshotRestorer;
import com.android.systemui.shared.customization.data.content.CustomizationProviderClient;
import com.android.systemui.shared.customization.data.content.CustomizationProviderClientImpl;
import com.android.wallpaper.config.BaseFlags;
import com.android.wallpaper.module.DrawableLayerResolver;
import com.android.wallpaper.module.PackageStatusNotifier;
import com.android.wallpaper.module.UserEventLogger;
import com.android.wallpaper.picker.undo.domain.interactor.SnapshotRestorer;
import com.android.wallpaper.testing.TestInjector;

import java.util.HashMap;
import java.util.Map;

import kotlinx.coroutines.Dispatchers;

/**
 * Test implementation of the dependency injector.
 */
public class TestCustomizationInjector extends TestInjector implements CustomizationInjector {
    private CustomizationPreferences mCustomizationPreferences;
    private ThemeManager mThemeManager;
    private PackageStatusNotifier mPackageStatusNotifier;
    private DrawableLayerResolver mDrawableLayerResolver;
    private UserEventLogger mUserEventLogger;
    private KeyguardQuickAffordancePickerInteractor mKeyguardQuickAffordancePickerInteractor;
    private BaseFlags mFlags;
    private CustomizationProviderClient mCustomizationProviderClient;
    private KeyguardQuickAffordanceSnapshotRestorer mKeyguardQuickAffordanceSnapshotRestorer;

    @Override
    public CustomizationPreferences getCustomizationPreferences(Context context) {
        if (mCustomizationPreferences == null) {
            mCustomizationPreferences = new TestDefaultCustomizationPreferences(context);
        }
        return mCustomizationPreferences;
    }

    @Override
    public ThemeManager getThemeManager(
            ThemeBundleProvider provider,
            FragmentActivity activity,
            OverlayManagerCompat overlayManagerCompat,
            ThemesUserEventLogger logger) {
        if (mThemeManager == null) {
            mThemeManager = new TestThemeManager(provider, activity, overlayManagerCompat, logger);
        }
        return mThemeManager;
    }

    @Override
    public PackageStatusNotifier getPackageStatusNotifier(Context context) {
        if (mPackageStatusNotifier == null) {
            mPackageStatusNotifier =  new TestPackageStatusNotifier();
        }
        return mPackageStatusNotifier;
    }

    @Override
    public DrawableLayerResolver getDrawableLayerResolver() {
        if (mDrawableLayerResolver == null) {
            mDrawableLayerResolver = new TestDrawableLayerResolver();
        }
        return mDrawableLayerResolver;
    }

    @Override
    public UserEventLogger getUserEventLogger(Context unused) {
        if (mUserEventLogger == null) {
            mUserEventLogger = new TestThemesUserEventLogger();
        }
        return mUserEventLogger;
    }

    @Override
    public KeyguardQuickAffordancePickerInteractor getKeyguardQuickAffordancePickerInteractor(
            Context context) {
        if (mKeyguardQuickAffordancePickerInteractor == null) {
            final CustomizationProviderClient client =
                    new CustomizationProviderClientImpl(context, Dispatchers.getIO());
            mKeyguardQuickAffordancePickerInteractor = new KeyguardQuickAffordancePickerInteractor(
                    new KeyguardQuickAffordancePickerRepository(client, Dispatchers.getIO()),
                    client,
                    () -> getKeyguardQuickAffordanceSnapshotRestorer(context));
        }
        return mKeyguardQuickAffordancePickerInteractor;
    }

    @Override
    public BaseFlags getFlags() {
        if (mFlags == null) {
            mFlags = new BaseFlags() {};
        }

        return mFlags;
    }

    @Override
    public Map<Integer, SnapshotRestorer> getSnapshotRestorers(Context context) {
        final Map<Integer, SnapshotRestorer> restorers = new HashMap<>();
        restorers.put(
                KEY_QUICK_AFFORDANCE_SNAPSHOT_RESTORER,
                getKeyguardQuickAffordanceSnapshotRestorer(context));
        return restorers;
    }

    /** Returns the {@link CustomizationProviderClient}. */
    private CustomizationProviderClient getKeyguardQuickAffordancePickerProviderClient(
            Context context) {
        if (mCustomizationProviderClient == null) {
            mCustomizationProviderClient =
                    new CustomizationProviderClientImpl(context, Dispatchers.getIO());
        }

        return mCustomizationProviderClient;
    }

    private KeyguardQuickAffordanceSnapshotRestorer getKeyguardQuickAffordanceSnapshotRestorer(
            Context context) {
        if (mKeyguardQuickAffordanceSnapshotRestorer == null) {
            mKeyguardQuickAffordanceSnapshotRestorer = new KeyguardQuickAffordanceSnapshotRestorer(
                    getKeyguardQuickAffordancePickerInteractor(context),
                    getKeyguardQuickAffordancePickerProviderClient(context));
        }

        return mKeyguardQuickAffordanceSnapshotRestorer;
    }

    private static final int KEY_QUICK_AFFORDANCE_SNAPSHOT_RESTORER = 1;
}
