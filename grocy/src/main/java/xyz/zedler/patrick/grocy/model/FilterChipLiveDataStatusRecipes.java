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

package xyz.zedler.patrick.grocy.model;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;
import androidx.annotation.PluralsRes;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.Constants.PREF;

public class FilterChipLiveDataStatusRecipes extends FilterChipLiveData {

  public final static int STATUS_ALL = 0;
  public final static int STATUS_ENOUGH_IN_STOCK = 1;
  public final static int STATUS_NOT_ENOUGH_BUT_IN_SHOPPING_LIST = 2;
  public final static int STATUS_NOT_ENOUGH = 3;

  private final Application application;
  private final SharedPreferences sharedPrefs;
  private int enoughInStockCount = 0;
  private int notEnoughButInShoppingListCount = 0;
  private int notEnoughCount = 0;
  private boolean showDoneTasks;

  public FilterChipLiveDataStatusRecipes(Application application, Runnable clickListener) {
    this.application = application;
    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(application);
    showDoneTasks = sharedPrefs.getBoolean(PREF.TASKS_SHOW_DONE, false);

    setStatus(STATUS_ALL, null);
    if (clickListener != null) {
      setMenuItemClickListener(item -> {
        setStatus(item.getItemId(), item.getTitle().toString());
        emitValue();
        clickListener.run();
        return true;
      });
    }
  }

  public int getStatus() {
    return getItemIdChecked();
  }

  public FilterChipLiveDataStatusRecipes setStatus(int status, @Nullable String text) {
    if (status == STATUS_ALL) {
      setActive(false);
      setText(application.getString(R.string.property_status));
    } else {
      setActive(true);
      assert text != null;
      setText(text);
    }
    setItemIdChecked(status);
    return this;
  }

  public FilterChipLiveDataStatusRecipes setEnoughInStockCount(int enoughInStockCount) {
    this.enoughInStockCount = enoughInStockCount;
    return this;
  }

  public FilterChipLiveDataStatusRecipes setNotEnoughButInShoppingListCount(int notEnoughButInShoppingListCount) {
    this.notEnoughButInShoppingListCount = notEnoughButInShoppingListCount;
    return this;
  }

  public FilterChipLiveDataStatusRecipes setNotEnoughCount(int notEnoughCount) {
    this.notEnoughCount = notEnoughCount;
    return this;
  }

  public void emitCounts() {
    ArrayList<MenuItemData> menuItemDataList = new ArrayList<>();
    menuItemDataList.add(new MenuItemData(
        STATUS_ALL,
        0,
        application.getString(R.string.action_no_filter)
    ));
    menuItemDataList.add(new MenuItemData(
        STATUS_ENOUGH_IN_STOCK,
        0,
        application.getString(R.string.msg_recipes_enough_in_stock_filter, enoughInStockCount)
    ));
    menuItemDataList.add(new MenuItemData(
        STATUS_NOT_ENOUGH_BUT_IN_SHOPPING_LIST,
        0,
        application.getString(R.string.msg_recipes_not_enough_but_on_shopping_list_filter, notEnoughButInShoppingListCount)
    ));
    menuItemDataList.add(new MenuItemData(
        STATUS_NOT_ENOUGH,
        0,
        application.getString(R.string.msg_recipes_not_enough_filter, notEnoughCount)
    ));
    setMenuItemDataList(menuItemDataList);
    setMenuItemGroups(
        new MenuItemGroup(0, true, true)
    );
    for (MenuItemData menuItemData : menuItemDataList) {
      if (getItemIdChecked() != STATUS_ALL && getItemIdChecked() == menuItemData.getItemId()) {
        setText(menuItemData.getText());
      }
    }
    emitValue();
  }

  private String getQuString(@PluralsRes int string, int count) {
    return application.getResources().getQuantityString(string, count, count);
  }
}