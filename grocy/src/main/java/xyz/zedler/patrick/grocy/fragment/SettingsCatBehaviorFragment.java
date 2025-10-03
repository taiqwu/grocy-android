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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.BEHAVIOR;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentSettingsCatBehaviorBinding;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.HapticUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;
import xyz.zedler.patrick.grocy.viewmodel.SettingsViewModel;

public class SettingsCatBehaviorFragment extends BaseFragment {

  private final static String TAG = SettingsCatBehaviorFragment.class.getSimpleName();

  private FragmentSettingsCatBehaviorBinding binding;
  private MainActivity activity;
  private SettingsViewModel viewModel;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState
  ) {
    binding = FragmentSettingsCatBehaviorBinding.inflate(inflater, container, false);
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
    viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
    binding.setActivity(activity);
    binding.setFragment(this);
    binding.setViewModel(viewModel);
    binding.setSharedPrefs(PreferenceManager.getDefaultSharedPreferences(activity));
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
      } else if (event.getType() == Event.UPDATE_BOTTOM_APP_BAR) {
        activity.updateBottomNavigationMenuButton();
        activity.getScrollBehavior().setBottomBarVisibility(
            activity.hasBottomNavigationIcon(), !activity.hasBottomNavigationIcon()
        );
      }
    });

    binding.switchHaptic.setChecked(
        getSharedPrefs().getBoolean(
            Constants.SETTINGS.BEHAVIOR.HAPTIC, HapticUtil.areSystemHapticsTurnedOn(activity)
        )
    );
    binding.switchHaptic.setOnCheckedChangeListener((buttonView, isChecked) -> {
      activity.setHapticEnabled(isChecked);
      getSharedPrefs().edit().putBoolean(Constants.SETTINGS.BEHAVIOR.HAPTIC, isChecked).apply();
      performHapticClick();
      ViewUtil.startIcon(binding.imageHaptic);
    });
    binding.switchHaptic.jumpDrawablesToCurrentState();

    binding.switchBeginner.post(() -> {
      binding.switchBeginner.jumpDrawablesToCurrentState();
      binding.switchBottomMenu.jumpDrawablesToCurrentState();
      binding.switchExpand.jumpDrawablesToCurrentState();
      binding.switchKeepScreenOnRecipes.jumpDrawablesToCurrentState();
      binding.switchTurnOnQuickMode.jumpDrawablesToCurrentState();
      binding.switchDateKeyboardInput.jumpDrawablesToCurrentState();
      binding.switchDateKeyboardReverse.jumpDrawablesToCurrentState();
      binding.switchOpenFoodFacts.jumpDrawablesToCurrentState();
      binding.switchSpeedUpStart.jumpDrawablesToCurrentState();
    });

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

  public String getMessageDuration() {
    return getResources().getQuantityString(
        R.plurals.property_seconds_num,
        viewModel.getMessageDuration(),
        viewModel.getMessageDuration()
    );
  }

  @Override
  public void saveInput(String text, Bundle argsBundle) {
    String type = argsBundle.getString(ARGUMENT.TYPE);
    if (type == null) return;
    if (type.equals(BEHAVIOR.MESSAGE_DURATION) && NumUtil.isStringInt(text)) {
      viewModel.setMessageDuration(Integer.parseInt(text));
      binding.textMessageDuration.setText(getMessageDuration());
    }
  }
}
