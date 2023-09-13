package com.android.customization

import com.android.customization.module.CustomizationInjector
import com.android.customization.module.CustomizationPreferences
import com.android.customization.testing.TestCustomizationInjector
import com.android.customization.testing.TestDefaultCustomizationPreferences
import com.android.wallpaper.module.AppModule
import com.android.wallpaper.module.Injector
import com.android.wallpaper.module.WallpaperPreferences
import com.android.wallpaper.testing.TestInjector
import com.android.wallpaper.testing.TestWallpaperPreferences
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [AppModule::class])
abstract class TestModule {
    //// WallpaperPicker2 prod

    @Binds @Singleton abstract fun bindInjector(impl: TestCustomizationInjector): Injector

    @Binds
    @Singleton
    abstract fun bindWallpaperPrefs(impl: TestDefaultCustomizationPreferences): WallpaperPreferences

    //// WallpaperPicker2 test

    @Binds @Singleton abstract fun bindTestInjector(impl: TestCustomizationInjector): TestInjector

    @Binds
    @Singleton
    abstract fun bindTestWallpaperPrefs(
        impl: TestDefaultCustomizationPreferences
    ): TestWallpaperPreferences

    //// ThemePicker prod

    @Binds
    @Singleton
    abstract fun bindCustomizationInjector(impl: TestCustomizationInjector): CustomizationInjector

    @Binds
    @Singleton
    abstract fun bindCustomizationPrefs(
        impl: TestDefaultCustomizationPreferences
    ): CustomizationPreferences
}
