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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentMasterDataOverviewBinding;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.viewmodel.MasterDataOverviewViewModel;

public class MasterDataOverviewFragment extends BaseFragment {

  private final static String TAG = MasterDataOverviewFragment.class.getSimpleName();

  private MainActivity activity;
  private FragmentMasterDataOverviewBinding binding;
  private MasterDataOverviewViewModel viewModel;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentMasterDataOverviewBinding.inflate(
        inflater, container, false
    );
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    if (binding != null) {
      binding = null;
    }
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) requireActivity();
    viewModel = new ViewModelProvider(this).get(MasterDataOverviewViewModel.class);
    binding.setViewModel(viewModel);
    binding.setLifecycleOwner(getViewLifecycleOwner());

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBar);
    systemBarBehavior.setContainer(binding.swipe);
    systemBarBehavior.setScroll(binding.scroll, binding.constraint);
    systemBarBehavior.applyAppBarInsetOnContainer(false);
    systemBarBehavior.applyStatusBarInsetOnContainer(false);
    systemBarBehavior.setUp();
    activity.setSystemBarBehavior(systemBarBehavior);

    binding.toolbar.setNavigationOnClickListener(v -> activity.navUtil.navigateUp());

    binding.linearProducts.setOnClickListener(v -> activity.navUtil.navigate(
        MasterDataOverviewFragmentDirections
            .actionMasterDataOverviewFragmentToMasterObjectListFragment(
                GrocyApi.ENTITY.PRODUCTS
            ))
    );
    binding.linearQuantityUnits.setOnClickListener(v -> activity.navUtil.navigate(
        MasterDataOverviewFragmentDirections
            .actionMasterDataOverviewFragmentToMasterObjectListFragment(
                GrocyApi.ENTITY.QUANTITY_UNITS
            ))
    );
    binding.linearLocations.setOnClickListener(v -> activity.navUtil.navigate(
        MasterDataOverviewFragmentDirections
            .actionMasterDataOverviewFragmentToMasterObjectListFragment(
                GrocyApi.ENTITY.LOCATIONS
            ))
    );
    binding.linearProductGroups.setOnClickListener(v -> activity.navUtil.navigate(
        MasterDataOverviewFragmentDirections
            .actionMasterDataOverviewFragmentToMasterObjectListFragment(
                GrocyApi.ENTITY.PRODUCT_GROUPS
            ))
    );
    binding.linearStores.setOnClickListener(v -> activity.navUtil.navigate(
        MasterDataOverviewFragmentDirections
            .actionMasterDataOverviewFragmentToMasterObjectListFragment(
                GrocyApi.ENTITY.STORES
            ))
    );
    binding.linearTaskCategories.setOnClickListener(v -> activity.navUtil.navigate(
        MasterDataOverviewFragmentDirections
            .actionMasterDataOverviewFragmentToMasterObjectListFragment(
                GrocyApi.ENTITY.TASK_CATEGORIES
            ))
    );
    binding.linearChores.setOnClickListener(v -> viewModel.showMessageWithAction(
        R.string.msg_not_implemented_yet,
        R.string.action_open_server,
        () -> {
          Intent browserIntent = new Intent(
              Intent.ACTION_VIEW, Uri.parse(activity.getGrocyApi().getBaseUrl() + "/chores")
          );
          startActivity(browserIntent);
        },
        getSharedPrefs().getInt(
            Constants.SETTINGS.BEHAVIOR.MESSAGE_DURATION,
            Constants.SETTINGS_DEFAULT.BEHAVIOR.MESSAGE_DURATION
        )
    ));

    viewModel.getEventHandler().observeEvent(getViewLifecycleOwner(), event -> {
      if (event.getType() == Event.SNACKBAR_MESSAGE) {
        activity.showSnackbar(
            ((SnackbarMessage) event).getSnackbar(activity.binding.coordinatorMain)
        );
      }
    });

    viewModel.getProductsLive().observe(
        getViewLifecycleOwner(),
        products -> binding.countProducts.setText(
            products != null ? String.valueOf(products.size())
                : getString(R.string.subtitle_unknown)
        )
    );
    viewModel.getLocationsLive().observe(
        getViewLifecycleOwner(),
        locations -> binding.countLocations.setText(
            locations != null ? String.valueOf(locations.size())
                : getString(R.string.subtitle_unknown)
        )
    );
    viewModel.getStoresLive().observe(
        getViewLifecycleOwner(),
        stores -> binding.countStores.setText(
            stores != null ? String.valueOf(stores.size())
                : getString(R.string.subtitle_unknown)
        )
    );
    viewModel.getQuantityUnitsLive().observe(
        getViewLifecycleOwner(),
        quantityUnits -> binding.countQuantityUnits.setText(
            quantityUnits != null
                ? String.valueOf(quantityUnits.size())
                : getString(R.string.subtitle_unknown)
        )
    );
    viewModel.getProductGroupsLive().observe(
        getViewLifecycleOwner(),
        productGroups -> binding.countProductGroups.setText(
            productGroups != null
                ? String.valueOf(productGroups.size())
                : getString(R.string.subtitle_unknown)
        )
    );
    viewModel.getTaskCategoriesLive().observe(
        getViewLifecycleOwner(),
        taskCategories -> binding.countTaskCategories.setText(
            taskCategories != null
                ? String.valueOf(taskCategories.size())
                : getString(R.string.subtitle_unknown)
        )
    );

    if (savedInstanceState == null) {
      viewModel.loadFromDatabase(true);
    }

    hideDisabledFeatures();

    // UPDATE UI

    activity.getScrollBehavior().setNestedOverScrollFixEnabled(true);
    activity.getScrollBehavior().setUpScroll(
        binding.appBar, false, binding.scroll, false
    );
    activity.getScrollBehavior().setBottomBarVisibility(true);
    activity.updateBottomAppBar(false, R.menu.menu_empty);
  }

  private void hideDisabledFeatures() {
    if (!viewModel.isFeatureEnabled(PREF.FEATURE_STOCK_LOCATION_TRACKING)) {
      binding.linearLocations.setVisibility(View.GONE);
    }
    if (!viewModel.isFeatureEnabled(PREF.FEATURE_STOCK_PRICE_TRACKING)) {
      binding.linearStores.setVisibility(View.GONE);
    }
    if (!viewModel.isFeatureEnabled(PREF.FEATURE_TASKS)) {
      binding.linearTaskCategories.setVisibility(View.GONE);
    }
    if (!viewModel.isFeatureEnabled(PREF.FEATURE_CHORES)) {
      binding.linearChores.setVisibility(View.GONE);
    }
  }

  @Override
  public void updateConnectivity(boolean online) {
    if (!online == viewModel.isOffline()) {
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
