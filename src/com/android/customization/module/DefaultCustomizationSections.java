package com.android.customization.module;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import com.android.customization.model.color.ColorSectionController;
import com.android.customization.model.color.ColorSectionController2;
import com.android.customization.model.grid.GridOptionsManager;
import com.android.customization.model.grid.GridSectionController;
import com.android.customization.model.mode.DarkModeSectionController;
import com.android.customization.model.themedicon.ThemedIconSectionController;
import com.android.customization.model.themedicon.ThemedIconSwitchProvider;
import com.android.customization.picker.clock.data.repository.ClockRegistryProvider;
import com.android.customization.picker.notifications.ui.section.NotificationSectionController;
import com.android.customization.picker.notifications.ui.viewmodel.NotificationSectionViewModel;
import com.android.customization.picker.preview.ui.section.PreviewWithClockCarouselSectionController;
import com.android.customization.picker.preview.ui.section.PreviewWithClockCarouselSectionController.ClockCarouselViewModelProvider;
import com.android.customization.picker.preview.ui.section.PreviewWithClockCarouselSectionController.ClockViewFactoryProvider;
import com.android.customization.picker.quickaffordance.domain.interactor.KeyguardQuickAffordancePickerInteractor;
import com.android.customization.picker.quickaffordance.ui.section.KeyguardQuickAffordanceSectionController;
import com.android.customization.picker.quickaffordance.ui.viewmodel.KeyguardQuickAffordancePickerViewModel;
import com.android.customization.picker.settings.ui.section.MoreSettingsSectionController;
import com.android.wallpaper.config.BaseFlags;
import com.android.wallpaper.model.CustomizationSectionController;
import com.android.wallpaper.model.CustomizationSectionController.CustomizationSectionNavigationController;
import com.android.wallpaper.model.PermissionRequester;
import com.android.wallpaper.model.WallpaperColorsViewModel;
import com.android.wallpaper.model.WallpaperPreviewNavigator;
import com.android.wallpaper.model.WallpaperSectionController;
import com.android.wallpaper.model.WorkspaceViewModel;
import com.android.wallpaper.module.CurrentWallpaperInfoFactory;
import com.android.wallpaper.module.CustomizationSections;
import com.android.wallpaper.picker.customization.ui.section.ConnectedSectionController;
import com.android.wallpaper.picker.customization.ui.section.ScreenPreviewSectionController;
import com.android.wallpaper.picker.customization.ui.section.WallpaperQuickSwitchSectionController;
import com.android.wallpaper.picker.customization.ui.viewmodel.WallpaperQuickSwitchViewModel;
import com.android.wallpaper.util.DisplayUtils;

import java.util.ArrayList;
import java.util.List;

/** {@link CustomizationSections} for the customization picker. */
public final class DefaultCustomizationSections implements CustomizationSections {

    private final KeyguardQuickAffordancePickerInteractor mKeyguardQuickAffordancePickerInteractor;
    private final KeyguardQuickAffordancePickerViewModel.Factory
            mKeyguardQuickAffordancePickerViewModelFactory;
    private final NotificationSectionViewModel.Factory mNotificationSectionViewModelFactory;
    private final BaseFlags mFlags;
    private final ClockRegistryProvider mClockRegistryProvider;
    private final PreviewWithClockCarouselSectionController.ClockCarouselViewModelProvider
            mClockCarouselViewModelProvider;
    private final PreviewWithClockCarouselSectionController.ClockViewFactoryProvider
            mClockViewFactoryProvider;

    public DefaultCustomizationSections(
            KeyguardQuickAffordancePickerInteractor keyguardQuickAffordancePickerInteractor,
            KeyguardQuickAffordancePickerViewModel.Factory
                    keyguardQuickAffordancePickerViewModelFactory,
            NotificationSectionViewModel.Factory notificationSectionViewModelFactory,
            BaseFlags flags,
            ClockRegistryProvider clockRegistryProvider,
            ClockCarouselViewModelProvider clockCarouselViewModelProvider,
            ClockViewFactoryProvider clockViewFactoryProvider) {
        mKeyguardQuickAffordancePickerInteractor = keyguardQuickAffordancePickerInteractor;
        mKeyguardQuickAffordancePickerViewModelFactory =
                keyguardQuickAffordancePickerViewModelFactory;
        mNotificationSectionViewModelFactory = notificationSectionViewModelFactory;
        mFlags = flags;
        mClockRegistryProvider = clockRegistryProvider;
        mClockCarouselViewModelProvider = clockCarouselViewModelProvider;
        mClockViewFactoryProvider = clockViewFactoryProvider;
    }

    @Override
    public List<CustomizationSectionController<?>> getRevampedUISectionControllersForScreen(
            Screen screen,
            FragmentActivity activity,
            LifecycleOwner lifecycleOwner,
            WallpaperColorsViewModel wallpaperColorsViewModel,
            WorkspaceViewModel workspaceViewModel,
            PermissionRequester permissionRequester,
            WallpaperPreviewNavigator wallpaperPreviewNavigator,
            CustomizationSectionNavigationController sectionNavigationController,
            @Nullable Bundle savedInstanceState,
            CurrentWallpaperInfoFactory wallpaperInfoFactory,
            DisplayUtils displayUtils,
            WallpaperQuickSwitchViewModel wallpaperQuickSwitchViewModel) {
        List<CustomizationSectionController<?>> sectionControllers = new ArrayList<>();

        // Wallpaper section.
        sectionControllers.add(
                mFlags.isCustomClocksEnabled(activity)
                        ? new PreviewWithClockCarouselSectionController(
                        activity,
                        lifecycleOwner,
                        screen,
                        wallpaperInfoFactory,
                        wallpaperColorsViewModel,
                        displayUtils,
                        mClockRegistryProvider,
                        mClockCarouselViewModelProvider,
                        mClockViewFactoryProvider)
                        : new ScreenPreviewSectionController(
                                activity,
                                lifecycleOwner,
                                screen,
                                wallpaperInfoFactory,
                                wallpaperColorsViewModel,
                                displayUtils));

        sectionControllers.add(
                new ConnectedSectionController(
                        // Theme color section.
                        new ColorSectionController2(
                                activity,
                                wallpaperColorsViewModel,
                                lifecycleOwner,
                                sectionNavigationController),
                        // Wallpaper quick switch section.
                        new WallpaperQuickSwitchSectionController(
                                screen,
                                wallpaperQuickSwitchViewModel,
                                lifecycleOwner,
                                sectionNavigationController),
                        /* reverseOrderWhenHorizontal= */ true));

        switch (screen) {
            case LOCK_SCREEN:
                // Lock screen quick affordances section.
                sectionControllers.add(
                        new KeyguardQuickAffordanceSectionController(
                                sectionNavigationController,
                                mKeyguardQuickAffordancePickerInteractor,
                                new ViewModelProvider(
                                        activity,
                                        mKeyguardQuickAffordancePickerViewModelFactory)
                                        .get(KeyguardQuickAffordancePickerViewModel.class),
                                lifecycleOwner));

                // Notifications section.
                sectionControllers.add(
                        new NotificationSectionController(
                                new ViewModelProvider(
                                        activity,
                                        mNotificationSectionViewModelFactory)
                                        .get(NotificationSectionViewModel.class),
                                lifecycleOwner));

                // More settings section.
                sectionControllers.add(new MoreSettingsSectionController());
                break;

            case HOME_SCREEN:
                // Dark/Light theme section.
                sectionControllers.add(new DarkModeSectionController(activity,
                        lifecycleOwner.getLifecycle()));

                // Themed app icon section.
                sectionControllers.add(new ThemedIconSectionController(
                        ThemedIconSwitchProvider.getInstance(activity), workspaceViewModel,
                        savedInstanceState));

                // App grid section.
                sectionControllers.add(new GridSectionController(
                        GridOptionsManager.getInstance(activity), sectionNavigationController));
                break;
        }

        return sectionControllers;
    }

    @Override
    public List<CustomizationSectionController<?>> getAllSectionControllers(
            FragmentActivity activity,
            LifecycleOwner lifecycleOwner,
            WallpaperColorsViewModel wallpaperColorsViewModel,
            WorkspaceViewModel workspaceViewModel,
            PermissionRequester permissionRequester,
            WallpaperPreviewNavigator wallpaperPreviewNavigator,
            CustomizationSectionNavigationController sectionNavigationController,
            @Nullable Bundle savedInstanceState,
            DisplayUtils displayUtils) {
        List<CustomizationSectionController<?>> sectionControllers = new ArrayList<>();

        // Wallpaper section.
        sectionControllers.add(new WallpaperSectionController(
                activity, lifecycleOwner, permissionRequester, wallpaperColorsViewModel,
                workspaceViewModel, sectionNavigationController, wallpaperPreviewNavigator,
                savedInstanceState, displayUtils));

        // Theme color section.
        sectionControllers.add(new ColorSectionController(
                activity, wallpaperColorsViewModel, lifecycleOwner, savedInstanceState));

        // Dark/Light theme section.
        sectionControllers.add(new DarkModeSectionController(activity,
                lifecycleOwner.getLifecycle()));

        // Themed app icon section.
        sectionControllers.add(new ThemedIconSectionController(
                ThemedIconSwitchProvider.getInstance(activity), workspaceViewModel,
                savedInstanceState));

        // App grid section.
        sectionControllers.add(new GridSectionController(
                GridOptionsManager.getInstance(activity), sectionNavigationController));

        return sectionControllers;
    }
}
