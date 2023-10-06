package com.android.customization.picker.clock.ui

import android.view.View
import androidx.lifecycle.LifecycleOwner
import com.android.customization.picker.clock.ui.view.ClockViewFactory
import com.android.systemui.plugins.ClockController

/**
 * This is a fake [ClockViewFactory]. Only implement the function if it's actually called in a test.
 */
class FakeClockViewFactory : ClockViewFactory {

    override fun getController(clockId: String): ClockController {
        TODO("Not yet implemented")
    }

    override fun getLargeView(clockId: String): View {
        TODO("Not yet implemented")
    }

    override fun getSmallView(clockId: String): View {
        TODO("Not yet implemented")
    }

    override fun updateColorForAllClocks(seedColor: Int?) {
        TODO("Not yet implemented")
    }

    override fun updateColor(clockId: String, seedColor: Int?) {
        TODO("Not yet implemented")
    }

    override fun updateRegionDarkness() {
        TODO("Not yet implemented")
    }

    override fun updateTimeFormat(clockId: String) {
        TODO("Not yet implemented")
    }

    override fun registerTimeTicker(owner: LifecycleOwner) {
        TODO("Not yet implemented")
    }

    override fun onDestroy() {
        TODO("Not yet implemented")
    }

    override fun unregisterTimeTicker(owner: LifecycleOwner) {
        TODO("Not yet implemented")
    }
}
