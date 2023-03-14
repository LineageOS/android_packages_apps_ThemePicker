package com.android.customization.picker.clock

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.UserHandle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.setPadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.internal.annotations.VisibleForTesting
import com.android.systemui.plugins.ClockMetadata
import com.android.systemui.plugins.ClockProviderPlugin
import com.android.systemui.plugins.Plugin
import com.android.systemui.plugins.PluginListener
import com.android.systemui.plugins.PluginManager
import com.android.systemui.shared.clocks.ClockRegistry
import com.android.systemui.shared.clocks.DefaultClockProvider
import com.android.systemui.shared.plugins.PluginActionManager
import com.android.systemui.shared.plugins.PluginEnabler
import com.android.systemui.shared.plugins.PluginEnabler.ENABLED
import com.android.systemui.shared.plugins.PluginInstance
import com.android.systemui.shared.plugins.PluginManagerImpl
import com.android.systemui.shared.plugins.PluginPrefs
import com.android.systemui.shared.system.UncaughtExceptionPreHandlerManager_Factory
import com.android.wallpaper.R
import com.android.wallpaper.picker.AppbarFragment
import java.util.concurrent.Executors

private val TAG = ClockCustomDemoFragment::class.simpleName

class ClockCustomDemoFragment : AppbarFragment() {
    @VisibleForTesting lateinit var clockRegistry: ClockRegistry
    val isDebugDevice = true
    val privilegedPlugins = listOf<String>()
    val action = ClockProviderPlugin.ACTION
    lateinit var view: ViewGroup
    @VisibleForTesting lateinit var recyclerView: RecyclerView
    lateinit var pluginManager: PluginManager
    @VisibleForTesting
    val pluginListener =
        object : PluginListener<ClockProviderPlugin> {
            override fun onPluginConnected(plugin: ClockProviderPlugin, context: Context) {
                val listInUse = clockRegistry.getClocks().filter { "NOT_IN_USE" !in it.clockId }
                recyclerView.adapter = ClockRecyclerAdapter(listInUse, context, clockRegistry)
            }
        }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val defaultClockProvider =
            DefaultClockProvider(context, LayoutInflater.from(context), context.resources)
        pluginManager = createPluginManager(context)
        clockRegistry =
            ClockRegistry(
                context,
                pluginManager,
                Handler.getMain(),
                isEnabled = true,
                userHandle = UserHandle.USER_OWNER,
                defaultClockProvider
            )
        pluginManager.addPluginListener(pluginListener, ClockProviderPlugin::class.java, true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_clock_custom_picker_demo, container, false)
        setUpToolbar(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.requireViewById(R.id.clock_preview_card_list_demo)
        recyclerView.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        super.onViewCreated(view, savedInstanceState)
    }

    override fun getDefaultTitle(): CharSequence {
        return getString(R.string.clock_title)
    }

    private fun createPluginManager(context: Context): PluginManager {
        val instanceFactory =
            PluginInstance.Factory(
                this::class.java.classLoader,
                PluginInstance.InstanceFactory<Plugin>(),
                PluginInstance.VersionChecker(),
                privilegedPlugins,
                isDebugDevice
            )

        /*
         * let SystemUI handle plugin, in this class assume plugins are enabled
         */
        val pluginEnabler =
            object : PluginEnabler {
                override fun setEnabled(component: ComponentName) {}

                override fun setDisabled(
                    component: ComponentName,
                    @PluginEnabler.DisableReason reason: Int
                ) {}

                override fun isEnabled(component: ComponentName): Boolean {
                    return true
                }

                @PluginEnabler.DisableReason
                override fun getDisableReason(componentName: ComponentName): Int {
                    return ENABLED
                }
            }

        val pluginActionManager =
            PluginActionManager.Factory(
                context,
                context.packageManager,
                context.mainExecutor,
                Executors.newSingleThreadExecutor(),
                context.getSystemService(NotificationManager::class.java),
                pluginEnabler,
                privilegedPlugins,
                instanceFactory
            )
        return PluginManagerImpl(
            context,
            pluginActionManager,
            isDebugDevice,
            uncaughtExceptionPreHandlerManager,
            pluginEnabler,
            PluginPrefs(context),
            listOf()
        )
    }

    companion object {
        private val uncaughtExceptionPreHandlerManager =
            UncaughtExceptionPreHandlerManager_Factory.create().get()
    }

    internal class ClockRecyclerAdapter(
        val list: List<ClockMetadata>,
        val context: Context,
        val clockRegistry: ClockRegistry
    ) : RecyclerView.Adapter<ClockRecyclerAdapter.ViewHolder>() {
        class ViewHolder(val view: View, val textView: TextView, val onItemClicked: (Int) -> Unit) :
            RecyclerView.ViewHolder(view) {
            init {
                itemView.setOnClickListener { onItemClicked(absoluteAdapterPosition) }
            }
        }

        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
            val rootView = FrameLayout(viewGroup.context)
            val textView =
                TextView(ContextThemeWrapper(viewGroup.context, R.style.SectionTitleTextStyle))
            textView.setPadding(ITEM_PADDING)
            rootView.addView(textView)
            val lp = RecyclerView.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            rootView.setLayoutParams(lp)
            return ViewHolder(
                rootView,
                textView,
                { clockRegistry.currentClockId = list[it].clockId }
            )
        }

        override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
            viewHolder.textView.text = list[position].name
        }

        override fun getItemCount() = list.size

        companion object {
            val ITEM_PADDING = 40
        }
    }
}
