/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.wallpaper.picker.di.modules

import android.content.Context
import com.android.customization.model.font.FontManager
import com.android.customization.model.grid.DefaultShapeGridManager
import com.android.customization.model.grid.ShapeGridManager
import com.android.customization.model.theme.OverlayManagerCompat
import com.android.customization.module.logging.AppSessionId
import com.android.customization.module.logging.AppSessionIdImpl
import com.android.customization.module.logging.SysUiStatsLoggerFactory
import com.android.customization.module.logging.SysUiStatsLoggerFactoryImpl
import com.android.customization.picker.mode.shared.util.DarkModeUtil
import com.android.customization.picker.mode.shared.util.DarkModeUtilImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ThemePickerSharedAppModule {

    @Binds @Singleton abstract fun bindAppSessionId(impl: AppSessionIdImpl): AppSessionId

    @Binds @Singleton abstract fun bindDarkModeUtil(impl: DarkModeUtilImpl): DarkModeUtil

    @Binds
    @Singleton
    abstract fun bindGridOptionsManager(impl: DefaultShapeGridManager): ShapeGridManager

    @Binds
    @Singleton
    abstract fun bindSysUiStatsLoggerFactory(
        impl: SysUiStatsLoggerFactoryImpl
    ): SysUiStatsLoggerFactory

    companion object {
        @Provides
        @Singleton
        fun provideFontManager(@ApplicationContext context: Context): FontManager {
            return FontManager.getInstance(context, OverlayManagerCompat(context))
        }
    }
}
