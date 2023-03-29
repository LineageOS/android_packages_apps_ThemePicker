/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.customization.picker.clock.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.constraintlayout.helper.widget.Carousel
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.view.get
import com.android.systemui.plugins.ClockController
import com.android.wallpaper.R

class ClockCarouselView(
    context: Context,
    attrs: AttributeSet,
) :
    FrameLayout(
        context,
        attrs,
    ) {

    private val carousel: Carousel
    private val motionLayout: MotionLayout
    private lateinit var adapter: ClockCarouselAdapter
    private lateinit var scalingUpClock: ClockController
    private lateinit var scalingDownClock: ClockController

    init {
        val clockCarousel = LayoutInflater.from(context).inflate(R.layout.clock_carousel, this)
        carousel = clockCarousel.requireViewById(R.id.carousel)
        motionLayout = clockCarousel.requireViewById(R.id.motion_container)
    }

    fun setUpClockCarouselView(
        clockIds: List<String>,
        onGetClockController: (clockId: String) -> ClockController,
        onClockSelected: (clockId: String) -> Unit,
        getPreviewRatio: () -> Float,
    ) {
        adapter =
            ClockCarouselAdapter(clockIds, onGetClockController, onClockSelected, getPreviewRatio)
        carousel.setAdapter(adapter)
        carousel.refresh()
        motionLayout.setTransitionListener(
            object : MotionLayout.TransitionListener {
                override fun onTransitionStarted(
                    motionLayout: MotionLayout?,
                    startId: Int,
                    endId: Int
                ) {
                    val scalingDownClockId = adapter.clockIds[carousel.currentIndex]
                    val scalingUpIdx =
                        if (endId == R.id.next) (carousel.currentIndex + 1) % adapter.count()
                        else (carousel.currentIndex - 1 + adapter.count()) % adapter.count()
                    val scalingUpClockId = adapter.clockIds[scalingUpIdx]
                    scalingDownClock = adapter.onGetClockController(scalingDownClockId)
                    scalingUpClock = adapter.onGetClockController(scalingUpClockId)
                }

                override fun onTransitionChange(
                    motionLayout: MotionLayout?,
                    startId: Int,
                    endId: Int,
                    progress: Float
                ) {
                    scalingDownClock.animations.onPickerCarouselSwiping(
                        1 - progress,
                        getPreviewRatio()
                    )
                    scalingUpClock.animations.onPickerCarouselSwiping(progress, getPreviewRatio())
                }

                override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {}

                override fun onTransitionTrigger(
                    motionLayout: MotionLayout?,
                    triggerId: Int,
                    positive: Boolean,
                    progress: Float
                ) {}
            }
        )
    }

    fun setSelectedClockIndex(
        index: Int,
    ) {
        carousel.jumpToIndex(index)
    }

    class ClockCarouselAdapter(
        val clockIds: List<String>,
        val onGetClockController: (clockId: String) -> ClockController,
        private val onClockSelected: (clockId: String) -> Unit,
        val getPreviewRatio: () -> Float,
    ) : Carousel.Adapter {

        override fun count(): Int {
            return clockIds.size
        }

        override fun populate(view: View?, index: Int) {
            val viewRoot = view as ViewGroup
            val clockHostView = viewRoot[0] as ViewGroup
            clockHostView.removeAllViews()
            val clockView = onGetClockController(clockIds[index]).largeClock.view
            // The clock view might still be attached to an existing parent. Detach before adding to
            // another parent.
            (clockView.parent as? ViewGroup)?.removeView(clockView)
            clockHostView.addView(clockView)
            // initialize scaling state for all clocks
            if (view.id != MIDDLE_VIEW_IN_START_STATE) {
                onGetClockController(clockIds[index])
                    .animations
                    .onPickerCarouselSwiping(0F, getPreviewRatio())
            } else {
                onGetClockController(clockIds[index])
                    .animations
                    .onPickerCarouselSwiping(1F, getPreviewRatio())
            }
        }

        override fun onNewItem(index: Int) {
            onClockSelected.invoke(clockIds[index])
        }
    }

    companion object {
        const val MIDDLE_VIEW_IN_START_STATE = R.id.item_view_2
    }
}
