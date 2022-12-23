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

package com.android.customization.picker.quickaffordance.ui.binder

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.android.customization.picker.quickaffordance.ui.viewmodel.KeyguardQuickAffordancePickerViewModel
import com.android.wallpaper.R

object KeyguardQuickAffordanceEnablementDialogBinder {

    fun bind(
        view: View,
        viewModel: KeyguardQuickAffordancePickerViewModel.DialogViewModel,
        onDismissed: () -> Unit,
    ) {
        view.requireViewById<ImageView>(R.id.icon).setImageDrawable(viewModel.icon)
        view.requireViewById<TextView>(R.id.title).text =
            view.context.getString(
                R.string.keyguard_affordance_enablement_dialog_title,
                viewModel.name
            )
        view.requireViewById<TextView>(R.id.message).text = buildString {
            viewModel.instructions.forEachIndexed { index, instruction ->
                append(instruction)
                if (index < viewModel.instructions.size - 1) {
                    append("\n")
                }
            }
        }
        view.requireViewById<TextView>(R.id.button).apply {
            text = viewModel.actionText
            setOnClickListener {
                if (viewModel.intent != null) {
                    view.context.startActivity(viewModel.intent)
                } else {
                    onDismissed()
                }
            }
        }
    }
}
