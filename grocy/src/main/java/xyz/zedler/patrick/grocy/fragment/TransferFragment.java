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

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentTransferBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ProductOverviewBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ProductOverviewBottomSheetArgs;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.model.StockEntry;
import xyz.zedler.patrick.grocy.model.StockLocation;
import xyz.zedler.patrick.grocy.scanner.EmbeddedFragmentScanner;
import xyz.zedler.patrick.grocy.scanner.EmbeddedFragmentScanner.BarcodeListener;
import xyz.zedler.patrick.grocy.scanner.EmbeddedFragmentScannerBundle;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.Constants.FAB;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;
import xyz.zedler.patrick.grocy.viewmodel.TransferViewModel;
import xyz.zedler.patrick.grocy.viewmodel.TransferViewModel.TransferViewModelFactory;

public class TransferFragment extends BaseFragment implements BarcodeListener {

  private final static String TAG = TransferFragment.class.getSimpleName();

  private MainActivity activity;
  private FragmentTransferBinding binding;
  private TransferViewModel viewModel;
  private InfoFullscreenHelper infoFullscreenHelper;
  private EmbeddedFragmentScanner embeddedFragmentScanner;
  private Boolean backFromChooseProductPage;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentTransferBinding.inflate(inflater, container, false);
    embeddedFragmentScanner = new EmbeddedFragmentScannerBundle(
        this,
        binding.containerScanner,
        this
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
    binding = null;
  }

  @Override
  public void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) requireActivity();
    TransferFragmentArgs args = TransferFragmentArgs.fromBundle(requireArguments());

    viewModel = new ViewModelProvider(this, new TransferViewModelFactory(activity.getApplication(),
        args)
    ).get(TransferViewModel.class);
    binding.setActivity(activity);
    binding.setViewModel(viewModel);
    binding.setFragment(this);
    binding.setFormData(viewModel.getFormData());
    binding.setLifecycleOwner(getViewLifecycleOwner());

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBar);
    systemBarBehavior.setContainer(binding.swipe);
    systemBarBehavior.setScroll(binding.scroll, binding.constraint);
    systemBarBehavior.setUp();
    activity.setSystemBarBehavior(systemBarBehavior);

    binding.toolbar.setNavigationOnClickListener(v -> activity.navUtil.navigateUp());

    infoFullscreenHelper = new InfoFullscreenHelper(binding.container);

    viewModel.getInfoFullscreenLive().observe(
        getViewLifecycleOwner(),
        infoFullscreen -> infoFullscreenHelper.setInfo(infoFullscreen)
    );
    viewModel.getIsLoadingLive().observe(getViewLifecycleOwner(), isDownloading ->
        binding.swipe.setRefreshing(isDownloading)
    );
    viewModel.getEventHandler().observeEvent(getViewLifecycleOwner(), event -> {
      if (event.getType() == Event.SNACKBAR_MESSAGE) {
        activity.showSnackbar(
            ((SnackbarMessage) event).getSnackbar(activity.binding.coordinatorMain)
        );
      } else if (event.getType() == Event.CONSUME_SUCCESS) {
        assert getArguments() != null;
        if (PurchaseFragmentArgs.fromBundle(getArguments()).getCloseWhenFinished()) {
          activity.navUtil.navigateUp();
        } else {
          viewModel.getFormData().clearForm();
          focusProductInputIfNecessary();
          embeddedFragmentScanner.startScannerIfVisible();
        }
      } else if (event.getType() == Event.BOTTOM_SHEET) {
        BottomSheetEvent bottomSheetEvent = (BottomSheetEvent) event;
        activity.showBottomSheet(bottomSheetEvent.getBottomSheet(), event.getBundle());
      } else if (event.getType() == Event.FOCUS_INVALID_VIEWS) {
        focusNextInvalidView();
      } else if (event.getType() == Event.QUICK_MODE_ENABLED) {
        focusProductInputIfNecessary();
      } else if (event.getType() == Event.QUICK_MODE_DISABLED) {
        clearInputFocus();
      } else if (event.getType() == Event.CONTINUE_SCANNING) {
        embeddedFragmentScanner.startScannerIfVisible();
      } else if (event.getType() == Event.CHOOSE_PRODUCT) {
        String barcode = event.getBundle().getString(ARGUMENT.BARCODE);
        activity.navUtil.navigate(
            TransferFragmentDirections
                .actionTransferFragmentToChooseProductFragment(barcode)
                .setForbidCreateProduct(true)
        );
      } else if (event.getType() == Event.CONFIRM_FREEZING) {
        new MaterialAlertDialogBuilder(activity, R.style.ThemeOverlay_Grocy_AlertDialog_Caution)
            .setTitle(R.string.title_confirmation)
            .setMessage(getString(R.string.msg_should_not_be_frozen))
            .setPositiveButton(R.string.action_proceed, (dialog, which) -> {
              performHapticClick();
              viewModel.transferProduct(true);
            }).setNegativeButton(R.string.action_cancel, (dialog, which) -> performHapticClick())
            .setOnCancelListener(dialog -> performHapticClick()).create().show();
      }
    });

    Integer productIdSavedSate = (Integer) getFromThisDestinationNow(Constants.ARGUMENT.PRODUCT_ID);
    if (productIdSavedSate != null) {
      removeForThisDestination(Constants.ARGUMENT.PRODUCT_ID);
      viewModel.setProductWillBeFilled(true);
      viewModel.setQueueEmptyAction(() -> {
        viewModel.setProduct(productIdSavedSate, null, null);
        viewModel.setProductWillBeFilled(false);
      });
    } else if (NumUtil.isStringInt(args.getProductId())) {
      int productId = Integer.parseInt(args.getProductId());
      setArguments(new TransferFragmentArgs.Builder(args)
          .setProductId(null).build().toBundle());
      viewModel.setQueueEmptyAction(
          () -> viewModel.setProduct(productId, null, null)
      );
    }
    String barcode = (String) getFromThisDestinationNow(ARGUMENT.BARCODE);
    if (barcode != null) {
      removeForThisDestination(Constants.ARGUMENT.BARCODE);
      viewModel.addBarcodeToExistingProduct(barcode);
    }

    backFromChooseProductPage = (Boolean)
        getFromThisDestinationNow(ARGUMENT.BACK_FROM_CHOOSE_PRODUCT_PAGE);
    if (backFromChooseProductPage != null) {
      removeForThisDestination(ARGUMENT.BACK_FROM_CHOOSE_PRODUCT_PAGE);
    }
    embeddedFragmentScanner.setScannerVisibilityLive(
        viewModel.getFormData().getScannerVisibilityLive(),
        backFromChooseProductPage != null && backFromChooseProductPage
            && (viewModel.getFormData().getProductDetailsLive().getValue() != null
            || viewModel.isProductWillBeFilled()) && viewModel.getFormData().isScannerVisible()
    );

    int colorBlue = ResUtil.getColor(activity, R.attr.colorCustomBlue);
    viewModel.getQuickModeEnabled().observe(
        getViewLifecycleOwner(), value -> binding.toolbar.setTitleTextColor(
            value ? colorBlue : ResUtil.getColor(activity, R.attr.colorOnSurface)
        )
    );
    binding.textInputAmount.setHelperTextColor(ColorStateList.valueOf(colorBlue));
    viewModel.getFormData().getToLocationErrorLive().observe(
        getViewLifecycleOwner(), value -> binding.textLocationTo.setTextColor(
            ResUtil.getColor(activity, value ? R.attr.colorError : R.attr.colorOnSurfaceVariant)
        )
    );
    viewModel.getFormData().getQuantityUnitErrorLive().observe(
        getViewLifecycleOwner(), value -> binding.textQuantityUnit.setTextColor(
            ResUtil.getColor(activity, value ? R.attr.colorError : R.attr.colorOnSurfaceVariant)
        )
    );

    // following line is necessary because no observers are set in Views
    viewModel.getFormData().getQuantityUnitStockLive().observe(getViewLifecycleOwner(), i -> {
    });

    //hideDisabledFeatures();

    if (savedInstanceState == null) {
        viewModel.loadFromDatabase(true);
    }

    focusProductInputIfNecessary();

    activity.getScrollBehavior().setNestedOverScrollFixEnabled(true);
    activity.getScrollBehavior().setUpScroll(binding.appBar, false, binding.scroll);
    activity.getScrollBehavior().setBottomBarVisibility(true);
    activity.updateBottomAppBar(true, R.menu.menu_transfer, this::onMenuItemClick);
    activity.updateFab(
        R.drawable.ic_round_swap_horiz,
        R.string.action_transfer,
        FAB.TAG.TRANSFER,
        args.getAnimateStart() && savedInstanceState == null,
        () -> {
          if (viewModel.isQuickModeEnabled()
              && viewModel.getFormData().isCurrentProductFlowNotInterrupted()) {
            focusNextInvalidView();
          } else if (!viewModel.getFormData().isProductNameValid()) {
            clearFocusAndCheckProductInput();
          } else {
            viewModel.transferProduct();
          }
        }
    );
  }

  @Override
  public void onResume() {
    super.onResume();
    if (backFromChooseProductPage != null && backFromChooseProductPage
        && (viewModel.getFormData().getProductDetailsLive().getValue() != null
        || viewModel.isProductWillBeFilled())) {
      backFromChooseProductPage = false;
      return;
    }
    embeddedFragmentScanner.onResume();
  }

  @Override
  public void onPause() {
    embeddedFragmentScanner.onPause();
    super.onPause();
  }

  @Override
  public void onDestroy() {
    if (embeddedFragmentScanner != null) embeddedFragmentScanner.onDestroy();
    super.onDestroy();
  }

  @Override
  public void onBarcodeRecognized(String rawValue) {
    clearInputFocus();
    if (!viewModel.isQuickModeEnabled()) {
      viewModel.getFormData().toggleScannerVisibility();
    }
    viewModel.onBarcodeRecognized(rawValue);
  }

  public void toggleTorch() {
    embeddedFragmentScanner.toggleTorch();
  }

  @Override
  public void selectQuantityUnit(QuantityUnit quantityUnit) {
    viewModel.getFormData().getQuantityUnitLive().setValue(quantityUnit);
  }

  @Override
  public void selectStockLocation(StockLocation stockLocation) {
    MutableLiveData<StockLocation> stockLocationLive = viewModel.getFormData()
        .getFromLocationLive();
    boolean locationHasChanged = stockLocation != stockLocationLive.getValue();
    stockLocationLive.setValue(stockLocation);
    if (!locationHasChanged) return;
    viewModel.getFormData().getUseSpecificLive().setValue(false);
    viewModel.getFormData().getSpecificStockEntryLive().setValue(null);
    viewModel.getFormData().isAmountValid();
  }

  @Override
  public void selectLocation(Location location) {
    viewModel.getFormData().getToLocationLive().setValue(location);
  }

  @Override
  public void selectStockEntry(StockEntry stockEntry) {
    viewModel.getFormData().getSpecificStockEntryLive().setValue(stockEntry);
    viewModel.getFormData().isAmountValid();
  }

  @Override
  public void addBarcodeToExistingProduct(String barcode) {
    viewModel.addBarcodeToExistingProduct(barcode);
    binding.autoCompleteConsumeProduct.requestFocus();
    activity.showKeyboard(binding.autoCompleteConsumeProduct);
  }

  @Override
  public void addBarcodeToNewProduct(String barcode) {
    viewModel.addBarcodeToExistingProduct(barcode);
  }

  public void toggleScannerVisibility() {
    viewModel.getFormData().toggleScannerVisibility();
      if (viewModel.getFormData().isScannerVisible()) {
          clearInputFocus();
      }
  }

  public void clearAmountFieldAndFocusIt() {
    binding.editTextAmount.setText("");
    activity.showKeyboard(binding.editTextAmount);
  }

  public void clearInputFocus() {
    activity.hideKeyboard();
    binding.dummyFocusView.requestFocus();
    binding.autoCompleteConsumeProduct.clearFocus();
    binding.quantityUnitContainer.clearFocus();
    binding.textInputAmount.clearFocus();
    binding.linearToLocation.clearFocus();
  }

  public void onItemAutoCompleteClick(AdapterView<?> adapterView, int pos) {
    Product product = (Product) adapterView.getItemAtPosition(pos);
    clearInputFocus();
      if (product == null) {
          return;
      }
    viewModel.setProduct(product.getId(), null, null);
  }

  public void clearFocusAndCheckProductInput() {
    clearInputFocus();
    viewModel.checkProductInput();
  }

  public void clearFocusAndCheckProductInputExternal() {
    clearInputFocus();
    String input = viewModel.getFormData().getProductNameLive().getValue();
    if (input == null || input.isEmpty()) return;
    viewModel.onBarcodeRecognized(viewModel.getFormData().getProductNameLive().getValue());
  }

  public void focusProductInputIfNecessary() {
      if (!viewModel.isQuickModeEnabled() || viewModel.getFormData().isScannerVisible()) {
          return;
      }
    ProductDetails productDetails = viewModel.getFormData().getProductDetailsLive().getValue();
    String productNameInput = viewModel.getFormData().getProductNameLive().getValue();
    if (productDetails == null && (productNameInput == null || productNameInput.isEmpty())) {
      binding.autoCompleteConsumeProduct.requestFocus();
      if (viewModel.getFormData().getExternalScannerEnabled()) {
        activity.hideKeyboard();
      } else {
        activity.showKeyboard(binding.autoCompleteConsumeProduct);
      }
    }
  }

  public void focusNextInvalidView() {
    View nextView = null;
    if (!viewModel.getFormData().isProductNameValid()) {
      nextView = binding.autoCompleteConsumeProduct;
    } else if (!viewModel.getFormData().isAmountValid()) {
      nextView = binding.editTextAmount;
    } else if (!viewModel.getFormData().isToLocationValid()) {
      nextView = binding.linearToLocation;
    }
    if (nextView == null) {
      clearInputFocus();
      viewModel.showConfirmationBottomSheet();
      return;
    }
    nextView.requestFocus();
    if (nextView instanceof EditText) activity.showKeyboard((EditText) nextView);
  }

  public void clearInputFocusOrFocusNextInvalidView() {
    if (viewModel.isQuickModeEnabled()
        && viewModel.getFormData().isCurrentProductFlowNotInterrupted()) {
      focusNextInvalidView();
    } else {
      clearInputFocus();
    }
  }

  @Override
  public void startTransaction() {
    viewModel.transferProduct();
  }

  @Override
  public void interruptCurrentProductFlow() {
    viewModel.getFormData().setCurrentProductFlowInterrupted(true);
  }

  @Override
  public void onBottomSheetDismissed() {
    clearInputFocusOrFocusNextInvalidView();
  }

  private boolean onMenuItemClick(MenuItem item) {
    if (item.getItemId() == R.id.action_product_overview) {
      ViewUtil.startIcon(item);
        if (!viewModel.getFormData().isProductNameValid()) {
            return false;
        }
      activity.showBottomSheet(
          new ProductOverviewBottomSheet(),
          new ProductOverviewBottomSheetArgs.Builder().setProductDetails(
              viewModel.getFormData().getProductDetailsLive().getValue()
          ).build().toBundle()
      );
      return true;
    } else if (item.getItemId() == R.id.action_clear_form) {
      clearInputFocus();
      viewModel.getFormData().clearForm();
      embeddedFragmentScanner.startScannerIfVisible();
      return true;
    }
    return false;
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
