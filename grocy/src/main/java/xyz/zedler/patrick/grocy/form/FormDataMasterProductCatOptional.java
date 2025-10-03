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

package xyz.zedler.patrick.grocy.form;

import android.app.Application;
import android.content.SharedPreferences;
import android.text.Html;
import android.text.Spanned;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import java.util.ArrayList;
import java.util.List;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.VersionUtil;

public class FormDataMasterProductCatOptional {

  private final Application application;
  private final SharedPreferences sharedPrefs;
  private final MutableLiveData<Boolean> displayHelpLive;
  private final MutableLiveData<Boolean> displayPictureWarningLive;
  private final MutableLiveData<Boolean> isActiveLive;
  private final MutableLiveData<Boolean> scannerVisibilityLive;
  private final MutableLiveData<List<Product>> productsLive;
  private final MutableLiveData<Product> parentProductLive;
  private final MutableLiveData<String> parentProductNameLive;
  private final MutableLiveData<Boolean> parentProductEnabled;
  private final MutableLiveData<Integer> parentProductNameErrorLive;
  private final MutableLiveData<String> descriptionLive;
  private final MutableLiveData<Spanned> descriptionSpannedLive;
  private final MutableLiveData<List<ProductGroup>> productGroupsLive;
  private final MutableLiveData<ProductGroup> productGroupLive;
  private final LiveData<String> productGroupNameLive;
  private final MutableLiveData<String> energyLive;
  private final MutableLiveData<Integer> defaultStockLabelTypeLive;
  private final MutableLiveData<Boolean> neverShowOnStockLive;
  private final MutableLiveData<Boolean> noOwnStockLive;
  private final MutableLiveData<Boolean> shouldNotBeFrozenLive;
  private final MutableLiveData<String> pictureFilenameLive;

  private final MutableLiveData<Product> productLive;
  private boolean filledWithProduct;

  public FormDataMasterProductCatOptional(
      Application application,
      SharedPreferences sharedPrefs,
      boolean beginnerMode
  ) {
    this.application = application;
    this.sharedPrefs = sharedPrefs;
    displayHelpLive = new MutableLiveData<>(beginnerMode);
    displayPictureWarningLive = new MutableLiveData<>(false);
    isActiveLive = new MutableLiveData<>();
    scannerVisibilityLive = new MutableLiveData<>(false);
    productsLive = new MutableLiveData<>(new ArrayList<>());
    parentProductLive = new MutableLiveData<>();
    parentProductNameLive = (MutableLiveData<String>) Transformations.map(
        parentProductLive,
        product -> product != null ? product.getName() : null
    );
    parentProductEnabled = new MutableLiveData<>(true);
    parentProductNameErrorLive = new MutableLiveData<>();
    descriptionLive = new MutableLiveData<>();
    descriptionSpannedLive = new MutableLiveData<>();
    productGroupsLive = new MutableLiveData<>();
    productGroupLive = new MutableLiveData<>();
    productGroupNameLive = Transformations.map(
        productGroupLive,
        productGroup -> productGroup != null ? productGroup.getName() : null
    );
    energyLive = new MutableLiveData<>();
    defaultStockLabelTypeLive = new MutableLiveData<>(0);
    neverShowOnStockLive = new MutableLiveData<>(false);
    noOwnStockLive = new MutableLiveData<>(false);
    shouldNotBeFrozenLive = new MutableLiveData<>(false);
    pictureFilenameLive = new MutableLiveData<>("");

    productLive = new MutableLiveData<>();
    filledWithProduct = false;
  }

  public MutableLiveData<Boolean> getDisplayHelpLive() {
    return displayHelpLive;
  }

  public void toggleDisplayHelpLive() {
    assert displayHelpLive.getValue() != null;
    displayHelpLive.setValue(!displayHelpLive.getValue());
  }

  public MutableLiveData<Boolean> getDisplayPictureWarningLive() {
    return displayPictureWarningLive;
  }

  public void toggleDisplayPictureWarningLive() {
    assert displayPictureWarningLive.getValue() != null;
    displayPictureWarningLive.setValue(!displayPictureWarningLive.getValue());
  }

  public MutableLiveData<Boolean> getIsActiveLive() {
    return isActiveLive;
  }

  public void toggleActiveLive() {
    isActiveLive.setValue(isActiveLive.getValue() == null || !isActiveLive.getValue());
  }

  public MutableLiveData<List<Product>> getProductsLive() {
    return productsLive;
  }

  public MutableLiveData<Product> getParentProductLive() {
    return parentProductLive;
  }

  public MutableLiveData<String> getParentProductNameLive() {
    return parentProductNameLive;
  }

  public MutableLiveData<Integer> getParentProductNameErrorLive() {
    return parentProductNameErrorLive;
  }

  public MutableLiveData<Boolean> getParentProductEnabled() {
    return parentProductEnabled;
  }

  public MutableLiveData<String> getDescriptionLive() {
    return descriptionLive;
  }

  public MutableLiveData<Spanned> getDescriptionSpannedLive() {
    return descriptionSpannedLive;
  }

  public MutableLiveData<List<ProductGroup>> getProductGroupsLive() {
    return productGroupsLive;
  }

  public MutableLiveData<ProductGroup> getProductGroupLive() {
    return productGroupLive;
  }

  public LiveData<String> getProductGroupNameLive() {
    return productGroupNameLive;
  }

  public MutableLiveData<String> getEnergyLive() {
    return energyLive;
  }

  public MutableLiveData<Integer> getDefaultStockLabelTypeLive() {
    return defaultStockLabelTypeLive;
  }

  public void setDefaultStockLabelTypeLive(int type) {
    defaultStockLabelTypeLive.setValue(type);
  }

  public MutableLiveData<Boolean> getNeverShowOnStockLive() {
    return neverShowOnStockLive;
  }

  public void toggleNeverShowOnStockLive() {
    neverShowOnStockLive.setValue(
        neverShowOnStockLive.getValue() == null || !neverShowOnStockLive.getValue()
    );
  }

  public MutableLiveData<Boolean> getNoOwnStockLive() {
    return noOwnStockLive;
  }

  public void toggleNoOwnStockLive() {
    noOwnStockLive.setValue(
        noOwnStockLive.getValue() == null || !noOwnStockLive.getValue()
    );
  }

  public boolean getNoOwnStockVisible() {
    return VersionUtil.isGrocyServerMin330(sharedPrefs);
  }

  public MutableLiveData<Boolean> getShouldNotBeFrozenLive() {
    return shouldNotBeFrozenLive;
  }

  public void toggleShouldNotBeFrozenLive() {
    shouldNotBeFrozenLive.setValue(
        shouldNotBeFrozenLive.getValue() == null || !shouldNotBeFrozenLive.getValue()
    );
  }

  public MutableLiveData<String> getPictureFilenameLive() {
    return pictureFilenameLive;
  }

  public MutableLiveData<Boolean> getScannerVisibilityLive() {
    return scannerVisibilityLive;
  }

  public boolean isScannerVisible() {
    assert scannerVisibilityLive.getValue() != null;
    return scannerVisibilityLive.getValue();
  }

  public void toggleScannerVisibility() {
    scannerVisibilityLive.setValue(!isScannerVisible());
  }

  public MutableLiveData<Product> getProductLive() {
    return productLive;
  }

  public boolean isParentProductValid() {
    if (parentProductNameLive.getValue() == null || parentProductNameLive.getValue().isEmpty()) {
      if (parentProductLive.getValue() != null) {
        parentProductLive.setValue(null);
      }
      parentProductNameErrorLive.setValue(null);
      return true;
    }
    Product product = getProductFromName(parentProductNameLive.getValue());
    if (product != null) {
      parentProductLive.setValue(product);
      parentProductNameErrorLive.setValue(null);
      return true;
    } else {
      parentProductNameErrorLive.setValue(R.string.error_invalid_product);
      return false;
    }
  }

  private Product getProductFromName(String name) {
    if (productsLive.getValue() == null) {
      return null;
    }
    for (Product product : productsLive.getValue()) {
      if (product.getName().equals(name)) {
        return product;
      }
    }
    return null;
  }

  private Product getProductFromId(String id) {
    if (productsLive.getValue() == null || id == null) {
      return null;
    }
    int idInt = Integer.parseInt(id);
    for (Product product : productsLive.getValue()) {
      if (product.getId() == idInt) {
        return product;
      }
    }
    return null;
  }

  private ProductGroup getProductGroupFromId(String id) {
    if (productGroupsLive.getValue() == null || id == null || id.isEmpty()) {
      return null;
    }
    int idInt = Integer.parseInt(id);
    for (ProductGroup productGroup : productGroupsLive.getValue()) {
      if (productGroup.getId() == idInt) {
        return productGroup;
      }
    }
    return null;
  }

  public boolean isFormValid() {
    boolean valid = isParentProductValid();
        /*boolean valid = isProductNameValid();
        valid = isAmountValid() && valid;
        valid = isQuantityUnitValid() && valid;*/
    return valid;
  }

  public static boolean isFormInvalid(@Nullable Product product) {
    if (product == null) {
      return true;
    }
    boolean valid = true;
    return !valid;
  }

  public Product fillProduct(@NonNull Product product) {
    if (!isFormValid()) {
      return product;
    }
    ProductGroup pGroup = productGroupLive.getValue();
    if (isActiveLive.getValue() != null) {
      product.setActive(isActiveLive.getValue());
    }
    product.setParentProductId(parentProductLive.getValue() != null
        ? String.valueOf(parentProductLive.getValue().getId()) : null);
    product.setDescription(descriptionLive.getValue() != null
        ? descriptionLive.getValue() : null);
    product.setProductGroupId(pGroup != null ? String.valueOf(pGroup.getId()) : null);
    product.setCalories(energyLive.getValue());
    if (isFeatureLabelPrintEnabled()) {
      product.setDefaultStockLabelType(String.valueOf(defaultStockLabelTypeLive.getValue()));
    } else {
      product.setDefaultStockLabelType(String.valueOf(0));
    }
    if (neverShowOnStockLive.getValue() != null) {
      product.setHideOnStockOverviewBoolean(neverShowOnStockLive.getValue());
    }
    if (noOwnStockLive.getValue() != null) {
      product.setNoOwnStockBoolean(noOwnStockLive.getValue());
    }
    if (shouldNotBeFrozenLive.getValue() != null) {
      product.setShouldNotBeFrozenBoolean(shouldNotBeFrozenLive.getValue());
    }
    product.setPictureFileName(pictureFilenameLive.getValue());
    return product;
  }

  public void fillWithProductIfNecessary(Product product) {
    if (filledWithProduct || product == null) {
      return;
    }

    isActiveLive.setValue(product.isActive());
    parentProductLive.setValue(getProductFromId(product.getParentProductId()));

    List<Product> products = productsLive.getValue();
    if (products != null) {
      for (Product productTemp : products) {
        if (NumUtil.isStringInt(productTemp.getParentProductId())
            && Integer.parseInt(productTemp.getParentProductId()) == product.getId()) {
          parentProductEnabled.setValue(false);
          break;
        }
      }
    }
    descriptionLive.setValue(product.getDescription());
    descriptionSpannedLive.setValue(product.getDescription() != null
        ? Html.fromHtml(product.getDescription()) : null);
    productGroupLive.setValue(getProductGroupFromId(product.getProductGroupId()));
    energyLive.setValue(product.getCalories());
    defaultStockLabelTypeLive.setValue(product.getDefaultStockLabelTypeInt());
    neverShowOnStockLive.setValue(product.getHideOnStockOverviewBoolean());
    noOwnStockLive.setValue(product.getNoOwnStockBoolean());
    shouldNotBeFrozenLive.setValue(product.getShouldNotBeFrozenBoolean());
    pictureFilenameLive.setValue(product.getPictureFileName());
    filledWithProduct = true;
  }

  public boolean isFeatureLabelPrintEnabled() {
    return sharedPrefs.getBoolean(PREF.FEATURE_LABEL_PRINTER, false);
  }

  public boolean isFeatureFreezingEnabled() {
    return sharedPrefs.getBoolean(PREF.FEATURE_STOCK_FREEZING_TRACKING, false);
  }
}
