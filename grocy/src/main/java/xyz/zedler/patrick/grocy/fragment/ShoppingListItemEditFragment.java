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
import android.text.InputType;
import android.view.FocusFinder;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.ACTION;
import xyz.zedler.patrick.grocy.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentShoppingListItemEditBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.QuantityUnitsBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ShoppingListsBottomSheet;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.scanner.EmbeddedFragmentScanner;
import xyz.zedler.patrick.grocy.scanner.EmbeddedFragmentScanner.BarcodeListener;
import xyz.zedler.patrick.grocy.scanner.EmbeddedFragmentScannerBundle;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;
import xyz.zedler.patrick.grocy.viewmodel.ShoppingListItemEditViewModel;

public class ShoppingListItemEditFragment extends BaseFragment implements BarcodeListener {

  private final static String TAG = ShoppingListItemEditFragment.class.getSimpleName();

  private MainActivity activity;
  private FragmentShoppingListItemEditBinding binding;
  private ShoppingListItemEditViewModel viewModel;
  private InfoFullscreenHelper infoFullscreenHelper;
  private EmbeddedFragmentScanner embeddedFragmentScanner;

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup group, Bundle state) {
    binding = FragmentShoppingListItemEditBinding.inflate(inflater, group, false);
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
    binding = null;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) requireActivity();
    ShoppingListItemEditFragmentArgs args = ShoppingListItemEditFragmentArgs
        .fromBundle(requireArguments());
    viewModel = new ViewModelProvider(this, new ShoppingListItemEditViewModel
        .ShoppingListItemEditViewModelFactory(activity.getApplication(), args)
    ).get(ShoppingListItemEditViewModel.class);
    binding.setActivity(activity);
    binding.setViewModel(viewModel);
    binding.setFormData(viewModel.getFormData());
    binding.setFragment(this);
    binding.setLifecycleOwner(getViewLifecycleOwner());

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBar);
    systemBarBehavior.setContainer(binding.swipe);
    systemBarBehavior.setScroll(binding.scroll, binding.constraint);
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
      } else if (event.getType() == Event.SET_SHOPPING_LIST_ID) {
        int id = event.getBundle().getInt(Constants.ARGUMENT.SELECTED_ID);
        setForDestination(R.id.shoppingListFragment, Constants.ARGUMENT.SELECTED_ID, id);
      } else if (event.getType() == Event.BOTTOM_SHEET) {
        BottomSheetEvent bottomSheetEvent = (BottomSheetEvent) event;
        activity.showBottomSheet(bottomSheetEvent.getBottomSheet(), event.getBundle());
      } else if (event.getType() == Event.CHOOSE_PRODUCT) {
        String barcode = event.getBundle().getString(ARGUMENT.BARCODE);
        activity.navUtil.navigate(
            R.id.chooseProductFragment,
            new ChooseProductFragmentArgs.Builder(barcode).build().toBundle()
        );
      } else if (event.getType() == Event.FOCUS_AMOUNT_FIELD) {
        clearAmountFieldAndFocusIt();
      }
    });

    Integer productIdSavedSate = (Integer) getFromThisDestinationNow(Constants.ARGUMENT.PRODUCT_ID);
    if (productIdSavedSate != null) {
      removeForThisDestination(Constants.ARGUMENT.PRODUCT_ID);
      viewModel.setQueueEmptyAction(() -> viewModel.setProduct(productIdSavedSate));
    } else if (NumUtil.isStringInt(args.getProductId())) {
      int productId = Integer.parseInt(args.getProductId());
      setArguments(new ShoppingListItemEditFragmentArgs.Builder(args)
          .setProductId(null).build().toBundle());
      viewModel.setQueueEmptyAction(() -> viewModel.setProduct(productId));
    } else if (savedInstanceState == null && args.getAction().equals(ACTION.CREATE)) {
      showInitialKeyboardIfConditionsAreMet();
    }

    infoFullscreenHelper = new InfoFullscreenHelper(binding.container);
    viewModel.getInfoFullscreenLive().observe(getViewLifecycleOwner(), infoFullscreen -> {
      infoFullscreenHelper.setInfo(infoFullscreen);
      if (infoFullscreen == null && savedInstanceState == null
          && args.getAction().equals(ACTION.CREATE)
      ) {
        showInitialKeyboardIfConditionsAreMet();
      }
    });

    viewModel.getOfflineLive().observe(getViewLifecycleOwner(), offline -> {
      InfoFullscreen infoFullscreen = offline ? new InfoFullscreen(
          InfoFullscreen.ERROR_OFFLINE,
          () -> updateConnectivity(true)
      ) : null;
      viewModel.getInfoFullscreenLive().setValue(infoFullscreen);
    });

    Boolean backFromChooseProductPage = (Boolean)
        getFromThisDestinationNow(ARGUMENT.BACK_FROM_CHOOSE_PRODUCT_PAGE);
    if (backFromChooseProductPage != null) {
      removeForThisDestination(ARGUMENT.BACK_FROM_CHOOSE_PRODUCT_PAGE);
      if (backFromChooseProductPage) {
        clearAmountFieldAndFocusIt();
      }
    }

    binding.textInputAmount.setHelperTextColor(ColorStateList.valueOf(
        ResUtil.getColor(activity, R.attr.colorCustomBlue)
    ));
    viewModel.getFormData().getQuantityUnitErrorLive().observe(
        getViewLifecycleOwner(), value -> binding.textQuantityUnit.setTextColor(
            ResUtil.getColor(activity, value ? R.attr.colorError : R.attr.colorOnSurfaceVariant)
        )
    );

    embeddedFragmentScanner.setScannerVisibilityLive(
        viewModel.getFormData().getScannerVisibilityLive()
    );

    viewModel.getFormData().getUseMultilineNoteLive().observe(getViewLifecycleOwner(), multi -> {
      if(multi) {
        binding.editTextNote.setInputType(InputType.TYPE_CLASS_TEXT
            | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        binding.editTextNote.setImeOptions(EditorInfo.IME_ACTION_UNSPECIFIED);
      } else {
        binding.editTextNote.setInputType(InputType.TYPE_CLASS_TEXT
            | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        binding.editTextNote.setImeOptions(EditorInfo.IME_ACTION_DONE);
      }
      if(binding.editTextNote.isFocused()) {
        activity.hideKeyboard();
        if (binding.editTextNote.getText() != null) {
          binding.editTextNote.setSelection(binding.editTextNote.getText().length());
        }
        binding.editTextNote.clearFocus();
        activity.showKeyboard(binding.editTextNote);
      }
    });

    // necessary because else getValue() doesn't give current value (?)
    viewModel.getFormData().getQuantityUnitsLive().observe(getViewLifecycleOwner(), qUs -> {
    });

    if (savedInstanceState == null && !args.getAction().equals(ACTION.EDIT)) {
      if (binding.autoCompleteProduct.getText() == null
          || binding.autoCompleteProduct.getText().length() == 0) {
        activity.showKeyboard(binding.autoCompleteProduct);
      }
    }

    if (savedInstanceState == null) {
      viewModel.loadFromDatabase(true);
    }

    // UPDATE UI

    activity.getScrollBehavior().setNestedOverScrollFixEnabled(true);
    activity.getScrollBehavior().setUpScroll(binding.appBar, false, binding.scroll);
    activity.getScrollBehavior().setBottomBarVisibility(true);
    activity.updateBottomAppBar(
        true,
        viewModel.isActionEdit()
            ? R.menu.menu_shopping_list_item_edit_edit
            : R.menu.menu_shopping_list_item_edit_create,
        this::onMenuItemClick
    );
    activity.updateFab(
        R.drawable.ic_round_backup,
        R.string.action_save,
        Constants.FAB.TAG.SAVE,
        args.getAnimateStart() && savedInstanceState == null,
        () -> {
          if (!viewModel.getFormData().isProductNameValid()) {
            clearFocusAndCheckProductInput();
          } else {
            viewModel.saveItem();
          }
        }
    );
  }

  @Override
  public void onResume() {
    super.onResume();
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
    viewModel.getFormData().toggleScannerVisibility();
    viewModel.onBarcodeRecognized(rawValue);
  }

  public void toggleTorch() {
    embeddedFragmentScanner.toggleTorch();
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

  public void saveItemOrClearInputFocus() {
    if (viewModel.getFormData().isFormValid()) {
      viewModel.saveItem();
    } else {
      clearInputFocus();
    }
  }

  public void clearFocusAndCheckProductInput() {
    clearInputFocus();
    viewModel.checkProductInput();
  }

  public void clearInputFocus() {
    activity.hideKeyboard();
    binding.dummyFocusView.requestFocus();
    binding.textInputProduct.clearFocus();
    binding.textInputAmount.clearFocus();
    binding.textInputNote.clearFocus();
    binding.constraint.clearFocus();
    binding.quantityUnitContainer.clearFocus();
  }

  private void showInitialKeyboardIfConditionsAreMet() {
    if (binding.autoCompleteProduct.getText() == null
        || binding.autoCompleteProduct.getText().length() == 0) {
      activity.showKeyboard(binding.autoCompleteProduct);
    }
  }

  public void onItemAutoCompleteClick(AdapterView<?> adapterView, int pos) {
    Product product = (Product) adapterView.getItemAtPosition(pos);
    viewModel.setProduct(product, false);
    focusNextView();
  }

  public void onProductInputNextClick() {
    viewModel.checkProductInput();
    focusNextView();
  }

  public void clearFocusAndCheckProductInputExternal() {
    clearInputFocus();
    String input = viewModel.getFormData().getProductNameLive().getValue();
    if (input == null || input.isEmpty()) return;
    viewModel.onBarcodeRecognized(viewModel.getFormData().getProductNameLive().getValue());
  }

  public void focusNextView() {
    View nextView = FocusFinder.getInstance()
        .findNextFocus(binding.container, activity.getCurrentFocus(), View.FOCUS_DOWN);
    if (nextView == null) {
      clearInputFocus();
      return;
    }
    if (nextView.getId() == R.id.quantity_unit_container
        && viewModel.getFormData().getQuantityUnitsLive().getValue() != null
        && viewModel.getFormData().getQuantityUnitsLive().getValue().size() <= 1
    ) {
      nextView = binding.container.findViewById(R.id.edit_text_amount);
    }
    nextView.requestFocus();
    if (nextView instanceof EditText) {
      activity.showKeyboard((EditText) nextView);
    }
  }

  @Override
  public void onBottomSheetDismissed() {
    focusNextView();
  }

  public void showShoppingListsBottomSheet() {
    activity.showBottomSheet(new ShoppingListsBottomSheet());
  }

  public void showQuantityUnitsBottomSheet(boolean hasFocus) {
    if (!hasFocus) {
      return;
    }
    Bundle bundle = new Bundle();
    bundle.putParcelableArrayList(
        Constants.ARGUMENT.QUANTITY_UNITS,
        viewModel.getFormData().getQuantityUnitsLive().getValue()
    );
    QuantityUnit quantityUnit = viewModel.getFormData().getQuantityUnitLive().getValue();
    bundle.putInt(ARGUMENT.SELECTED_ID, quantityUnit != null ? quantityUnit.getId() : -1);
    activity.showBottomSheet(new QuantityUnitsBottomSheet(), bundle);
  }

  @Nullable
  @Override
  public MutableLiveData<Integer> getSelectedShoppingListIdLive() {
    return viewModel.getFormData().getShoppingListIdLive();
  }

  @Override
  public void selectShoppingList(ShoppingList shoppingList) {
    viewModel.getFormData().getShoppingListLive().setValue(shoppingList);
  }

  @Override
  public void selectQuantityUnit(QuantityUnit quantityUnit) {
    viewModel.getFormData().getQuantityUnitLive().setValue(quantityUnit);
  }

  private boolean onMenuItemClick(MenuItem item) {
    if (item.getItemId() == R.id.action_delete) {
      ViewUtil.startIcon(item);
      viewModel.deleteItem();
      return true;
    } else if (item.getItemId() == R.id.action_product_overview) {
      ViewUtil.startIcon(item);
      viewModel.showProductDetailsBottomSheet();
      return true;
    } else if (item.getItemId() == R.id.action_clear_form) {
      clearInputFocus();
      viewModel.getFormData().clearForm();
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
