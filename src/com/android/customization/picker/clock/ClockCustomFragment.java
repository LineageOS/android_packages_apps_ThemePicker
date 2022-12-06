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
package com.android.customization.picker.clock;

import static com.android.wallpaper.widget.BottomActionBar.BottomAction.APPLY;
import static com.android.wallpaper.widget.BottomActionBar.BottomAction.INFORMATION;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.android.customization.model.clock.custom.ClockCustomManager;
import com.android.customization.model.clock.custom.ClockOption;
import com.android.customization.widget.OptionSelectorController;
import com.android.wallpaper.R;
import com.android.wallpaper.picker.AppbarFragment;
import com.android.wallpaper.widget.BottomActionBar;

import com.google.common.collect.Lists;

/**
 * Fragment that contains the main UI for selecting and applying a custom clock.
 */
public class ClockCustomFragment extends AppbarFragment {

    OptionSelectorController<ClockOption> mClockSelectorController;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_clock_custom_picker, container, false);

        setUpToolbar(view);

        RecyclerView clockPreviewCardList = view.requireViewById(R.id.clock_preview_card_list);

        mClockSelectorController = new OptionSelectorController<>(clockPreviewCardList,
                Lists.newArrayList(new ClockOption(), new ClockOption(), new ClockOption(),
                        new ClockOption(), new ClockOption()), false,
                OptionSelectorController.CheckmarkStyle.CENTER_CHANGE_COLOR_WHEN_NOT_SELECTED);
        mClockSelectorController.initOptions(new ClockCustomManager());

        return view;
    }

    @Override
    public CharSequence getDefaultTitle() {
        return getString(R.string.clock_title);
    }

    @Override
    protected void onBottomActionBarReady(BottomActionBar bottomActionBar) {
        super.onBottomActionBarReady(bottomActionBar);
        bottomActionBar.showActionsOnly(INFORMATION, APPLY);
        bottomActionBar.show();
    }
}
