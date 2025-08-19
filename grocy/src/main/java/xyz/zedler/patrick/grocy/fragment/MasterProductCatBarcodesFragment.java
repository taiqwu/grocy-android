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
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.snackbar.Snackbar;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.ACTION;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.ProductBarcodeAdapter;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentMasterProductCatBarcodesBinding;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.ProductBarcode;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.viewmodel.MasterProductCatBarcodesViewModel;

public class MasterProductCatBarcodesFragment extends BaseFragment implements
    ProductBarcodeAdapter.ProductBarcodeAdapterListener {

  private final static String TAG = MasterProductCatBarcodesFragment.class.getSimpleName();

  private MainActivity activity;
  private ClickUtil clickUtil;
  private FragmentMasterProductCatBarcodesBinding binding;
  private MasterProductCatBarcodesViewModel viewModel;
  private InfoFullscreenHelper infoFullscreenHelper;
  private SystemBarBehavior systemBarBehavior;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentMasterProductCatBarcodesBinding.inflate(
        inflater, container, false
    );
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();

    if (infoFullscreenHelper != null) {
      infoFullscreenHelper.destroyInstance();
      infoFullscreenHelper = null;
    }
    if (binding != null) {
      binding.recycler.animate().cancel();
      binding = null;
    }
  }

  @Override
  public void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) requireActivity();
    clickUtil = new ClickUtil();

    MasterProductFragmentArgs args = MasterProductFragmentArgs
        .fromBundle(requireArguments());
    viewModel = new ViewModelProvider(this, new MasterProductCatBarcodesViewModel
        .MasterProductCatBarcodesViewModelFactory(activity.getApplication(), args)
    ).get(MasterProductCatBarcodesViewModel.class);

    binding.setActivity(activity);
    binding.setViewModel(viewModel);
    binding.setFragment(this);
    binding.setLifecycleOwner(getViewLifecycleOwner());

    systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBar);
    systemBarBehavior.setContainer(binding.swipe);
    systemBarBehavior.setRecycler(binding.recycler);
    systemBarBehavior.setUp();
    activity.setSystemBarBehavior(systemBarBehavior);

    binding.toolbar.setNavigationOnClickListener(v -> activity.navUtil.navigateUp());

    viewModel.getEventHandler().observeEvent(getViewLifecycleOwner(), event -> {
      if (event.getType() == Event.SNACKBAR_MESSAGE) {
        activity.showSnackbar(
            ((SnackbarMessage) event).getSnackbar(activity.binding.coordinatorMain)
        );
      } else if (event.getType() == Event.NAVIGATE_UP) {
        activity.navUtil.navigateUp();
      } else if (event.getType() == Event.BOTTOM_SHEET) {
        BottomSheetEvent bottomSheetEvent = (BottomSheetEvent) event;
        activity.showBottomSheet(bottomSheetEvent.getBottomSheet(), event.getBundle());
      }
    });

    infoFullscreenHelper = new InfoFullscreenHelper(binding.frameContainer);
    viewModel.getInfoFullscreenLive().observe(
        getViewLifecycleOwner(),
        infoFullscreen -> infoFullscreenHelper.setInfo(infoFullscreen)
    );

    binding.recycler.setLayoutManager(
        new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
    );
    ProductBarcodeAdapter adapter = new ProductBarcodeAdapter(requireContext(), this);
    binding.recycler.setAdapter(adapter);

    viewModel.getProductBarcodesLive().observe(getViewLifecycleOwner(), barcodes -> {
      if (barcodes == null) {
        return;
      }
      if (barcodes.isEmpty()) {
        InfoFullscreen info = new InfoFullscreen(InfoFullscreen.INFO_EMPTY_PRODUCT_BARCODES);
        viewModel.getInfoFullscreenLive().setValue(info);
      } else {
        viewModel.getInfoFullscreenLive().setValue(null);
      }
      adapter.updateData(
          barcodes,
          viewModel.getQuantityUnits(),
          viewModel.getStores(),
          () -> binding.recycler.scheduleLayoutAnimation()
      );
    });

    if (savedInstanceState == null) {
      viewModel.loadFromDatabase(true);
    }

    // UPDATE UI

    activity.getScrollBehavior().setNestedOverScrollFixEnabled(true);
    activity.getScrollBehavior().setUpScroll(
        binding.appBar, false, binding.recycler, true
    );
    activity.getScrollBehavior().setBottomBarVisibility(true);
    activity.updateBottomAppBar(
        true,
        R.menu.menu_master_product_edit_sub,
        menuItem -> {
          if (menuItem.getItemId() == R.id.action_delete) {
            Snackbar snackbar = activity.getSnackbar(
                R.string.msg_not_implemented_yet, true
            ).setAction(
                R.string.action_open_server,
                v -> {
                  Intent browserIntent = new Intent(
                      Intent.ACTION_VIEW,
                      Uri.parse(activity.getGrocyApi().getBaseUrl()
                          + "/product/" + viewModel.getFilledProduct().getId())
                  );
                  startActivity(browserIntent);
                }
            );
            activity.showSnackbar(snackbar);
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
    activity.updateFab(R.drawable.ic_round_add_anim,
        R.string.action_add,
        Constants.FAB.TAG.ADD,
        savedInstanceState == null,
        () -> activity.navUtil.navigate(MasterProductCatBarcodesFragmentDirections
            .actionMasterProductCatBarcodesFragmentToMasterProductCatBarcodesEditFragment(
                viewModel.getFilledProduct()
            )
        )
    );
  }

  @Override
  public void onItemRowClicked(ProductBarcode productBarcode) {
    if (clickUtil.isDisabled()) {
      return;
    }
    activity.navUtil.navigate(MasterProductCatBarcodesFragmentDirections
        .actionMasterProductCatBarcodesFragmentToMasterProductCatBarcodesEditFragment(viewModel.getFilledProduct())
        .setProductBarcode(productBarcode)
    );
  }

  @Override
  public void updateConnectivity(boolean isOnline) {
    if (!isOnline == viewModel.isOffline()) {
      return;
    }
    viewModel.downloadData(false);
    if (systemBarBehavior != null) {
      systemBarBehavior.refresh();
    }
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
