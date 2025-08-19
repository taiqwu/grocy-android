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
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.ACTION;
import xyz.zedler.patrick.grocy.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentMasterProductCatAmountBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.InputBottomSheet;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.form.FormDataMasterProductCatAmount;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;
import xyz.zedler.patrick.grocy.viewmodel.MasterProductCatAmountViewModel;

public class MasterProductCatAmountFragment extends BaseFragment {

  private final static String TAG = MasterProductCatAmountFragment.class.getSimpleName();

  private MainActivity activity;
  private FragmentMasterProductCatAmountBinding binding;
  private MasterProductCatAmountViewModel viewModel;
  private InfoFullscreenHelper infoFullscreenHelper;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentMasterProductCatAmountBinding.inflate(
        inflater, container, false
    );
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
    MasterProductCatAmountFragmentArgs args = MasterProductCatAmountFragmentArgs
        .fromBundle(requireArguments());
    viewModel = new ViewModelProvider(this, new MasterProductCatAmountViewModel
        .MasterProductCatAmountViewModelFactory(activity.getApplication(), args)
    ).get(MasterProductCatAmountViewModel.class);
    binding.setActivity(activity);
    binding.setFormData(viewModel.getFormData());
    binding.setViewModel(viewModel);
    binding.setFragment(this);
    binding.setLifecycleOwner(getViewLifecycleOwner());

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBar);
    systemBarBehavior.setScroll(binding.scroll, binding.constraint);
    systemBarBehavior.setUp();
    activity.setSystemBarBehavior(systemBarBehavior);

    binding.toolbar.setNavigationOnClickListener(v -> {
      onBackPressed();
      activity.navUtil.navigateUp();
    });

    viewModel.getEventHandler().observeEvent(getViewLifecycleOwner(), event -> {
      if (event.getType() == Event.SNACKBAR_MESSAGE) {
        activity.showSnackbar(
            ((SnackbarMessage) event).getSnackbar(activity.binding.coordinatorMain)
        );
      } else if (event.getType() == Event.NAVIGATE_UP) {
        activity.navUtil.navigateUp();
      } else if (event.getType() == Event.SET_SHOPPING_LIST_ID) {
        int id = event.getBundle().getInt(Constants.ARGUMENT.SELECTED_ID);
        setForDestination(R.id.shoppingListFragment, Constants.ARGUMENT.SELECTED_ID, id);
      } else if (event.getType() == Event.BOTTOM_SHEET) {
        BottomSheetEvent bottomSheetEvent = (BottomSheetEvent) event;
        activity.showBottomSheet(bottomSheetEvent.getBottomSheet(), event.getBundle());
      }
    });

    infoFullscreenHelper = new InfoFullscreenHelper(binding.container);
    viewModel.getInfoFullscreenLive().observe(
        getViewLifecycleOwner(),
        infoFullscreen -> infoFullscreenHelper.setInfo(infoFullscreen)
    );

    viewModel.getFormData().getTareWeightErrorLive().observe(
        getViewLifecycleOwner(), value -> binding.textTareWeightLabel.setTextColor(
            ResUtil.getColor(
                activity, value == null ? R.attr.colorError : R.attr.colorOnSurfaceVariant
            )
        )
    );

    if (savedInstanceState == null) {
      viewModel.loadFromDatabase(true);
    }

    // UPDATE UI

    activity.getScrollBehavior().setNestedOverScrollFixEnabled(false);
    activity.getScrollBehavior().setUpScroll(
        binding.appBar, false, binding.scroll, false
    );
    activity.getScrollBehavior().setBottomBarVisibility(true);
    activity.updateBottomAppBar(
        true,
        viewModel.isActionEdit()
            ? R.menu.menu_master_product_edit
            : R.menu.menu_master_product_create,
        menuItem -> {
          if (menuItem.getItemId() == R.id.action_delete) {
            setForDestination(
                R.id.masterProductFragment,
                Constants.ARGUMENT.ACTION,
                Constants.ACTION.DELETE
            );
            activity.performOnBackPressed();
            return true;
          }
          if (menuItem.getItemId() == R.id.action_save) {
            setForDestination(
                R.id.masterProductFragment,
                Constants.ARGUMENT.ACTION,
                ACTION.SAVE_CLOSE
            );
            activity.performOnBackPressed();
            return true;
          }
          return false;
        }
    );
    boolean showSaveWithCloseButton = viewModel.isActionEdit() || args.getForceSaveWithClose();
    activity.updateFab(
        showSaveWithCloseButton ? R.drawable.ic_round_save : R.drawable.ic_round_save_as,
        showSaveWithCloseButton ? R.string.action_save : R.string.action_save_not_close,
        showSaveWithCloseButton ? Constants.FAB.TAG.SAVE : Constants.FAB.TAG.SAVE_NOT_CLOSE,
        savedInstanceState == null,
        () -> {
          setForDestination(
              R.id.masterProductFragment,
              Constants.ARGUMENT.ACTION,
              showSaveWithCloseButton ? ACTION.SAVE_CLOSE : ACTION.SAVE_NOT_CLOSE
          );
          activity.performOnBackPressed();
        }
    );
  }

  public void clearInputFocus() {
    activity.hideKeyboard();
  }

  public void showInputNumberBottomSheet(int type) {
    Bundle bundle = new Bundle();
    bundle.putInt(FormDataMasterProductCatAmount.AMOUNT_ARG, type);
    bundle.putDouble(Constants.ARGUMENT.NUMBER, viewModel.getFormData().getAmount(type));
    String hint = null;
    if (type == FormDataMasterProductCatAmount.MIN_AMOUNT) {
      hint = getString(R.string.property_amount_min_stock);
    } else if (type == FormDataMasterProductCatAmount.QUICK_CONSUME_AMOUNT) {
      hint = getString(R.string.property_amount_quick_consume);
    } else if (type == FormDataMasterProductCatAmount.QUICK_OPEN_AMOUNT) {
      hint = getString(R.string.property_amount_quick_open);
    } else if (type == FormDataMasterProductCatAmount.TARE_WEIGHT) {
      hint = getString(R.string.property_tare_weight);
    }
    bundle.putString(ARGUMENT.HINT, hint);
    activity.showBottomSheet(new InputBottomSheet(), bundle);
  }

  public void showInputNumberBottomSheet(int type, View imageView) {
    ViewUtil.startIcon(imageView);
    showInputNumberBottomSheet(type);
  }

  @Override
  public void saveInput(String text, Bundle argsBundle) {
    viewModel.getFormData().setAmount(text, argsBundle);
  }

  @Override
  public boolean onBackPressed() {
    setForDestination(
        R.id.masterProductFragment,
        Constants.ARGUMENT.PRODUCT,
        viewModel.getFilledProduct()
    );
    return false;
  }

  @Override
  public void updateConnectivity(boolean isOnline) {
    if (!isOnline == viewModel.isOffline()) {
      return;
    }
    viewModel.downloadData(false);
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
