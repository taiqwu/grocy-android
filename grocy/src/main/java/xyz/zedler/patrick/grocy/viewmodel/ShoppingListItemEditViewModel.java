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

package xyz.zedler.patrick.grocy.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import java.util.HashMap;
import java.util.List;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.form.FormDataShoppingListItemEdit;
import xyz.zedler.patrick.grocy.fragment.ShoppingListItemEditFragmentArgs;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.InputProductBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ProductOverviewBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ProductOverviewBottomSheetArgs;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductBarcode;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversionResolved;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.repository.ShoppingListItemEditRepository;
import xyz.zedler.patrick.grocy.util.ArrayUtil;
import xyz.zedler.patrick.grocy.util.GrocycodeUtil;
import xyz.zedler.patrick.grocy.util.GrocycodeUtil.Grocycode;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PrefsUtil;
import xyz.zedler.patrick.grocy.util.QuantityUnitConversionUtil;
import xyz.zedler.patrick.grocy.util.VersionUtil;

public class ShoppingListItemEditViewModel extends BaseViewModel {

  private static final String TAG = ShoppingListItemEditViewModel.class.getSimpleName();

  private final SharedPreferences sharedPrefs;
  private final DownloadHelper dlHelper;
  private final GrocyApi grocyApi;
  private final ShoppingListItemEditRepository repository;
  private final FormDataShoppingListItemEdit formData;
  private final ShoppingListItemEditFragmentArgs args;

  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;

  private List<ShoppingList> shoppingLists;
  private List<Product> products;
  private List<ProductBarcode> barcodes;
  private List<QuantityUnitConversionResolved> unitConversions;
  private HashMap<Integer, QuantityUnit> quantityUnitHashMap;

  private Runnable queueEmptyAction;
  private final boolean debug;
  private final boolean isActionEdit;
  private final int maxDecimalPlacesAmount;

  public ShoppingListItemEditViewModel(
      @NonNull Application application,
      @NonNull ShoppingListItemEditFragmentArgs startupArgs
  ) {
    super(application);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);
    maxDecimalPlacesAmount = sharedPrefs.getInt(
        STOCK.DECIMAL_PLACES_AMOUNT,
        SETTINGS_DEFAULT.STOCK.DECIMAL_PLACES_AMOUNT
    );

    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue, getOfflineLive());
    grocyApi = new GrocyApi(getApplication());
    repository = new ShoppingListItemEditRepository(application);
    formData = new FormDataShoppingListItemEdit(application);
    args = startupArgs;
    isActionEdit = startupArgs.getAction().equals(Constants.ACTION.EDIT);

    infoFullscreenLive = new MutableLiveData<>();
  }

  public FormDataShoppingListItemEdit getFormData() {
    return formData;
  }

  public void loadFromDatabase(boolean downloadAfterLoading) {
    repository.loadFromDatabase(data -> {
      this.shoppingLists = data.getShoppingLists();
      this.products = data.getProducts();
      this.barcodes = data.getBarcodes();
      this.quantityUnitHashMap = ArrayUtil.getQuantityUnitsHashMap(data.getQuantityUnits());
      this.unitConversions = data.getQuantityUnitConversions();
      formData.getProductsLive().setValue(Product.getActiveProductsOnly(products));
      ShoppingList selectedShoppingList = formData.getShoppingListLive().getValue();
      if (!isActionEdit && selectedShoppingList == null) {
        formData.getShoppingListLive().setValue(getLastShoppingList());
      }
      if (downloadAfterLoading) {
        downloadData(false);
      } else {
        fillWithShoppingListItemIfNecessary();
        if (queueEmptyAction != null) {
          queueEmptyAction.run();
          queueEmptyAction = null;
        }
      }
    }, error -> onError(error, TAG));
  }

  public void downloadData(boolean forceUpdate) {
    dlHelper.updateData(
        updated -> {
          if (updated) {
            loadFromDatabase(false);
          } else {
            fillWithShoppingListItemIfNecessary();
            if (queueEmptyAction != null) {
              queueEmptyAction.run();
              queueEmptyAction = null;
            }
          }
        },
        error -> onError(error, TAG),
        forceUpdate,
        false,
        ShoppingListItem.class,
        Product.class,
        QuantityUnit.class,
        QuantityUnitConversionResolved.class,
        ProductBarcode.class
    );
  }

  public void saveItem() {
    if (!formData.isFormValid()) {
      showMessage(R.string.error_missing_information);
      return;
    }

    ShoppingListItem item = null;
    if (isActionEdit) {
      item = args.getShoppingListItem();
    }
    item = formData.fillShoppingListItem(item);
    JSONObject jsonObject = ShoppingListItem.getJsonFromShoppingListItem(item, false,
        debug, TAG);

    if (isActionEdit) {
      dlHelper.put(
          grocyApi.getObject(GrocyApi.ENTITY.SHOPPING_LIST, item.getId()),
          jsonObject,
          response -> saveProductBarcodeAndNavigateUp(),
          error -> {
            showNetworkErrorMessage(error);
            if (debug) {
              Log.e(TAG, "saveItem: " + error);
            }
          }
      );
    } else {
      dlHelper.post(
          grocyApi.getObjects(GrocyApi.ENTITY.SHOPPING_LIST),
          jsonObject,
          response -> saveProductBarcodeAndNavigateUp(),
          error -> {
            showNetworkErrorMessage(error);
            if (debug) {
              Log.e(TAG, "saveItem: " + error);
            }
          }
      );
    }
  }

  private void saveProductBarcodeAndNavigateUp() {
    ProductBarcode productBarcode = formData.fillProductBarcode(null);
    if (productBarcode.getBarcode() == null) {
      navigateUp();
      return;
    }
    ProductBarcode.addProductBarcode(
        dlHelper,
        ProductBarcode.getJsonFromProductBarcode(productBarcode, debug, TAG),
        this::navigateUp,
        error -> navigateUp()
    ).perform(dlHelper.getUuid());
  }

  private void fillWithShoppingListItemIfNecessary() {
    if (!isActionEdit || formData.isFilledWithShoppingListItem()) {
      return;
    }

    ShoppingListItem item = args.getShoppingListItem();
    assert item != null;

    ShoppingList shoppingList = getShoppingList(item.getShoppingListIdInt());
    formData.getShoppingListLive().setValue(shoppingList);

    double amount = item.getAmountDouble();
    QuantityUnit quantityUnit = null;

    Product product = item.getProductId() != null ? getProduct(item.getProductIdInt()) : null;
    if (product != null) {
      formData.getProductLive().setValue(product);
      formData.getProductNameLive().setValue(product.getName());

      HashMap<QuantityUnit, Double> unitFactors = QuantityUnitConversionUtil.getUnitFactors(
          quantityUnitHashMap,
          unitConversions,
          product,
          VersionUtil.isGrocyServerMin400(sharedPrefs)
      );
      formData.getQuantityUnitsFactorsLive().setValue(unitFactors);
      formData.getQuantityUnitStockLive().setValue(
          quantityUnitHashMap.get(product.getQuIdStockInt())
      );

      quantityUnit = quantityUnitHashMap.get(item.getQuIdInt());

      Double factor = unitFactors.get(quantityUnit);
      if (factor != null && !VersionUtil.isGrocyServerMin400(sharedPrefs) && quantityUnit != null
          && quantityUnit.getId() == product.getQuIdPurchaseInt()) {
        factor = 1 / factor;
      }
      if (factor != null) amount *= factor;
    }

    formData.getAmountLive().setValue(NumUtil.trimAmount(amount, maxDecimalPlacesAmount));
    formData.getQuantityUnitLive().setValue(quantityUnit);

    formData.getNoteLive().setValue(item.getNote());
    formData.setFilledWithShoppingListItem(true);
  }

  public void setProduct(Product product, boolean explicitlyFocusAmountField) {
    if (product == null) {
      return;
    }
    formData.getProductLive().setValue(product);
    formData.getProductNameLive().setValue(product.getName());

    HashMap<QuantityUnit, Double> unitFactors = QuantityUnitConversionUtil.getUnitFactors(
        quantityUnitHashMap,
        unitConversions,
        product,
        VersionUtil.isGrocyServerMin400(sharedPrefs)
    );
    formData.getQuantityUnitsFactorsLive().setValue(unitFactors);
    formData.getQuantityUnitStockLive().setValue(
        quantityUnitHashMap.get(product.getQuIdStockInt())
    );

    QuantityUnit purchase = quantityUnitHashMap.get(product.getQuIdPurchaseInt());
    formData.getQuantityUnitLive().setValue(purchase);

    if (explicitlyFocusAmountField) {
      sendEvent(Event.FOCUS_AMOUNT_FIELD);
    } else {
      formData.isFormValid();
    }
  }

  public void setProduct(int productId) {
    if (products == null) {
      return;
    }
    Product product = getProduct(productId);
    if (product == null) {
      return;
    }
    setProduct(product, false);
  }

  public void onBarcodeRecognized(String barcode) {
    Product product = null;
    Grocycode grocycode = GrocycodeUtil.getGrocycode(barcode);
    if (grocycode != null && grocycode.isProduct()) {
      product = Product.getProductFromId(products, grocycode.getObjectId());
      if (product == null) {
        formData.clearForm();
        showMessage(R.string.msg_not_found);
        return;
      }
    } else if (grocycode != null) {
      formData.clearForm();
      showMessage(R.string.error_wrong_grocycode_type);
      return;
    }
    if (product == null) {
      product = Product.getProductFromBarcode(products, barcodes, barcode);
    }
    if (product != null) {
      setProduct(product, true);
    } else {
      Bundle bundle = new Bundle();
      bundle.putString(ARGUMENT.BARCODE, barcode);
      sendEvent(Event.CHOOSE_PRODUCT, bundle);
    }
  }

  public void showProductDetailsBottomSheet() {
    Product product = checkProductInput();
    if (product == null) {
      return;
    }
    ProductDetails.getProductDetails(dlHelper, product.getId(), details -> showBottomSheet(
        new ProductOverviewBottomSheet(),
        new ProductOverviewBottomSheetArgs.Builder()
            .setProductDetails(details).build().toBundle()
    )).perform(dlHelper.getUuid());
  }

  public void deleteItem() {
    if (!isActionEdit()) {
      return;
    }
    ShoppingListItem shoppingListItem = args.getShoppingListItem();
    assert shoppingListItem != null;
    dlHelper.delete(
        grocyApi.getObject(
            GrocyApi.ENTITY.SHOPPING_LIST,
            shoppingListItem.getId()
        ),
        response -> navigateUp(),
        this::showNetworkErrorMessage
    );
  }

  public Product checkProductInput() {
    formData.isProductNameValid();
    String input = formData.getProductNameLive().getValue();
    if (input == null || input.isEmpty()) {
      return null;
    }
    Product product = getProductFromName(input);

    Product currentProduct = formData.getProductLive().getValue();
    if (currentProduct != null && product != null && currentProduct.getId() == product.getId()) {
      return product;
    }

    if (product != null) {
      setProduct(product, false);
    } else {
      Bundle bundle = new Bundle();
      bundle.putString(Constants.ARGUMENT.PRODUCT_INPUT, input);
      showBottomSheet(new InputProductBottomSheet(), bundle);
    }
    return product;
  }

  private ShoppingList getLastShoppingList() {
    int lastId = sharedPrefs.getInt(Constants.PREF.SHOPPING_LIST_LAST_ID, 1);
    return getShoppingList(lastId);
  }

  private ShoppingList getShoppingList(int id) {
    for (ShoppingList shoppingList : shoppingLists) {
      if (shoppingList.getId() == id) {
        return shoppingList;
      }
    }
    return null;
  }

  @Nullable
  public Product getProduct(int id) {
    for (Product product : products) {
      if (product.getId() == id) {
        return product;
      }
    }
    return null;
  }

  private Product getProductFromName(String name) {
    for (Product product : products) {
      if (product.getName().equals(name)) {
        return product;
      }
    }
    return null;
  }

  public boolean isActionEdit() {
    return isActionEdit;
  }

  public boolean isFeatureMultiShoppingListsEnabled() {
    return sharedPrefs.getBoolean(
        Constants.PREF.FEATURE_MULTIPLE_SHOPPING_LISTS, true
    );
  }

  @NonNull
  public MutableLiveData<Boolean> getIsLoadingLive() {
    return isLoadingLive;
  }

  @NonNull
  public MutableLiveData<InfoFullscreen> getInfoFullscreenLive() {
    return infoFullscreenLive;
  }

  public void setQueueEmptyAction(Runnable queueEmptyAction) {
    this.queueEmptyAction = queueEmptyAction;
  }

  public boolean getExternalScannerEnabled() {
    return sharedPrefs.getBoolean(
        Constants.SETTINGS.SCANNER.EXTERNAL_SCANNER,
        Constants.SETTINGS_DEFAULT.SCANNER.EXTERNAL_SCANNER
    );
  }

  public boolean isFeatureEnabled(String pref) {
    if (pref == null) {
      return true;
    }
    return sharedPrefs.getBoolean(pref, true);
  }

  @Override
  protected void onCleared() {
    dlHelper.destroy();
    super.onCleared();
  }

  public static class ShoppingListItemEditViewModelFactory implements ViewModelProvider.Factory {

    private final Application application;
    private final ShoppingListItemEditFragmentArgs args;

    public ShoppingListItemEditViewModelFactory(
        Application application,
        ShoppingListItemEditFragmentArgs args
    ) {
      this.application = application;
      this.args = args;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return (T) new ShoppingListItemEditViewModel(application, args);
    }
  }
}
