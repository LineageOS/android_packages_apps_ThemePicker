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
import com.android.customization.model.color.ColorOption
import com.android.customization.model.grid.GridOption
import com.android.customization.module.SysUiStatsLogger
import com.android.systemui.shared.system.SysUiStatsLog
import com.android.wallpaper.module.WallpaperPreferences
import com.android.wallpaper.module.WallpaperStatusChecker
import com.android.wallpaper.module.logging.NoOpUserEventLogger
import com.android.wallpaper.module.logging.UserEventLogger.Companion.EffectStatus
import com.android.wallpaper.util.LaunchSourceUtils

/** StatsLog-backed implementation of [ThemesUserEventLogger]. */
class StatsLogUserEventLogger(
    private val preferences: WallpaperPreferences,
    private val wallpaperStatusChecker: WallpaperStatusChecker
) : NoOpUserEventLogger(), ThemesUserEventLogger {

    override fun logAppLaunched(launchSource: Intent) {
        SysUiStatsLogger(SysUiStatsLog.STYLE_UICHANGED__ACTION__APP_LAUNCHED)
            .setLaunchedPreference(getAppLaunchSource(launchSource))
            .log()
    }

    override fun logActionClicked(collectionId: String, actionLabelResId: Int) {
        SysUiStatsLogger(StyleEnums.WALLPAPER_EXPLORE)
            .setWallpaperCategoryHash(getIdHashCode(collectionId))
            .log()
    }

    override fun logSnapshot() {
        val isLockWallpaperSet = wallpaperStatusChecker.isLockWallpaperSet()
        val homeCollectionId = preferences.homeWallpaperCollectionId
        val homeRemoteId = preferences.homeWallpaperRemoteId
        val effects = preferences.homeWallpaperEffects
        val homeWallpaperId =
            if (TextUtils.isEmpty(homeRemoteId)) preferences.homeWallpaperServiceName
            else homeRemoteId
        val lockCollectionId =
            if (isLockWallpaperSet) preferences.lockWallpaperCollectionId else homeCollectionId
        val lockWallpaperId =
            if (isLockWallpaperSet) preferences.lockWallpaperRemoteId else homeWallpaperId
        SysUiStatsLogger(StyleEnums.SNAPSHOT)
            .setWallpaperCategoryHash(getIdHashCode(homeCollectionId))
            .setWallpaperIdHash(getIdHashCode(homeWallpaperId))
            .setLockWallpaperCategoryHash(getIdHashCode(lockCollectionId))
            .setLockWallpaperIdHash(getIdHashCode(lockWallpaperId))
            .setFirstLaunchDateSinceSetup(preferences.firstLaunchDateSinceSetup)
            .setFirstWallpaperApplyDateSinceSetup(preferences.firstWallpaperApplyDateSinceSetup)
            .setAppLaunchCount(preferences.appLaunchCount)
            .setEffectIdHash(getIdHashCode(effects))
            .log()
    }

    override fun logWallpaperApplied(
        collectionId: String?,
        wallpaperId: String?,
        effects: String?,
    ) {
        SysUiStatsLogger(StyleEnums.WALLPAPER_APPLIED)
            .setWallpaperCategoryHash(getIdHashCode(collectionId))
            .setWallpaperIdHash(getIdHashCode(wallpaperId))
            .setEffectIdHash(getIdHashCode(effects))
            .log()
    }

    override fun logEffectApply(
        effect: String,
        @EffectStatus status: Int,
        timeElapsedMillis: Long,
        resultCode: Int
    ) {
        SysUiStatsLogger(StyleEnums.WALLPAPER_EFFECT_APPLIED)
            .setEffectPreference(status)
            .setEffectIdHash(getIdHashCode(effect))
            .setTimeElapsed(timeElapsedMillis)
            .setEffectResultCode(resultCode)
            .log()
    }

    override fun logEffectProbe(effect: String, @EffectStatus status: Int) {
        SysUiStatsLogger(StyleEnums.WALLPAPER_EFFECT_PROBE)
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
            .setEffectPreference(status)
            .setEffectIdHash(getIdHashCode(effect))
            .setTimeElapsed(timeElapsedMillis)
            .log()
    }

    override fun logColorApplied(action: Int, colorOption: ColorOption) {
        SysUiStatsLogger(action)
            .setColorPreference(colorOption.index)
            .setColorVariant(colorOption.style.ordinal + 1)
            .log()
    }

    override fun logGridApplied(grid: GridOption) {
        SysUiStatsLogger(StyleEnums.PICKER_APPLIED).setLauncherGrid(grid.cols).log()
    }

    private fun getAppLaunchSource(launchSource: Intent): Int {
        return if (launchSource.hasExtra(LaunchSourceUtils.WALLPAPER_LAUNCH_SOURCE)) {
            when (launchSource.getStringExtra(LaunchSourceUtils.WALLPAPER_LAUNCH_SOURCE)) {
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
        } else if (launchSource.hasExtra(LaunchSourceUtils.LAUNCH_SETTINGS_SEARCH)) {
            SysUiStatsLog.STYLE_UICHANGED__LAUNCHED_PREFERENCE__LAUNCHED_SETTINGS_SEARCH
        } else if (
            launchSource.action != null &&
                launchSource.action == WallpaperManager.ACTION_CROP_AND_SET_WALLPAPER
        ) {
            SysUiStatsLog.STYLE_UICHANGED__LAUNCHED_PREFERENCE__LAUNCHED_CROP_AND_SET_ACTION
        } else if (
            launchSource.categories != null &&
                launchSource.categories.contains(Intent.CATEGORY_LAUNCHER)
        ) {
            SysUiStatsLog.STYLE_UICHANGED__LAUNCHED_PREFERENCE__LAUNCHED_LAUNCH_ICON
        } else {
            SysUiStatsLog.STYLE_UICHANGED__LAUNCHED_PREFERENCE__LAUNCHED_PREFERENCE_UNSPECIFIED
        }
    }

    private fun getIdHashCode(id: String?): Int {
        return id?.hashCode() ?: 0
    }
}
