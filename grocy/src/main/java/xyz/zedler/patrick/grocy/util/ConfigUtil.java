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

import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.Nullable;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.CHORES;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.SHOPPING_LIST;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnMultiTypeErrorListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnStringResponseListener;
import xyz.zedler.patrick.grocy.web.NetworkQueue;
import xyz.zedler.patrick.grocy.web.NetworkQueue.QueueItem;

public class ConfigUtil {

  private final static String TAG = ConfigUtil.class.getSimpleName();

  public static void loadInfo(
      DownloadHelper dlHelper,
      GrocyApi api,
      SharedPreferences prefs,
      @Nullable Runnable onSuccessAction,
      @Nullable DownloadHelper.OnMultiTypeErrorListener onError
  ) {

    boolean debug = prefs.getBoolean(
        Constants.SETTINGS.DEBUGGING.ENABLE_DEBUGGING,
        Constants.SETTINGS_DEFAULT.DEBUGGING.ENABLE_DEBUGGING
    );

    NetworkQueue queue = dlHelper.newQueue(updated -> {
      if (onSuccessAction != null) {
        onSuccessAction.run();
      }
    }, error -> {
      if (onError != null) {
        onError.onError(error);
      }
    });

    queue.append(
        getStringData(
            dlHelper,
            api.getSystemConfig(),
            response -> storeSystemConfig(response, prefs, debug)
        ),
        getStringData(
            dlHelper,
            api.getUserSettings(),
            response -> storeUserSettings(response, prefs, debug)
        ),
        getStringData(
            dlHelper,
            api.getSystemInfo(),
            response -> storeSystemInfo(response, prefs, debug)
        )
    ).start();
  }

  private static void storeSystemConfig(String response, SharedPreferences prefs, boolean debug) {
    try {
      JSONObject jsonObject = new JSONObject(response);
      SharedPreferences.Editor editor = prefs.edit()
          .putString(
              Constants.PREF.CURRENCY,
              jsonObject.getString("CURRENCY")
          )
          .putString(
              Constants.PREF.CALENDAR_FIRST_DAY_OF_WEEK,
              jsonObject.getString("CALENDAR_FIRST_DAY_OF_WEEK")
          )
          .putString(
              Constants.PREF.MEAL_PLAN_FIRST_DAY_OF_WEEK,
              jsonObject.getString("MEAL_PLAN_FIRST_DAY_OF_WEEK")
          )
          .putBoolean(
              Constants.PREF.FEATURE_STOCK,
              getBooleanConfig(jsonObject, "FEATURE_FLAG_STOCK")
          )
          .putBoolean(
              Constants.PREF.FEATURE_SHOPPING_LIST,
              getBooleanConfig(jsonObject, "FEATURE_FLAG_SHOPPINGLIST")
          )
          .putBoolean(
              Constants.PREF.FEATURE_STOCK_PRICE_TRACKING,
              getBooleanConfig(jsonObject, "FEATURE_FLAG_STOCK_PRICE_TRACKING")
          )
          .putBoolean(
              Constants.PREF.FEATURE_MULTIPLE_SHOPPING_LISTS,
              getBooleanConfig(jsonObject, "FEATURE_FLAG_SHOPPINGLIST_MULTIPLE_LISTS")
          )
          .putBoolean(
              Constants.PREF.FEATURE_STOCK_LOCATION_TRACKING,
              getBooleanConfig(jsonObject, "FEATURE_FLAG_STOCK_LOCATION_TRACKING")
          )
          .putBoolean(
              Constants.PREF.FEATURE_STOCK_BBD_TRACKING,
              getBooleanConfig(jsonObject, "FEATURE_FLAG_STOCK_BEST_BEFORE_DATE_TRACKING")
          )
          .putBoolean(
              Constants.PREF.FEATURE_STOCK_OPENED_TRACKING,
              getBooleanConfig(jsonObject, "FEATURE_FLAG_STOCK_PRODUCT_OPENED_TRACKING")
          )
          .putBoolean(
              PREF.FEATURE_RECIPES,
              getBooleanConfig(jsonObject, "FEATURE_FLAG_RECIPES")
          )
          .putBoolean(
              PREF.FEATURE_TASKS,
              getBooleanConfig(jsonObject, "FEATURE_FLAG_TASKS")
          )
          .putBoolean(
              PREF.FEATURE_CHORES,
              getBooleanConfig(jsonObject, "FEATURE_FLAG_CHORES")
          )
          .putBoolean(
              PREF.FEATURE_CHORES_ASSIGNMENTS,
              getBooleanConfig(jsonObject, "FEATURE_FLAG_CHORES_ASSIGNMENTS")
          ).putBoolean(
              PREF.FEATURE_LABEL_PRINTER,
              getBooleanConfig(jsonObject, "FEATURE_FLAG_LABEL_PRINTER")
          );
      if (jsonObject.has("FEATURE_FLAG_STOCK_PRODUCT_FREEZING")) {
        editor.putBoolean(
            Constants.PREF.FEATURE_STOCK_FREEZING_TRACKING,
            getBooleanConfig(jsonObject, "FEATURE_FLAG_STOCK_PRODUCT_FREEZING")
        );
      }
      if (jsonObject.has("ENERGY_UNIT")) {
        editor.putString(
            Constants.PREF.ENERGY_UNIT,
            jsonObject.getString("ENERGY_UNIT")
        );
      }
      editor.apply();
    } catch (JSONException e) {
      if (debug) {
        Log.e(TAG, "downloadConfig: " + e);
      }
    }
    if (debug) {
      Log.i(TAG, "downloadConfig: config = " + response);
    }
  }

  private static void storeUserSettings(String response, SharedPreferences prefs, boolean debug) {
    try {
      JSONObject jsonObject = new JSONObject(response);
      SharedPreferences.Editor editor = prefs.edit().putInt(
          STOCK.LOCATION,
          jsonObject.getInt(STOCK.LOCATION)
      ).putInt(
          STOCK.PRODUCT_GROUP,
          jsonObject.getInt(STOCK.PRODUCT_GROUP)
      ).putInt(
          STOCK.QUANTITY_UNIT,
          jsonObject.getInt(STOCK.QUANTITY_UNIT)
      ).putString(
          STOCK.DUE_SOON_DAYS,
          jsonObject.getString(STOCK.DUE_SOON_DAYS)
      ).putBoolean(
          STOCK.DISPLAY_DOTS_IN_STOCK,
          getBoolean(jsonObject, STOCK.DISPLAY_DOTS_IN_STOCK,
              SETTINGS_DEFAULT.STOCK.DISPLAY_DOTS_IN_STOCK, prefs)
      ).putString(
          STOCK.DEFAULT_PURCHASE_AMOUNT,
          jsonObject.getString(STOCK.DEFAULT_PURCHASE_AMOUNT)
      ).putBoolean(
          STOCK.SHOW_PURCHASED_DATE,
          getBoolean(jsonObject, STOCK.SHOW_PURCHASED_DATE,
              SETTINGS_DEFAULT.STOCK.SHOW_PURCHASED_DATE, prefs)
      ).putString(
          STOCK.DEFAULT_CONSUME_AMOUNT,
          jsonObject.getString(STOCK.DEFAULT_CONSUME_AMOUNT)
      ).putBoolean(
          STOCK.USE_QUICK_CONSUME_AMOUNT,
          getBoolean(jsonObject, STOCK.USE_QUICK_CONSUME_AMOUNT,
              SETTINGS_DEFAULT.STOCK.USE_QUICK_CONSUME_AMOUNT, prefs)
      ).putBoolean(
          STOCK.TREAT_OPENED_OUT_OF_STOCK,
          getBoolean(jsonObject, STOCK.TREAT_OPENED_OUT_OF_STOCK,
              SETTINGS_DEFAULT.STOCK.TREAT_OPENED_OUT_OF_STOCK, prefs)
      ).putBoolean(
          SHOPPING_LIST.AUTO_ADD,
          getBoolean(jsonObject, SHOPPING_LIST.AUTO_ADD,
              SETTINGS_DEFAULT.SHOPPING_LIST.AUTO_ADD, prefs)
      );
      if (jsonObject.has(STOCK.DEFAULT_DUE_DAYS)) {
        editor.putInt(
            STOCK.DEFAULT_DUE_DAYS,
            jsonObject.getInt(STOCK.DEFAULT_DUE_DAYS)
        );
      }
      if (jsonObject.has(SHOPPING_LIST.AUTO_ADD_LIST_ID)) {
        editor.putInt(
            SHOPPING_LIST.AUTO_ADD_LIST_ID,
            jsonObject.getInt(SHOPPING_LIST.AUTO_ADD_LIST_ID)
        );
      }
      if (jsonObject.has(STOCK.DECIMAL_PLACES_AMOUNT)) {
        editor.putInt(
            STOCK.DECIMAL_PLACES_AMOUNT,
            jsonObject.getInt(STOCK.DECIMAL_PLACES_AMOUNT)
        );
      }
      if (jsonObject.has(STOCK.DECIMAL_PLACES_PRICES_INPUT)) {
        editor.putInt(
            STOCK.DECIMAL_PLACES_PRICES_INPUT,
            jsonObject.getInt(STOCK.DECIMAL_PLACES_PRICES_INPUT)
        );
      }
      if (jsonObject.has(STOCK.DECIMAL_PLACES_PRICES_DISPLAY)) {
        editor.putInt(
            STOCK.DECIMAL_PLACES_PRICES_DISPLAY,
            jsonObject.getInt(STOCK.DECIMAL_PLACES_PRICES_DISPLAY)
        );
      }
      if (jsonObject.has(STOCK.AUTO_DECIMAL_SEPARATOR_PRICES)) {
        editor.putBoolean(
            STOCK.AUTO_DECIMAL_SEPARATOR_PRICES,
            getBoolean(jsonObject, STOCK.AUTO_DECIMAL_SEPARATOR_PRICES,
                SETTINGS_DEFAULT.STOCK.AUTO_DECIMAL_SEPARATOR_PRICES, prefs)
        );
      }
      if (jsonObject.has(CHORES.DUE_SOON_DAYS)) {
        editor.putInt(
            CHORES.DUE_SOON_DAYS,
            jsonObject.getInt(CHORES.DUE_SOON_DAYS)
        );
      }
      editor.apply();
    } catch (JSONException e) {
      if (debug) {
        Log.e(TAG, "downloadUserSettings: " + e);
      }
    }
    if (debug) {
      Log.i(TAG, "downloadUserSettings: settings = " + response);
    }
  }

  private static boolean getBoolean(
      JSONObject jsonObject,
      String settingKey,
      boolean settingDefault,
      @Nullable SharedPreferences prefs
  ) {
    try {
      Object settingValue = jsonObject.get(settingKey);
      if (settingValue instanceof Integer) {
        return ((int) settingValue) == 1;
      } else if (settingValue instanceof Boolean) {
        return ((boolean) settingValue);
      } else if (settingValue instanceof String && ((String) settingValue).isEmpty()) {
        return false;
      } else if (settingValue instanceof String && NumUtil.isStringInt((String) settingValue)) {
        return Integer.parseInt((String) settingValue) == 1;
      } else if (settingValue instanceof String && settingValue.equals("false")) {
        return false;
      } else if (settingValue instanceof String && settingValue.equals("true")) {
        return true;
      } else {
        return prefs != null ? prefs.getBoolean(settingKey, settingDefault) : settingDefault;
      }
    } catch (JSONException e) {
      Log.e(TAG, "downloadUserSettings: getBoolean: settingKey="
          + settingKey + " Exception:" + e);
      try {
        return prefs != null ? prefs.getBoolean(settingKey, settingDefault) : settingDefault;
      } catch (ClassCastException e1) {
        return settingDefault;
      }
    }
  }

  private static boolean getBooleanConfig(JSONObject jsonObject, String settingKey) {
    return getBoolean(jsonObject, settingKey, true, null);
  }

  private static void storeSystemInfo(String response, SharedPreferences prefs, boolean debug) {
    try {
      prefs.edit()
          .putString(
              Constants.PREF.GROCY_VERSION,
              new JSONObject(response).getJSONObject(
                  "grocy_version"
              ).getString("Version")
          ).apply();
      if (debug) {
        Log.i(TAG, "downloadSystemInfo: " + response);
      }
    } catch (JSONException e) {
      if (debug) {
        Log.e(TAG, "downloadSystemInfo: " + e);
      }
    }
  }

  public static QueueItem getStringData(
      DownloadHelper dlHelper,
      String url,
      OnStringResponseListener onResponseListener
  ) {
    return new QueueItem() {
      @Override
      public void perform(
          @Nullable OnStringResponseListener responseListener,
          @Nullable OnMultiTypeErrorListener errorListener,
          @Nullable String uuid
      ) {
        dlHelper.get(
            url,
            uuid,
            response -> {
              if (dlHelper.debug) {
                Log.i(
                    dlHelper.tag,
                    "download StringData from " + url + " : " + response
                );
              }
              if (onResponseListener != null) {
                onResponseListener.onResponse(response);
              }
              if (responseListener != null) {
                responseListener.onResponse(response);
              }
            },
            error -> {
              if (dlHelper.debug) {
                Log.e(dlHelper.tag, "download StringData: " + error);
              }
              if (errorListener != null) {
                errorListener.onError(error);
              }
            }
        );
      }
    };
  }
}
