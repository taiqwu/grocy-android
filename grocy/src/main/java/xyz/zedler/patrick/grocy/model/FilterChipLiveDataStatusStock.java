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

public class FilterChipLiveDataStatusStock extends FilterChipLiveData {

  public final static int STATUS_ALL = 0;
  public final static int STATUS_DUE_SOON = 1;
  public final static int STATUS_OVERDUE = 2;
  public final static int STATUS_EXPIRED = 3;
  public final static int STATUS_BELOW_MIN = 4;
  public final static int STATUS_IN_STOCK = 5;
  public final static int STATUS_OPENED = 6;
  public final static int STATUS_NOT_FRESH = 7;

  private final Application application;
  private final SharedPreferences sharedPrefs;
  private int dueSoonCount = 0;
  private int overdueCount = 0;
  private int expiredCount = 0;
  private int notFreshCount = 0;
  private int belowStockCount = 0;
  private int inStockCount = 0;
  private int openedCount = 0;

  public FilterChipLiveDataStatusStock(Application application, Runnable clickListener) {
    this.application = application;
    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(application);
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

  public void setStatus(int status, @Nullable String text) {
    if (status == STATUS_ALL) {
      setActive(false);
      setText(application.getString(R.string.property_status));
    } else if (status == STATUS_NOT_FRESH) {
      setActive(true);
      setText(getQuString(R.plurals.msg_not_fresh_products_short, notFreshCount));
    } else {
      setActive(true);
      assert text != null;
      setText(text);
    }
    setItemIdChecked(status);
  }

  public FilterChipLiveDataStatusStock setDueSoonCount(int dueSoonCount) {
    this.dueSoonCount = dueSoonCount;
    return this;
  }

  public FilterChipLiveDataStatusStock setOverdueCount(int overdueCount) {
    this.overdueCount = overdueCount;
    return this;
  }

  public FilterChipLiveDataStatusStock setExpiredCount(int expiredCount) {
    this.expiredCount = expiredCount;
    return this;
  }

  public FilterChipLiveDataStatusStock setNotFreshCount(int notFreshCount) {
    this.notFreshCount = notFreshCount;
    return this;
  }

  public FilterChipLiveDataStatusStock setBelowStockCount(int belowStockCount) {
    this.belowStockCount = belowStockCount;
    return this;
  }

  public FilterChipLiveDataStatusStock setInStockCount(int inStockCount) {
    this.inStockCount = inStockCount;
    return this;
  }

  public FilterChipLiveDataStatusStock setOpenedCount(int openedCount) {
    this.openedCount = openedCount;
    return this;
  }

  public void emitCounts() {
    ArrayList<MenuItemData> menuItemDataList = new ArrayList<>();
    menuItemDataList.add(new MenuItemData(
        STATUS_ALL,
        0,
        application.getString(R.string.action_no_filter)
    ));
    if (sharedPrefs.getBoolean(PREF.FEATURE_STOCK_BBD_TRACKING, true)) {
      menuItemDataList.add(new MenuItemData(
          STATUS_NOT_FRESH,
          1,
          getQuString(R.plurals.msg_not_fresh_products_long, notFreshCount)
      ));
      menuItemDataList.add(new MenuItemData(
          STATUS_DUE_SOON,
          1,
          getQuString(R.plurals.msg_due_products, dueSoonCount)
      ));
      menuItemDataList.add(new MenuItemData(
          STATUS_OVERDUE,
          1,
          getQuString(R.plurals.msg_overdue_products, overdueCount)
      ));
      menuItemDataList.add(new MenuItemData(
          STATUS_EXPIRED,
          1,
          getQuString(R.plurals.msg_expired_products, expiredCount)
      ));
    }
    menuItemDataList.add(new MenuItemData(
        STATUS_BELOW_MIN,
        2,
        getQuString(R.plurals.msg_missing_products, belowStockCount)
    ));
    menuItemDataList.add(new MenuItemData(
        STATUS_IN_STOCK,
        2,
        getQuString(R.plurals.msg_in_stock_products, inStockCount)
    ));
    if (sharedPrefs.getBoolean(PREF.FEATURE_STOCK_OPENED_TRACKING, true)) {
      menuItemDataList.add(new MenuItemData(
          STATUS_OPENED,
          2,
          getQuString(R.plurals.msg_opened_products, openedCount)
      ));
    }
    setMenuItemDataList(menuItemDataList);
    setMenuItemGroups(
        new MenuItemGroup(0, true, true),
        new MenuItemGroup(1, true, true),
        new MenuItemGroup(2, true, true)
    );
    setStatus(getStatus(), null); // for updated text on chip
    emitValue();
  }

  private String getQuString(@PluralsRes int string, int count) {
    return application.getResources().getQuantityString(string, count, count);
  }
}