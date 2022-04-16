package com.android.customization.model.color

import android.app.WallpaperColors
import android.content.Context
import android.content.res.ColorStateList
import android.util.Log
import androidx.core.graphics.ColorUtils
import com.android.customization.model.ResourcesApkProvider
import com.android.wallpaper.compat.WallpaperManagerCompat
import com.android.wallpaper.module.InjectorProvider
import com.android.systemui.monet.ColorScheme
import com.android.customization.model.color.ColorUtils.toColorString
import kotlinx.coroutines.CoroutineScope
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class ColorProvider(context: Context, stubPackageName: String) :
    ResourcesApkProvider(context, stubPackageName), ColorOptionsProvider {
    var colorBundles: List<ColorOption>? = null
    var homeWallpaperColors: WallpaperColors? = null
    var lockWallpaperColors: WallpaperColors? = null
    val scope: CoroutineScope? = null
    private fun buildBundle(
        seed: Int,
        index: Int,
        mIsDefault: Boolean,
        source: String?,
        list: ArrayList<ColorOption>
    ) {

        val hashMap: HashMap<String?, String?> = HashMap()
        val colorScheme = ColorScheme(seed, false)
        val colorSchemeDark = ColorScheme(seed, true)
        val secondaryColorLight = intArrayOf(
            ColorUtils.setAlphaComponent(colorScheme.accent1[2], 255), ColorUtils.setAlphaComponent(
                colorScheme.accent1[2], 255
            ), ColorStateList.valueOf(
                colorScheme.accent3[6]
            ).withLStar(85.0f).colors[0], ColorUtils.setAlphaComponent(
                colorScheme.accent1[6], 255
            )
        )
        val secondaryColorDark = intArrayOf(
            ColorUtils.setAlphaComponent(colorSchemeDark.accent1[2], 255),
            ColorUtils.setAlphaComponent(
                colorSchemeDark.accent1[2], 255
            ),
            ColorStateList.valueOf(
                colorSchemeDark.accent3[6]
            ).withLStar(85.0f).colors[0],
            ColorUtils.setAlphaComponent(
                colorSchemeDark.accent1[6], 255
            )
        )
        var source3 = ""
        val source2 = if (mIsDefault) {
            source3
        } else {
            toColorString(seed)
        }
        hashMap["android.theme.customization.system_palette"] = source2
        if (!mIsDefault) {
            source3 = toColorString(seed)
        }
        hashMap["android.theme.customization.accent_color"] = source3
        list.add(
            ColorSeedOption(
                source,
                hashMap,
                mIsDefault,
                source!!,
                1 + index,
                ColorSeedOption.PreviewInfo(secondaryColorLight, secondaryColorDark)
            )
        )
    }

    private fun loadPreset() {
        val bundlesList = ArrayList<String>()
        val bundleNames = mStubApkResources.getStringArray(
            mStubApkResources.getIdentifier(
                "color_bundles",
                "array",
                mStubPackageName
            )
        )
        for (i in bundleNames.indices) {
            if (i == 4) break
            bundlesList.add(bundleNames[i])
        }
        val colorPresetBundles = ArrayList<ColorOption>()
        var position = 1;
        for (bundle in bundlesList) {
            val hashMap: HashMap<String?, String?> = HashMap()
            val bundleName = getItemStringFromStub("bundle_name_", bundle)
            val bundleColorPrimary = getItemColorFromStub("color_primary_", bundle)
            val bundleColorSecondary = getItemColorFromStub("color_secondary_", bundle)
            hashMap["android.theme.customization.system_palette"] =
                toColorString(bundleColorSecondary)
            hashMap["android.theme.customization.accent_color"] = toColorString(bundleColorPrimary)
            val accentColor = ColorScheme(bundleColorPrimary, false).accentColor
            val accentColor2 = ColorScheme(bundleColorPrimary, true).accentColor
            colorPresetBundles.add(
                ColorBundle(
                    bundleName,
                    hashMap,
                    false,
                    index = position,
                    mPreviewInfo = ColorBundle.PreviewInfo(
                        accentColor,
                        accentColor2
                    )
                )
            )
            position++
        }
        this.colorBundles = colorPresetBundles
    }

    fun buildColorSeeds(
        wallpaperColors: WallpaperColors,
        count: Int,
        source: String?,
        isDefault: Boolean,
        list: ArrayList<ColorOption>
    ) {
        val list2: List<Int>
        val list3: List<Int>
        val seedColors: List<Int> = ColorScheme.getSeedColors(wallpaperColors)
        loadPreset()
        buildBundle(
            seedColors[0],
            0,
            isDefault,
            source,
            list
        )
        val size = seedColors.size - 1
        list2 = if (size <= 0) {
            ArrayList()
        } else if (size != 1) {
            val arrayList: ArrayList<Int> = ArrayList(size)
            val listIterator = seedColors.listIterator(1)
            while (listIterator.hasNext()) {
                arrayList.add(listIterator.next())
            }
            arrayList
        } else if (seedColors.isNotEmpty()) {
            listOf(seedColors[seedColors.size - 1])
        } else {
            throw NoSuchElementException("List is empty.")
        }
        val i3 = count - 1
        var index4 = 0
        if (i3 >= 0) {
            list3 = if (i3 == 0) {
                ArrayList()
            } else (if (i3 >= list2.size) {
                list2
            } else if (i3 == 1) {
                listOf(list2[0])
            } else {
                val arrayList2: ArrayList<Int> = ArrayList(i3)
                var i5 = 0
                for (obj in list2) {
                    arrayList2.add(obj)
                    i5++
                    if (i5 == i3) {
                        break
                    }
                }
                arrayList2
            })
            for (seed in list3) {
                index4++
                buildBundle(seed, index4, false, source, list)
            }
            return
        }
        throw IllegalArgumentException("Requested element count $i3 is less than zero.")
    }

    companion object {

        fun loadSeedColors(
            colorProvider: ColorProvider,
            wallpaperColors: WallpaperColors?,
            wallpaperColors2: WallpaperColors?
        ) {
            val arrayList = ArrayList<ColorOption>()
            if (wallpaperColors != null) {
                val count = if (wallpaperColors2 == null) 4 else 2
                if (wallpaperColors2 != null) {
                    val wallpaperManagerCompat: WallpaperManagerCompat =
                        InjectorProvider.getInjector()
                            .getWallpaperManagerCompat(colorProvider.mContext)
                    var isDefault = true
                    if (wallpaperManagerCompat.getWallpaperId(WallpaperManagerCompat.FLAG_LOCK) <= wallpaperManagerCompat.getWallpaperId(
                            WallpaperManagerCompat.FLAG_SYSTEM
                        )
                    ) {
                        isDefault = false
                    }
                    colorProvider.buildColorSeeds(
                        if (isDefault) wallpaperColors2 else wallpaperColors,
                        count,
                        if (isDefault) "lock_wallpaper" else "home_wallpaper",
                        true,
                        arrayList
                    )
                    colorProvider.buildColorSeeds(
                        if (isDefault) wallpaperColors else wallpaperColors2,
                        count,
                        if (isDefault) "home_wallpaper" else "lock_wallpaper",
                        false,
                        arrayList
                    )
                } else {
                    colorProvider.buildColorSeeds(
                        wallpaperColors,
                        count,
                        "home_wallpaper",
                        true,
                        arrayList
                    )
                }
                val list = colorProvider.colorBundles
                val arrayList2 = ArrayList<ColorOption>()
                if (list != null) {
                    for (t in list) {
                        arrayList2.add(t)
                    }
                }
                arrayList.addAll(arrayList2)
                colorProvider.colorBundles = arrayList
            }
        }
    }

}
