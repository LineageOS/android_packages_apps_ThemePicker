package com.android.customization.model.color

import android.app.WallpaperColors
import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import com.android.wallpaper.R
import com.android.customization.model.CustomizationManager
import com.android.customization.model.CustomizationManager.OptionsFetchedListener
import com.android.customization.picker.color.ColorSectionView
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.Executors

class ColorCustomizationManager(
    val mProvider: ColorOptionsProvider,
    private val mContentResolver: ContentResolver
) : CustomizationManager<ColorOption> {
    var mCurrentOverlays: Map<String, String>? = null
    private var mCurrentSource: String? = null
    var mHomeWallpaperColors: WallpaperColors? = null
    var mLockWallpaperColors: WallpaperColors? = null

    companion object {
        var COLOR_OVERLAY_SETTINGS: Set<String>? = null
        private var sColorCustomizationManager: ColorCustomizationManager? = null
        val sExecutorService = Executors.newSingleThreadExecutor()!!
        fun getInstance(
            context: Context
        ): ColorCustomizationManager? {
            if (sColorCustomizationManager == null) {
                val applicationContext = context.applicationContext
                sColorCustomizationManager = ColorCustomizationManager(
                    ColorProvider(
                        applicationContext, applicationContext.getString(
                            R.string.themes_stub_package
                        )
                    ), applicationContext.contentResolver
                )
            }
            return sColorCustomizationManager
        }

        init {
            val hashSet = HashSet<String>()
            COLOR_OVERLAY_SETTINGS = hashSet
            hashSet.add("android.theme.customization.system_palette")
            hashSet.add("android.theme.customization.accent_color")
            hashSet.add("android.theme.customization.color_source")
        }
    }

    init {
        mContentResolver.registerContentObserver(Settings.Secure.CONTENT_URI, true, object :
            ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                if (TextUtils.equals(
                        uri!!.lastPathSegment,
                        "theme_customization_overlay_packages"
                    )
                ) {
                    mCurrentOverlays = null
                }

            }
        })
    }

    val currentColorSource: String?
        get() {
            if (mCurrentSource == null) {
                parseSettings(storedOverlays)
            }
            return mCurrentSource
        }
    val storedOverlays: String?
         get() = Settings.Secure.getString(mContentResolver, "theme_customization_overlay_packages")

    fun parseSettings(str: String?) {
        val hashMap = HashMap<String, String>()
        if (str != null) {
            try {
                val jSONObject = JSONObject(str)
                val names = jSONObject.names()
                if (names != null) {
                    for (i in 0 until names.length()) {
                        val string = names.getString(i)
                        if ((COLOR_OVERLAY_SETTINGS as HashSet<String>?)!!.contains(string)) {
                            try {
                                hashMap[string] = jSONObject.getString(string)
                            } catch (e: JSONException) {
                                Log.e(
                                    "ColorCustomizationManager",
                                    "parseColorOverlays: " + e.localizedMessage,
                                    e
                                )
                            }
                        }
                    }
                }
            } catch (e2: JSONException) {
                Log.e("ColorCustomizationManager", e2.localizedMessage!!)
            }
        }
        mCurrentSource = hashMap.remove("android.theme.customization.color_source")
        mCurrentOverlays = hashMap
    }

    override fun isAvailable(): Boolean {
        return true
    }

    fun setThemeBundle(colorSectionController: ColorSectionController, option: ColorOption) {
        if (SystemClock.elapsedRealtime() - colorSectionController.mLastColorApplyingTime >= 500) {
            colorSectionController.mLastColorApplyingTime = SystemClock.elapsedRealtime()
            val callback: CustomizationManager.Callback = object : CustomizationManager.Callback {
                override fun onError(th2: Throwable?) {
                    Log.w("ColorSectionController", "Apply theme with error: null")
                }

                override fun onSuccess() {
                    val colorSectionView: ColorSectionView =
                        colorSectionController.mColorSectionView!!
                    colorSectionView.announceForAccessibility(
                        colorSectionView.context.getString(
                            R.string.color_changed
                        )
                    )
                    val wallpaperColors = colorSectionController.mLockWallpaperColors
                    var i3 = 0
                    val z2 =
                        wallpaperColors == null || wallpaperColors == colorSectionController.mHomeWallpaperColors
                    if (TextUtils.equals(option.source, "preset")) {
                        i3 = 26
                    } else if (z2) {
                        i3 = 25
                    } else {
                        val source = option.source
                        if (source == "lock_wallpaper") {
                            i3 = 24
                        } else if (source == "home_wallpaper") {
                            i3 = 23
                        }
                    }
                    colorSectionController.mEventLogger.logColorApplied(i3, option.mIndex)
                }
            }
            sExecutorService.submit {
                applyBundle(option, callback)
            }
            return
        }
    }

    private fun applyBundle(option: ColorOption, callback: CustomizationManager.Callback) {
        var mStoredOverlays = storedOverlays
        if (TextUtils.isEmpty(mStoredOverlays) || mStoredOverlays == null) {
            mStoredOverlays = "{}"
        }
        var z4: Boolean
        var jSONObject: JSONObject? = null
        try {
            jSONObject = JSONObject(mStoredOverlays)
            try {
                val jsonPackages: JSONObject = option.getJsonPackages(true)
                val it: Iterator<*> =
                    (COLOR_OVERLAY_SETTINGS as HashSet?)!!.iterator()
                while (it.hasNext()) {
                    jSONObject.remove(it.next() as String?)
                }
                val keys: Iterator<String> = jsonPackages.keys()
                while (keys.hasNext()) {
                    val next = keys.next()
                    jSONObject.put(next, jsonPackages.get(next))
                }
                jSONObject.put(
                    "android.theme.customization.color_source",
                    option.source
                )
                jSONObject.put(
                    "android.theme.customization.color_index",
                    option.mIndex.toString()
                )
                if ("preset" != option.source) {
                    val wallpaperColors = mLockWallpaperColors
                    if (wallpaperColors != null && wallpaperColors != mHomeWallpaperColors) {
                        z4 = false
                        jSONObject.put(
                            "android.theme.customization.color_both",
                            if (!z4) "1" else "0"
                        )
                    }
                    z4 = true
                    jSONObject.put(
                        "android.theme.customization.color_both",
                        if (!z4) "1" else "0"
                    )
                } else {
                    jSONObject.remove("android.theme.customization.color_both")
                }
            } catch (e2: JSONException) {
                e2.printStackTrace()
                Handler(Looper.getMainLooper()).post {
                    val success = Settings.Secure.putString(
                        mContentResolver,
                        "theme_customization_overlay_packages", jSONObject.toString()
                    )
                    if (success) callback.onSuccess()
                    else callback.onError(null)
                }
                return
            }
        } catch (e3: JSONException) {
            e3.printStackTrace()
        }
        Handler(Looper.getMainLooper()).post {
            val success = jSONObject != null && Settings.Secure.putString(
                mContentResolver,
                "theme_customization_overlay_packages", jSONObject.toString()
            )
            if (success) callback.onSuccess()
            else callback.onError(null)
        }
    }

    override fun apply(option: ColorOption, callback: CustomizationManager.Callback) {
        applyBundle(option, callback)
    }
    override fun fetchOptions(callback: OptionsFetchedListener<ColorOption>, reload: Boolean) {}
}
