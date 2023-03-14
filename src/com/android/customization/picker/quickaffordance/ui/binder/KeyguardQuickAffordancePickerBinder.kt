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

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.ViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.customization.picker.quickaffordance.ui.adapter.AffordancesAdapter
import com.android.customization.picker.quickaffordance.ui.adapter.SlotTabAdapter
import com.android.customization.picker.quickaffordance.ui.viewmodel.KeyguardQuickAffordancePickerViewModel
import com.android.wallpaper.R
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

object KeyguardQuickAffordancePickerBinder {

    /** Binds view with view-model for a lock screen quick affordance picker experience. */
    @JvmStatic
    fun bind(
        view: View,
        viewModel: KeyguardQuickAffordancePickerViewModel,
        lifecycleOwner: LifecycleOwner,
    ) {
        val slotTabView: RecyclerView = view.requireViewById(R.id.slot_tabs)
        val affordancesView: RecyclerView = view.requireViewById(R.id.affordances)

        val slotTabAdapter = SlotTabAdapter()
        slotTabView.adapter = slotTabAdapter
        slotTabView.layoutManager =
            LinearLayoutManager(view.context, RecyclerView.HORIZONTAL, false)
        slotTabView.addItemDecoration(ItemSpacing())
        val affordancesAdapter = AffordancesAdapter()
        affordancesView.adapter = affordancesAdapter
        affordancesView.layoutManager =
            LinearLayoutManager(view.context, RecyclerView.HORIZONTAL, false)
        affordancesView.addItemDecoration(ItemSpacing())

        var dialog: Dialog? = null

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.slots
                        .map { slotById -> slotById.values }
                        .collect { slots -> slotTabAdapter.setItems(slots.toList()) }
                }

                launch {
                    viewModel.quickAffordances.collect { affordances ->
                        affordancesAdapter.setItems(affordances)
                    }
                }

                launch {
                    viewModel.dialog.distinctUntilChanged().collect { dialogRequest ->
                        dialog?.dismiss()
                        dialog =
                            if (dialogRequest != null) {
                                showDialog(
                                    context = view.context,
                                    request = dialogRequest,
                                    onDismissed = viewModel::onDialogDismissed
                                )
                            } else {
                                null
                            }
                    }
                }
            }
        }
    }

    private fun showDialog(
        context: Context,
        request: KeyguardQuickAffordancePickerViewModel.DialogViewModel,
        onDismissed: () -> Unit,
    ): Dialog {
        val view: View =
            LayoutInflater.from(context)
                .inflate(
                    R.layout.keyguard_quick_affordance_enablement_dialog,
                    null,
                )
        KeyguardQuickAffordanceEnablementDialogBinder.bind(
            view = view,
            viewModel = request,
            onDismissed = onDismissed,
        )

        return AlertDialog.Builder(context, R.style.LightDialogTheme)
            .setView(view)
            .setOnDismissListener { onDismissed() }
            .show()
    }

    private class ItemSpacing : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(outRect: Rect, itemPosition: Int, parent: RecyclerView) {
            val addSpacingToStart = itemPosition > 0
            val addSpacingToEnd = itemPosition < (parent.adapter?.itemCount ?: 0) - 1
            val isRtl = parent.layoutManager?.layoutDirection == ViewCompat.LAYOUT_DIRECTION_RTL
            val density = parent.context.resources.displayMetrics.density
            if (!isRtl) {
                outRect.left = if (addSpacingToStart) ITEM_SPACING_DP.toPx(density) else 0
                outRect.right = if (addSpacingToEnd) ITEM_SPACING_DP.toPx(density) else 0
            } else {
                outRect.left = if (addSpacingToEnd) ITEM_SPACING_DP.toPx(density) else 0
                outRect.right = if (addSpacingToStart) ITEM_SPACING_DP.toPx(density) else 0
            }
        }

        private fun Int.toPx(density: Float): Int {
            return (this * density).toInt()
        }

        companion object {
            private const val ITEM_SPACING_DP = 8
        }
    }
}
