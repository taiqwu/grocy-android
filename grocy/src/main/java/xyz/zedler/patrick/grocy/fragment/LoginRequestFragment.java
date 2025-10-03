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
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentLoginRequestBinding;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.UiUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;
import xyz.zedler.patrick.grocy.viewmodel.LoginRequestViewModel;

public class LoginRequestFragment extends BaseFragment {

  private FragmentLoginRequestBinding binding;
  private MainActivity activity;
  private LoginRequestViewModel viewModel;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentLoginRequestBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  @Override
  public void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) requireActivity();
    LoginRequestFragmentArgs args = LoginRequestFragmentArgs.fromBundle(requireArguments());
    viewModel = new ViewModelProvider(this, new LoginRequestViewModel
        .LoginViewModelFactory(activity.getApplication(), args)
    ).get(LoginRequestViewModel.class);
    binding.setViewModel(viewModel);
    binding.setActivity(activity);
    binding.setFragment(this);
    binding.setClickUtil(new ClickUtil());
    binding.setLifecycleOwner(getViewLifecycleOwner());

    viewModel.getEventHandler().observeEvent(getViewLifecycleOwner(), event -> {
      if (event.getType() == Event.SNACKBAR_MESSAGE) {
        activity.showSnackbar(
            ((SnackbarMessage) event).getSnackbar(activity.binding.coordinatorMain)
        );
      } else if (event.getType() == Event.LOGIN_SUCCESS) {
        // BottomAppBar should now be visible when navigating
        activity.getScrollBehavior().setCanBottomAppBarBeVisible(true);

        activity.updateGrocyApi();
        activity.netUtil.createWebSocketClient();
        activity.netUtil.resetHassSessionTimer();
        new Handler().postDelayed(this::navigateToStartDestination, 500);
      } else if (event.getType() == Event.BOTTOM_SHEET) {
        BottomSheetEvent bottomSheetEvent = (BottomSheetEvent) event;
        activity.showBottomSheet(bottomSheetEvent.getBottomSheet(), event.getBundle());
      }
    });

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setScroll(binding.scroll, binding.linearContainerScroll);
    systemBarBehavior.setUp();
    activity.setSystemBarBehavior(systemBarBehavior);

    ViewUtil.startIcon(binding.imageLogo);

    activity.getScrollBehavior().setNestedOverScrollFixEnabled(false);
    activity.getScrollBehavior().setProvideTopScroll(false);
    activity.getScrollBehavior().setCanBottomAppBarBeVisible(false);
    activity.getScrollBehavior().setBottomBarVisibility(false, true, false);

    if (UiUtil.areAnimationsEnabled(activity)) {
      new Handler().postDelayed(() -> login(true), 300);
    } else {
      login(true);
    }
  }

  private void navigateToStartDestination() {
    activity.navUtil.updateStartDestination();
    NavOptions.Builder builder = new NavOptions.Builder();
    builder.setPopUpTo(R.id.navigation_main, true);
    activity.navUtil.navigate(
        findNavController().getGraph().getStartDestinationId(), builder.build()
    );
  }

  @Override
  public void login(boolean checkVersion) {
    viewModel.clearHassData();
    new Handler().postDelayed(() -> viewModel.login(checkVersion), 500);
  }
}
