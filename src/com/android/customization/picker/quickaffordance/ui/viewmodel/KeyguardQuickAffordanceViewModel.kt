/*
 * Copyright (C) 2022 The Android Open Source Project
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
 *
 */

package com.android.customization.picker.quickaffordance.ui.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import com.android.wallpaper.R

/** Models UI state for a single lock screen quick affordance in a picker experience. */
data class KeyguardQuickAffordanceViewModel(
    /** An icon for the quick affordance. */
    val icon: Drawable,

    /** A content description for the icon. */
    val contentDescription: String,

    /** Whether this quick affordance is selected in its slot. */
    val isSelected: Boolean,

    /** Whether this quick affordance is enabled. */
    val isEnabled: Boolean,

    /** Notifies that the quick affordance has been clicked by the user. */
    val onClicked: (() -> Unit)?,
) {
    companion object {
        @SuppressLint("UseCompatLoadingForDrawables")
        fun none(
            context: Context,
            isSelected: Boolean,
            onSelected: () -> Unit,
        ): KeyguardQuickAffordanceViewModel {
            return KeyguardQuickAffordanceViewModel(
                icon = checkNotNull(context.getDrawable(R.drawable.link_off)),
                contentDescription = context.getString(R.string.keyguard_affordance_none),
                isSelected = isSelected,
                onClicked =
                    if (isSelected) {
                        null
                    } else {
                        onSelected
                    },
                isEnabled = true,
            )
        }
    }
}
