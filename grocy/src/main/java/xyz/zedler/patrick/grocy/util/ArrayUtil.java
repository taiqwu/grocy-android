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

package xyz.zedler.patrick.grocy.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import xyz.zedler.patrick.grocy.model.Chore;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.MealPlanEntry;
import xyz.zedler.patrick.grocy.model.MissingItem;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductAveragePrice;
import xyz.zedler.patrick.grocy.model.ProductBarcode;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.ProductLastPurchased;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.Recipe;
import xyz.zedler.patrick.grocy.model.RecipeFulfillment;
import xyz.zedler.patrick.grocy.model.RecipePosition;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.model.Task;
import xyz.zedler.patrick.grocy.model.TaskCategory;
import xyz.zedler.patrick.grocy.model.User;
import xyz.zedler.patrick.grocy.model.Userfield;

public class ArrayUtil {

  public static HashMap<Integer, Product> getProductsHashMap(List<Product> products) {
    HashMap<Integer, Product> hashMap = new HashMap<>();
    for (Product p : products) {
      hashMap.put(p.getId(), p);
    }
    return hashMap;
  }

  public static HashMap<Integer, String> getProductNamesHashMap(List<Product> products) {
    if (products == null) {
      return null;
    }
    HashMap<Integer, String> productNamesHashMap = new HashMap<>();
    for (Product product : products) {
      productNamesHashMap.put(product.getId(), product.getName());
    }
    return productNamesHashMap;
  }

  public static HashMap<Integer, ProductLastPurchased> getProductLastPurchasedHashMap(
      List<ProductLastPurchased> productsLastPurchased
  ) {
    HashMap<Integer, ProductLastPurchased> hashMap = new HashMap<>();
    if (productsLastPurchased == null) return hashMap;
    for (ProductLastPurchased p : productsLastPurchased) {
      hashMap.put(p.getProductId(), p);
    }
    return hashMap;
  }

  public static HashMap<Integer, String> getProductAveragePriceHashMap(
      List<ProductAveragePrice> productsAveragePrice
  ) {
    HashMap<Integer, String> hashMap = new HashMap<>();
    if (productsAveragePrice == null) return hashMap;
    for (ProductAveragePrice p : productsAveragePrice) {
      hashMap.put(p.getProductId(), p.getPrice());
    }
    return hashMap;
  }

  public static ArrayList<Integer> getMissingProductsIds(List<MissingItem> missingItems) {
    ArrayList<Integer> missingProductIds = new ArrayList<>();
    for (MissingItem missingItem : missingItems) {
      missingProductIds.add(missingItem.getId());
    }
    return missingProductIds;
  }

  public static HashMap<Integer, Location> getLocationsHashMap(List<Location> locations) {
    HashMap<Integer, Location> hashMap = new HashMap<>();
    for (Location l : locations) {
      hashMap.put(l.getId(), l);
    }
    return hashMap;
  }

  public static HashMap<Integer, ProductGroup> getProductGroupsHashMap(
      List<ProductGroup> productGroups
  ) {
    HashMap<Integer, ProductGroup> hashMap = new HashMap<>();
    for (ProductGroup p : productGroups) {
      hashMap.put(p.getId(), p);
    }
    return hashMap;
  }

  public static HashMap<String, ProductBarcode> getProductBarcodesHashMap(
      List<ProductBarcode> productBarcodes
  ) {
    HashMap<String, ProductBarcode> productBarcodeHashMap = new HashMap<>();
    for (ProductBarcode barcode : productBarcodes) {
      productBarcodeHashMap.put(barcode.getBarcode().toLowerCase(), barcode);
    }
    return productBarcodeHashMap;
  }

  public static HashMap<Integer, Store> getStoresHashMap(List<Store> stores) {
    HashMap<Integer, Store> hashMap = new HashMap<>();
    for (Store s : stores) {
      hashMap.put(s.getId(), s);
    }
    return hashMap;
  }

  public static HashMap<Integer, QuantityUnit> getQuantityUnitsHashMap(
      List<QuantityUnit> quantityUnits
  ) {
    HashMap<Integer, QuantityUnit> hashMap = new HashMap<>();
    for (QuantityUnit q : quantityUnits) {
      hashMap.put(q.getId(), q);
    }
    return hashMap;
  }

  public static HashMap<Integer, Task> getTasksHashMap(List<Task> tasks) {
    HashMap<Integer, Task> hashMap = new HashMap<>();
    for (Task t : tasks) {
      hashMap.put(t.getId(), t);
    }
    return hashMap;
  }

  public static HashMap<Integer, TaskCategory> getTaskCategoriesHashMap(
      List<TaskCategory> taskCategories
  ) {
    HashMap<Integer, TaskCategory> hashMap = new HashMap<>();
    for (TaskCategory t : taskCategories) {
      hashMap.put(t.getId(), t);
    }
    return hashMap;
  }

  public static HashMap<Integer, Chore> getChoresHashMap(List<Chore> chores) {
    HashMap<Integer, Chore> hashMap = new HashMap<>();
    for (Chore c : chores) {
      hashMap.put(c.getId(), c);
    }
    return hashMap;
  }

  public static HashMap<Integer, User> getUsersHashMap(List<User> users) {
    HashMap<Integer, User> hashMap = new HashMap<>();
    for (User u : users) {
      hashMap.put(u.getId(), u);
    }
    return hashMap;
  }

  public static HashMap<Integer, ShoppingListItem> getShoppingListItemHashMap(
      List<ShoppingListItem> shoppingListItems
  ) {
    if (shoppingListItems == null) {
      return null;
    }
    HashMap<Integer, ShoppingListItem> hashMap = new HashMap<>();
    for (ShoppingListItem item : shoppingListItems) {
      hashMap.put(item.getId(), item);
    }
    return hashMap;
  }

  public static HashMap<Integer, StockItem> getStockItemHashMap(List<StockItem> stockItems) {
    HashMap<Integer, StockItem> stockItemHashMap = new HashMap<>();
    for (StockItem stockItem : stockItems) {
      stockItemHashMap.put(stockItem.getProductId(), stockItem);
    }
    return stockItemHashMap;
  }

  public static HashMap<Integer, Recipe> getRecipesHashMap(List<Recipe> recipes) {
    HashMap<Integer, Recipe> recipeHashMap = new HashMap<>();
    for (Recipe recipe : recipes) {
      recipeHashMap.put(recipe.getId(), recipe);
    }
    return recipeHashMap;
  }

  public static List<Recipe> getRecipesWithoutShadowRecipes(List<Recipe> allRecipes) {
    List<Recipe> recipes = new ArrayList<>();
    for (Recipe recipe : allRecipes) {
      if (recipe.getId() >= 0) {
        recipes.add(recipe);
      }
    }
    return recipes;
  }

  public static List<Recipe> getShadowRecipes(List<Recipe> allRecipes) {
    List<Recipe> shadowRecipes = new ArrayList<>();
    for (Recipe recipe : allRecipes) {
      if (recipe.getId() < 0) {
        shadowRecipes.add(recipe);
      }
    }
    return shadowRecipes;
  }

  public static HashMap<Integer, RecipePosition> getRecipePositionHashMap(
      List<RecipePosition> recipePositions
  ) {
    HashMap<Integer, RecipePosition> recipePositionHashMap = new HashMap<>();
    for (RecipePosition recipePosition : recipePositions) {
      recipePositionHashMap.put(recipePosition.getId(), recipePosition);
    }
    return recipePositionHashMap;
  }

  public static HashMap<Integer, RecipeFulfillment> getRecipeFulfillmentHashMap(
      List<RecipeFulfillment> recipeFulfillments
  ) {
    HashMap<Integer, RecipeFulfillment> recipeFulfillmentHashMap = new HashMap<>();
    for (RecipeFulfillment recipeFulfillment : recipeFulfillments) {
      recipeFulfillmentHashMap.put(recipeFulfillment.getRecipeId(), recipeFulfillment);
    }
    return recipeFulfillmentHashMap;
  }

  public static HashMap<String, RecipeFulfillment> getRecipeResolvedFulfillmentForMealplanHashMap(
      HashMap<Integer, RecipeFulfillment> recipeFulfillmentHashMap,
      List<Recipe> recipes
  ) {
    HashMap<String, RecipeFulfillment> resolvedFulfillments = new HashMap<>();
    for (Recipe recipe : recipes) {
      if (recipe.getId() >= 0) {
        continue;
      }
      RecipeFulfillment recipeFulfillment = recipeFulfillmentHashMap.get(recipe.getId());
      resolvedFulfillments.put(recipe.getName(), recipeFulfillment);
    }
    return resolvedFulfillments;
  }

  public static HashMap<String, List<MealPlanEntry>> getMealPlanEntriesForDayHashMap(
      List<MealPlanEntry> mealPlanEntries
  ) {
    HashMap<String, List<MealPlanEntry>> hashMap = new HashMap<>();
    for (MealPlanEntry mealPlanEntry : mealPlanEntries) {
      List<MealPlanEntry> mealPlanEntriesForDay = hashMap.get(mealPlanEntry.getDay());
      if (mealPlanEntriesForDay != null) {
        mealPlanEntriesForDay.add(mealPlanEntry);
      } else {
        mealPlanEntriesForDay = new ArrayList<>();
        mealPlanEntriesForDay.add(mealPlanEntry);
        hashMap.put(mealPlanEntry.getDay(), mealPlanEntriesForDay);
      }
    }
    return hashMap;
  }

  public static boolean contains(String[] array, String value) {
    if (array != null) {
      for (String i : array) {
        if (i != null && i.equals(value)) {
          return true;
        }
      }
    }
    return false;
  }

  public static HashMap<String, Userfield> getUserfieldHashMap(List<Userfield> userfields) {
    HashMap<String, Userfield> hashMap = new HashMap<>();
    for (Userfield u : userfields) {
      hashMap.put(u.getName(), u);
    }
    return hashMap;
  }

  public static boolean areListsEqualIgnoreOrder(List<String> list1, List<String> list2) {
    Set<String> set1 = new HashSet<>(list1);
    Set<String> set2 = new HashSet<>(list2);
    return set1.equals(set2);
  }
}
