package com.android.customization.model.color

import android.content.res.Configuration
import android.graphics.PorterDuff
import android.view.View
import android.widget.ImageView
import com.android.wallpaper.R

class ColorSeedOption(
    title: String?,
    map: Map<String?, String?>?,
    isDefault: Boolean,
    override val source: String,
    index: Int,
    private val mPreviewInfo: PreviewInfo
) : ColorOption(
    title!!, map!!, isDefault, index
) {
    private val mPreviewColorIds = intArrayOf(
        R.id.color_preview_0,
        R.id.color_preview_1,
        R.id.color_preview_2,
        R.id.color_preview_3
    )

    override fun bindThumbnailTile(view: View) {
        val padding: Int
        val resources = view.context.resources
        var iterator = 0
        val mPreviewColorTint =
            if ((resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) mPreviewInfo.darkColors else mPreviewInfo.lightColors
        padding = if (view.isActivated) {
            resources.getDimensionPixelSize(R.dimen.color_seed_option_tile_padding_selected)
        } else {
            resources.getDimensionPixelSize(R.dimen.color_seed_option_tile_padding)
        }
        while (true) {
            if (iterator < mPreviewColorIds.size) {
                val imageView = view.findViewById<View>(mPreviewColorIds[iterator]) as ImageView
                imageView.drawable.setColorFilter(mPreviewColorTint[iterator], PorterDuff.Mode.SRC)
                imageView.setPadding(padding, padding, padding, padding)
                iterator++
            } else {
                view.contentDescription =
                    view.context.getString(R.string.wallpaper_color_title)
                return
            }
        }
    }

    override fun getLayoutResId(): Int {
        return R.layout.color_seed_option
    }

    class PreviewInfo(var lightColors: IntArray, var darkColors: IntArray)
}
