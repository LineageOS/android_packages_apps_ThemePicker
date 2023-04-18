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
import com.android.systemui.plugins.ClockController
import com.android.wallpaper.R
import java.lang.Float.max

class ClockCarouselView(
    context: Context,
    attrs: AttributeSet,
) :
    FrameLayout(
        context,
        attrs,
    ) {

    var isCarouselInTransition = false

    private val carousel: Carousel
    private val motionLayout: MotionLayout
    private lateinit var adapter: ClockCarouselAdapter
    private lateinit var scalingUpClockController: ClockController
    private lateinit var scalingDownClockController: ClockController
    private var scalingUpClockView: View? = null
    private var scalingDownClockView: View? = null
    private var showingCardView: View? = null
    private var hidingCardView: View? = null

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
                    isCarouselInTransition = true
                    val scalingDownClockId = adapter.clockIds[carousel.currentIndex]
                    val scalingUpIdx =
                        if (endId == R.id.next) (carousel.currentIndex + 1) % adapter.count()
                        else (carousel.currentIndex - 1 + adapter.count()) % adapter.count()
                    val scalingUpClockId = adapter.clockIds[scalingUpIdx]
                    scalingDownClockController = adapter.onGetClockController(scalingDownClockId)
                    scalingUpClockController = adapter.onGetClockController(scalingUpClockId)
                    scalingDownClockView = motionLayout?.findViewById(R.id.clock_scale_view_2)
                    scalingUpClockView =
                        motionLayout?.findViewById(
                            if (endId == R.id.next) R.id.clock_scale_view_3
                            else R.id.clock_scale_view_1
                        )
                    showingCardView = motionLayout?.findViewById(R.id.item_card_2)
                    hidingCardView =
                        motionLayout?.findViewById(
                            if (endId == R.id.next) R.id.item_card_3 else R.id.item_card_1
                        )
                }

                override fun onTransitionChange(
                    motionLayout: MotionLayout?,
                    startId: Int,
                    endId: Int,
                    progress: Float
                ) {
                    scalingDownClockController.largeClock.animations.onPickerCarouselSwiping(
                        1 - progress,
                        getPreviewRatio()
                    )
                    scalingUpClockController.largeClock.animations.onPickerCarouselSwiping(
                        progress,
                        getPreviewRatio()
                    )
                    val scalingUpScale = getScalingUpScale(progress)
                    val scalingDownScale = getScalingDownScale(progress)
                    scalingUpClockView?.scaleX = scalingUpScale
                    scalingUpClockView?.scaleY = scalingUpScale
                    scalingDownClockView?.scaleX = scalingDownScale
                    scalingDownClockView?.scaleY = scalingDownScale
                    showingCardView?.alpha = getShowingAlpha(progress)
                    hidingCardView?.alpha = getHidingAlpha(progress)
                }

                override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                    isCarouselInTransition = false
                }

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
            val viewRoot = view as? ViewGroup ?: return
            val cardView =
                getClockCardViewId(viewRoot.id)?.let { viewRoot.findViewById(it) as? View }
                    ?: return
            val clockScaleView =
                getClockScaleViewId(viewRoot.id)?.let { viewRoot.findViewById(it) as? View }
                    ?: return
            val clockHostView =
                getClockHostViewId(viewRoot.id)?.let { viewRoot.findViewById(it) as? ViewGroup }
                    ?: return

            clockHostView.removeAllViews()
            val clockView = onGetClockController(clockIds[index]).largeClock.view
            // Making sure the large clock tick to the correct time
            onGetClockController(clockIds[index]).largeClock.events.onTimeTick()
            // The clock view might still be attached to an existing parent. Detach before adding to
            // another parent.
            (clockView.parent as? ViewGroup)?.removeView(clockView)
            clockHostView.addView(clockView)
            // initialize scaling state for all clocks
            if (!isMiddleView(viewRoot.id)) {
                cardView.alpha = 1f
                clockScaleView.scaleX = CLOCK_CAROUSEL_VIEW_SCALE
                clockScaleView.scaleY = CLOCK_CAROUSEL_VIEW_SCALE
                onGetClockController(clockIds[index])
                    .largeClock
                    .animations
                    .onPickerCarouselSwiping(0F, getPreviewRatio())
            } else {
                cardView.alpha = 0f
                clockScaleView.scaleX = 1f
                clockScaleView.scaleY = 1f
                onGetClockController(clockIds[index])
                    .largeClock
                    .animations
                    .onPickerCarouselSwiping(1F, getPreviewRatio())
            }
        }

        override fun onNewItem(index: Int) {
            onClockSelected.invoke(clockIds[index])
        }
    }

    companion object {
        const val CLOCK_CAROUSEL_VIEW_SCALE = 0.5f

        fun getScalingUpScale(progress: Float) =
            CLOCK_CAROUSEL_VIEW_SCALE + progress * (1f - CLOCK_CAROUSEL_VIEW_SCALE)

        fun getScalingDownScale(progress: Float) = 1f - progress * (1f - CLOCK_CAROUSEL_VIEW_SCALE)

        // This makes the card only starts to reveal in the last quarter of the trip so
        // the card won't overlap the preview.
        fun getShowingAlpha(progress: Float) = max(progress - 0.75f, 0f) * 4

        // This makes the card starts to hide in the first quarter of the trip so the
        // card won't overlap the preview.
        fun getHidingAlpha(progress: Float) = max(1f - progress * 4, 0f)

        fun getClockHostViewId(rootViewId: Int): Int? {
            return when (rootViewId) {
                R.id.item_view_0 -> R.id.clock_host_view_0
                R.id.item_view_1 -> R.id.clock_host_view_1
                R.id.item_view_2 -> R.id.clock_host_view_2
                R.id.item_view_3 -> R.id.clock_host_view_3
                R.id.item_view_4 -> R.id.clock_host_view_4
                else -> null
            }
        }

        fun getClockScaleViewId(rootViewId: Int): Int? {
            return when (rootViewId) {
                R.id.item_view_0 -> R.id.clock_scale_view_0
                R.id.item_view_1 -> R.id.clock_scale_view_1
                R.id.item_view_2 -> R.id.clock_scale_view_2
                R.id.item_view_3 -> R.id.clock_scale_view_3
                R.id.item_view_4 -> R.id.clock_scale_view_4
                else -> null
            }
        }

        fun getClockCardViewId(rootViewId: Int): Int? {
            return when (rootViewId) {
                R.id.item_view_0 -> R.id.item_card_0
                R.id.item_view_1 -> R.id.item_card_1
                R.id.item_view_2 -> R.id.item_card_2
                R.id.item_view_3 -> R.id.item_card_3
                R.id.item_view_4 -> R.id.item_card_4
                else -> null
            }
        }

        fun isMiddleView(rootViewId: Int): Boolean {
            return rootViewId == R.id.item_view_2
        }
    }
}
