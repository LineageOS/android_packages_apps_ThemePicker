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
package com.android.customization.module;

import static com.android.wallpaper.picker.PreviewFragment.ARG_FULL_SCREEN;
import static com.android.wallpaper.picker.PreviewFragment.ARG_PREVIEW_MODE;
import static com.android.wallpaper.picker.PreviewFragment.ARG_TESTING_MODE_ENABLED;
import static com.android.wallpaper.picker.PreviewFragment.ARG_VIEW_AS_HOME;
import static com.android.wallpaper.picker.PreviewFragment.ARG_WALLPAPER;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.android.customization.model.theme.OverlayManagerCompat;
import com.android.customization.model.theme.ThemeBundleProvider;
import com.android.customization.model.theme.ThemeManager;
import com.android.wallpaper.model.LiveWallpaperInfo;
import com.android.wallpaper.model.WallpaperInfo;
import com.android.wallpaper.module.CustomizationSections;
import com.android.wallpaper.module.WallpaperPicker2Injector;
import com.android.wallpaper.module.WallpaperPreferences;
import com.android.wallpaper.picker.CustomizationPickerActivity;
import com.android.wallpaper.picker.ImagePreviewFragment;
import com.android.wallpaper.picker.LivePreviewFragment;
import com.android.wallpaper.picker.PreviewFragment;

/**
 * A concrete, real implementation of the dependency provider.
 */
public class ThemePickerInjector extends WallpaperPicker2Injector
        implements CustomizationInjector {
    private CustomizationSections mCustomizationSections;
    private ThemesUserEventLogger mUserEventLogger;
    private WallpaperPreferences mPrefs;

    @Override
    public CustomizationSections getCustomizationSections() {
        if (mCustomizationSections == null) {
            mCustomizationSections = new DefaultCustomizationSections();
        }
        return mCustomizationSections;
    }

    @Override
    public Intent getDeepLinkRedirectIntent(Context context, Uri uri) {
        Intent intent = new Intent();
        intent.setClass(context, CustomizationPickerActivity.class);
        intent.setData(uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return intent;
    }

    @Override
    public String getDownloadableIntentAction() {
        return null;
    }

    @Override
    public Fragment getPreviewFragment(
            Context context,
            WallpaperInfo wallpaperInfo,
            int mode,
            boolean viewAsHome,
            boolean viewFullScreen,
            boolean testingModeEnabled) {
        Bundle args = new Bundle();
        args.putParcelable(ARG_WALLPAPER, wallpaperInfo);
        args.putInt(ARG_PREVIEW_MODE, mode);
        args.putBoolean(ARG_VIEW_AS_HOME, viewAsHome);
        args.putBoolean(ARG_FULL_SCREEN, viewFullScreen);
        args.putBoolean(ARG_TESTING_MODE_ENABLED, testingModeEnabled);
        PreviewFragment fragment = wallpaperInfo instanceof LiveWallpaperInfo
                ? new LivePreviewFragment() : new ImagePreviewFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public synchronized ThemesUserEventLogger getUserEventLogger(Context context) {
        if (mUserEventLogger == null) {
            mUserEventLogger = new StatsLogUserEventLogger(context);
        }
        return mUserEventLogger;
    }

    @Override
    public synchronized WallpaperPreferences getPreferences(Context context) {
        if (mPrefs == null) {
            mPrefs = new DefaultCustomizationPreferences(context.getApplicationContext());
        }
        return mPrefs;
    }

    //
    // Functions from {@link CustomizationInjector}
    //
    @Override
    public CustomizationPreferences getCustomizationPreferences(Context context) {
        return (CustomizationPreferences) getPreferences(context);
    }

    @Override
    public ThemeManager getThemeManager(ThemeBundleProvider provider, FragmentActivity activity,
            OverlayManagerCompat overlayManagerCompat, ThemesUserEventLogger logger) {
        return new ThemeManager(provider, activity, overlayManagerCompat, logger);
    }
}
