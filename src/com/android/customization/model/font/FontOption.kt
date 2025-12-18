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

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.android.customization.model.CustomizationManager
import com.android.customization.model.CustomizationOption
import com.android.themepicker.R
import com.android.wallpaper.util.ResourceUtils

class FontOption(
    private val mOverlayPackage: String?,
    private val mTitle: String,
    val headlineFont: Typeface?,
    val bodyFont: Typeface?,
) : CustomizationOption<FontOption> {

    override fun bindThumbnailTile(view: View) {
        val colorAttr =
            if (view.isActivated || view.id == R.id.option_entry_icon_container) {
                android.R.attr.textColorPrimary
            } else {
                android.R.attr.textColorTertiary
            }

        val colorFilter = ResourceUtils.getColorAttr(view.context, colorAttr)
        view.findViewById<TextView>(R.id.thumbnail_text).setTextColor(colorFilter)
        view.contentDescription = mTitle
    }

    override fun isActive(manager: CustomizationManager<FontOption>): Boolean {
        val fontManager = manager as FontManager
        return fontManager.isActive(this)
    }

    override fun getLayoutResId(): Int {
        return R.layout.theme_font_option
    }

    override fun getTitle(): String {
        return mTitle
    }

    val packageName: String?
        get() = mOverlayPackage

    fun bindPreview(container: ViewGroup) {
        val cardBody = container.findViewById<ViewGroup>(R.id.theme_preview_card_body_container)
        if (cardBody.childCount == 0) {
            LayoutInflater.from(container.context)
                .inflate(R.layout.preview_card_font_content, cardBody, true)
        }

        val title = container.findViewById<TextView>(R.id.font_card_title)
        title.typeface = headlineFont

        val bodyText = container.findViewById<TextView>(R.id.font_card_body)
        bodyText.typeface = bodyFont

        container
            .findViewById<View>(R.id.font_card_divider)
            .setBackgroundColor(title.currentTextColor)
    }
}
