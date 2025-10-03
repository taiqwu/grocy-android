/*
 * This file is part of Grocy Android.
 *
 * Grocy Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grocy Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2020-2024 by Patrick Zedler and Dominic Zedler
 * Copyright (c) 2024-2025 by Patrick Zedler
 */

package xyz.zedler.patrick.grocy.fragment;

import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.divider.MaterialDivider;
import java.util.List;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.Constants.CONTRAST;
import xyz.zedler.patrick.grocy.Constants.SETTINGS;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.APPEARANCE;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.Constants.THEME;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentSettingsCatAppearanceBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.LanguagesBottomSheet;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.LocaleUtil;
import xyz.zedler.patrick.grocy.util.RestartUtil;
import xyz.zedler.patrick.grocy.util.UiUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;
import xyz.zedler.patrick.grocy.view.ThemeSelectionCardView;
import xyz.zedler.patrick.grocy.viewmodel.SettingsViewModel;

public class SettingsCatAppearanceFragment extends BaseFragment {

  private final static String TAG = SettingsCatAppearanceFragment.class.getSimpleName();

  private FragmentSettingsCatAppearanceBinding binding;
  private MainActivity activity;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState
  ) {
    binding = FragmentSettingsCatAppearanceBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) requireActivity();
    SettingsViewModel viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
    binding.setActivity(activity);
    binding.setFragment(this);
    binding.setViewModel(viewModel);
    binding.setClickUtil(new ClickUtil());
    binding.setLifecycleOwner(getViewLifecycleOwner());

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBar);
    systemBarBehavior.setScroll(binding.scroll, binding.constraint);
    systemBarBehavior.setUp();
    activity.setSystemBarBehavior(systemBarBehavior);

    binding.toolbar.setNavigationOnClickListener(v -> activity.navUtil.navigateUp());

    viewModel.getEventHandler().observe(getViewLifecycleOwner(), event -> {
      if (event.getType() == Event.SNACKBAR_MESSAGE) {
        activity.showSnackbar(
            ((SnackbarMessage) event).getSnackbar(activity.binding.coordinatorMain)
        );
      } else if (event.getType() == Event.BOTTOM_SHEET) {
        BottomSheetEvent bottomSheetEvent = (BottomSheetEvent) event;
        activity.showBottomSheet(bottomSheetEvent.getBottomSheet(), event.getBundle());
      }
    });

    setUpThemeSelection();
    updateShortcuts();

    int id;
    switch (getSharedPrefs().getInt(
        SETTINGS.APPEARANCE.DARK_MODE, SETTINGS_DEFAULT.APPEARANCE.DARK_MODE)
    ) {
      case AppCompatDelegate.MODE_NIGHT_NO:
        id = R.id.button_other_theme_light;
        break;
      case AppCompatDelegate.MODE_NIGHT_YES:
        id = R.id.button_other_theme_dark;
        break;
      default:
        id = R.id.button_other_theme_auto;
        break;
    }
    binding.toggleOtherTheme.check(id);
    binding.toggleOtherTheme.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
      if (!isChecked) {
        return;
      }
      int pref;
      if (checkedId == R.id.button_other_theme_light) {
        pref = AppCompatDelegate.MODE_NIGHT_NO;
      } else if (checkedId == R.id.button_other_theme_dark) {
        pref = AppCompatDelegate.MODE_NIGHT_YES;
      } else {
        pref = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
      }
      getSharedPrefs().edit().putInt(SETTINGS.APPEARANCE.DARK_MODE, pref).apply();
      performHapticClick();
      RestartUtil.restartToApply(activity, 0, getInstanceState());
    });

    int idContrast;
    switch (getSharedPrefs().getString(
        APPEARANCE.UI_CONTRAST, SETTINGS_DEFAULT.APPEARANCE.UI_CONTRAST)) {
      case CONTRAST.MEDIUM:
        idContrast = R.id.button_other_contrast_medium;
        break;
      case CONTRAST.HIGH:
        idContrast = R.id.button_other_contrast_high;
        break;
      default:
        idContrast = R.id.button_other_contrast_standard;
        break;
    }
    binding.toggleOtherContrast.check(idContrast);
    binding.toggleOtherContrast.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
      if (!isChecked) {
        return;
      }
      String pref;
      if (checkedId == R.id.button_other_contrast_medium) {
        pref = CONTRAST.MEDIUM;
      } else if (checkedId == R.id.button_other_contrast_high) {
        pref = CONTRAST.HIGH;
      } else {
        pref = CONTRAST.STANDARD;
      }
      getSharedPrefs().edit().putString(APPEARANCE.UI_CONTRAST, pref).apply();
      performHapticClick();
      ViewUtil.startIcon(binding.imageSettingsContrast);
      RestartUtil.restartToApply(activity, 0, getInstanceState());
    });
    boolean enabled = !getSharedPrefs().getString(
        APPEARANCE.THEME, SETTINGS_DEFAULT.APPEARANCE.THEME
    ).equals(THEME.DYNAMIC);
    binding.toggleOtherContrast.setEnabled(enabled);
    binding.textSettingsContrastDynamic.setVisibility(enabled ? View.GONE : View.VISIBLE);
    binding.textSettingsContrastDynamic.setText(
        VERSION.SDK_INT >= VERSION_CODES.UPSIDE_DOWN_CAKE
            ? R.string.setting_contrast_dynamic
            : R.string.setting_contrast_dynamic_unsupported
    );

    activity.getScrollBehavior().setNestedOverScrollFixEnabled(false);
    activity.getScrollBehavior().setUpScroll(
        binding.appBar, false, binding.scroll, false
    );
    activity.getScrollBehavior().setBottomBarVisibility(
        activity.hasBottomNavigationIcon(), !activity.hasBottomNavigationIcon()
    );
    activity.updateBottomAppBar(false, R.menu.menu_empty);

    setForPreviousDestination(Constants.ARGUMENT.ANIMATED, false);
  }

  public String getLanguage() {
    return LocaleUtil.followsSystem()
        ? getString(R.string.setting_language_system)
        : LocaleUtil.getLocaleName();
  }

  public void showLanguageSelection() {
    ViewUtil.startIcon(binding.imageLanguage);
    activity.showBottomSheet(new LanguagesBottomSheet());
  }

  private void setUpThemeSelection() {
    boolean hasDynamic = DynamicColors.isDynamicColorAvailable();
    ViewGroup container = binding.linearOtherThemeContainer;
    for (int i = hasDynamic ? -1 : 0; i < 4; i++) {
      String name;
      int resId;
      switch (i) {
        case -1:
          name = THEME.DYNAMIC;
          resId = -1;
          break;
        case 0:
          name = THEME.RED;
          resId = getContrastThemeResId(
              R.style.Theme_Grocy_Red,
              R.style.ThemeOverlay_Grocy_Red_MediumContrast,
              R.style.ThemeOverlay_Grocy_Red_HighContrast
          );
          break;
        case 1:
          name = THEME.YELLOW;
          resId = getContrastThemeResId(
              R.style.Theme_Grocy_Yellow,
              R.style.ThemeOverlay_Grocy_Yellow_MediumContrast,
              R.style.ThemeOverlay_Grocy_Yellow_HighContrast
          );
          break;
        case 3:
          name = THEME.BLUE;
          resId = getContrastThemeResId(
              R.style.Theme_Grocy_Blue,
              R.style.ThemeOverlay_Grocy_Blue_MediumContrast,
              R.style.ThemeOverlay_Grocy_Blue_HighContrast
          );
          break;
        default:
          name = THEME.GREEN;
          resId = getContrastThemeResId(
              R.style.Theme_Grocy_Green,
              R.style.ThemeOverlay_Grocy_Green_MediumContrast,
              R.style.ThemeOverlay_Grocy_Green_HighContrast
          );
          break;
      }

      ThemeSelectionCardView card = new ThemeSelectionCardView(activity);
      card.setNestedContext(
          i == -1 && VERSION.SDK_INT >= VERSION_CODES.S
              ? DynamicColors.wrapContextIfAvailable(activity)
              : new ContextThemeWrapper(activity, resId)
      );
      card.setOnClickListener(v -> {
        if (!card.isChecked()) {
          ViewUtil.uncheckAllChildren(container);
          card.setChecked(true);
          card.startCheckedIcon();
          ViewUtil.startIcon(binding.imageOtherTheme);
          performHapticClick();
          getSharedPrefs().edit().putString(SETTINGS.APPEARANCE.THEME, name).apply();
          RestartUtil.restartToApply(activity, 100, getInstanceState());
        }
      });

      String selected = getSharedPrefs().getString(
          SETTINGS.APPEARANCE.THEME, SETTINGS_DEFAULT.APPEARANCE.THEME
      );
      boolean isSelected;
      if (selected.isEmpty()) {
        isSelected = hasDynamic ? name.equals(THEME.DYNAMIC) : name.equals(THEME.GREEN);
      } else {
        isSelected = selected.equals(name);
      }
      card.setChecked(isSelected);
      container.addView(card);

      if (hasDynamic && i == -1) {
        MaterialDivider divider = new MaterialDivider(activity);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
            UiUtil.dpToPx(activity, 1), UiUtil.dpToPx(activity, 40)
        );
        int marginLeft, marginRight;
        if (UiUtil.isLayoutRtl(activity)) {
          marginLeft = UiUtil.dpToPx(activity, 8);
          marginRight = UiUtil.dpToPx(activity, 4);
        } else {
          marginLeft = UiUtil.dpToPx(activity, 4);
          marginRight = UiUtil.dpToPx(activity, 8);
        }
        layoutParams.setMargins(marginLeft, 0, marginRight, 0);
        layoutParams.gravity = Gravity.CENTER_VERTICAL;
        divider.setLayoutParams(layoutParams);
        container.addView(divider);
      }
    }

    Bundle bundleInstanceState = activity.getIntent().getBundleExtra(ARGUMENT.INSTANCE_STATE);
    if (bundleInstanceState != null) {
      binding.scrollOtherTheme.scrollTo(
          bundleInstanceState.getInt(ARGUMENT.SCROLL_POSITION + 1, 0), 0
      );
    }
  }

  private Bundle getInstanceState() {
    Bundle bundle = new Bundle();
    if (binding != null) {
      bundle.putInt(ARGUMENT.SCROLL_POSITION + 1, binding.scrollOtherTheme.getScrollX());
    }
    return bundle;
  }

  @Override
  public void updateShortcuts() {
    String subtitleShortcuts;
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
      ShortcutManager shortcutManager = activity.getSystemService(ShortcutManager.class);
      List<ShortcutInfo> shortcutInfos = shortcutManager.getDynamicShortcuts();
      StringBuilder shortcutLabels = new StringBuilder();
      for (ShortcutInfo shortcutInfo : shortcutInfos) {
        shortcutLabels.append(shortcutInfo.getShortLabel());
        if (shortcutInfo != shortcutInfos.get(shortcutInfos.size() - 1)) {
          shortcutLabels.append(", ");
        }
      }
      if (shortcutLabels.length() != 0) {
        subtitleShortcuts = shortcutLabels.toString();
      } else {
        subtitleShortcuts = getString(R.string.subtitle_none_selected);
      }
    } else {
      subtitleShortcuts = getString(R.string.subtitle_not_supported);
    }
    binding.subtitleShortcuts.setText(subtitleShortcuts);
  }

  private int getContrastThemeResId(
      @StyleRes int resIdStandard,
      @StyleRes int resIdMedium,
      @StyleRes int resIdHigh
  ) {
    switch (getSharedPrefs().getString(APPEARANCE.UI_CONTRAST, SETTINGS_DEFAULT.APPEARANCE.UI_CONTRAST)) {
      case CONTRAST.MEDIUM:
        return resIdMedium;
      case CONTRAST.HIGH:
        return resIdHigh;
      default:
        return resIdStandard;
    }
  }
}
