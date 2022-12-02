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

package com.android.customization.picker.quickaffordance.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.customization.picker.quickaffordance.ui.viewmodel.KeyguardQuickAffordanceViewModel
import com.android.wallpaper.R

/** Adapts between lock screen quick affordance items and views. */
class AffordancesAdapter : RecyclerView.Adapter<AffordancesAdapter.ViewHolder>() {

    private val items = mutableListOf<KeyguardQuickAffordanceViewModel>()

    fun setItems(items: List<KeyguardQuickAffordanceViewModel>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iconContainerView: View = itemView.requireViewById(R.id.icon_container)
        val iconView: ImageView = itemView.requireViewById(R.id.icon)
        val nameView: TextView = itemView.requireViewById(R.id.name)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.keyguard_quick_affordance,
                    parent,
                    false,
                )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.itemView.alpha =
            if (item.isEnabled) {
                ALPHA_ENABLED
            } else {
                ALPHA_DISABLED
            }

        holder.itemView.setOnClickListener(
            if (item.onClicked != null) {
                View.OnClickListener { item.onClicked.invoke() }
            } else {
                null
            }
        )
        holder.iconContainerView.setBackgroundResource(
            if (item.isSelected) {
                R.drawable.keyguard_quick_affordance_icon_container_background_selected
            } else {
                R.drawable.keyguard_quick_affordance_icon_container_background
            }
        )
        holder.iconView.isSelected = item.isSelected
        holder.nameView.isSelected = item.isSelected
        holder.iconView.setImageDrawable(item.icon)
        holder.nameView.text = item.contentDescription
        holder.nameView.isSelected = item.isSelected
    }

    companion object {
        private const val ALPHA_ENABLED = 1f
        private const val ALPHA_DISABLED = 0.3f
    }
}
