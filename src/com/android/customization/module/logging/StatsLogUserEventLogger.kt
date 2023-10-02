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
import com.android.wallpaper.module.WallpaperPersister.DEST_BOTH
import com.android.wallpaper.module.WallpaperPersister.DEST_HOME_SCREEN
import com.android.wallpaper.module.WallpaperPersister.DEST_LOCK_SCREEN
import com.android.wallpaper.module.WallpaperPreferences
import com.android.wallpaper.module.logging.NoOpUserEventLogger
import com.android.wallpaper.module.logging.UserEventLogger.EffectStatus
import com.android.wallpaper.module.logging.UserEventLogger.SetWallpaperEntryPoint
import com.android.wallpaper.module.logging.UserEventLogger.WallpaperDestination
import com.android.wallpaper.util.LaunchSourceUtils

/** StatsLog-backed implementation of [ThemesUserEventLogger]. */
class StatsLogUserEventLogger(private val preferences: WallpaperPreferences) :
    NoOpUserEventLogger(), ThemesUserEventLogger {

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
        SysUiStatsLogger(StyleEnums.SNAPSHOT)
            .setWallpaperCategoryHash(preferences.getHomeCategoryHash())
            .setWallpaperIdHash(preferences.getHomeWallpaperIdHash())
            .setLockWallpaperCategoryHash(preferences.getLockCategoryHash())
            .setLockWallpaperIdHash(preferences.getLockWallpaperIdHash())
            .setEffectIdHash(getIdHashCode(preferences.homeWallpaperEffects))
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

    /** If not set, the output hash is 0. */
    private fun WallpaperPreferences.getHomeCategoryHash(): Int {
        return getIdHashCode(homeWallpaperCollectionId)
    }

    /** If not set, the output hash is 0. */
    private fun WallpaperPreferences.getHomeWallpaperIdHash(): Int {
        val remoteId = homeWallpaperRemoteId
        val wallpaperId = if (!TextUtils.isEmpty(remoteId)) remoteId else homeWallpaperServiceName
        return getIdHashCode(wallpaperId)
    }

    /** If not set, the output hash is 0. */
    private fun WallpaperPreferences.getLockCategoryHash(): Int {
        return getIdHashCode(lockWallpaperCollectionId)
    }

    /** If not set, the output hash is 0. */
    private fun WallpaperPreferences.getLockWallpaperIdHash(): Int {
        val remoteId = lockWallpaperRemoteId
        val wallpaperId = if (!TextUtils.isEmpty(remoteId)) remoteId else lockWallpaperServiceName
        return getIdHashCode(wallpaperId)
    }

    private fun getIdHashCode(id: String?): Int {
        return id?.hashCode() ?: 0
    }
}
