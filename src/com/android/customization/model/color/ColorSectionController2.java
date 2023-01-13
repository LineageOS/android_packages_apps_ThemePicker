/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.customization.model.color;

import static com.android.customization.model.color.ColorOptionsProvider.COLOR_SOURCE_HOME;
import static com.android.customization.model.color.ColorOptionsProvider.COLOR_SOURCE_LOCK;
import static com.android.customization.model.color.ColorOptionsProvider.COLOR_SOURCE_PRESET;

import android.app.Activity;
import android.app.WallpaperColors;
import android.content.Context;
import android.os.Handler;
import android.os.SystemClock;
import android.stats.style.StyleEnums;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;

import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;

import com.android.customization.model.CustomizationManager;
import com.android.customization.model.theme.OverlayManagerCompat;
import com.android.customization.module.CustomizationInjector;
import com.android.customization.module.ThemesUserEventLogger;
import com.android.customization.picker.color.ColorPickerFragment;
import com.android.customization.picker.color.ColorSectionView2;
import com.android.wallpaper.R;
import com.android.wallpaper.model.CustomizationSectionController;
import com.android.wallpaper.model.WallpaperColorsViewModel;
import com.android.wallpaper.module.InjectorProvider;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

/**
 * Color section view's controller for the logic of color customization.
 */
public class ColorSectionController2 implements CustomizationSectionController<ColorSectionView2> {

    private static final String TAG = "ColorSectionController";
    private static final long MIN_COLOR_APPLY_PERIOD = 500L;

    private final ThemesUserEventLogger mEventLogger;
    private final ColorCustomizationManager mColorManager;
    private final WallpaperColorsViewModel mWallpaperColorsViewModel;
    private final LifecycleOwner mLifecycleOwner;
    private final CustomizationSectionNavigationController mSectionNavigationController;

    private List<ColorOption> mWallpaperColorOptions = new ArrayList<>();
    private List<ColorOption> mPresetColorOptions = new ArrayList<>();
    private ColorOption mSelectedColor;
    @Nullable private WallpaperColors mHomeWallpaperColors;
    @Nullable private WallpaperColors mLockWallpaperColors;
    // Uses a boolean value to indicate whether wallpaper color is ready because WallpaperColors
    // maybe be null when it's ready.
    private boolean mHomeWallpaperColorsReady;
    private boolean mLockWallpaperColorsReady;
    private long mLastColorApplyingTime = 0L;
    private ColorSectionView2 mColorSectionView;

    public ColorSectionController2(Activity activity, WallpaperColorsViewModel viewModel,
            LifecycleOwner lifecycleOwner,
            CustomizationSectionNavigationController sectionNavigationController) {
        CustomizationInjector injector = (CustomizationInjector) InjectorProvider.getInjector();
        mEventLogger = (ThemesUserEventLogger) injector.getUserEventLogger(activity);
        mColorManager = ColorCustomizationManager.getInstance(activity,
                new OverlayManagerCompat(activity));
        mWallpaperColorsViewModel = viewModel;
        mLifecycleOwner = lifecycleOwner;
        mSectionNavigationController = sectionNavigationController;
    }

    @Override
    public boolean isAvailable(@Nullable Context context) {
        return context != null && ColorUtils.isMonetEnabled(context) && mColorManager.isAvailable();
    }

    @Override
    public ColorSectionView2 createView(Context context) {
        mColorSectionView = (ColorSectionView2) LayoutInflater.from(context).inflate(
                R.layout.color_section_view2, /* root= */ null);

        mWallpaperColorsViewModel.getHomeWallpaperColors().observe(mLifecycleOwner,
                homeColors -> {
                    mHomeWallpaperColors = homeColors;
                    mHomeWallpaperColorsReady = true;
                    maybeLoadColors();
                });
        mWallpaperColorsViewModel.getLockWallpaperColors().observe(mLifecycleOwner,
                lockColors -> {
                    mLockWallpaperColors = lockColors;
                    mLockWallpaperColorsReady = true;
                    maybeLoadColors();
                });
        return mColorSectionView;
    }

    private void maybeLoadColors() {
        if (mHomeWallpaperColorsReady && mLockWallpaperColorsReady) {
            mColorManager.setWallpaperColors(mHomeWallpaperColors, mLockWallpaperColors);
            loadColorOptions(/* reload= */ false);
        }
    }

    private void loadColorOptions(boolean reload) {
        mColorManager.fetchOptions(new CustomizationManager.OptionsFetchedListener<ColorOption>() {
            @Override
            public void onOptionsLoaded(List<ColorOption> options) {
                List<ColorOption> wallpaperColorOptions = new ArrayList<>();
                List<ColorOption> presetColorOptions = new ArrayList<>();
                for (ColorOption option : options) {
                    if (option instanceof ColorSeedOption) {
                        wallpaperColorOptions.add(option);
                    } else if (option instanceof ColorBundle) {
                        presetColorOptions.add(option);
                    }
                }
                mWallpaperColorOptions = wallpaperColorOptions;
                mPresetColorOptions = presetColorOptions;
                mSelectedColor = findActiveColorOption(mWallpaperColorOptions,
                        mPresetColorOptions);

                mColorSectionView.post(() -> setUpColorSectionView(mWallpaperColorOptions,
                        mPresetColorOptions));
            }

            @Override
            public void onError(@Nullable Throwable throwable) {
                if (throwable != null) {
                    Log.e(TAG, "Error loading theme bundles", throwable);
                }
            }
        }, reload);
    }

    private void setUpColorSectionView(List<ColorOption> wallpaperColorOptions,
            List<ColorOption> presetColorOptions) {
        int wallpaperOptionSize = wallpaperColorOptions.size();

        List<ColorOption> subOptions = wallpaperColorOptions.subList(0,
                Math.min(5, wallpaperOptionSize));
        // add additional options based on preset colors if there are less than 5 wallpaper colors
        List<ColorOption> additionalSubOptions = presetColorOptions.subList(0,
                Math.min(Math.max(0, 5 - wallpaperOptionSize), presetColorOptions.size()));
        subOptions.addAll(additionalSubOptions);

        mColorSectionView.setOverflowOnClick(() -> {
            mSectionNavigationController.navigateTo(new ColorPickerFragment());
            return null;
        });
        mColorSectionView.setColorOptionOnClick(selectedOption -> {
            if (mSelectedColor.equals(selectedOption)) {
                return null;
            }
            mSelectedColor = (ColorOption) selectedOption;
            // Post with delay for color option to run ripple.
            new Handler().postDelayed(()-> applyColor(mSelectedColor), /* delayMillis= */ 100);
            return null;
        });
        mColorSectionView.setItems(subOptions, mColorManager);
    }

    private ColorOption findActiveColorOption(List<ColorOption> wallpaperColorOptions,
            List<ColorOption> presetColorOptions) {
        ColorOption activeColorOption = null;
        for (ColorOption colorOption : Lists.newArrayList(
                Iterables.concat(wallpaperColorOptions, presetColorOptions))) {
            if (colorOption.isActive(mColorManager)) {
                activeColorOption = colorOption;
                break;
            }
        }
        // Use the first one option by default. This should not happen as above should have an
        // active option found.
        if (activeColorOption == null) {
            activeColorOption = wallpaperColorOptions.isEmpty()
                    ? presetColorOptions.get(0)
                    : wallpaperColorOptions.get(0);
        }
        return activeColorOption;
    }

    private void applyColor(ColorOption colorOption) {
        if (SystemClock.elapsedRealtime() - mLastColorApplyingTime < MIN_COLOR_APPLY_PERIOD) {
            return;
        }
        mLastColorApplyingTime = SystemClock.elapsedRealtime();
        mColorManager.apply(colorOption, new CustomizationManager.Callback() {
            @Override
            public void onSuccess() {
                mColorSectionView.announceForAccessibility(
                        mColorSectionView.getContext().getString(R.string.color_changed));
                mEventLogger.logColorApplied(getColorAction(colorOption), colorOption);
            }

            @Override
            public void onError(@Nullable Throwable throwable) {
                Log.w(TAG, "Apply theme with error: " + throwable);
            }
        });
    }

    private int getColorAction(ColorOption colorOption) {
        int action = StyleEnums.DEFAULT_ACTION;
        boolean isForBoth = mLockWallpaperColors == null || mLockWallpaperColors.equals(
                mHomeWallpaperColors);

        if (TextUtils.equals(colorOption.getSource(), COLOR_SOURCE_PRESET)) {
            action = StyleEnums.COLOR_PRESET_APPLIED;
        } else if (isForBoth) {
            action = StyleEnums.COLOR_WALLPAPER_HOME_LOCK_APPLIED;
        } else {
            switch (colorOption.getSource()) {
                case COLOR_SOURCE_HOME:
                    action = StyleEnums.COLOR_WALLPAPER_HOME_APPLIED;
                    break;
                case COLOR_SOURCE_LOCK:
                    action = StyleEnums.COLOR_WALLPAPER_LOCK_APPLIED;
                    break;
            }
        }
        return action;
    }
}
