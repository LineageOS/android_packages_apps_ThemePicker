/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.customization.picker.icon.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.android.customization.picker.icon.data.GlobalIconShapeManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint(BroadcastReceiver::class)
class GlobalIconShapeSyncReceiver : Hilt_GlobalIconShapeSyncReceiver() {
    @Inject lateinit var globalIconShapeManager: GlobalIconShapeManager

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        globalIconShapeManager.scheduleSync()
    }
}
