package com.android.customization.model.color

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.ImageView
import com.android.wallpaper.R

class ColorBundle(
    title: String?,
    map: Map<String?, String?>?,
    isDefault: Boolean,
    index: Int,
    private val mPreviewInfo: PreviewInfo
) : ColorOption(
    title!!, map!!, isDefault, index
) {
    class PreviewInfo(
        val secondaryColorLight: Int,
        val secondaryColorDark: Int,
    )

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun bindThumbnailTile(view: View) {
        val resources = view.context.resources
        val thumbnailView = view.findViewById<ImageView>(R.id.color_preview_icon)
        val secondaryColor =
            if ((resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) mPreviewInfo.secondaryColorDark
            else mPreviewInfo.secondaryColorLight
        val gradientDrawable = view.resources.getDrawable(
            R.drawable.color_chip_medium_filled, thumbnailView.context.theme
        ) as GradientDrawable
        if (secondaryColor != 0) {
            gradientDrawable.setTintList(ColorStateList.valueOf(secondaryColor))
        } else {
            gradientDrawable.setTintList(ColorStateList.valueOf(resources.getColor(R.color.material_white_100)))
        }
        thumbnailView.setImageDrawable(gradientDrawable)
        val context = view.context
        if (mContentDescription == null) {
            val string = context.getString(R.string.default_theme_title)
            mContentDescription = if (mIsDefault) {
                string
            } else {
                mTitle
            }
        }
        view.contentDescription = mContentDescription
    }

    override fun getLayoutResId(): Int {
        return R.layout.color_option
    }

    override val source: String
        get() = "preset"
}
