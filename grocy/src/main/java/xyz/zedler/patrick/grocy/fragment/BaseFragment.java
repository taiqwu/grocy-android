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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Spanned;
import android.view.KeyEvent;
import android.view.MotionEvent;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import com.android.volley.VolleyError;
import com.google.android.material.transition.MaterialSharedAxis;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.model.ChoreEntry;
import xyz.zedler.patrick.grocy.model.Language;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.StockEntry;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.model.StockLocation;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.model.Task;
import xyz.zedler.patrick.grocy.model.TaskCategory;
import xyz.zedler.patrick.grocy.model.User;
import xyz.zedler.patrick.grocy.util.NavUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;

public class BaseFragment extends Fragment {

  private MainActivity activity;
  private ViewUtil viewUtil;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    activity = (MainActivity) requireActivity();
    viewUtil = new ViewUtil();

    setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, true));
    setReenterTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, false));
    setReturnTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, false));
    setExitTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, true));
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    viewUtil.cleanUp();
  }

  public SharedPreferences getSharedPrefs() {
    return activity.getSharedPrefs();
  }

  public ViewUtil getViewUtil() {
    return viewUtil;
  }

  public void performHapticClick() {
    activity.hapticUtil.click();
  }

  public void performHapticHeavyClick() {
    activity.hapticUtil.heavyClick();
  }

  protected String getErrorMessage(VolleyError volleyError) {
    if (volleyError != null && volleyError.networkResponse != null) {
      if (volleyError.networkResponse.statusCode == 403) {
        return getString(R.string.error_permission);
      }
    }
    return getString(R.string.error_undefined);
  }

  public boolean dispatchTouchEvent(MotionEvent event) {
    return false;
  }

  public void getActivityResult(int requestCode, int resultCode, @Nullable Intent data) {}

  public boolean isSearchVisible() {
    return false;
  }

  public void dismissSearch() {}

  public void onBottomSheetDismissed() {}

  public boolean onBackPressed() {
    return false;
  }

  public boolean onKeyDown(int keyCode, KeyEvent event) {
    return false;
  }

  public boolean onKeyUp(int keyCode, KeyEvent event) {
    return false;
  }

  public void editObject(Object object) {}

  public void copyProduct(Product product) {}

  public void deleteObject(int objectId) {}

  public void deleteShoppingList(ShoppingList shoppingList) {}

  public void clearShoppingList(ShoppingList shoppingList, boolean onlyDoneItems) {}

  public void toggleDoneStatus(ShoppingListItem shoppingListItem) {}

  public void purchaseItem(ShoppingListItem shoppingListItem) {}

  public void deleteItem(ShoppingListItem shoppingListItem) {}

  public void editItem(ShoppingListItem shoppingListItem) {}

  public void trackNextChoreSchedule(ChoreEntry choreEntry) {}

  public void skipNextChoreSchedule(ChoreEntry choreEntry) {}

  public void trackChoreExecutionNow(ChoreEntry choreEntry) {}

  public void rescheduleNextExecution(ChoreEntry choreEntry) {}

  public void toggleDoneStatus(Task task) {}

  public void deleteTask(Task task) {}

  public void editTask(Task task) {}

  public void deleteRecipePosition(int recipePositionId) {}

  @Nullable
  public MutableLiveData<Integer> getSelectedShoppingListIdLive() {
    return null;
  }

  public void updateConnectivity(boolean isOnline) {}

  public void selectShoppingList(ShoppingList shoppingList) {}

  public void selectProduct(Product product) {}

  public void createQuantityUnit(Bundle argsBundle) {}

  public void selectQuantityUnit(QuantityUnit quantityUnit) {}

  public void selectQuantityUnit(QuantityUnit quantityUnit, Bundle argsBundle) {}

  public void selectPurchasedDate(String purchasedDate) {}

  public void selectDueDate(String dueDate) {}

  public void selectStockLocation(StockLocation stockLocation) {}

  public void selectStockEntry(StockEntry stockEntry) {}

  public void createProductGroup() {}

  public void selectProductGroup(ProductGroup productGroup) {}

  public void createLocation(Bundle args) {}

  public void selectLocation(Location location) {}

  public void selectLocation(Location location, Bundle argsBundle) {}

  public void selectStore(Store store) {}

  public void selectStore(Store store, boolean pinClicked) {}

  public void selectTaskCategory(TaskCategory taskCategory) {}

  public void selectUser(User user) {}

  public void setLanguage(Language language) {}

  public void addBarcodeToNewProduct(String barcode) {}

  public void addBarcodeToExistingProduct(String barcode) {}

  public void saveText(Spanned spanned) {}

  public void saveInput(String text, Bundle argsBundle) {}

  public void performAction(String action, StockItem stockItem) {}

  public void performAction(String action, StockEntry stockEntry) {}

  public void updateShortcuts() {}

  public void updateBarcodeFormats() {}

  public void startTransaction() {}

  public void startTransaction(boolean open) {}

  public void interruptCurrentProductFlow() {}

  public void enableLoginButtons() {}

  public void login(boolean checkVersion) {}

  @NonNull
  public NavController findNavController() {
    return NavHostFragment.findNavController(this);
  }

  public void navigateUp() {
    activity.navUtil.navigateUp();
  }

  public void navigateDeepLinkHorizontally(@StringRes int uri, @NonNull Bundle args) {
    activity.navUtil.navigateDeepLink(NavUtil.getUriWithArgs(getString(uri), args));
  }

  /**
   * Get data from last fragment (which was in backStack on the top of the current one). The last
   * fragment stored this data with <code>setForPreviousFragment</code>.
   *
   * @param key              (String): identifier for value
   * @param observerListener (ObserverListener): observer for callback after value was received
   */
  public void getFromThisDestination(String key, ObserverListener observerListener) {
    NavBackStackEntry backStackEntry = findNavController().getCurrentBackStackEntry();
    assert backStackEntry != null;
    backStackEntry.getSavedStateHandle().getLiveData(key).removeObservers(
        getViewLifecycleOwner()
    );
    backStackEntry.getSavedStateHandle().getLiveData(key).observe(
        getViewLifecycleOwner(),
        value -> {
          observerListener.onChange(value);
          backStackEntry.getSavedStateHandle().remove(key);
          backStackEntry.getSavedStateHandle().getLiveData(key).removeObservers(
              getViewLifecycleOwner()
          );
        }
    );
  }

  /**
   * Returns data from last destination (which was in backStack on the top of the current one)
   * immediately. The last destination stored this data with <code>setForPreviousDestination</code>.
   *
   * @param key (String): identifier for value
   * @return Object: the value or null, if no data was set
   */
  @Nullable
  public Object getFromThisDestinationNow(String key) {
    NavBackStackEntry backStackEntry = findNavController().getCurrentBackStackEntry();
    assert backStackEntry != null;
    return backStackEntry.getSavedStateHandle().get(key);
  }

  /**
   * Set data for previous destination (which is in backStack below the current one)
   *
   * @param key   (String): identifier for value
   * @param value (Object): the value to store
   */
  public void setForPreviousDestination(String key, Object value) {
    NavBackStackEntry backStackEntry = findNavController().getPreviousBackStackEntry();
    assert backStackEntry != null;
    backStackEntry.getSavedStateHandle().set(key, value);
  }

  /**
   * Set data for this destination (which is on top of the backStack)
   *
   * @param key   (String): identifier for value
   * @param value (Object): the value to store
   */
  public void setForThisDestination(String key, Object value) {
    NavBackStackEntry backStackEntry = findNavController().getCurrentBackStackEntry();
    assert backStackEntry != null;
    backStackEntry.getSavedStateHandle().set(key, value);
  }

  /**
   * Set data for any destination (if multiple instances are in backStack, the topmost one)
   *
   * @param destinationId (int): identifier for destination
   * @param key           (String): identifier for value
   * @param value         (Object): the value to store
   */
  public void setForDestination(@IdRes int destinationId, String key, Object value) {
    NavBackStackEntry backStackEntry;
    try {
      backStackEntry = findNavController().getBackStackEntry(destinationId);
    } catch (IllegalArgumentException e) {
      backStackEntry = null;
    }
    if (backStackEntry == null) {
      return;
    }
    backStackEntry.getSavedStateHandle().set(key, value);
  }

  /**
   * Remove set data of this destination (which is on top of the backStack)
   *
   * @param key (String): identifier for value
   */
  public void removeForThisDestination(String key) {
    NavBackStackEntry backStackEntry = findNavController().getCurrentBackStackEntry();
    assert backStackEntry != null;
    backStackEntry.getSavedStateHandle().remove(key);
  }

  NavDestination getPreviousDestination() {
    NavBackStackEntry backStackEntry = findNavController().getPreviousBackStackEntry();
    assert backStackEntry != null;
    return backStackEntry.getDestination();
  }

  public void setOption(Object value, String option) {}

  interface ObserverListener {
    void onChange(Object value);
  }
}
