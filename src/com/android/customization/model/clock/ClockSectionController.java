/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.customization.model.clock;

import android.content.Context;
import android.view.LayoutInflater;

import androidx.annotation.Nullable;

import com.android.customization.picker.clock.ClockCustomFragment;
import com.android.customization.picker.clock.ClockSectionView;
import com.android.wallpaper.R;
import com.android.wallpaper.config.Flags;
import com.android.wallpaper.model.CustomizationSectionController;

/** A {@link CustomizationSectionController} for clock customization. */
public class ClockSectionController implements CustomizationSectionController<ClockSectionView> {

    private final CustomizationSectionNavigationController mNavigationController;

    public ClockSectionController(CustomizationSectionNavigationController navigationController) {
        mNavigationController = navigationController;
    }

    @Override
    public boolean isAvailable(@Nullable Context context) {
        return Flags.enableClockCustomization;
    }

    @Override
    public ClockSectionView createView(Context context) {
        ClockSectionView view = (ClockSectionView) LayoutInflater.from(context).inflate(
                R.layout.clock_section_view,
                null);
        view.setOnClickListener(v -> mNavigationController.navigateTo(new ClockCustomFragment()));
        return view;
    }
}
