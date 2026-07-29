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

package com.android.customization.picker.icon.data

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.om.FabricatedOverlay
import android.content.om.OverlayIdentifier
import android.content.om.OverlayManager
import android.content.om.OverlayManagerTransaction
import android.graphics.RectF
import android.os.UserHandle
import android.util.Base64
import android.util.Log
import android.util.PathParser
import android.util.TypedValue
import com.android.customization.model.grid.ShapeGridManager
import com.android.customization.picker.icon.receiver.GlobalIconShapeSyncJobService
import com.android.wallpaper.picker.di.modules.BackgroundDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/** Persists and projects Launcher shape selection into the framework icon mask. */
@Singleton
class GlobalIconShapeManager
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val shapeGridManager: ShapeGridManager,
    @BackgroundDispatcher private val backgroundDispatcher: CoroutineDispatcher,
) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val overlayManager: OverlayManager = context.getSystemService(OverlayManager::class.java)
    private val _isEnabled = MutableStateFlow(preferences.getBoolean(PREF_ENABLED, true))
    private val preferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == PREF_ENABLED) {
                _isEnabled.value = preferences.getBoolean(PREF_ENABLED, true)
            }
        }

    val isEnabled: StateFlow<Boolean> = _isEnabled

    init {
        preferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    /** Applies or removes global mask. Does not persist enabled state when operation fails. */
    suspend fun setEnabled(enabled: Boolean, shapeKey: String): Result<Unit> =
        withContext(backgroundDispatcher) {
            runCatching {
                if (enabled) {
                    val pathData = findShapePath(shapeKey)
                    val pathFingerprint = maybeApplyShape(pathData)
                    check(
                        preferences
                            .edit()
                            .putBoolean(PREF_ENABLED, true)
                            .putString(PREF_APPLIED_PATH_FINGERPRINT, pathFingerprint)
                            .commit()
                    ) {
                        "Failed to persist global icon shape preference"
                    }
                } else {
                    removeShape()
                    check(
                        preferences
                            .edit()
                            .putBoolean(PREF_ENABLED, false)
                            .remove(PREF_APPLIED_PATH_FINGERPRINT)
                            .commit()
                    ) {
                        "Failed to persist global icon shape preference"
                    }
                }
            }.onFailure { Log.e(TAG, "Failed to update global icon shape", it) }
        }

    /** Reconciles the enabled FRRO with Launcher's current selected shape. Safe to call repeatedly. */
    suspend fun syncIfEnabled(): Result<Unit> =
        withContext(backgroundDispatcher) {
            if (!preferences.getBoolean(PREF_ENABLED, true)) {
                return@withContext Result.success(Unit)
            }
            runCatching {
                val selected = shapeGridManager.getShapeOptions().singleOrNull { it.isCurrent }
                    ?: error("Launcher did not return exactly one selected icon shape")
                val pathFingerprint = maybeApplyShape(selected.path)
                if (
                    preferences.getString(PREF_APPLIED_PATH_FINGERPRINT, null) != pathFingerprint
                ) {
                    check(
                        preferences
                            .edit()
                            .putString(
                                PREF_APPLIED_PATH_FINGERPRINT,
                                pathFingerprint,
                            )
                            .commit()
                    ) {
                        "Failed to persist applied icon shape fingerprint"
                    }
                }
            }.onFailure { Log.e(TAG, "Failed to synchronize global icon shape", it) }
        }

    /** Schedules a persisted retry after boot or package replacement. */
    fun scheduleSync() {
        if (!preferences.getBoolean(PREF_ENABLED, true)) return
        val job =
            JobInfo.Builder(
                    SYNC_JOB_ID,
                    ComponentName(context, GlobalIconShapeSyncJobService::class.java),
                )
                .setMinimumLatency(SYNC_DELAY_MILLIS)
                .setOverrideDeadline(SYNC_DEADLINE_MILLIS)
                .setPersisted(true)
                .build()
        val result = context.getSystemService(JobScheduler::class.java).schedule(job)
        if (result == JobScheduler.RESULT_FAILURE) {
            Log.w(TAG, "Unable to schedule global icon shape synchronization")
        }
    }

    private suspend fun findShapePath(shapeKey: String): String =
        shapeGridManager.getShapeOptions().firstOrNull { it.key == shapeKey }?.path
            ?: error("Launcher shape '$shapeKey' is unavailable")

    private fun maybeApplyShape(pathData: String): String {
        val pathFingerprint = pathFingerprint(pathData)
        if (!isShapeApplied(pathFingerprint)) {
            applyShape(pathData)
        }
        return pathFingerprint
    }

    private fun isShapeApplied(pathFingerprint: String): Boolean {
        val userId = UserHandle.myUserId()
        val identifier = OverlayIdentifier(context.packageName, overlayName(userId))
        val overlayInfo = overlayManager.getOverlayInfo(identifier, UserHandle.of(userId))
        return overlayInfo?.isEnabled == true &&
            preferences.getString(PREF_APPLIED_PATH_FINGERPRINT, null) == pathFingerprint
    }

    private fun pathFingerprint(pathData: String): String =
        Base64.encodeToString(
            MessageDigest.getInstance("SHA-256").digest(pathData.toByteArray(Charsets.UTF_8)),
            Base64.NO_WRAP,
        )

    private fun applyShape(pathData: String) {
        validatePath(pathData)
        val userId = UserHandle.myUserId()
        val overlay =
            FabricatedOverlay.Builder(context.packageName, overlayName(userId), ANDROID_PACKAGE)
                .setResourceValue(ICON_MASK_RESOURCE, TypedValue.TYPE_STRING, pathData, null)
                .build()
        val identifier = overlay.identifier
        overlayManager.commit(
            OverlayManagerTransaction.Builder()
                .registerFabricatedOverlay(overlay)
                .setEnabled(identifier, true, userId)
                .build()
        )
        check(overlayManager.getOverlayInfo(identifier, UserHandle.of(userId))?.isEnabled == true) {
            "Global icon shape overlay was not enabled"
        }
    }

    private fun removeShape() {
        val userId = UserHandle.myUserId()
        val identifier = OverlayIdentifier(context.packageName, overlayName(userId))
        if (overlayManager.getOverlayInfo(identifier, UserHandle.of(userId)) == null) {
            return
        }
        overlayManager.commit(
            OverlayManagerTransaction.Builder().unregisterFabricatedOverlay(identifier).build()
        )
    }

    private fun validatePath(pathData: String) {
        require(pathData.length in 1..MAX_PATH_LENGTH) { "Icon mask path length is invalid" }
        val path = PathParser.createPathFromPathData(pathData)
            ?: error("Icon mask path could not be parsed")
        val bounds = RectF()
        path.computeBounds(bounds, true)
        require(
            bounds.left.isFinite() &&
                bounds.top.isFinite() &&
                bounds.right.isFinite() &&
                bounds.bottom.isFinite() &&
                bounds.width() >= MIN_PATH_SIZE &&
                bounds.height() >= MIN_PATH_SIZE &&
                bounds.left >= MIN_PATH_COORDINATE &&
                bounds.top >= MIN_PATH_COORDINATE &&
                bounds.right <= MAX_PATH_COORDINATE &&
                bounds.bottom <= MAX_PATH_COORDINATE
        ) { "Icon mask path bounds are invalid: $bounds" }
    }

    private fun overlayName(userId: Int): String = "${OVERLAY_NAME}_u$userId"

    companion object {
        private const val TAG = "GlobalIconShapeManager"
        private const val PREFERENCES_NAME = "global_icon_shape"
        private const val PREF_ENABLED = "enabled"
        private const val PREF_APPLIED_PATH_FINGERPRINT = "applied_path_fingerprint"
        private const val OVERLAY_NAME = "lineage_launcher_icon_shape"
        private const val ANDROID_PACKAGE = "android"
        private const val ICON_MASK_RESOURCE = "android:string/config_icon_mask"
        private const val SYNC_JOB_ID = 230201
        private const val SYNC_DELAY_MILLIS = 5_000L
        private const val SYNC_DEADLINE_MILLIS = 60_000L
        private const val MAX_PATH_LENGTH = 16 * 1024
        private const val MIN_PATH_SIZE = 1f
        // Launcher paths use a 100 x 100 viewport and a small overshoot for some shapes.
        private const val MIN_PATH_COORDINATE = -25f
        private const val MAX_PATH_COORDINATE = 125f
    }
}
