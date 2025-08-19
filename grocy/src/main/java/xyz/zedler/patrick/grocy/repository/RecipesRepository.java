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
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversionResolved;
import xyz.zedler.patrick.grocy.model.Recipe;
import xyz.zedler.patrick.grocy.model.RecipeFulfillment;
import xyz.zedler.patrick.grocy.model.RecipePosition;
import xyz.zedler.patrick.grocy.model.RecipePositionResolved;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.model.Userfield;
import xyz.zedler.patrick.grocy.util.RxJavaUtil;

public class RecipesRepository {

  private final AppDatabase appDatabase;

  public RecipesRepository(Application application) {
    this.appDatabase = AppDatabase.getAppDatabase(application);
  }

  public interface RecipesDataListener {

    void actionFinished(RecipesData data);
  }

  public static class RecipesData {

    private final List<Recipe> recipes;
    private final List<RecipeFulfillment> recipeFulfillments;
    private final List<RecipePosition> recipePositions;
    private final List<RecipePositionResolved> recipePositionsResolved;
    private final List<Product> products;
    private final List<QuantityUnit> quantityUnits;
    private final List<QuantityUnitConversionResolved> quantityUnitConversions;
    private final List<StockItem> stockItems;
    private final List<ShoppingListItem> shoppingListItems;
    private final List<Userfield> userfields;

    public RecipesData(
        List<Recipe> recipes,
        List<RecipeFulfillment> recipeFulfillments,
        List<RecipePosition> recipePositions,
        List<RecipePositionResolved> recipePositionsResolved,
        List<Product> products,
        List<QuantityUnit> quantityUnits,
        List<QuantityUnitConversionResolved> quantityUnitConversions,
        List<StockItem> stockItems,
        List<ShoppingListItem> shoppingListItems,
        List<Userfield> userfields
    ) {
      this.recipes = recipes;
      this.recipeFulfillments = recipeFulfillments;
      this.recipePositions = recipePositions;
      this.recipePositionsResolved = recipePositionsResolved;
      this.products = products;
      this.quantityUnits = quantityUnits;
      this.quantityUnitConversions = quantityUnitConversions;
      this.stockItems = stockItems;
      this.shoppingListItems = shoppingListItems;
      this.userfields = userfields;
    }

    public List<Recipe> getRecipes() {
      return recipes;
    }

    public List<RecipeFulfillment> getRecipeFulfillments() {
      return recipeFulfillments;
    }

    public List<RecipePosition> getRecipePositions() {
      return recipePositions;
    }

    public List<RecipePositionResolved> getRecipePositionsResolved() {
      return recipePositionsResolved;
    }

    public List<Product> getProducts() {
      return products;
    }

    public List<QuantityUnit> getQuantityUnits() {
      return quantityUnits;
    }

    public List<QuantityUnitConversionResolved> getQuantityUnitConversionsResolved() {
      return quantityUnitConversions;
    }

    public List<StockItem> getStockItems() {
      return stockItems;
    }

    public List<ShoppingListItem> getShoppingListItems() {
      return shoppingListItems;
    }

    public List<Userfield> getUserfields() {
      return userfields;
    }
  }

  public void loadFromDatabase(RecipesDataListener onSuccess, Consumer<Throwable> onError) {
    RxJavaUtil
        .zip(
            appDatabase.recipeDao().getRecipes(),
            appDatabase.recipeFulfillmentDao().getRecipeFulfillments(),
            appDatabase.recipePositionDao().getRecipePositions(),
            appDatabase.recipePositionResolvedDao().getRecipePositionsResolved(),
            appDatabase.productDao().getProducts(),
            appDatabase.quantityUnitDao().getQuantityUnits(),
            appDatabase.quantityUnitConversionResolvedDao().getConversionsResolved(),
            appDatabase.stockItemDao().getStockItems(),
            appDatabase.shoppingListItemDao().getShoppingListItems(),
            appDatabase.userfieldDao().getUserfields(),
            RecipesData::new
        )
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess(onSuccess::actionFinished)
        .doOnError(onError)
        .onErrorComplete()
        .subscribe();
  }
}
