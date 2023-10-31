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
package com.android.customization.module.logging

import android.app.WallpaperManager
import android.content.Intent
import android.stats.style.StyleEnums
import android.text.TextUtils
import com.android.customization.model.grid.GridOption
import com.android.customization.module.logging.ThemesUserEventLogger.ClockSize
import com.android.customization.module.logging.ThemesUserEventLogger.ColorSource
import com.android.systemui.shared.system.SysUiStatsLog
import com.android.wallpaper.module.WallpaperPersister.DEST_BOTH
import com.android.wallpaper.module.WallpaperPersister.DEST_HOME_SCREEN
import com.android.wallpaper.module.WallpaperPersister.DEST_LOCK_SCREEN
import com.android.wallpaper.module.WallpaperPreferences
import com.android.wallpaper.module.logging.UserEventLogger.EffectStatus
import com.android.wallpaper.module.logging.UserEventLogger.SetWallpaperEntryPoint
import com.android.wallpaper.module.logging.UserEventLogger.WallpaperDestination
import com.android.wallpaper.util.LaunchSourceUtils
import javax.inject.Inject
import javax.inject.Singleton

/** StatsLog-backed implementation of [ThemesUserEventLogger]. */
@Singleton
class ThemesUserEventLoggerImpl
@Inject
constructor(
    private val preferences: WallpaperPreferences,
    private val appSessionId: AppSessionId,
) : ThemesUserEventLogger {

    override fun logSnapshot() {
        SysUiStatsLogger(StyleEnums.SNAPSHOT)
            .setWallpaperCategoryHash(preferences.getHomeCategoryHash())
            .setWallpaperIdHash(preferences.getHomeWallpaperIdHash())
            .setLockWallpaperCategoryHash(preferences.getLockCategoryHash())
            .setLockWallpaperIdHash(preferences.getLockWallpaperIdHash())
            .setEffectIdHash(preferences.getHomeWallpaperEffectsIdHash())
            .log()
    }

    override fun logAppLaunched(launchSource: Intent) {
        SysUiStatsLogger(StyleEnums.APP_LAUNCHED)
            .setAppSessionId(appSessionId.createNewId().getId())
            .setLaunchedPreference(launchSource.getAppLaunchSource())
            .log()
    }

    override fun logWallpaperApplied(
        collectionId: String?,
        wallpaperId: String?,
        effects: String?,
        @SetWallpaperEntryPoint setWallpaperEntryPoint: Int,
        @WallpaperDestination destination: Int,
    ) {
        val categoryHash = getIdHashCode(collectionId)
        val wallpaperIdHash = getIdHashCode(wallpaperId)
        val isHomeWallpaperSet = destination == DEST_HOME_SCREEN || destination == DEST_BOTH
        val isLockWallpaperSet = destination == DEST_LOCK_SCREEN || destination == DEST_BOTH
        SysUiStatsLogger(StyleEnums.WALLPAPER_APPLIED)
            .setAppSessionId(appSessionId.getId())
            .setWallpaperCategoryHash(if (isHomeWallpaperSet) categoryHash else 0)
            .setWallpaperIdHash(if (isHomeWallpaperSet) wallpaperIdHash else 0)
            .setLockWallpaperCategoryHash(if (isLockWallpaperSet) categoryHash else 0)
            .setLockWallpaperIdHash(if (isLockWallpaperSet) wallpaperIdHash else 0)
            .setEffectIdHash(getIdHashCode(effects))
            .setSetWallpaperEntryPoint(setWallpaperEntryPoint)
            .setWallpaperDestination(destination)
            .log()
    }

    override fun logEffectApply(
        effect: String,
        @EffectStatus status: Int,
        timeElapsedMillis: Long,
        resultCode: Int
    ) {
        SysUiStatsLogger(StyleEnums.WALLPAPER_EFFECT_APPLIED)
            .setAppSessionId(appSessionId.getId())
            .setEffectPreference(status)
            .setEffectIdHash(getIdHashCode(effect))
            .setTimeElapsed(timeElapsedMillis)
            .setEffectResultCode(resultCode)
            .log()
    }

    override fun logEffectProbe(effect: String, @EffectStatus status: Int) {
        SysUiStatsLogger(StyleEnums.WALLPAPER_EFFECT_PROBE)
            .setAppSessionId(appSessionId.getId())
            .setEffectPreference(status)
            .setEffectIdHash(getIdHashCode(effect))
            .log()
    }

    override fun logEffectForegroundDownload(
        effect: String,
        @EffectStatus status: Int,
        timeElapsedMillis: Long
    ) {
        SysUiStatsLogger(StyleEnums.WALLPAPER_EFFECT_FG_DOWNLOAD)
            .setAppSessionId(appSessionId.getId())
            .setEffectPreference(status)
            .setEffectIdHash(getIdHashCode(effect))
            .setTimeElapsed(timeElapsedMillis)
            .log()
    }

    override fun logResetApplied() {
        SysUiStatsLogger(StyleEnums.RESET_APPLIED).setAppSessionId(appSessionId.getId()).log()
    }

    override fun logWallpaperExploreButtonClicked() {
        SysUiStatsLogger(StyleEnums.WALLPAPER_EXPLORE).setAppSessionId(appSessionId.getId()).log()
    }

    override fun logThemeColorApplied(
        @ColorSource source: Int,
        variant: Int,
        seedColor: Int,
    ) {
        SysUiStatsLogger(StyleEnums.THEME_COLOR_APPLIED)
            .setAppSessionId(appSessionId.getId())
            .setColorSource(source)
            .setColorVariant(variant)
            .setSeedColor(seedColor)
            .log()
    }

    override fun logGridApplied(grid: GridOption) {
        SysUiStatsLogger(StyleEnums.GRID_APPLIED)
            .setAppSessionId(appSessionId.getId())
            .setLauncherGrid(grid.getLauncherGridInt())
            .log()
    }

    override fun logClockApplied(clockId: String) {
        SysUiStatsLogger(StyleEnums.CLOCK_APPLIED)
            .setAppSessionId(appSessionId.getId())
            .setClockPackageHash(getIdHashCode(clockId))
            .log()
    }

    override fun logClockColorApplied(seedColor: Int) {
        SysUiStatsLogger(StyleEnums.CLOCK_COLOR_APPLIED)
            .setAppSessionId(appSessionId.getId())
            .setSeedColor(seedColor)
            .log()
    }

    override fun logClockSizeApplied(@ClockSize clockSize: Int) {
        SysUiStatsLogger(StyleEnums.CLOCK_SIZE_APPLIED)
            .setAppSessionId(appSessionId.getId())
            .setClockSize(clockSize)
            .log()
    }

    override fun logThemedIconApplied(useThemeIcon: Boolean) {
        SysUiStatsLogger(StyleEnums.THEMED_ICON_APPLIED)
            .setAppSessionId(appSessionId.getId())
            .setToggleOn(useThemeIcon)
            .log()
    }

    override fun logLockScreenNotificationApplied(showLockScreenNotifications: Boolean) {
        SysUiStatsLogger(StyleEnums.LOCK_SCREEN_NOTIFICATION_APPLIED)
            .setAppSessionId(appSessionId.getId())
            .setToggleOn(showLockScreenNotifications)
            .log()
    }

    override fun logShortcutApplied(shortcut: String, shortcutSlotId: String) {
        SysUiStatsLogger(StyleEnums.SHORTCUT_APPLIED)
            .setAppSessionId(appSessionId.getId())
            .setShortcut(shortcut)
            .setShortcutSlotId(shortcutSlotId)
            .log()
    }

    override fun logDarkThemeApplied(useDarkTheme: Boolean) {
        SysUiStatsLogger(StyleEnums.DARK_THEME_APPLIED)
            .setAppSessionId(appSessionId.getId())
            .setToggleOn(useDarkTheme)
            .log()
    }

    /**
     * The grid integer depends on the column and row numbers. For example: 4x5 is 405 13x37 is 1337
     * The upper limit for the column / row count is 99.
     */
    private fun GridOption.getLauncherGridInt(): Int {
        return cols * 100 + rows
    }

    private fun Intent.getAppLaunchSource(): Int {
        return if (hasExtra(LaunchSourceUtils.WALLPAPER_LAUNCH_SOURCE)) {
            when (getStringExtra(LaunchSourceUtils.WALLPAPER_LAUNCH_SOURCE)) {
                LaunchSourceUtils.LAUNCH_SOURCE_LAUNCHER ->
                    SysUiStatsLog.STYLE_UICHANGED__LAUNCHED_PREFERENCE__LAUNCHED_LAUNCHER
                LaunchSourceUtils.LAUNCH_SOURCE_SETTINGS ->
                    SysUiStatsLog.STYLE_UICHANGED__LAUNCHED_PREFERENCE__LAUNCHED_SETTINGS
                LaunchSourceUtils.LAUNCH_SOURCE_SUW ->
                    SysUiStatsLog.STYLE_UICHANGED__LAUNCHED_PREFERENCE__LAUNCHED_SUW
                LaunchSourceUtils.LAUNCH_SOURCE_TIPS ->
                    SysUiStatsLog.STYLE_UICHANGED__LAUNCHED_PREFERENCE__LAUNCHED_TIPS
                LaunchSourceUtils.LAUNCH_SOURCE_DEEP_LINK ->
                    SysUiStatsLog.STYLE_UICHANGED__LAUNCHED_PREFERENCE__LAUNCHED_DEEP_LINK
                else ->
                    SysUiStatsLog
                        .STYLE_UICHANGED__LAUNCHED_PREFERENCE__LAUNCHED_PREFERENCE_UNSPECIFIED
            }
        } else if (hasExtra(LaunchSourceUtils.LAUNCH_SETTINGS_SEARCH)) {
            SysUiStatsLog.STYLE_UICHANGED__LAUNCHED_PREFERENCE__LAUNCHED_SETTINGS_SEARCH
        } else if (action != null && action == WallpaperManager.ACTION_CROP_AND_SET_WALLPAPER) {
            SysUiStatsLog.STYLE_UICHANGED__LAUNCHED_PREFERENCE__LAUNCHED_CROP_AND_SET_ACTION
        } else if (categories != null && categories.contains(Intent.CATEGORY_LAUNCHER)) {
            SysUiStatsLog.STYLE_UICHANGED__LAUNCHED_PREFERENCE__LAUNCHED_LAUNCH_ICON
        } else {
            SysUiStatsLog.STYLE_UICHANGED__LAUNCHED_PREFERENCE__LAUNCHED_PREFERENCE_UNSPECIFIED
        }
    }

    /** If not set, the output hash is 0. */
    private fun WallpaperPreferences.getHomeCategoryHash(): Int {
        return getIdHashCode(getHomeWallpaperCollectionId())
    }

    /** If not set, the output hash is 0. */
    private fun WallpaperPreferences.getHomeWallpaperIdHash(): Int {
        val remoteId = getHomeWallpaperRemoteId()
        val wallpaperId =
            if (!TextUtils.isEmpty(remoteId)) remoteId else getHomeWallpaperServiceName()
        return getIdHashCode(wallpaperId)
    }

    /** If not set, the output hash is 0. */
    private fun WallpaperPreferences.getLockCategoryHash(): Int {
        return getIdHashCode(getLockWallpaperCollectionId())
    }

    /** If not set, the output hash is 0. */
    private fun WallpaperPreferences.getLockWallpaperIdHash(): Int {
        val remoteId = getLockWallpaperRemoteId()
        val wallpaperId =
            if (!TextUtils.isEmpty(remoteId)) remoteId else getLockWallpaperServiceName()
        return getIdHashCode(wallpaperId)
    }

    /** If not set, the output hash is 0. */
    private fun WallpaperPreferences.getHomeWallpaperEffectsIdHash(): Int {
        return getIdHashCode(getHomeWallpaperEffects())
    }

    private fun getIdHashCode(id: String?): Int {
        return id?.hashCode() ?: 0
    }
}
