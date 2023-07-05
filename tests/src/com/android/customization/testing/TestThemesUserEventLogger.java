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
package com.android.customization.testing;

import com.android.customization.model.color.ColorOption;
import com.android.customization.model.grid.GridOption;
import com.android.customization.model.theme.ThemeBundle;
import com.android.customization.module.ThemesUserEventLogger;
import com.android.wallpaper.testing.TestUserEventLogger;

/**
 * Test implementation of {@link ThemesUserEventLogger}.
 */
public class TestThemesUserEventLogger extends TestUserEventLogger
        implements ThemesUserEventLogger {
    @Override
    public void logThemeSelected(ThemeBundle theme, boolean isCustomTheme) {
        // Do nothing.
    }

    @Override
    public void logThemeApplied(ThemeBundle theme, boolean isCustomTheme) {
        // Do nothing.
    }

    @Override
    public void logColorApplied(int action, ColorOption colorOption) {
        // Do nothing.
    }

    @Override
    public void logGridSelected(GridOption grid) {
        // Do nothing.
    }

    @Override
    public void logGridApplied(GridOption grid) {
        // Do nothing.
    }
}
