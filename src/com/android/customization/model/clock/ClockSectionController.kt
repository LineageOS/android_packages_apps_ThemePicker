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
 */
package com.android.customization.model.clock

import android.content.Context
import android.view.LayoutInflater
import com.android.customization.picker.clock.ClockCustomDemoFragment
import com.android.customization.picker.clock.ClockSectionView
import com.android.systemui.shared.customization.data.content.CustomizationProviderClient
import com.android.systemui.shared.customization.data.content.CustomizationProviderContract as Contract
import com.android.wallpaper.R
import com.android.wallpaper.model.CustomizationSectionController
import com.android.wallpaper.model.CustomizationSectionController.CustomizationSectionNavigationController
import kotlinx.coroutines.runBlocking

/** A [CustomizationSectionController] for clock customization. */
class ClockSectionController(
    private val navigationController: CustomizationSectionNavigationController,
    private val customizationProviderClient: CustomizationProviderClient,
) : CustomizationSectionController<ClockSectionView?> {
    override fun isAvailable(context: Context?): Boolean {
        return runBlocking { customizationProviderClient.queryFlags() }
            .firstOrNull { it.name == Contract.FlagsTable.FLAG_NAME_CUSTOM_CLOCKS_ENABLED }
            ?.value == true
    }

    override fun createView(context: Context): ClockSectionView {
        val view =
            LayoutInflater.from(context)
                .inflate(
                    R.layout.clock_section_view,
                    null,
                ) as ClockSectionView
        view.setOnClickListener { navigationController.navigateTo(ClockCustomDemoFragment()) }
        return view
    }
}
