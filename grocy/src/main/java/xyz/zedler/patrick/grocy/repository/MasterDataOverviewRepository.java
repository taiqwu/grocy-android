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
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.List;
import xyz.zedler.patrick.grocy.database.AppDatabase;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.model.TaskCategory;

public class MasterDataOverviewRepository {

  private final AppDatabase appDatabase;

  public MasterDataOverviewRepository(Application application) {
    this.appDatabase = AppDatabase.getAppDatabase(application);
  }

  public interface DataListener {
    void actionFinished(MasterDataOverviewData data);
  }

  public static class MasterDataOverviewData {

    private final List<Store> stores;
    private final List<Location> locations;
    private final List<ProductGroup> productGroups;
    private final List<QuantityUnit> quantityUnits;
    private final List<Product> products;
    private final List<TaskCategory> taskCategories;

    public MasterDataOverviewData(
        List<Store> stores,
        List<Location> locations,
        List<ProductGroup> productGroups,
        List<QuantityUnit> quantityUnits,
        List<Product> products,
        List<TaskCategory> taskCategories
    ) {
      this.stores = stores;
      this.locations = locations;
      this.productGroups = productGroups;
      this.quantityUnits = quantityUnits;
      this.products = products;
      this.taskCategories = taskCategories;
    }

    public List<Store> getStores() {
      return stores;
    }

    public List<Location> getLocations() {
      return locations;
    }

    public List<ProductGroup> getProductGroups() {
      return productGroups;
    }

    public List<QuantityUnit> getQuantityUnits() {
      return quantityUnits;
    }

    public List<Product> getProducts() {
      return products;
    }

    public List<TaskCategory> getTaskCategories() {
      return taskCategories;
    }
  }

  public void loadFromDatabase(DataListener onSuccess, Consumer<Throwable> onError) {
    Single
        .zip(
            appDatabase.storeDao().getStores(),
            appDatabase.locationDao().getLocations(),
            appDatabase.productGroupDao().getProductGroups(),
            appDatabase.quantityUnitDao().getQuantityUnits(),
            appDatabase.productDao().getProducts(),
            appDatabase.taskCategoryDao().getTaskCategories(),
            MasterDataOverviewData::new
        )
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess(onSuccess::actionFinished)
        .doOnError(onError)
        .onErrorComplete()
        .subscribe();
  }
}
