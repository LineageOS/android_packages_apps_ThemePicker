package com.android.customization.model.color

import android.text.TextUtils
import android.util.Log
import com.android.customization.model.CustomizationManager
import com.android.customization.model.CustomizationOption
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import java.util.stream.Collectors

abstract class ColorOption(
    val mTitle: String,
    map: Map<String?, String?>,
    val mIsDefault: Boolean,
    val mIndex: Int
) : CustomizationOption<ColorOption> {
    var mContentDescription: CharSequence? = null
    private val mPackagesByCategory: Map<String?, String?>

    fun getJsonPackages(z: Boolean): JSONObject {
        val jSONObject: JSONObject = if (mIsDefault) {
            JSONObject()
        } else {
            val jSONObject2 = JSONObject(mPackagesByCategory)
            val keys = jSONObject2.keys()
            val hashSet: HashSet<String> = HashSet<String>()
            while (keys.hasNext()) {
                val next = keys.next()
                if (jSONObject2.isNull(next)) {
                    hashSet.add(next)
                }
            }
            val it: Iterator<*> = hashSet.iterator()
            while (it.hasNext()) {
                jSONObject2.remove(it.next() as String?)
            }
            jSONObject2
        }
        if (z) {
            try {
                jSONObject.put(TIMESTAMP_FIELD, System.currentTimeMillis())
            } catch (unused: JSONException) {
                Log.e("ColorOption", "Couldn't add timestamp to serialized themebundle")
            }
        }
        return jSONObject
    }

    abstract val source: String

    override fun getTitle(): String {
        return mTitle
    }

    override fun isActive(customizationManager: CustomizationManager<ColorOption>): Boolean {
        val colorCustomizationManager = customizationManager as ColorCustomizationManager
        if (mIsDefault) {
            val storedOverlays = colorCustomizationManager.storedOverlays
            if (!TextUtils.isEmpty(storedOverlays) && "{}" != storedOverlays) {
                if (colorCustomizationManager.mCurrentOverlays == null) {
                    colorCustomizationManager.parseSettings(colorCustomizationManager.storedOverlays)
                }
                if (colorCustomizationManager.mCurrentOverlays!!.isNotEmpty() &&
                    (storedOverlays!!.contains("android.theme.customization.system_palette") ||
                     storedOverlays.contains("android.theme.customization.accent_color"))
                ) {
                    return false
                }
            }
            return true
        }
        if (colorCustomizationManager.mCurrentOverlays == null) {
            colorCustomizationManager.parseSettings(colorCustomizationManager.storedOverlays)
        }
        val map = colorCustomizationManager.mCurrentOverlays
        val currentColorSource = colorCustomizationManager.currentColorSource
        return (TextUtils.isEmpty(currentColorSource) || source == currentColorSource) && mPackagesByCategory == map
    }

    companion object {
        const val TIMESTAMP_FIELD = "_applied_timestamp"
    }

    init {
        mPackagesByCategory = Collections.unmodifiableMap(
            map.entries.stream().filter { t -> t.value != null }
                .collect(
                    Collectors.toMap(
                        { t -> t.key },
                        { t -> t.value }
                    )
                ) as Map<String?, String?>
        )

    }
}
