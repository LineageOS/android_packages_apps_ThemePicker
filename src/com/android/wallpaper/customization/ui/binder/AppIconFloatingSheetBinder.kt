/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.wallpaper.customization.ui.binder

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.customization.picker.common.ui.view.SingleRowListItemSpacing
import com.android.customization.picker.grid.ui.viewmodel.ShapeIconViewModel
import com.android.customization.picker.icon.shared.model.IconStyleModel
import com.android.customization.picker.icon.ui.util.IconStyleViewUtil
import com.android.themepicker.R
import com.android.wallpaper.config.BaseFlags
import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerHomeCustomizationOption.APP_ICONS
import com.android.wallpaper.customization.ui.view.ShapeTileDrawable
import com.android.wallpaper.customization.ui.viewmodel.AppIconPickerViewModel.Tab
import com.android.wallpaper.customization.ui.viewmodel.ThemePickerCustomizationOptionsViewModel
import com.android.wallpaper.picker.common.icon.ui.viewbinder.IconViewBinder
import com.android.wallpaper.picker.common.icon.ui.viewmodel.Icon
import com.android.wallpaper.picker.customization.ui.binder.ColorUpdateBinder
import com.android.wallpaper.picker.customization.ui.view.FloatingToolbar
import com.android.wallpaper.picker.customization.ui.view.adapter.FloatingToolbarTabAdapter
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.option.ui.adapter.OptionItemAdapter2
import com.google.android.material.materialswitch.MaterialSwitch
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

object AppIconFloatingSheetBinder {

    fun bind(
        view: View,
        optionsViewModel: ThemePickerCustomizationOptionsViewModel,
        iconStyleViewUtil: IconStyleViewUtil,
        colorUpdateViewModel: ColorUpdateViewModel,
        lifecycleOwner: LifecycleOwner,
        backgroundDispatcher: CoroutineDispatcher,
    ) {
        val viewModel = optionsViewModel.appIconPickerViewModel
        val isFloatingSheetActive = { optionsViewModel.selectedOption.value == APP_ICONS }

        val isExtendibleThemeManager = BaseFlags.get().isExtendibleThemeManager()
        val tabs = view.requireViewById<FloatingToolbar>(R.id.floating_toolbar)
        val tabAdapter: FloatingToolbarTabAdapter?
        if (isExtendibleThemeManager) {
            tabs.isVisible = true
            val tabContainer =
                tabs.findViewById<ViewGroup>(
                    com.android.wallpaper.R.id.floating_toolbar_tab_container
                )
            ColorUpdateBinder.bind(
                setColor = { color ->
                    DrawableCompat.setTint(DrawableCompat.wrap(tabContainer.background), color)
                },
                color = colorUpdateViewModel.floatingToolbarBackground,
                shouldAnimate = isFloatingSheetActive,
                lifecycleOwner = lifecycleOwner,
            )
            tabAdapter =
                FloatingToolbarTabAdapter(
                        colorUpdateViewModel = WeakReference(colorUpdateViewModel),
                        shouldAnimateColor = isFloatingSheetActive,
                    )
                    .also { tabs.setAdapter(it) }
        } else {
            tabAdapter = null
        }

        val floatingSheetContainer =
            view.requireViewById<ViewGroup>(R.id.floating_sheet_content_container)
        ColorUpdateBinder.bind(
            setColor = { color ->
                DrawableCompat.setTint(
                    DrawableCompat.wrap(floatingSheetContainer.background),
                    color,
                )
            },
            color = colorUpdateViewModel.colorSurfaceBright,
            shouldAnimate = isFloatingSheetActive,
            lifecycleOwner = lifecycleOwner,
        )

        val styleContent = view.requireViewById<View>(R.id.app_icon_style_container)
        val shapeContent = view.requireViewById<View>(R.id.app_shape_content)

        val shapeOptionListAdapter =
            createShapeOptionItemAdapter(
                colorUpdateViewModel = colorUpdateViewModel,
                shouldAnimateColor = isFloatingSheetActive,
                lifecycleOwner = lifecycleOwner,
                backgroundDispatcher = backgroundDispatcher,
            )
        val shapeOptionList =
            view.requireViewById<RecyclerView>(R.id.shape_options).also {
                it.initOptionList(view.context, shapeOptionListAdapter)
            }

        val styleOptionListAdapter =
            createStyleOptionItemAdapter(
                iconStyleViewUtil = iconStyleViewUtil,
                colorUpdateViewModel = colorUpdateViewModel,
                shouldAnimateColor = isFloatingSheetActive,
                lifecycleOwner = lifecycleOwner,
                backgroundDispatcher = backgroundDispatcher,
            )
        val styleOptionList =
            view.requireViewById<RecyclerView>(R.id.icon_style_options).also {
                it.initOptionList(view.context, styleOptionListAdapter)
            }

        val themedIconsSwitch = view.requireViewById<MaterialSwitch>(R.id.themed_icon_toggle)
        val themedIconEntry = view.requireViewById<ViewGroup>(R.id.themed_icon_toggle_entry)
        val themedIconTitle = view.requireViewById<TextView>(R.id.themed_icon_toggle_title)
        val globalIconShapeSwitch =
            view.requireViewById<MaterialSwitch>(R.id.global_icon_shape_toggle)
        val globalIconShapeEntry =
            view.requireViewById<ViewGroup>(R.id.global_icon_shape_toggle_entry)
        val globalIconShapeTitle =
            view.requireViewById<TextView>(R.id.global_icon_shape_toggle_title)

        data class FloatingSheetHeightsViewModel(
            val styleContentHeight: Int? = null,
            val shapeContentHeight: Int? = null,
        )
        val floatingSheetHeights: MutableStateFlow<FloatingSheetHeightsViewModel> =
            MutableStateFlow(FloatingSheetHeightsViewModel())

        if (isExtendibleThemeManager) {
            styleContent.viewTreeObserver.addOnGlobalLayoutListener(
                object : OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        if (
                            styleContent.height != 0 &&
                                floatingSheetHeights.value.styleContentHeight != styleContent.height
                        ) {
                            floatingSheetHeights.value =
                                floatingSheetHeights.value.copy(
                                    styleContentHeight = styleContent.height
                                )
                            // Keep the height of the style floating sheet fixed so text renders
                            // correctly after changing tabs.
                            styleContent.layoutParams =
                                styleContent.layoutParams.apply { height = styleContent.height }
                            styleContent.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        }
                    }
                }
            )

            shapeContent.viewTreeObserver.addOnGlobalLayoutListener(
                object : OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        if (
                            shapeContent.height != 0 &&
                                floatingSheetHeights.value.shapeContentHeight != shapeContent.height
                        ) {
                            floatingSheetHeights.value =
                                floatingSheetHeights.value.copy(
                                    shapeContentHeight = shapeContent.height
                                )
                            shapeContent.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        }
                    }
                }
            )
        }

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.shapeOptions.collect { options ->
                        shapeOptionListAdapter.setItems(options) {
                            val indexToFocus =
                                options.indexOfFirst { it.isSelected.value }.coerceAtLeast(0)
                            (shapeOptionList.layoutManager as LinearLayoutManager).scrollToPosition(
                                indexToFocus
                            )
                        }
                    }
                }

                launch {
                    viewModel.isShapeOptionsAvailable.collect { isAvailable ->
                        globalIconShapeEntry.isVisible = isAvailable
                        globalIconShapeSwitch.isEnabled = isAvailable
                    }
                }

                launch {
                    var switchBinding: SwitchColorBinder.Binding? = null
                    var titleBinding: ColorUpdateBinder.Binding? = null
                    viewModel.previewingGlobalIconShapeEnabled.collect {
                        globalIconShapeSwitch.isChecked = it
                        titleBinding?.destroy()
                        titleBinding =
                            bindTitleColor(
                                globalIconShapeTitle,
                                colorUpdateViewModel,
                                isFloatingSheetActive,
                                lifecycleOwner,
                            )
                        switchBinding?.destroy()
                        switchBinding =
                            SwitchColorBinder.bind(
                                switch = globalIconShapeSwitch,
                                isChecked = it,
                                colorUpdateViewModel = colorUpdateViewModel,
                                shouldAnimateColor = isFloatingSheetActive,
                                lifecycleOwner = lifecycleOwner,
                            )
                    }
                }

                launch {
                    viewModel.toggleGlobalIconShape.collect { toggle ->
                        globalIconShapeSwitch.setOnCheckedChangeListener { _, _ ->
                            launch { toggle.invoke() }
                        }
                    }
                }

                if (isExtendibleThemeManager) {
                    themedIconEntry.isVisible = false

                    launch {
                        viewModel.styleOptions.collect { options ->
                            styleOptionListAdapter.setItems(options) {
                                val indexToFocus =
                                    options.indexOfFirst { it.isSelected.value }.coerceAtLeast(0)
                                (styleOptionList.layoutManager as LinearLayoutManager)
                                    .scrollToPosition(indexToFocus)
                            }
                        }
                    }

                    launch {
                        viewModel.tabs.collect {
                            if (it.size > 1) {
                                tabAdapter?.submitList(it)
                            } else {
                                tabs.isVisible = false
                            }
                        }
                    }

                    launch {
                        val verticalPadding =
                            view.resources.getDimensionPixelSize(
                                R.dimen.floating_sheet_content_vertical_padding
                            )
                        var currentTab: Tab? = null
                        combine(floatingSheetHeights, viewModel.selectedTab, ::Pair).collect {
                            (heights, selectedTab) ->
                            val (styleContentHeight, shapeContentHeight) = heights
                            styleContentHeight ?: return@collect
                            shapeContentHeight ?: return@collect
                            selectedTab ?: return@collect

                            styleContent.isVisible = (currentTab == Tab.STYLE)
                            shapeContent.isVisible = (currentTab == Tab.SHAPE)

                            val fromHeight = floatingSheetContainer.height
                            val toHeight =
                                when (selectedTab) {
                                    Tab.STYLE -> styleContentHeight
                                    Tab.SHAPE -> shapeContentHeight
                                } + 2 * verticalPadding
                            val currentContent: View? =
                                when (currentTab) {
                                    Tab.STYLE -> styleContent
                                    Tab.SHAPE -> shapeContent
                                    else -> null
                                }
                            val selectedContent: View =
                                when (selectedTab) {
                                    Tab.STYLE -> styleContent
                                    Tab.SHAPE -> shapeContent
                                }
                            FloatingSheetHeightAnimationBinder.bind(
                                floatingSheetContainer,
                                fromHeight,
                                toHeight,
                                currentContent,
                                selectedContent,
                            )
                            currentTab = selectedTab
                        }
                    }
                } else {
                    styleContent.isVisible = false

                    launch {
                        viewModel.isShapeOptionsAvailable.collect { shapeAvailable ->
                            shapeContent.isVisible = shapeAvailable
                        }
                    }

                    launch {
                        viewModel.isThemedIconAvailable.collect { isAvailable ->
                            themedIconEntry.isVisible = isAvailable
                            themedIconsSwitch.isEnabled = isAvailable
                        }
                    }

                    launch {
                        var switchBinding: SwitchColorBinder.Binding? = null
                        var titleBinding: ColorUpdateBinder.Binding? = null
                        viewModel.previewingIsThemeIconEnabled.collect {
                            themedIconsSwitch.isChecked = it
                            titleBinding?.destroy()
                            titleBinding =
                                bindTitleColor(
                                    themedIconTitle,
                                    colorUpdateViewModel,
                                    isFloatingSheetActive,
                                    lifecycleOwner,
                                )
                            switchBinding?.destroy()
                            switchBinding =
                                SwitchColorBinder.bind(
                                    switch = themedIconsSwitch,
                                    isChecked = it,
                                    colorUpdateViewModel = colorUpdateViewModel,
                                    shouldAnimateColor = isFloatingSheetActive,
                                    lifecycleOwner = lifecycleOwner,
                                )
                        }
                    }

                    launch {
                        viewModel.toggleThemedIcon.collect {
                            themedIconsSwitch.setOnCheckedChangeListener { _, _ ->
                                launch { it.invoke() }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun bindTitleColor(
        title: TextView,
        colorUpdateViewModel: ColorUpdateViewModel,
        shouldAnimateColor: () -> Boolean,
        lifecycleOwner: LifecycleOwner,
    ): ColorUpdateBinder.Binding {
        val titleBinding =
            ColorUpdateBinder.bind(
                setColor = { color -> title.setTextColor(color) },
                color = colorUpdateViewModel.colorOnSurface,
                shouldAnimate = shouldAnimateColor,
                lifecycleOwner = lifecycleOwner,
            )
        return object : ColorUpdateBinder.Binding {
            override fun destroy() {
                titleBinding.destroy()
            }
        }
    }

    private fun createStyleOptionItemAdapter(
        iconStyleViewUtil: IconStyleViewUtil,
        colorUpdateViewModel: ColorUpdateViewModel,
        shouldAnimateColor: () -> Boolean,
        lifecycleOwner: LifecycleOwner,
        backgroundDispatcher: CoroutineDispatcher,
    ): OptionItemAdapter2<IconStyleModel> {
        return OptionItemAdapter2(
            layoutResourceId = R.layout.icon_style_option2,
            lifecycleOwner = lifecycleOwner,
            backgroundDispatcher = backgroundDispatcher,
            bindPayload = { view: View, iconStyleModel: IconStyleModel ->
                val optionIcon = view.requireViewById<ViewGroup>(R.id.option_icon)
                val buttonIcon = view.requireViewById<ViewGroup>(R.id.button_icon)
                val icon = iconStyleModel.icon
                if (iconStyleModel.isExternalLink) {
                    optionIcon.visibility = View.GONE
                    buttonIcon.visibility = View.VISIBLE
                    val imageView = view.requireViewById<ImageView>(R.id.button_foreground)
                    icon?.let { IconViewBinder.bind(imageView, it) }
                    view.setOnClickListener {
                        iconStyleViewUtil.getOnClick(iconStyleModel.iconStyle)?.invoke()
                    }
                } else {
                    optionIcon.visibility = View.VISIBLE
                    buttonIcon.visibility = View.GONE
                    val imageView =
                        view.requireViewById<ImageView>(com.android.wallpaper.R.id.foreground)
                    icon?.let { IconViewBinder.bind(imageView, it) }
                }
                // If the icon is a themed icon, bind its foreground and background color
                val disposableHandle =
                    if (iconStyleModel.isThemedIcon) {
                        ((icon as? Icon.Loaded)?.drawable as? ShapeTileDrawable)?.let {
                            ShapeIconViewBinder.bindPreviewIconColor(
                                shapeTileDrawable = it,
                                colorUpdateViewModel = colorUpdateViewModel,
                                shouldAnimateColor = shouldAnimateColor,
                                lifecycleOwner = lifecycleOwner,
                            )
                        }
                    } else if (iconStyleModel.isExternalLink) {
                        ShapeIconViewBinder.bindButtonIconColor(
                            foreground = buttonIcon.requireViewById(R.id.button_foreground),
                            background = buttonIcon.requireViewById(R.id.button_background),
                            colorUpdateViewModel = colorUpdateViewModel,
                            shouldAnimateColor = shouldAnimateColor,
                            lifecycleOwner = lifecycleOwner,
                        )
                    } else null
                return@OptionItemAdapter2 disposableHandle
            },
            colorUpdateViewModel = WeakReference(colorUpdateViewModel),
            shouldAnimateColor = shouldAnimateColor,
        )
    }

    private fun createShapeOptionItemAdapter(
        colorUpdateViewModel: ColorUpdateViewModel,
        shouldAnimateColor: () -> Boolean,
        lifecycleOwner: LifecycleOwner,
        backgroundDispatcher: CoroutineDispatcher,
    ): OptionItemAdapter2<ShapeIconViewModel> =
        OptionItemAdapter2(
            layoutResourceId = R.layout.shape_option2,
            lifecycleOwner = lifecycleOwner,
            backgroundDispatcher = backgroundDispatcher,
            bindPayload = { view: View, shapeIcon: ShapeIconViewModel ->
                val imageView = view.findViewById(R.id.foreground) as? ImageView
                imageView?.let { ShapeIconViewBinder.bind(imageView, shapeIcon) }
                return@OptionItemAdapter2 null
            },
            colorUpdateViewModel = WeakReference(colorUpdateViewModel),
            shouldAnimateColor = shouldAnimateColor,
        )

    private fun RecyclerView.initOptionList(context: Context, adapter: OptionItemAdapter2<*>) {
        apply {
            this.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            addItemDecoration(
                SingleRowListItemSpacing(
                    edgeItemSpacePx =
                        context.resources.getDimensionPixelSize(
                            R.dimen.floating_sheet_content_horizontal_padding
                        ),
                    itemHorizontalSpacePx =
                        context.resources.getDimensionPixelSize(
                            R.dimen.floating_sheet_list_item_horizontal_space
                        ),
                )
            )
            this.adapter = adapter
        }
    }
}
