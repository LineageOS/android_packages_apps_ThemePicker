/*
 * Copyright (C) The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.customization.model.font

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.content.res.Resources
import android.content.res.Resources.NotFoundException
import android.graphics.Typeface
import android.os.UserHandle
import android.util.Log
import com.android.customization.model.ResourceConstants
import com.android.customization.model.ResourceConstants.ANDROID_PACKAGE
import com.android.customization.model.ResourceConstants.CONFIG_BODY_FONT_FAMILY
import com.android.customization.model.ResourceConstants.CONFIG_HEADLINE_FONT_FAMILY
import com.android.customization.model.ResourceConstants.OVERLAY_CATEGORY_FONT
import com.android.customization.model.theme.OverlayManagerCompat
import com.android.themepicker.R

class FontOptionProvider(private val mContext: Context, manager: OverlayManagerCompat) {
    private val mPm: PackageManager = mContext.packageManager
    private val mOverlayPackages: List<String>
    private val mOptions = mutableListOf<FontOption>()
    private val mActiveOverlay: String?

    init {
        mOverlayPackages =
            manager.getOverlayPackagesForCategory(
                OVERLAY_CATEGORY_FONT,
                UserHandle.myUserId(),
                *ResourceConstants.getPackagesToOverlay(mContext),
            )
        mActiveOverlay = manager.getEnabledPackageName(ANDROID_PACKAGE, OVERLAY_CATEGORY_FONT)
    }

    fun getOptions(reload: Boolean): List<FontOption> {
        if (reload) mOptions.clear()
        if (mOptions.isEmpty()) loadOptions()
        return mOptions
    }

    private fun loadOptions() {
        addDefault()
        for (overlayPackage in mOverlayPackages) {
            try {
                val overlayRes = mPm.getResourcesForApplication(overlayPackage)
                val headlineFont =
                    Typeface.create(
                        getFontFamily(overlayPackage, overlayRes, CONFIG_HEADLINE_FONT_FAMILY),
                        Typeface.NORMAL,
                    )
                val bodyFont =
                    Typeface.create(
                        getFontFamily(overlayPackage, overlayRes, CONFIG_BODY_FONT_FAMILY),
                        Typeface.NORMAL,
                    )
                val label = mPm.getApplicationInfo(overlayPackage, 0).loadLabel(mPm).toString()
                mOptions.add(FontOption(overlayPackage, label, headlineFont, bodyFont))
            } catch (e: Exception) {
                when (e) {
                    is NameNotFoundException,
                    is NotFoundException -> {
                        Log.w(TAG, "Couldn't load font overlay $overlayPackage, will skip it", e)
                    }
                    else -> throw e
                }
            }
        }
    }

    private fun addDefault() {
        val system = Resources.getSystem()
        val headlineFont =
            Typeface.create(
                system.getString(
                    system.getIdentifier(CONFIG_HEADLINE_FONT_FAMILY, "string", ANDROID_PACKAGE)
                ),
                Typeface.NORMAL,
            )
        val bodyFont =
            Typeface.create(
                system.getString(
                    system.getIdentifier(CONFIG_BODY_FONT_FAMILY, "string", ANDROID_PACKAGE)
                ),
                Typeface.NORMAL,
            )
        mOptions.add(
            FontOption(
                null,
                mContext.getString(R.string.default_theme_title),
                headlineFont,
                bodyFont,
            )
        )
    }

    private fun getFontFamily(
        overlayPackage: String,
        overlayRes: Resources,
        configName: String,
    ): String {
        return overlayRes.getString(overlayRes.getIdentifier(configName, "string", overlayPackage))
    }

    companion object {
        private const val TAG = "FontOptionProvider"
    }
}
