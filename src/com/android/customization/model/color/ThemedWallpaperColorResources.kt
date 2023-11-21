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

package com.android.customization.model.color

import android.R
import android.app.WallpaperColors
import android.content.Context
import android.provider.Settings
import android.util.Log
import com.android.customization.model.ResourceConstants.OVERLAY_CATEGORY_THEME_STYLE
import com.android.systemui.monet.ColorScheme
import com.android.systemui.monet.Style
import org.json.JSONException
import org.json.JSONObject

class ThemedWallpaperColorResources(wallpaperColors: WallpaperColors, context: Context) :
    WallpaperColorResources(wallpaperColors) {

    init {
        val wallpaperColorScheme =
            ColorScheme(
                wallpaperColors = wallpaperColors,
                darkTheme = false,
                style = fetchThemeStyleFromSetting(context)
            )
        addOverlayColor(wallpaperColorScheme.neutral1, R.color.system_neutral1_10)
        addOverlayColor(wallpaperColorScheme.neutral2, R.color.system_neutral2_10)
        addOverlayColor(wallpaperColorScheme.accent1, R.color.system_accent1_10)
        addOverlayColor(wallpaperColorScheme.accent2, R.color.system_accent2_10)
        addOverlayColor(wallpaperColorScheme.accent3, R.color.system_accent3_10)
    }

    private fun fetchThemeStyleFromSetting(context: Context): Style {
        val overlayPackageJson =
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES,
            )
        return if (!overlayPackageJson.isNullOrEmpty()) {
            try {
                val jsonObject = JSONObject(overlayPackageJson)
                Style.valueOf(jsonObject.getString(OVERLAY_CATEGORY_THEME_STYLE))
            } catch (e: (JSONException)) {
                Log.i(TAG, "Failed to parse THEME_CUSTOMIZATION_OVERLAY_PACKAGES.", e)
                Style.TONAL_SPOT
            } catch (e: IllegalArgumentException) {
                Log.i(TAG, "Failed to parse THEME_CUSTOMIZATION_OVERLAY_PACKAGES.", e)
                Style.TONAL_SPOT
            }
        } else {
            Style.TONAL_SPOT
        }
    }

    companion object {
        private const val TAG = "ThemedWallpaperColorResources"
    }
}
