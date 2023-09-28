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
package com.android.customization.module.logging

import com.android.customization.model.color.ColorOption
import com.android.customization.model.grid.GridOption
import com.android.wallpaper.module.logging.UserEventLogger

/** Extension of [UserEventLogger] that adds ThemePicker specific events. */
interface ThemesUserEventLogger : UserEventLogger {
    /**
     * Logs the color usage while color is applied.
     *
     * @param action color applied action.
     * @param colorOption applied color option.
     */
    fun logColorApplied(action: Int, colorOption: ColorOption)

    fun logGridApplied(grid: GridOption)
}
