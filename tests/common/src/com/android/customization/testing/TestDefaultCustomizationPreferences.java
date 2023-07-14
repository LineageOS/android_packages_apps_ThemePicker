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

import android.content.Context;

import com.android.customization.module.DefaultCustomizationPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * Test implementation of {@link DefaultCustomizationPreferences}.
 */
public class TestDefaultCustomizationPreferences extends DefaultCustomizationPreferences {

    private String mCustomThemes;
    private final Set<String> mTabVisited = new HashSet<>();

    public TestDefaultCustomizationPreferences(Context context) {
        super(context);
    }

    @Override
    public String getSerializedCustomThemes() {
        return mCustomThemes;
    }

    @Override
    public void storeCustomThemes(String serializedCustomThemes) {
        mCustomThemes = serializedCustomThemes;
    }

    @Override
    public boolean getTabVisited(String id) {
        return mTabVisited.contains(id);
    }

    @Override
    public void setTabVisited(String id) {
        mTabVisited.add(id);
    }
}
