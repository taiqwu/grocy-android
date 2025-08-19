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
import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.api.GrocyApi.ENTITY;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.FilterChipLiveData;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataFields;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataFields.Field;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataStatusRecipes;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataSort;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataSort.SortOption;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversionResolved;
import xyz.zedler.patrick.grocy.model.Recipe;
import xyz.zedler.patrick.grocy.model.RecipeFulfillment;
import xyz.zedler.patrick.grocy.model.RecipePosition;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.model.Userfield;
import xyz.zedler.patrick.grocy.repository.RecipesRepository;
import xyz.zedler.patrick.grocy.util.ArrayUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;

public class RecipesViewModel extends BaseViewModel {

  private final static String TAG = RecipesViewModel.class.getSimpleName();
  public final static String[] DISPLAYED_USERFIELD_ENTITIES = { ENTITY.RECIPES };

  public final static String SORT_NAME = "sort_name";
  public final static String SORT_ENERGY = "sort_calories";
  public final static String SORT_DUE_SCORE = "sort_due_score";

  public final static String FIELD_DUE_SCORE = "field_due_score";
  public final static String FIELD_FULFILLMENT = "field_fulfillment";
  public final static String FIELD_CALORIES = "field_calories";
  public final static String FIELD_DESIRED_SERVINGS = "field_desired_servings";
  public final static String FIELD_PICTURE = "field_picture";

  private final SharedPreferences sharedPrefs;
  private final DownloadHelper dlHelper;
  private final GrocyApi grocyApi;
  private final RecipesRepository repository;

  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
  private final MutableLiveData<ArrayList<Recipe>> filteredRecipesLive;
  private final FilterChipLiveDataStatusRecipes filterChipLiveDataStatus;
  private final FilterChipLiveDataSort filterChipLiveDataSort;
  private final FilterChipLiveDataFields filterChipLiveDataFields;

  private List<Recipe> recipes;
  private List<RecipeFulfillment> recipeFulfillments;
  private List<RecipePosition> recipePositions;
  private List<Product> products;
  private List<QuantityUnit> quantityUnits;
  private List<QuantityUnitConversionResolved> quantityUnitConversions;
  private HashMap<String, Userfield> userfieldHashMap;

  private String searchInput;

  public RecipesViewModel(@NonNull Application application) {
    super(application);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue, getOfflineLive());
    grocyApi = new GrocyApi(getApplication());
    repository = new RecipesRepository(application);

    infoFullscreenLive = new MutableLiveData<>();
    filteredRecipesLive = new MutableLiveData<>();

    filterChipLiveDataStatus = new FilterChipLiveDataStatusRecipes(
        getApplication(),
        this::updateFilteredRecipesWithTopScroll
    );
    filterChipLiveDataSort = new FilterChipLiveDataSort(
        getApplication(),
        Constants.PREF.RECIPES_SORT_MODE,
        Constants.PREF.RECIPES_SORT_ASCENDING,
        this::updateFilteredRecipesWithTopScroll,
        SORT_NAME,
        new SortOption(SORT_NAME, getString(R.string.property_name)),
        new SortOption(SORT_ENERGY, getString(R.string.property_energy_only)),
        sharedPrefs.getBoolean(PREF.FEATURE_STOCK_BBD_TRACKING, true) ?
            new SortOption(SORT_DUE_SCORE, getString(R.string.property_due_score)) : null
    );
    filterChipLiveDataFields = new FilterChipLiveDataFields(
        getApplication(),
        PREF.RECIPES_FIELDS,
        this::updateFilteredRecipes,
        new Field(FIELD_DUE_SCORE, getString(R.string.property_due_score), true),
        new Field(FIELD_FULFILLMENT, getString(R.string.property_requirements_fulfilled), true),
        new Field(FIELD_CALORIES, getString(R.string.property_calories), false),
        new Field(FIELD_DESIRED_SERVINGS, getString(R.string.property_servings_desired), false),
        new Field(FIELD_PICTURE, getString(R.string.property_picture), true)
    );
  }

  public void loadFromDatabase(boolean downloadAfterLoading) {
    repository.loadFromDatabase(data -> {
      recipes = ArrayUtil.getRecipesWithoutShadowRecipes(data.getRecipes());
      recipeFulfillments = data.getRecipeFulfillments();
      recipePositions = data.getRecipePositions();
      products = data.getProducts();
      quantityUnits = data.getQuantityUnits();
      quantityUnitConversions = data.getQuantityUnitConversionsResolved();
      userfieldHashMap = ArrayUtil.getUserfieldHashMap(data.getUserfields());
      filterChipLiveDataSort.setUserfields(data.getUserfields(), DISPLAYED_USERFIELD_ENTITIES);
      filterChipLiveDataFields.setUserfields(data.getUserfields(), DISPLAYED_USERFIELD_ENTITIES);

      updateFilteredRecipes();
      if (downloadAfterLoading) {
        downloadData(false);
      }
    }, error -> onError(error, TAG));
  }

  public void downloadData(boolean forceUpdate) {
    dlHelper.updateData(
        updated -> {
          if (updated) loadFromDatabase(false);
        },
        error -> onError(error, TAG),
        forceUpdate,
        true,
        Recipe.class,
        RecipeFulfillment.class,
        RecipePosition.class,
        Product.class,
        QuantityUnit.class,
        QuantityUnitConversionResolved.class,
        StockItem.class,
        ShoppingListItem.class,
        Userfield.class
    );
  }

  public void updateFilteredRecipes() {
    ArrayList<Recipe> filteredRecipes = new ArrayList<>();

    int enoughInStockCount = 0;
    int notEnoughInStockButInShoppingListCount = 0;
    int notEnoughInStockCount = 0;

    if (recipes == null || recipeFulfillments == null) {
      loadFromDatabase(true);
      return;
    }
    for (Recipe recipe : recipes) {
      RecipeFulfillment recipeFulfillment = RecipeFulfillment.getRecipeFulfillmentFromRecipeId(recipeFulfillments, recipe.getId());

      if (recipeFulfillment != null) {
        if (recipeFulfillment.isNeedFulfilled()) {
          enoughInStockCount++;
        } else if (recipeFulfillment.isNeedFulfilledWithShoppingList()) {
          notEnoughInStockButInShoppingListCount++;
        } else {
          notEnoughInStockCount++;
        }

        if (filterChipLiveDataStatus.getStatus() != FilterChipLiveDataStatusRecipes.STATUS_ALL) {
          if (filterChipLiveDataStatus.getStatus() == FilterChipLiveDataStatusRecipes.STATUS_ENOUGH_IN_STOCK
              && !recipeFulfillment.isNeedFulfilled()
              || filterChipLiveDataStatus.getStatus() == FilterChipLiveDataStatusRecipes.STATUS_NOT_ENOUGH_BUT_IN_SHOPPING_LIST
              && (recipeFulfillment.isNeedFulfilled() || !recipeFulfillment.isNeedFulfilledWithShoppingList())
              || filterChipLiveDataStatus.getStatus() == FilterChipLiveDataStatusRecipes.STATUS_NOT_ENOUGH
              && (recipeFulfillment.isNeedFulfilled() || recipeFulfillment.isNeedFulfilledWithShoppingList())) {
            continue;
          }
        }
      }

      boolean searchContainsItem = true;
      if (searchInput != null && !searchInput.isEmpty()) {
        searchContainsItem = recipe.getName().toLowerCase().contains(searchInput);

        if (!searchContainsItem && recipeFulfillment != null
            && recipeFulfillment.getProductNamesCommaSeparated() != null) {
          searchContainsItem = recipeFulfillment.getProductNamesCommaSeparated()
              .toLowerCase().contains(searchInput);
        }
      }

      if (!searchContainsItem) {
        continue;
      }

      filteredRecipes.add(recipe);
    }

    String sortMode = filterChipLiveDataSort.getSortMode();
    boolean sortAscending = filterChipLiveDataSort.isSortAscending();

    if (sortMode.equals(SORT_ENERGY)) {
      SortUtil.sortRecipesByCalories(filteredRecipes, recipeFulfillments, sortAscending);
    } else if (sortMode.equals(SORT_DUE_SCORE)) {
      SortUtil.sortRecipesByDueScore(filteredRecipes, recipeFulfillments, sortAscending);
    } else if (sortMode.startsWith(Userfield.NAME_PREFIX)) {
      String userfieldName = sortMode.substring(Userfield.NAME_PREFIX.length());
      Userfield userfield = userfieldHashMap.get(userfieldName);
      if (userfield != null) {
        SortUtil.sortRecipesByUserfieldValue(
            filteredRecipes,
            userfield,
            sortAscending
        );
      } else {
        SortUtil.sortRecipesByName(filteredRecipes, sortAscending);
      }
    } else {
      SortUtil.sortRecipesByName(filteredRecipes, sortAscending);
    }

    filterChipLiveDataStatus
            .setEnoughInStockCount(enoughInStockCount)
            .setNotEnoughButInShoppingListCount(notEnoughInStockButInShoppingListCount)
            .setNotEnoughCount(notEnoughInStockCount)
            .emitCounts();

    filteredRecipesLive.setValue(filteredRecipes);
  }

  public void updateFilteredRecipesWithTopScroll() {
    updateFilteredRecipes();
    sendEvent(Event.SCROLL_UP);
  }

  public ArrayList<RecipeFulfillment> getRecipeFulfillments() {
    return new ArrayList<>(recipeFulfillments);
  }

  public ArrayList<RecipePosition> getRecipePositions() {
    return new ArrayList<>(recipePositions);
  }

  public ArrayList<Product> getProducts() {
    return new ArrayList<>(products);
  }

  public ArrayList<QuantityUnit> getQuantityUnits() {
    return new ArrayList<>(quantityUnits);
  }

  public List<QuantityUnitConversionResolved> getQuantityUnitConversions() {
    return quantityUnitConversions;
  }

  public HashMap<String, Userfield> getUserfieldHashMap() {
    return userfieldHashMap;
  }

  public boolean isSearchActive() {
    return searchInput != null && !searchInput.isEmpty();
  }

  public void resetSearch() {
    searchInput = null;
    setIsSearchVisible(false);
  }

  public MutableLiveData<ArrayList<Recipe>> getFilteredRecipesLive() {
    return filteredRecipesLive;
  }

  public FilterChipLiveData.Listener getFilterChipLiveDataStatus() {
    return () -> filterChipLiveDataStatus;
  }

  public FilterChipLiveData.Listener getFilterChipLiveDataSort() {
    return () -> filterChipLiveDataSort;
  }

  public FilterChipLiveData.Listener getFilterChipLiveDataFields() {
    return () -> filterChipLiveDataFields;
  }

  public List<String> getActiveFields() {
    return filterChipLiveDataFields.getActiveFields();
  }

  @Override
  public SharedPreferences getSharedPrefs() {
    return sharedPrefs;
  }

  public void updateSearchInput(String input) {
    this.searchInput = input.toLowerCase();
    updateFilteredRecipes();
  }

  public String getSortMode() {
    return filterChipLiveDataSort.getSortMode();
  }

  public boolean isSortAscending() {
    return filterChipLiveDataSort.isSortAscending();
  }

  @NonNull
  public MutableLiveData<Boolean> getIsLoadingLive() {
    return isLoadingLive;
  }

  @NonNull
  public MutableLiveData<InfoFullscreen> getInfoFullscreenLive() {
    return infoFullscreenLive;
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
}
