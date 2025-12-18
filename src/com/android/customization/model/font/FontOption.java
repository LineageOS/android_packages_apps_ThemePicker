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
package com.android.customization.model.font;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.customization.model.CustomizationManager;
import com.android.customization.model.CustomizationOption;
import com.android.themepicker.R;
import com.android.wallpaper.util.ResourceUtils;

public class FontOption implements CustomizationOption<FontOption> {

    private final String mTitle;
    private final String mOverlayPackage;
    private final Typeface mHeadlineFont;
    private final Typeface mBodyFont;

    public FontOption(String overlayPackage, String title, Typeface headlineFont,
            Typeface bodyFont) {
        mOverlayPackage = overlayPackage;
        mTitle = title;
        mHeadlineFont = headlineFont;
        mBodyFont = bodyFont;
    }

    @Override
    public void bindThumbnailTile(View view) {
        int colorFilter = ResourceUtils.getColorAttr(view.getContext(),
                view.isActivated() || view.getId() == R.id.option_entry_icon_container
                        ? android.R.attr.textColorPrimary
                        : android.R.attr.textColorTertiary);
        ((TextView) view.findViewById(R.id.thumbnail_text)).setTextColor(colorFilter);
        view.setContentDescription(mTitle);
    }

    @Override
    public boolean isActive(CustomizationManager<FontOption> manager) {
        FontManager fontManager = (FontManager) manager;
        return fontManager.isActive(this);
    }

    @Override
    public int getLayoutResId() {
        return R.layout.theme_font_option;
    }

    @Override
    public String getTitle() {
        return mTitle;
    }

    public String getPackageName() {
        return mOverlayPackage;
    }

    public void bindPreview(ViewGroup container) {
        ViewGroup cardBody = container.findViewById(R.id.theme_preview_card_body_container);
        if (cardBody.getChildCount() == 0) {
            LayoutInflater.from(container.getContext()).inflate(
                    R.layout.preview_card_font_content, cardBody, true);
        }
        TextView title = container.findViewById(R.id.font_card_title);
        title.setTypeface(mHeadlineFont);
        TextView bodyText = container.findViewById(R.id.font_card_body);
        bodyText.setTypeface(mBodyFont);
        container.findViewById(R.id.font_card_divider).setBackgroundColor(
                title.getCurrentTextColor());
    }

    public Typeface getHeadlineFont() {
        return mHeadlineFont;
    }

    public Typeface getBodyFont() {
        return mBodyFont;
    }
}
