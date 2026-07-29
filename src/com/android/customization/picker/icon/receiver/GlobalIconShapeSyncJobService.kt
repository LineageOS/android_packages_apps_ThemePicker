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

import android.app.job.JobParameters
import android.app.job.JobService
import com.android.customization.picker.icon.data.GlobalIconShapeManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@AndroidEntryPoint(JobService::class)
class GlobalIconShapeSyncJobService : Hilt_GlobalIconShapeSyncJobService() {
    @Inject lateinit var globalIconShapeManager: GlobalIconShapeManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var syncJob: Job? = null

    override fun onStartJob(params: JobParameters): Boolean {
        syncJob?.cancel()
        syncJob = scope.launch {
            val result = globalIconShapeManager.syncIfEnabled()
            // Launcher3 may not have published its shape provider immediately after boot.
            // Ask JobScheduler to retry instead of leaving the persisted preference unsynchronized.
            if (isActive) {
                jobFinished(params, result.isFailure)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        syncJob?.cancel()
        syncJob = null
        return true
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
