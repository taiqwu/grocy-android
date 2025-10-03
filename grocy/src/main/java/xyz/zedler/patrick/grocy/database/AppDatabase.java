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

package xyz.zedler.patrick.grocy.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import xyz.zedler.patrick.grocy.dao.ChoreDao;
import xyz.zedler.patrick.grocy.dao.ChoreEntryDao;
import xyz.zedler.patrick.grocy.dao.LocationDao;
import xyz.zedler.patrick.grocy.dao.MealPlanEntryDao;
import xyz.zedler.patrick.grocy.dao.MealPlanSectionDao;
import xyz.zedler.patrick.grocy.dao.MissingItemDao;
import xyz.zedler.patrick.grocy.dao.PendingProductBarcodeDao;
import xyz.zedler.patrick.grocy.dao.PendingProductDao;
import xyz.zedler.patrick.grocy.dao.ProductAveragePriceDao;
import xyz.zedler.patrick.grocy.dao.ProductBarcodeDao;
import xyz.zedler.patrick.grocy.dao.ProductDao;
import xyz.zedler.patrick.grocy.dao.ProductGroupDao;
import xyz.zedler.patrick.grocy.dao.ProductLastPurchasedDao;
import xyz.zedler.patrick.grocy.dao.QuantityUnitConversionDao;
import xyz.zedler.patrick.grocy.dao.QuantityUnitConversionResolvedDao;
import xyz.zedler.patrick.grocy.dao.QuantityUnitDao;
import xyz.zedler.patrick.grocy.dao.RecipeDao;
import xyz.zedler.patrick.grocy.dao.RecipeFulfillmentDao;
import xyz.zedler.patrick.grocy.dao.RecipeNestingDao;
import xyz.zedler.patrick.grocy.dao.RecipePositionDao;
import xyz.zedler.patrick.grocy.dao.RecipePositionResolvedDao;
import xyz.zedler.patrick.grocy.dao.ServerDao;
import xyz.zedler.patrick.grocy.dao.ShoppingListDao;
import xyz.zedler.patrick.grocy.dao.ShoppingListItemDao;
import xyz.zedler.patrick.grocy.dao.StockEntryDao;
import xyz.zedler.patrick.grocy.dao.StockItemDao;
import xyz.zedler.patrick.grocy.dao.StockLocationDao;
import xyz.zedler.patrick.grocy.dao.StoreDao;
import xyz.zedler.patrick.grocy.dao.StoredPurchaseDao;
import xyz.zedler.patrick.grocy.dao.TaskCategoryDao;
import xyz.zedler.patrick.grocy.dao.TaskDao;
import xyz.zedler.patrick.grocy.dao.UserDao;
import xyz.zedler.patrick.grocy.dao.UserfieldDao;
import xyz.zedler.patrick.grocy.dao.VolatileItemDao;
import xyz.zedler.patrick.grocy.model.Chore;
import xyz.zedler.patrick.grocy.model.ChoreEntry;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.MealPlanEntry;
import xyz.zedler.patrick.grocy.model.MealPlanSection;
import xyz.zedler.patrick.grocy.model.MissingItem;
import xyz.zedler.patrick.grocy.model.PendingProduct;
import xyz.zedler.patrick.grocy.model.PendingProductBarcode;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductAveragePrice;
import xyz.zedler.patrick.grocy.model.ProductBarcode;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.ProductLastPurchased;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversion;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversionResolved;
import xyz.zedler.patrick.grocy.model.Recipe;
import xyz.zedler.patrick.grocy.model.RecipeFulfillment;
import xyz.zedler.patrick.grocy.model.RecipeNesting;
import xyz.zedler.patrick.grocy.model.RecipeNestingResolved;
import xyz.zedler.patrick.grocy.model.RecipePosition;
import xyz.zedler.patrick.grocy.model.RecipePositionResolved;
import xyz.zedler.patrick.grocy.model.Server;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.StockEntry;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.model.StockLocation;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.model.StoredPurchase;
import xyz.zedler.patrick.grocy.model.Task;
import xyz.zedler.patrick.grocy.model.TaskCategory;
import xyz.zedler.patrick.grocy.model.User;
import xyz.zedler.patrick.grocy.model.Userfield;
import xyz.zedler.patrick.grocy.model.VolatileItem;
import xyz.zedler.patrick.grocy.repository.MainRepository.OnVersionListener;

@Database(
    entities = {
        ShoppingList.class,
        ShoppingListItem.class,
        Product.class,
        ProductGroup.class,
        QuantityUnit.class,
        Store.class,
        Location.class,
        VolatileItem.class,
        MissingItem.class,
        QuantityUnitConversion.class,
        QuantityUnitConversionResolved.class,
        ProductBarcode.class,
        StockItem.class,
        StockLocation.class,
        Task.class,
        TaskCategory.class,
        ProductLastPurchased.class,
        ProductAveragePrice.class,
        PendingProduct.class,
        PendingProductBarcode.class,
        StoredPurchase.class,
        User.class,
        Chore.class,
        ChoreEntry.class,
        StockEntry.class,
        Server.class,
        Recipe.class,
        RecipeFulfillment.class,
        RecipePosition.class,
        RecipePositionResolved.class,
        RecipeNesting.class,
        MealPlanEntry.class,
        MealPlanSection.class,
        Userfield.class
    },
    views = {
        RecipeNestingResolved.class
    },
    version = 54
)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

  private static AppDatabase INSTANCE;

  public abstract ShoppingListDao shoppingListDao();

  public abstract ShoppingListItemDao shoppingListItemDao();

  public abstract ProductDao productDao();

  public abstract ProductGroupDao productGroupDao();

  public abstract QuantityUnitDao quantityUnitDao();

  public abstract StoreDao storeDao();

  public abstract LocationDao locationDao();

  public abstract VolatileItemDao volatileItemDao();

  public abstract MissingItemDao missingItemDao();

  public abstract QuantityUnitConversionDao quantityUnitConversionDao();

  public abstract QuantityUnitConversionResolvedDao quantityUnitConversionResolvedDao();

  public abstract ProductBarcodeDao productBarcodeDao();

  public abstract StockItemDao stockItemDao();

  public abstract StockLocationDao stockLocationDao();

  public abstract TaskDao taskDao();

  public abstract TaskCategoryDao taskCategoryDao();

  public abstract ProductLastPurchasedDao productLastPurchasedDao();

  public abstract ProductAveragePriceDao productAveragePriceDao();

  public abstract PendingProductDao pendingProductDao();

  public abstract PendingProductBarcodeDao pendingProductBarcodeDao();

  public abstract StoredPurchaseDao storedPurchaseDao();

  public abstract UserDao userDao();

  public abstract ChoreDao choreDao();

  public abstract ChoreEntryDao choreEntryDao();

  public abstract RecipeDao recipeDao();

  public abstract RecipeFulfillmentDao recipeFulfillmentDao();

  public abstract RecipePositionDao recipePositionDao();

  public abstract RecipePositionResolvedDao recipePositionResolvedDao();

  public abstract RecipeNestingDao recipeNestingDao();

  public abstract MealPlanEntryDao mealPlanEntryDao();

  public abstract MealPlanSectionDao mealPlanSectionDao();

  public abstract StockEntryDao stockEntryDao();

  public abstract UserfieldDao userfieldDao();

  public abstract ServerDao serverDao();

  public static AppDatabase getAppDatabase(Context context) {
    if (INSTANCE == null) {
      INSTANCE = Room.databaseBuilder(
          context.getApplicationContext(),
          AppDatabase.class,
          "app_database"
      ).fallbackToDestructiveMigration().build();
    }
    return INSTANCE;
  }

  public static void destroyInstance() {
    INSTANCE = null;
  }

  public void getVersion(OnVersionListener versionListener) {
    Single.fromCallable(() -> getOpenHelper().getReadableDatabase().getVersion())
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess(versionListener::onVersion)
        .onErrorComplete()
        .subscribe();
  }
}
