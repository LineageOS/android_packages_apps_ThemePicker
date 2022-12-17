package com.android.customization.picker.clock

import android.os.Handler
import android.os.UserHandle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.systemui.plugins.ClockId
import com.android.systemui.plugins.ClockMetadata
import com.android.systemui.plugins.ClockProvider
import com.android.systemui.plugins.ClockProviderPlugin
import com.android.systemui.plugins.PluginManager
import com.android.systemui.shared.clocks.ClockRegistry
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests of [ClockCustomDemoFragment]. */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ClockCustomDemoFragmentTest {
    private lateinit var mActivity: AppCompatActivity
    private var mClockCustomDemoFragment: ClockCustomDemoFragment? = null
    private lateinit var registry: ClockRegistry
    @Mock private lateinit var mockPluginManager: PluginManager
    @Mock private lateinit var mockHandler: Handler
    @Mock private lateinit var fakePlugin: ClockProviderPlugin
    @Mock private lateinit var defaultClockProvider: ClockProvider

    private var settingValue: String = ""

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        mActivity = Robolectric.buildActivity(AppCompatActivity::class.java).get()
        mClockCustomDemoFragment = ClockCustomDemoFragment()
        Mockito.`when`(defaultClockProvider.getClocks())
            .thenReturn(listOf(ClockMetadata("DEFAULT", "Default Clock")))
        registry =
            object :
                ClockRegistry(
                    mActivity,
                    mockPluginManager,
                    mockHandler,
                    isEnabled = true,
                    userHandle = UserHandle.USER_ALL,
                    defaultClockProvider = defaultClockProvider
                ) {
                override var currentClockId: ClockId
                    get() = settingValue
                    set(value) {
                        settingValue = value
                    }

                override fun getClocks(): List<ClockMetadata> {
                    return defaultClockProvider.getClocks() +
                        listOf(
                            ClockMetadata("CLOCK_1", "Clock 1"),
                            ClockMetadata("CLOCK_2", "Clock 2"),
                            ClockMetadata("CLOCK_NOT_IN_USE", "Clock not in use")
                        )
                }
            }

        mClockCustomDemoFragment!!.clockRegistry = registry
        mClockCustomDemoFragment!!.recyclerView = RecyclerView(mActivity)
        mClockCustomDemoFragment!!.recyclerView.layoutManager =
            LinearLayoutManager(mActivity, RecyclerView.VERTICAL, false)
        mClockCustomDemoFragment!!.pluginListener.onPluginConnected(fakePlugin, mActivity)
    }

    @Test
    fun testItemCount_getCorrectClockCount() {
        Assert.assertEquals(3, mClockCustomDemoFragment!!.recyclerView.adapter!!.itemCount)
    }

    @Test
    fun testClick_setCorrectClockId() {
        mClockCustomDemoFragment!!
            .recyclerView
            .measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        mClockCustomDemoFragment!!.recyclerView.layout(0, 0, 100, 10000)
        val testPosition = 1
        mClockCustomDemoFragment!!
            .recyclerView
            .findViewHolderForAdapterPosition(testPosition)
            ?.itemView
            ?.performClick()
        Assert.assertEquals("CLOCK_1", settingValue)
    }
}
