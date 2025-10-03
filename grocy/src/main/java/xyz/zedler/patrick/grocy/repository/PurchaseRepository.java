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

package xyz.zedler.patrick.grocy.repository;

import android.app.Application;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.List;
import xyz.zedler.patrick.grocy.database.AppDatabase;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.PendingProduct;
import xyz.zedler.patrick.grocy.model.PendingProductBarcode;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductBarcode;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversionResolved;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.model.StoredPurchase;
import xyz.zedler.patrick.grocy.util.RxJavaUtil;

public class PurchaseRepository {

  private final AppDatabase appDatabase;

  public PurchaseRepository(Application application) {
    this.appDatabase = AppDatabase.getAppDatabase(application);
  }

  public interface DataListener {
    void actionFinished(PurchaseData data);
  }

  public static class PurchaseData {

    private final List<Product> products;
    private final List<PendingProduct> pendingProducts;
    private final List<ProductBarcode> barcodes;
    private final List<PendingProductBarcode> pendingProductBarcodes;
    private final List<QuantityUnit> quantityUnits;
    private final List<QuantityUnitConversionResolved> quantityUnitConversions;
    private final List<Store> stores;
    private final List<Location> locations;
    private final List<ShoppingListItem> shoppingListItems;
    private final List<StoredPurchase> storedPurchases;

    public PurchaseData(
        List<Product> products,
        List<PendingProduct> pendingProducts,
        List<ProductBarcode> barcodes,
        List<PendingProductBarcode> pendingProductBarcodes,
        List<QuantityUnit> quantityUnits,
        List<QuantityUnitConversionResolved> quantityUnitConversions,
        List<Store> stores,
        List<Location> locations,
        List<ShoppingListItem> shoppingListItems,
        List<StoredPurchase> storedPurchases
    ) {
      this.products = products;
      this.pendingProducts = pendingProducts;
      this.barcodes = barcodes;
      this.pendingProductBarcodes = pendingProductBarcodes;
      this.quantityUnits = quantityUnits;
      this.quantityUnitConversions = quantityUnitConversions;
      this.stores = stores;
      this.locations = locations;
      this.shoppingListItems = shoppingListItems;
      this.storedPurchases = storedPurchases;
    }

    public List<Product> getProducts() {
      return products;
    }

    public List<PendingProduct> getPendingProducts() {
      return pendingProducts;
    }

    public List<ProductBarcode> getBarcodes() {
      return barcodes;
    }

    public List<PendingProductBarcode> getPendingProductBarcodes() {
      return pendingProductBarcodes;
    }

    public List<QuantityUnit> getQuantityUnits() {
      return quantityUnits;
    }

    public List<QuantityUnitConversionResolved> getQuantityUnitConversionsResolved() {
      return quantityUnitConversions;
    }

    public List<Store> getStores() {
      return stores;
    }

    public List<Location> getLocations() {
      return locations;
    }

    public List<ShoppingListItem> getShoppingListItems() {
      return shoppingListItems;
    }

    public List<StoredPurchase> getStoredPurchases() {
      return storedPurchases;
    }
  }

  public void loadFromDatabase(DataListener onSuccess, Consumer<Throwable> onError) {
    RxJavaUtil
        .zip(
            appDatabase.productDao().getProducts(),
            appDatabase.pendingProductDao().getPendingProducts(),
            appDatabase.productBarcodeDao().getProductBarcodes(),
            appDatabase.pendingProductBarcodeDao().getProductBarcodes(),
            appDatabase.quantityUnitDao().getQuantityUnits(),
            appDatabase.quantityUnitConversionResolvedDao().getConversionsResolved(),
            appDatabase.storeDao().getStores(),
            appDatabase.locationDao().getLocations(),
            appDatabase.shoppingListItemDao().getShoppingListItems(),
            appDatabase.storedPurchaseDao().getStoredPurchases(),
            PurchaseData::new
        )
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess(onSuccess::actionFinished)
        .doOnError(onError)
        .onErrorComplete()
        .subscribe();
  }

  public void insertPendingProduct(PendingProduct pendingProduct) {
    appDatabase.pendingProductDao().insertPendingProduct(pendingProduct)
        .subscribeOn(Schedulers.io()).subscribe();
  }

  public void insertStoredPurchase(
          StoredPurchase storedPurchase,
          SuccessIdListener onSuccess,
          Runnable onError
  ) {
    appDatabase.storedPurchaseDao().insertStoredPurchase(storedPurchase)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess(onSuccess::onSuccess)
        .doOnError(e -> onError.run())
        .subscribe();
  }

  public void deleteStoredPurchase(long id, Runnable onSuccess, Runnable onError) {
    appDatabase.storedPurchaseDao().deleteStoredPurchase(id)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess(i -> onSuccess.run())
        .doOnError(e -> onError.run())
        .subscribe();
  }

  public void insertPendingProductBarcode(PendingProductBarcode barcode, Runnable onFinished) {
    appDatabase.pendingProductBarcodeDao()
        .insertProductBarcode(barcode)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doFinally(onFinished::run)
        .subscribe();
  }

  public interface SuccessIdListener {
    void onSuccess(Long id);
  }
}
