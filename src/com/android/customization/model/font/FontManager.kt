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
import android.os.UserHandle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.android.customization.model.CustomizationManager
import com.android.customization.model.CustomizationManager.Callback
import com.android.customization.model.CustomizationManager.OptionsFetchedListener
import com.android.customization.model.ResourceConstants.ANDROID_PACKAGE
import com.android.customization.model.ResourceConstants.OVERLAY_CATEGORY_FONT
import com.android.customization.model.theme.OverlayManagerCompat
import org.json.JSONException
import org.json.JSONObject

class FontManager
internal constructor(
    private val mContext: Context,
    val overlayManager: OverlayManagerCompat,
    private val mProvider: FontOptionProvider,
) : CustomizationManager<FontOption> {

    private var mActiveOption: FontOption? = null

    override fun isAvailable(): Boolean {
        return overlayManager.isAvailable
    }

    override fun apply(option: FontOption, callback: Callback?) {
        if (!persistOverlay(option)) {
            Toast.makeText(
                    mContext,
                    "Failed to apply font, reboot to try again.",
                    Toast.LENGTH_SHORT,
                )
                .show()
            callback?.onError(null)
            return
        }

        val packageName = option.packageName
        if (packageName == null) {
            if (mActiveOption?.packageName == null) return

            val overlays =
                overlayManager.getOverlayPackagesForCategory(
                    OVERLAY_CATEGORY_FONT,
                    UserHandle.myUserId(),
                    ANDROID_PACKAGE,
                )
            for (overlay in overlays) {
                overlayManager.disableOverlay(overlay, UserHandle.myUserId())
            }
        } else {
            overlayManager.setEnabledExclusiveInCategory(packageName, UserHandle.myUserId())
        }

        callback?.onSuccess()
        mActiveOption = option
    }

    override fun fetchOptions(callback: OptionsFetchedListener<FontOption>, reload: Boolean) {
        val options = mProvider.getOptions(reload)
        for (option in options) {
            if (isActive(option)) {
                mActiveOption = option
                break
            }
        }
        callback.onOptionsLoaded(options)
    }

    fun isActive(option: FontOption): Boolean {
        val enabledPkg =
            overlayManager.getEnabledPackageName(ANDROID_PACKAGE, OVERLAY_CATEGORY_FONT)
        return if (enabledPkg != null) {
            enabledPkg == option.packageName
        } else {
            option.packageName == null
        }
    }

    private fun persistOverlay(toPersist: FontOption): Boolean {
        val value =
            Settings.Secure.getStringForUser(
                mContext.contentResolver,
                Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES,
                UserHandle.myUserId(),
            )

        val json: JSONObject =
            try {
                if (value == null) JSONObject() else JSONObject(value)
            } catch (e: JSONException) {
                Log.e(TAG, "Error parsing current settings value:\n${e.message}")
                return false
            }

        // removing all currently enabled overlays from the json
        json.remove(OVERLAY_CATEGORY_FONT)

        // adding the new ones
        try {
            json.put(OVERLAY_CATEGORY_FONT, toPersist.packageName)
        } catch (e: JSONException) {
            Log.e(TAG, "Error adding new settings value:\n${e.message}")
            return false
        }

        // updating the setting
        return Settings.Secure.putStringForUser(
            mContext.contentResolver,
            Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES,
            json.toString(),
            UserHandle.myUserId(),
        )
    }

    companion object {
        private const val TAG = "FontManager"
        private const val KEY_STATE_CURRENT_SELECTION = "FontManager.currentSelection"

        @Volatile private var sFontOptionManager: FontManager? = null

        @JvmStatic
        fun getInstance(context: Context, overlayManager: OverlayManagerCompat): FontManager {
            return sFontOptionManager
                ?: synchronized(this) {
                    sFontOptionManager
                        ?: FontManager(
                                context,
                                overlayManager,
                                FontOptionProvider(context.applicationContext, overlayManager),
                            )
                            .also { sFontOptionManager = it }
                }
        }
    }
}
