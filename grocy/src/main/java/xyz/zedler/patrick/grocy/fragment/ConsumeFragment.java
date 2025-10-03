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
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentConsumeBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ProductOverviewBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ProductOverviewBottomSheetArgs;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.model.StockEntry;
import xyz.zedler.patrick.grocy.model.StockLocation;
import xyz.zedler.patrick.grocy.scanner.EmbeddedFragmentScanner;
import xyz.zedler.patrick.grocy.scanner.EmbeddedFragmentScanner.BarcodeListener;
import xyz.zedler.patrick.grocy.scanner.EmbeddedFragmentScannerBundle;
import xyz.zedler.patrick.grocy.util.ClickUtil.InactivityUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;
import xyz.zedler.patrick.grocy.view.FormattedTextView;
import xyz.zedler.patrick.grocy.viewmodel.ConsumeViewModel;

public class ConsumeFragment extends BaseFragment implements BarcodeListener {

  private final static String TAG = ConsumeFragment.class.getSimpleName();
  private static final String DIALOG_FAB_INFO = "dialog_fab_info";

  private MainActivity activity;
  private FragmentConsumeBinding binding;
  private ConsumeViewModel viewModel;
  private InfoFullscreenHelper infoFullscreenHelper;
  private EmbeddedFragmentScanner embeddedFragmentScanner;
  private Boolean backFromChooseProductPage;
  private InactivityUtil inactivityUtil;
  private AlertDialog dialogFabInfo;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentConsumeBinding.inflate(inflater, container, false);
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
    ConsumeFragmentArgs args = ConsumeFragmentArgs.fromBundle(requireArguments());

    viewModel = new ViewModelProvider(this, new ConsumeViewModel
        .ConsumeViewModelFactory(activity.getApplication(), args)
    ).get(ConsumeViewModel.class);
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

    if (args.getStartWithScanner() && viewModel.isQuickModeReturnEnabled()
        && viewModel.isTurnOnQuickModeEnabled()) {
      inactivityUtil = new InactivityUtil(getLifecycle(), util -> viewModel.showMessageWithAction(
          R.string.msg_returning_to_overview,
          R.string.action_cancel,
          util::stopTimer,
          5
      ), this::navigateUp, 20);
    }

    infoFullscreenHelper = new InfoFullscreenHelper(binding.container);

    viewModel.getInfoFullscreenLive().observe(
        getViewLifecycleOwner(),
        infoFullscreen -> infoFullscreenHelper.setInfo(infoFullscreen)
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
        activity.navUtil.navigate(ConsumeFragmentDirections
            .actionConsumeFragmentToChooseProductFragment(barcode)
            .setForbidCreateProduct(true));
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
      setArguments(new ConsumeFragmentArgs.Builder(args)
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
    viewModel.getFormData().getQuantityUnitErrorLive().observe(
        getViewLifecycleOwner(), value -> binding.textQuantityUnit.setTextColor(
            ResUtil.getColor(activity, value ? R.attr.colorError : R.attr.colorOnSurfaceVariant)
        )
    );

    // following line is necessary because no observers are set in Views
    viewModel.getFormData().getQuantityUnitStockLive().observe(getViewLifecycleOwner(), i -> {
    });

    if (savedInstanceState == null) {
        viewModel.loadFromDatabase(true);
    }

    focusProductInputIfNecessary();

    // UPDATE UI

    activity.getScrollBehavior().setNestedOverScrollFixEnabled(true);
    activity.getScrollBehavior().setUpScroll(binding.appBar, false, binding.scroll);
    activity.getScrollBehavior().setBottomBarVisibility(true);
    activity.updateBottomAppBar(
        true,
        viewModel.isFeatureEnabled(Constants.PREF.FEATURE_STOCK_OPENED_TRACKING)
            ? R.menu.menu_consume_with_open
            : R.menu.menu_consume,
        this::onMenuItemClick
    );
    activity.updateFab(
        R.drawable.ic_round_consume_product,
        R.string.action_consume,
        Constants.FAB.TAG.CONSUME,
        args.getAnimateStart() && savedInstanceState == null,
        () -> onActionButtonClick(false)
    );
    if (savedInstanceState != null && savedInstanceState.getBoolean(DIALOG_FAB_INFO)) {
      new Handler(Looper.getMainLooper()).postDelayed(
          this::showFabInfoDialogIfAppropriate, 1
      );
    }
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
  public void onSaveInstanceState(@NonNull Bundle outState) {
    boolean isShowing = dialogFabInfo != null && dialogFabInfo.isShowing();
    outState.putBoolean(DIALOG_FAB_INFO, isShowing);
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent event) {
    if (inactivityUtil != null && (event.getAction() == MotionEvent.ACTION_DOWN
        || event.getAction() == MotionEvent.ACTION_UP)) {
      inactivityUtil.resetTimer();
    }
    return false;
  }

  public void onActionButtonClick(boolean open) {
    if (viewModel.isQuickModeEnabled()
        && viewModel.getFormData().isCurrentProductFlowNotInterrupted()) {
      focusNextInvalidView();
    } else if (!viewModel.getFormData().isProductNameValid()) {
      clearFocusAndCheckProductInput();
    } else if (open || !showFabInfoDialogIfAppropriate()) {
      ProductDetails productDetails = viewModel.getFormData().getProductDetailsLive().getValue();
      if (open && productDetails != null
          && productDetails.getProduct().getEnableTareWeightHandlingBoolean()) {
        viewModel.showMessage(R.string.error_open_product_not_supported);
        return;
      }
      viewModel.consumeProduct(open);
    }
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
        .getStockLocationLive();
    boolean locationHasChanged = stockLocation != stockLocationLive.getValue();
    stockLocationLive.setValue(stockLocation);
      if (!locationHasChanged) {
          return;
      }
    viewModel.getFormData().getUseSpecificLive().setValue(false);
    viewModel.getFormData().getSpecificStockEntryLive().setValue(null);
    viewModel.getFormData().isAmountValid();
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
  }

  public void onItemAutoCompleteClick(AdapterView<?> adapterView, int pos) {
    Product product = (Product) adapterView.getItemAtPosition(pos);
    if (!viewModel.isQuickModeEnabled()) clearInputFocus();
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
    EditText nextView = null;
    if (!viewModel.getFormData().isProductNameValid()) {
      nextView = binding.autoCompleteConsumeProduct;
    } else if (!viewModel.getFormData().isAmountValid()) {
      nextView = binding.editTextAmount;
    }
    if (nextView == null) {
      clearInputFocus();
      viewModel.showConfirmationBottomSheet();
      return;
    }
    nextView.requestFocus();
    activity.showKeyboard(nextView);
  }

  public void clearInputFocusOrFocusNextInvalidView() {
    if (viewModel.isQuickModeEnabled()
        && viewModel.getFormData().isCurrentProductFlowNotInterrupted()) {
      focusNextInvalidView();
    } else {
      clearInputFocus();
    }
  }

  public boolean showFabInfoDialogIfAppropriate() {
    if (viewModel.getConsumeFabInfoShown()) {
      return false;
    }
    FormattedTextView textView = new FormattedTextView(activity);
    textView.setTextColor(ResUtil.getColor(activity, R.attr.colorOnSurfaceVariant));
    textView.setTextSizeParagraph(14);
    textView.setBlockDistance(8);
    textView.setSideMargin(24);
    textView.setLastBlockWithBottomMargin(false);
    textView.setText(getString(R.string.msg_help_fab_consume_start));
    dialogFabInfo = new MaterialAlertDialogBuilder(
        activity, R.style.ThemeOverlay_Grocy_AlertDialog
    ).setTitle(R.string.title_help)
        .setView(textView)
        .setPositiveButton(R.string.action_proceed, (dialog, which) -> {
          performHapticClick();
          viewModel.setConsumeFabInfoShown();
          viewModel.consumeProduct(false);
        }).setNegativeButton(R.string.action_cancel, (dialog, which) -> {
          performHapticClick();
          viewModel.setConsumeFabInfoShown();
        }).setOnCancelListener(dialog -> performHapticClick()).create();
    dialogFabInfo.show();
    return true;
  }

  @Override
  public void startTransaction(boolean open) {
    viewModel.consumeProduct(open);
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
    } else if (item.getItemId() == R.id.action_open) {
      onActionButtonClick(true);
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
