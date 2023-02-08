package com.android.customization.picker.clock.ui.view

import android.content.Context
import android.view.View
import com.android.systemui.plugins.ClockController
import com.android.systemui.shared.clocks.ClockRegistry
import java.util.HashMap

class ClockViewFactory(private val context: Context, private val registry: ClockRegistry) {
    private val clockControllers: HashMap<String, ClockController> =
        HashMap<String, ClockController>()

    fun getView(clockId: String): View {
        return (clockControllers[clockId] ?: initClockController(clockId)).largeClock.view
    }

    private fun initClockController(clockId: String): ClockController {
        val controller =
            registry.createExampleClock(clockId).also { it?.initialize(context.resources, 0f, 0f) }
        checkNotNull(controller)
        clockControllers[clockId] = controller
        return controller
    }
}
