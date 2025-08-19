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
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.APPEARANCE;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.BEHAVIOR;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.NETWORK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.NOTIFICATIONS;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.RECIPES;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.SCANNER;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.SHOPPING_LIST;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.SHOPPING_MODE;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.BarcodeFormatsBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.CompatibilityBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.InputBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.LocationsBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ProductGroupsBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.QuantityUnitsBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ShoppingListsBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ShortcutsBottomSheet;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnSettingUploadListener;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.repository.MainRepository;
import xyz.zedler.patrick.grocy.util.ConfigUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PrefsUtil;
import xyz.zedler.patrick.grocy.util.ReminderUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.util.VersionUtil;

public class SettingsViewModel extends BaseViewModel {

  private static final String TAG = SettingsViewModel.class.getSimpleName();
  private final SharedPreferences sharedPrefs;

  private final DownloadHelper dlHelper;
  private final GrocyApi grocyApi;
  private final MainRepository repository;
  private final ReminderUtil reminderUtil;

  private MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<Boolean> getExternalScannerEnabledLive;
  private final MutableLiveData<Boolean> needsRestartLive;
  private final MutableLiveData<Boolean> torEnabledLive;
  private final MutableLiveData<Boolean> proxyEnabledLive;
  private final MutableLiveData<String> shoppingModeUpdateIntervalTextLive;
  private List<Location> locations;
  private final MutableLiveData<String> presetLocationTextLive;
  private List<ProductGroup> productGroups;
  private final MutableLiveData<String> presetProductGroupTextLive;
  private List<QuantityUnit> quantityUnits;
  private final MutableLiveData<String> presetQuantityUnitTextLive;
  private final MutableLiveData<String> defaultDueDaysTextLive;
  private final MutableLiveData<String> dueSoonDaysTextLive;
  private final MutableLiveData<String> defaultPurchaseAmountTextLive;
  private final MutableLiveData<String> defaultConsumeAmountTextLive;
  private final MutableLiveData<Boolean> autoAddToShoppingListLive;
  private final MutableLiveData<String> autoAddToShoppingListTextLive;
  private final MutableLiveData<Boolean> dueSoonNotificationsEnabledLive;
  private final MutableLiveData<String> dueSoonNotificationsTimeTextLive;
  private final MutableLiveData<Boolean> choresNotificationsEnabledLive;
  private final MutableLiveData<String> choresNotificationsTimeTextLive;
  private final MutableLiveData<Boolean> displayHelpForNotificationsLive;

  private final int allowedDecimalPlacesAmount;

  public SettingsViewModel(@NonNull Application application) {
    super(application);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue, getOfflineLive());
    grocyApi = new GrocyApi(getApplication());
    repository = new MainRepository(getApplication());
    reminderUtil = new ReminderUtil(getApplication());

    getExternalScannerEnabledLive = new MutableLiveData<>(getExternalScannerEnabled());
    needsRestartLive = new MutableLiveData<>(false);
    torEnabledLive = new MutableLiveData<>(getTorEnabled());
    proxyEnabledLive = new MutableLiveData<>(getProxyEnabled());
    shoppingModeUpdateIntervalTextLive = new MutableLiveData<>(getShoppingModeUpdateIntervalText());
    presetLocationTextLive = new MutableLiveData<>(getString(R.string.setting_loading));
    presetProductGroupTextLive = new MutableLiveData<>(getString(R.string.setting_loading));
    presetQuantityUnitTextLive = new MutableLiveData<>(getString(R.string.setting_loading));
    defaultDueDaysTextLive = new MutableLiveData<>(getDefaultDueDaysText());
    dueSoonDaysTextLive = new MutableLiveData<>(getDueSoonDaysText());
    defaultPurchaseAmountTextLive = new MutableLiveData<>(getDefaultPurchaseAmountText());
    defaultConsumeAmountTextLive = new MutableLiveData<>(getDefaultConsumeAmountText());
    autoAddToShoppingListLive = new MutableLiveData<>(sharedPrefs.getBoolean(
        SHOPPING_LIST.AUTO_ADD, SETTINGS_DEFAULT.SHOPPING_LIST.AUTO_ADD
    ));
    autoAddToShoppingListTextLive = new MutableLiveData<>(getString(R.string.setting_loading));
    dueSoonNotificationsEnabledLive = new MutableLiveData<>(getStockNotificationsEnabled());
    dueSoonNotificationsTimeTextLive = new MutableLiveData<>(getStockNotificationsTime());
    choresNotificationsEnabledLive = new MutableLiveData<>(getChoresNotificationsEnabled());
    choresNotificationsTimeTextLive = new MutableLiveData<>(getChoresNotificationsTime());
    displayHelpForNotificationsLive = new MutableLiveData<>(false);

    allowedDecimalPlacesAmount = sharedPrefs.getInt(
        STOCK.DECIMAL_PLACES_AMOUNT, SETTINGS_DEFAULT.STOCK.DECIMAL_PLACES_AMOUNT
    );
  }

  public boolean isVersionCompatible() {
    return getSupportedVersions().contains(
        sharedPrefs.getString(
            Constants.PREF.GROCY_VERSION,
            getString(R.string.date_unknown)
        )
    );
  }

  public boolean getIsVersionCompatible() {
    return isVersionCompatible();
  }

  public void showCompatibilityBottomSheet() {
    if (isVersionCompatible()) {
      return;
    }
    Bundle bundle = new Bundle();
    bundle.putString(Constants.ARGUMENT.SERVER, sharedPrefs.getString(
        Constants.PREF.SERVER_URL,
        getString(R.string.date_unknown)
    ));
    bundle.putString(Constants.ARGUMENT.KEY, sharedPrefs.getString(
        Constants.PREF.API_KEY,
        getString(R.string.date_unknown)
    ));
    bundle.putString(Constants.ARGUMENT.VERSION, sharedPrefs.getString(
        Constants.PREF.GROCY_VERSION,
        getString(R.string.date_unknown)
    ));
    bundle.putBoolean(Constants.ARGUMENT.DEMO_CHOSEN, isDemoInstance());
    bundle.putStringArrayList(
        Constants.ARGUMENT.SUPPORTED_VERSIONS,
        getSupportedVersions()
    );
    CompatibilityBottomSheet bottomSheet = new CompatibilityBottomSheet();
    showBottomSheet(bottomSheet, bundle);
  }

  public void reloadConfiguration(
      @Nullable Runnable runnableSuccess, @Nullable Runnable runnableError
  ) {
    ConfigUtil.loadInfo(
        dlHelper, grocyApi, sharedPrefs, runnableSuccess,
        volleyError -> {
          if (runnableError != null) {
            runnableError.run();
          }
          showErrorMessage();
        }
    );
  }

  public void uploadSetting(
      String settingKey,
      Object settingValue,
      OnSettingUploadListener listener
  ) {
    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put("value", settingValue);
    } catch (JSONException e) {
      e.printStackTrace();
      listener.onFinished(R.string.option_synced_error);
      return;
    }
    dlHelper.put(
        grocyApi.getUserSetting(settingKey),
        jsonObject,
        response -> listener.onFinished(R.string.option_synced_success),
        volleyError -> listener.onFinished(R.string.option_synced_error)
    );
  }

  public void showShortcutsBottomSheet() {
    showBottomSheet(new ShortcutsBottomSheet(), null);
  }

  public String getServerUrl() {
    return sharedPrefs.getString(Constants.PREF.SERVER_URL, null);
  }

  public int getTheme() {
    return sharedPrefs.getInt(APPEARANCE.DARK_MODE, SETTINGS_DEFAULT.APPEARANCE.DARK_MODE);
  }

  public boolean isThemeActive(int theme) {
    return getTheme() == theme;
  }

  public void setTheme(int theme) {
    sharedPrefs.edit().putInt(APPEARANCE.DARK_MODE, theme).apply();
  }

  public void showLoadingTimeoutBottomSheet() {
    Bundle bundle = new Bundle();
    bundle.putInt(Constants.ARGUMENT.NUMBER, getLoadingTimeout());
    bundle.putString(Constants.ARGUMENT.HINT, getString(R.string.property_seconds));
    bundle.putString(ARGUMENT.TYPE, NETWORK.LOADING_TIMEOUT);
    showBottomSheet(new InputBottomSheet(), bundle);
  }

  public int getLoadingTimeout() {
    return sharedPrefs.getInt(
        Constants.SETTINGS.NETWORK.LOADING_TIMEOUT,
        Constants.SETTINGS_DEFAULT.NETWORK.LOADING_TIMEOUT
    );
  }

  public void setLoadingTimeout(int seconds) {
    sharedPrefs.edit().putInt(Constants.SETTINGS.NETWORK.LOADING_TIMEOUT, seconds).apply();
  }

  public boolean getLoggingEnabled() {
    return sharedPrefs.getBoolean(
        Constants.SETTINGS.DEBUGGING.ENABLE_DEBUGGING,
        Constants.SETTINGS_DEFAULT.DEBUGGING.ENABLE_DEBUGGING
    );
  }

  public void setLoggingEnabled(boolean enabled) {
    sharedPrefs.edit()
        .putBoolean(Constants.SETTINGS.DEBUGGING.ENABLE_DEBUGGING, enabled).apply();
  }

  public boolean getBeginnerModeEnabled() {
    return sharedPrefs.getBoolean(
        Constants.SETTINGS.BEHAVIOR.BEGINNER_MODE,
        Constants.SETTINGS_DEFAULT.BEHAVIOR.BEGINNER_MODE
    );
  }

  public void setBeginnerModeEnabled(boolean enabled) {
    sharedPrefs.edit()
        .putBoolean(Constants.SETTINGS.BEHAVIOR.BEGINNER_MODE, enabled).apply();
  }

  public boolean getUseOpenFoodFactsEnabled() {
    return sharedPrefs.getBoolean(
        BEHAVIOR.FOOD_FACTS,
        SETTINGS_DEFAULT.BEHAVIOR.FOOD_FACTS
    );
  }

  public void setUseOpenFoodFactsEnabled(boolean enabled) {
    sharedPrefs.edit()
        .putBoolean(Constants.SETTINGS.BEHAVIOR.FOOD_FACTS, enabled).apply();
  }

  public boolean getShowMainMenuButtonEnabled() {
    return sharedPrefs.getBoolean(
        BEHAVIOR.SHOW_MAIN_MENU_BUTTON,
        Constants.SETTINGS_DEFAULT.BEHAVIOR.SHOW_MAIN_MENU_BUTTON
    );
  }

  public void setShowMainMenuButtonEnabled(boolean enabled) {
    sharedPrefs.edit()
        .putBoolean(Constants.SETTINGS.BEHAVIOR.SHOW_MAIN_MENU_BUTTON, enabled).apply();
    sendEvent(Event.UPDATE_BOTTOM_APP_BAR);
  }

  public boolean getExpandBottomSheetsEnabled() {
    return sharedPrefs.getBoolean(
        Constants.SETTINGS.BEHAVIOR.EXPAND_BOTTOM_SHEETS,
        Constants.SETTINGS_DEFAULT.BEHAVIOR.EXPAND_BOTTOM_SHEETS
    );
  }

  public void setExpandBottomSheetsEnabled(boolean enabled) {
    sharedPrefs.edit()
        .putBoolean(Constants.SETTINGS.BEHAVIOR.EXPAND_BOTTOM_SHEETS, enabled).apply();
  }

  public boolean getSpeedUpStartEnabled() {
    return sharedPrefs.getBoolean(
        Constants.SETTINGS.BEHAVIOR.SPEED_UP_START,
        Constants.SETTINGS_DEFAULT.BEHAVIOR.SPEED_UP_START
    );
  }

  public void setSpeedUpStartEnabled(boolean enabled) {
    sharedPrefs.edit().putBoolean(Constants.SETTINGS.BEHAVIOR.SPEED_UP_START, enabled).apply();
  }

  public boolean getTurnOnQuickModeEnabled() {
    return sharedPrefs.getBoolean(
        BEHAVIOR.TURN_ON_QUICK_MODE,
        SETTINGS_DEFAULT.BEHAVIOR.TURN_ON_QUICK_MODE
    );
  }

  public void setTurnOnQuickModeEnabled(boolean enabled) {
    sharedPrefs.edit().putBoolean(Constants.SETTINGS.BEHAVIOR.TURN_ON_QUICK_MODE, enabled).apply();
  }

  public boolean getQuickModeReturnEnabled() {
    return sharedPrefs.getBoolean(
        BEHAVIOR.QUICK_MODE_RETURN,
        SETTINGS_DEFAULT.BEHAVIOR.QUICK_MODE_RETURN
    );
  }

  public void setQuickModeReturnEnabled(boolean enabled) {
    sharedPrefs.edit().putBoolean(Constants.SETTINGS.BEHAVIOR.QUICK_MODE_RETURN, enabled).apply();
  }

  public boolean getDateKeyboardInputEnabled() {
    return sharedPrefs.getBoolean(
        BEHAVIOR.DATE_KEYBOARD_INPUT,
        Constants.SETTINGS_DEFAULT.BEHAVIOR.DATE_KEYBOARD_INPUT
    );
  }

  public void setDateKeyboardInputEnabled(boolean enabled) {
    sharedPrefs.edit()
        .putBoolean(Constants.SETTINGS.BEHAVIOR.DATE_KEYBOARD_INPUT, enabled).apply();
  }

  public boolean getDateKeyboardReverseEnabled() {
    return sharedPrefs.getBoolean(
        BEHAVIOR.DATE_KEYBOARD_REVERSE,
        Constants.SETTINGS_DEFAULT.BEHAVIOR.DATE_KEYBOARD_REVERSE
    );
  }

  public void setDateKeyboardReverseEnabled(boolean enabled) {
    sharedPrefs.edit()
        .putBoolean(Constants.SETTINGS.BEHAVIOR.DATE_KEYBOARD_REVERSE, enabled).apply();
  }

  public boolean getKeepScreenOnRecipesEnabled() {
    return sharedPrefs.getBoolean(
            RECIPES.KEEP_SCREEN_ON,
            SETTINGS_DEFAULT.RECIPES.KEEP_SCREEN_ON
    );
  }

  public void setKeepScreenOnRecipesEnabled(boolean enabled) {
    sharedPrefs.edit()
            .putBoolean(Constants.SETTINGS.RECIPES.KEEP_SCREEN_ON, enabled).apply();
  }

  public boolean getFrontCamEnabled() {
    return sharedPrefs.getBoolean(
        Constants.SETTINGS.SCANNER.FRONT_CAM,
        Constants.SETTINGS_DEFAULT.SCANNER.FRONT_CAM
    );
  }

  public void setFrontCamEnabled(boolean enabled) {
    sharedPrefs.edit().putBoolean(Constants.SETTINGS.SCANNER.FRONT_CAM, enabled).apply();
  }

  public boolean getScannerFormat2dEnabled() {
    return sharedPrefs.getBoolean(
        SCANNER.SCANNER_FORMAT_2D,
        Constants.SETTINGS_DEFAULT.SCANNER.SCANNER_FORMAT_2D
    );
  }

  public void setScannerFormat2dEnabled(boolean enabled) {
    sharedPrefs.edit().putBoolean(Constants.SETTINGS.SCANNER.SCANNER_FORMAT_2D, enabled).apply();
  }

  public void showBarcodeFormatsBottomSheet() {
    showBottomSheet(new BarcodeFormatsBottomSheet());
  }

  public boolean getExternalScannerEnabled() {
    return sharedPrefs.getBoolean(
        Constants.SETTINGS.SCANNER.EXTERNAL_SCANNER,
        Constants.SETTINGS_DEFAULT.SCANNER.EXTERNAL_SCANNER
    );
  }

  public MutableLiveData<Boolean> getGetExternalScannerEnabledLive() {
    return getExternalScannerEnabledLive;
  }

  public void setExternalScannerEnabled(boolean enabled) {
    sharedPrefs.edit().putBoolean(Constants.SETTINGS.SCANNER.EXTERNAL_SCANNER, enabled).apply();
    getExternalScannerEnabledLive.setValue(enabled);
  }

  public void loadShoppingLists() {
    int autoShoppingListId = sharedPrefs.getInt(
        SHOPPING_LIST.AUTO_ADD_LIST_ID, SETTINGS_DEFAULT.SHOPPING_LIST.AUTO_ADD_LIST_ID
    );
    ShoppingList.getShoppingLists(dlHelper, shoppingLists -> {
          ShoppingList shoppingList = ShoppingList.getFromId(shoppingLists, autoShoppingListId);
          autoAddToShoppingListTextLive.setValue(shoppingList != null ? shoppingList.getName()
              : getString(R.string.subtitle_none_selected));
        }, error -> autoAddToShoppingListTextLive.setValue(getString(R.string.setting_not_loaded))).perform(
            dlHelper.getUuid()
    );
  }

  public void showShoppingListsBottomSheet() {
    int autoShoppingListId = sharedPrefs.getInt(
        SHOPPING_LIST.AUTO_ADD_LIST_ID, SETTINGS_DEFAULT.SHOPPING_LIST.AUTO_ADD_LIST_ID
    );
    Bundle bundle = new Bundle();
    bundle.putInt(ARGUMENT.SELECTED_ID, autoShoppingListId);
    showBottomSheet(new ShoppingListsBottomSheet(), bundle);
  }

  public MutableLiveData<Boolean> getAutoAddToShoppingListLive() {
    return autoAddToShoppingListLive;
  }

  public MutableLiveData<String> getAutoAddToShoppingListTextLive() {
    return autoAddToShoppingListTextLive;
  }

  public void setAutoAddToShoppingListEnabled(boolean enabled) {
    sharedPrefs.edit().putBoolean(SHOPPING_LIST.AUTO_ADD, enabled).apply();
    uploadSetting(SHOPPING_LIST.AUTO_ADD, enabled, this::showMessage);
    autoAddToShoppingListLive.setValue(enabled);
  }

  public void setAutoAddToShoppingListId(ShoppingList shoppingList) {
    sharedPrefs.edit().putInt(SHOPPING_LIST.AUTO_ADD_LIST_ID, shoppingList.getId()).apply();
    autoAddToShoppingListTextLive.setValue(shoppingList.getName());
    uploadSetting(SHOPPING_LIST.AUTO_ADD_LIST_ID, shoppingList.getId(), this::showMessage);
  }

  public void showShoppingModeUpdateIntervalBottomSheet() {
    Bundle bundle = new Bundle();
    bundle.putInt(ARGUMENT.NUMBER, sharedPrefs.getInt(
        SHOPPING_MODE.UPDATE_INTERVAL,
        SETTINGS_DEFAULT.SHOPPING_MODE.UPDATE_INTERVAL
    ));
    bundle.putString(ARGUMENT.TYPE, SHOPPING_MODE.UPDATE_INTERVAL);
    bundle.putString(ARGUMENT.HINT, getString(R.string.property_seconds));
    showBottomSheet(new InputBottomSheet(), bundle);
  }

  public String getShoppingModeUpdateIntervalText() {
    return getApplication().getResources().getQuantityString(
        R.plurals.property_seconds_num,
        sharedPrefs.getInt(
            SHOPPING_MODE.UPDATE_INTERVAL,
            SETTINGS_DEFAULT.SHOPPING_MODE.UPDATE_INTERVAL
        ),
        sharedPrefs.getInt(
            SHOPPING_MODE.UPDATE_INTERVAL,
            SETTINGS_DEFAULT.SHOPPING_MODE.UPDATE_INTERVAL
        )
    );
  }

  public MutableLiveData<String> getShoppingModeUpdateIntervalTextLive() {
    return shoppingModeUpdateIntervalTextLive;
  }

  public void setShoppingModeUpdateInterval(String text) {
    int interval = 10;
    if (NumUtil.isStringInt(text)) {
      interval = Integer.parseInt(text);
      if (interval < 0) {
        interval = 10;
      }
    }
    sharedPrefs.edit().putInt(SHOPPING_MODE.UPDATE_INTERVAL, interval).apply();
    shoppingModeUpdateIntervalTextLive.setValue(
        getApplication().getResources().getQuantityString(
            R.plurals.property_seconds_num,
            interval,
            interval
        )
    );
  }

  public boolean getKeepScreenOnEnabled() {
    return sharedPrefs.getBoolean(
        Constants.SETTINGS.SHOPPING_MODE.KEEP_SCREEN_ON,
        Constants.SETTINGS_DEFAULT.SHOPPING_MODE.KEEP_SCREEN_ON
    );
  }

  public void setKeepScreenOnEnabled(boolean enabled) {
    sharedPrefs.edit().putBoolean(Constants.SETTINGS.SHOPPING_MODE.KEEP_SCREEN_ON, enabled)
        .apply();
  }

  public boolean getShowDoneItemsEnabled() {
    return sharedPrefs.getBoolean(
        Constants.SETTINGS.SHOPPING_MODE.SHOW_DONE_ITEMS,
        Constants.SETTINGS_DEFAULT.SHOPPING_MODE.SHOW_DONE_ITEMS
    );
  }

  public void setShowDoneItemsEnabled(boolean enabled) {
    sharedPrefs.edit().putBoolean(SHOPPING_MODE.SHOW_DONE_ITEMS, enabled)
        .apply();
  }

  public boolean getUseSmallerFontEnabled() {
    return sharedPrefs.getBoolean(
        SHOPPING_MODE.USE_SMALLER_FONT,
        SETTINGS_DEFAULT.SHOPPING_MODE.USE_SMALLER_FONT
    );
  }

  public void setUseSmallerFontEnabled(boolean enabled) {
    sharedPrefs.edit().putBoolean(SHOPPING_MODE.USE_SMALLER_FONT, enabled).apply();
  }

  public boolean getListIndicatorEnabled() {
    return sharedPrefs.getBoolean(
        Constants.SETTINGS.STOCK.DISPLAY_DOTS_IN_STOCK,
        Constants.SETTINGS_DEFAULT.STOCK.DISPLAY_DOTS_IN_STOCK
    );
  }

  public void setListIndicatorEnabled(boolean enabled) {
    sharedPrefs.edit().putBoolean(STOCK.DISPLAY_DOTS_IN_STOCK, enabled)
        .apply();
    uploadSetting(STOCK.DISPLAY_DOTS_IN_STOCK, enabled, this::showMessage);
  }

  public MutableLiveData<String> getPresetLocationTextLive() {
    return presetLocationTextLive;
  }

  public void setPresetLocation(Location location) {
    sharedPrefs.edit().putInt(STOCK.LOCATION, location.getId()).apply();
    uploadSetting(STOCK.LOCATION, location.getId(), this::showMessage);
    presetLocationTextLive.setValue(location.getName());
  }

  public MutableLiveData<String> getPresetProductGroupTextLive() {
    return presetProductGroupTextLive;
  }

  public void setPresetProductGroup(ProductGroup productGroup) {
    sharedPrefs.edit().putInt(STOCK.PRODUCT_GROUP, productGroup.getId()).apply();
    uploadSetting(STOCK.PRODUCT_GROUP, productGroup.getId(), this::showMessage);
    presetProductGroupTextLive.setValue(productGroup.getName());
  }

  public MutableLiveData<String> getPresetQuantityUnitTextLive() {
    return presetQuantityUnitTextLive;
  }

  public void setPresetQuantityUnit(QuantityUnit quantityUnit) {
    sharedPrefs.edit().putInt(STOCK.QUANTITY_UNIT, quantityUnit.getId()).apply();
    uploadSetting(STOCK.QUANTITY_UNIT, quantityUnit.getId(), this::showMessage);
    presetQuantityUnitTextLive.setValue(quantityUnit.getName());
  }

  public void loadProductPresets() {
    int locationId = sharedPrefs.getInt(STOCK.LOCATION, SETTINGS_DEFAULT.STOCK.LOCATION);
    int groupId = sharedPrefs.getInt(STOCK.PRODUCT_GROUP, SETTINGS_DEFAULT.STOCK.PRODUCT_GROUP);
    int unitId = sharedPrefs.getInt(STOCK.QUANTITY_UNIT, SETTINGS_DEFAULT.STOCK.QUANTITY_UNIT);
    Location.getLocations(
        dlHelper, locations -> {
          this.locations = locations;
          Location location = Location.getFromId(locations, locationId);
          presetLocationTextLive.setValue(location != null ? location.getName()
              : getString(R.string.subtitle_none_selected));
        }, error -> presetLocationTextLive.setValue(getString(R.string.setting_not_loaded))
    ).perform(dlHelper.getUuid());
    ProductGroup.getProductGroups(
        dlHelper, productGroups -> {
          SortUtil.sortProductGroupsByName(productGroups, true);
          this.productGroups = productGroups;
          ProductGroup productGroup = ProductGroup.getFromId(productGroups, groupId);
          presetProductGroupTextLive.setValue(productGroup != null ? productGroup.getName()
              : getString(R.string.subtitle_none_selected));
        }, error -> presetProductGroupTextLive.setValue(getString(R.string.setting_not_loaded))
    ).perform(dlHelper.getUuid());
    QuantityUnit.getQuantityUnits(
        dlHelper, quantityUnits -> {
          this.quantityUnits = quantityUnits;
          QuantityUnit quantityUnit = QuantityUnit.getFromId(quantityUnits, unitId);
          presetQuantityUnitTextLive.setValue(quantityUnit != null ? quantityUnit.getName()
              : getString(R.string.subtitle_none_selected));
        }, error -> presetQuantityUnitTextLive.setValue(getString(R.string.setting_not_loaded))
    ).perform(dlHelper.getUuid());
  }

  public void showLocationsBottomSheet() {
    if (locations == null) {
      return;
    }
    Bundle bundle = new Bundle();
    bundle.putParcelableArrayList(ARGUMENT.LOCATIONS, new ArrayList<>(locations));
    bundle.putInt(
        ARGUMENT.SELECTED_ID,
        sharedPrefs.getInt(STOCK.LOCATION, SETTINGS_DEFAULT.STOCK.LOCATION)
    );
    bundle.putBoolean(ARGUMENT.DISPLAY_EMPTY_OPTION, true);
    showBottomSheet(new LocationsBottomSheet(), bundle);
  }

  public void showProductGroupsBottomSheet() {
    if (productGroups == null) {
      return;
    }
    Bundle bundle = new Bundle();
    bundle.putParcelableArrayList(ARGUMENT.PRODUCT_GROUPS, new ArrayList<>(productGroups));
    bundle.putInt(
        ARGUMENT.SELECTED_ID,
        sharedPrefs.getInt(STOCK.PRODUCT_GROUP, SETTINGS_DEFAULT.STOCK.PRODUCT_GROUP)
    );
    bundle.putBoolean(ARGUMENT.DISPLAY_EMPTY_OPTION, true);
    showBottomSheet(new ProductGroupsBottomSheet(), bundle);
  }

  public void showQuantityUnitsBottomSheet() {
    if (quantityUnits == null) {
      return;
    }
    Bundle bundle = new Bundle();
    bundle.putParcelableArrayList(ARGUMENT.QUANTITY_UNITS, new ArrayList<>(quantityUnits));
    bundle.putInt(
        ARGUMENT.SELECTED_ID,
        sharedPrefs.getInt(STOCK.QUANTITY_UNIT, SETTINGS_DEFAULT.STOCK.QUANTITY_UNIT)
    );
    bundle.putBoolean(ARGUMENT.DISPLAY_EMPTY_OPTION, true);
    showBottomSheet(new QuantityUnitsBottomSheet(), bundle);
  }

  public void showDefaultDueDaysBottomSheet() {
    Bundle bundle = new Bundle();
    int days = sharedPrefs.getInt(STOCK.DEFAULT_DUE_DAYS, SETTINGS_DEFAULT.STOCK.DEFAULT_DUE_DAYS);
    bundle.putInt(ARGUMENT.NUMBER, days);
    bundle.putString(ARGUMENT.TYPE, STOCK.DEFAULT_DUE_DAYS);
    bundle.putString(ARGUMENT.HINT, getString(R.string.property_days));
    showBottomSheet(new InputBottomSheet(), bundle);
  }

  public String getDefaultDueDaysText() {
    int days = sharedPrefs.getInt(STOCK.DEFAULT_DUE_DAYS, SETTINGS_DEFAULT.STOCK.DEFAULT_DUE_DAYS);
    return getApplication().getResources().getQuantityString(R.plurals.date_days, days, days);
  }

  public MutableLiveData<String> getDefaultDueDaysTextLive() {
    return defaultDueDaysTextLive;
  }

  public void setDefaultDueDays(String text) {
    int days = 0;
    if (NumUtil.isStringInt(text)) {
      days = Integer.parseInt(text);
      if (days < -1) {
        days = -1;
      }
    }
    sharedPrefs.edit().putInt(STOCK.DEFAULT_DUE_DAYS, days).apply();
    defaultDueDaysTextLive.setValue(
        getApplication().getResources().getQuantityString(R.plurals.date_days, days, days)
    );
    uploadSetting(STOCK.DEFAULT_DUE_DAYS, days, this::showMessage);
  }

  public void showDueSoonDaysBottomSheet() {
    Bundle bundle = new Bundle();
    String days = sharedPrefs.getString(STOCK.DUE_SOON_DAYS, SETTINGS_DEFAULT.STOCK.DUE_SOON_DAYS);
    if (NumUtil.isStringInt(days)) {
      bundle.putInt(ARGUMENT.NUMBER, Integer.parseInt(days));
    } else {
      bundle.putInt(ARGUMENT.NUMBER, Integer.parseInt(SETTINGS_DEFAULT.STOCK.DUE_SOON_DAYS));
    }
    bundle.putString(ARGUMENT.TYPE, STOCK.DUE_SOON_DAYS);
    bundle.putString(ARGUMENT.HINT, getString(R.string.property_days));
    showBottomSheet(new InputBottomSheet(), bundle);
  }

  public String getDueSoonDaysText() {
    String days = sharedPrefs.getString(STOCK.DUE_SOON_DAYS, SETTINGS_DEFAULT.STOCK.DUE_SOON_DAYS);
    int daysInt;
    if (NumUtil.isStringInt(days)) {
      daysInt = Integer.parseInt(days);
    } else {
      daysInt = Integer.parseInt(SETTINGS_DEFAULT.STOCK.DUE_SOON_DAYS);
    }
    return getApplication().getResources().getQuantityString(R.plurals.date_days, daysInt, daysInt);
  }

  public MutableLiveData<String> getDueSoonDaysTextLive() {
    return dueSoonDaysTextLive;
  }

  public void setDueSoonDays(String text) {
    int interval = 5;
    if (NumUtil.isStringInt(text)) {
      interval = Integer.parseInt(text);
      if (interval < 1) {
        interval = 5;
      }
    }
    sharedPrefs.edit().putString(STOCK.DUE_SOON_DAYS, String.valueOf(interval)).apply();
    dueSoonDaysTextLive.setValue(
        getApplication().getResources().getQuantityString(R.plurals.date_days, interval, interval)
    );
    uploadSetting(STOCK.DUE_SOON_DAYS, String.valueOf(interval), this::showMessage);
  }

  public boolean hasServerNewOptionTreatOpenedAsOutOfStock() {
    return VersionUtil.isGrocyServerMin320(sharedPrefs);
  }

  public boolean getTreatOpenedAsOutOfStockEnabled() {
    if (!hasServerNewOptionTreatOpenedAsOutOfStock()) {
      return false;
    }
    return sharedPrefs.getBoolean(
        STOCK.TREAT_OPENED_OUT_OF_STOCK,
        Constants.SETTINGS_DEFAULT.STOCK.TREAT_OPENED_OUT_OF_STOCK
    );
  }

  public void setTreatOpenedAsOutOfStockEnabled(boolean enabled) {
    if (!hasServerNewOptionTreatOpenedAsOutOfStock()) {
      return;
    }
    sharedPrefs.edit().putBoolean(STOCK.TREAT_OPENED_OUT_OF_STOCK, enabled).apply();
    uploadSetting(STOCK.TREAT_OPENED_OUT_OF_STOCK, enabled, this::showMessage);
  }

  public void showDefaultPurchaseAmountBottomSheet() {
    Bundle bundle = new Bundle();
    String amount = sharedPrefs.getString(
        STOCK.DEFAULT_PURCHASE_AMOUNT,
        SETTINGS_DEFAULT.STOCK.DEFAULT_PURCHASE_AMOUNT
    );
    if (NumUtil.isStringDouble(amount)) {
      bundle.putDouble(ARGUMENT.NUMBER, NumUtil.toDouble(amount));
    } else {
      bundle.putDouble(
          ARGUMENT.NUMBER,
          NumUtil.toDouble(SETTINGS_DEFAULT.STOCK.DEFAULT_PURCHASE_AMOUNT)
      );
    }
    bundle.putString(ARGUMENT.TYPE, STOCK.DEFAULT_PURCHASE_AMOUNT);
    bundle.putString(ARGUMENT.HINT, getString(R.string.property_amount));
    showBottomSheet(new InputBottomSheet(), bundle);
  }

  public String getDefaultPurchaseAmountText() {
    String amount = sharedPrefs.getString(
        STOCK.DEFAULT_PURCHASE_AMOUNT,
        SETTINGS_DEFAULT.STOCK.DEFAULT_PURCHASE_AMOUNT
    );
    double amountDouble;
    if (NumUtil.isStringDouble(amount)) {
      amountDouble = NumUtil.toDouble(amount);
    } else {
      amountDouble = NumUtil.toDouble(SETTINGS_DEFAULT.STOCK.DEFAULT_PURCHASE_AMOUNT);
    }
    return NumUtil.trimAmount(amountDouble, allowedDecimalPlacesAmount);
  }

  public MutableLiveData<String> getDefaultPurchaseAmountTextLive() {
    return defaultPurchaseAmountTextLive;
  }

  public void setDefaultPurchaseAmount(String text) {
    double amount = 0;
    if (NumUtil.isStringDouble(text)) {
      amount = NumUtil.toDouble(text);
      if (amount < 0) {
        amount = 0;
      }
    }
    sharedPrefs.edit().putString(STOCK.DEFAULT_PURCHASE_AMOUNT, NumUtil.trimAmount(amount, allowedDecimalPlacesAmount)).apply();
    defaultPurchaseAmountTextLive.setValue(NumUtil.trimAmount(amount, allowedDecimalPlacesAmount));
    uploadSetting(STOCK.DEFAULT_PURCHASE_AMOUNT, NumUtil.trimAmount(amount, allowedDecimalPlacesAmount), this::showMessage);
  }

  public boolean getPurchasedDateEnabled() {
    return sharedPrefs.getBoolean(
        Constants.SETTINGS.STOCK.SHOW_PURCHASED_DATE,
        Constants.SETTINGS_DEFAULT.STOCK.SHOW_PURCHASED_DATE
    );
  }

  public void setPurchasedDateEnabled(boolean enabled) {
    sharedPrefs.edit().putBoolean(Constants.SETTINGS.STOCK.SHOW_PURCHASED_DATE, enabled)
        .apply();
    uploadSetting(STOCK.SHOW_PURCHASED_DATE, enabled, this::showMessage);
  }

  public void showDefaultConsumeAmountBottomSheet() {
    Bundle bundle = new Bundle();
    String amount = sharedPrefs.getString(
        STOCK.DEFAULT_CONSUME_AMOUNT,
        SETTINGS_DEFAULT.STOCK.DEFAULT_CONSUME_AMOUNT
    );
    if (NumUtil.isStringDouble(amount)) {
      bundle.putDouble(ARGUMENT.NUMBER, NumUtil.toDouble(amount));
    } else {
      bundle.putDouble(
          ARGUMENT.NUMBER,
          NumUtil.toDouble(SETTINGS_DEFAULT.STOCK.DEFAULT_CONSUME_AMOUNT)
      );
    }
    bundle.putString(ARGUMENT.TYPE, STOCK.DEFAULT_CONSUME_AMOUNT);
    bundle.putString(ARGUMENT.HINT, getString(R.string.property_amount));
    showBottomSheet(new InputBottomSheet(), bundle);
  }

  public String getDefaultConsumeAmountText() {
    String amount = sharedPrefs.getString(
        STOCK.DEFAULT_CONSUME_AMOUNT,
        SETTINGS_DEFAULT.STOCK.DEFAULT_CONSUME_AMOUNT
    );
    double amountDouble;
    if (NumUtil.isStringDouble(amount)) {
      amountDouble = NumUtil.toDouble(amount);
    } else {
      amountDouble = NumUtil.toDouble(SETTINGS_DEFAULT.STOCK.DEFAULT_CONSUME_AMOUNT);
    }
    return NumUtil.trimAmount(amountDouble, allowedDecimalPlacesAmount);
  }

  public MutableLiveData<String> getDefaultConsumeAmountTextLive() {
    return defaultConsumeAmountTextLive;
  }

  public void setDefaultConsumeAmount(String text) {
    double amount = 0;
    if (NumUtil.isStringDouble(text)) {
      amount = NumUtil.toDouble(text);
      if (amount < 0) {
        amount = 0;
      }
    }
    sharedPrefs.edit().putString(STOCK.DEFAULT_CONSUME_AMOUNT, NumUtil.trimAmount(amount, allowedDecimalPlacesAmount)).apply();
    defaultConsumeAmountTextLive.setValue(NumUtil.trimAmount(amount, allowedDecimalPlacesAmount));
    uploadSetting(STOCK.DEFAULT_CONSUME_AMOUNT, NumUtil.trimAmount(amount, allowedDecimalPlacesAmount), this::showMessage);
  }

  public boolean getUseQuickConsumeAmountEnabled() {
    return sharedPrefs.getBoolean(
        STOCK.USE_QUICK_CONSUME_AMOUNT,
        SETTINGS_DEFAULT.STOCK.USE_QUICK_CONSUME_AMOUNT
    );
  }

  public void setUseQuickConsumeAmountEnabled(boolean enabled) {
    sharedPrefs.edit().putBoolean(Constants.SETTINGS.STOCK.USE_QUICK_CONSUME_AMOUNT, enabled)
        .apply();
    uploadSetting(STOCK.USE_QUICK_CONSUME_AMOUNT, enabled, this::showMessage);
  }

  public boolean getLoadingCircleEnabled() {
    return sharedPrefs.getBoolean(
        Constants.SETTINGS.NETWORK.LOADING_CIRCLE,
        Constants.SETTINGS_DEFAULT.NETWORK.LOADING_CIRCLE
    );
  }

  public void setLoadingCircleEnabled(boolean enabled) {
    sharedPrefs.edit().putBoolean(Constants.SETTINGS.NETWORK.LOADING_CIRCLE, enabled).apply();
  }

  public MutableLiveData<Boolean> getNeedsRestartLive() {
    return needsRestartLive;
  }

  public MutableLiveData<Boolean> getTorEnabledLive() {
    return torEnabledLive;
  }

  public boolean getTorEnabled() {
    return sharedPrefs.getBoolean(NETWORK.TOR, SETTINGS_DEFAULT.NETWORK.TOR);
  }

  public void setTorEnabled(boolean enabled) {
    if (enabled != getTorEnabled()) needsRestartLive.setValue(true);
    sharedPrefs.edit().putBoolean(NETWORK.TOR, enabled).apply();
  }

  public MutableLiveData<Boolean> getProxyEnabledLive() {
    return proxyEnabledLive;
  }

  public boolean getProxyEnabled() {
    return sharedPrefs.getBoolean(NETWORK.PROXY, SETTINGS_DEFAULT.NETWORK.PROXY);
  }

  public void setProxyEnabled(boolean enabled) {
    if (enabled != getProxyEnabled()) needsRestartLive.setValue(true);
    sharedPrefs.edit().putBoolean(NETWORK.PROXY, enabled).apply();
  }

  public String getProxyHost() {
    return sharedPrefs.getString(NETWORK.PROXY_HOST, SETTINGS_DEFAULT.NETWORK.PROXY_HOST);
  }

  public void setProxyHost(String host) {
    sharedPrefs.edit().putString(NETWORK.PROXY_HOST, host).apply();
    needsRestartLive.setValue(true);
  }

  public void showProxyHostBottomSheet() {
    Bundle bundle = new Bundle();
    bundle.putString(ARGUMENT.TEXT, getProxyHost());
    bundle.putString(Constants.ARGUMENT.HINT, getString(R.string.setting_proxy_host));
    bundle.putString(ARGUMENT.TYPE, NETWORK.PROXY_HOST);
    showBottomSheet(new InputBottomSheet(), bundle);
  }

  public int getProxyPort() {
    return sharedPrefs.getInt(NETWORK.PROXY_PORT, SETTINGS_DEFAULT.NETWORK.PROXY_PORT);
  }

  public void setProxyPort(int port) {
    sharedPrefs.edit().putInt(NETWORK.PROXY_PORT, port).apply();
    needsRestartLive.setValue(true);
  }

  public void showMessageDurationBottomSheet() {
    Bundle bundle = new Bundle();
    bundle.putInt(Constants.ARGUMENT.NUMBER, getMessageDuration());
    bundle.putString(Constants.ARGUMENT.HINT, getString(R.string.property_seconds));
    bundle.putString(ARGUMENT.TYPE, BEHAVIOR.MESSAGE_DURATION);
    showBottomSheet(new InputBottomSheet(), bundle);
  }

  public void setMessageDuration(int duration) {
    sharedPrefs.edit().putInt(BEHAVIOR.MESSAGE_DURATION, duration).apply();
  }

  public int getMessageDuration() {
    return sharedPrefs.getInt(BEHAVIOR.MESSAGE_DURATION, SETTINGS_DEFAULT.BEHAVIOR.MESSAGE_DURATION);
  }

  public void showProxyPortBottomSheet() {
    Bundle bundle = new Bundle();
    bundle.putInt(Constants.ARGUMENT.NUMBER, getProxyPort());
    bundle.putString(Constants.ARGUMENT.HINT, getString(R.string.setting_proxy_port));
    bundle.putString(ARGUMENT.TYPE, NETWORK.PROXY_PORT);
    showBottomSheet(new InputBottomSheet(), bundle);
  }

  public ReminderUtil getReminderUtil() {
    return reminderUtil;
  }

  public boolean getStockNotificationsEnabled() {
    boolean isActivated = sharedPrefs.getBoolean(
        NOTIFICATIONS.STOCK_ENABLE, SETTINGS_DEFAULT.NOTIFICATIONS.STOCK_ENABLE
    );
    if (isActivated && !reminderUtil.hasPermission()) {
      isActivated = false;
      setStockNotificationsEnabled(false);
    }
    return isActivated;
  }

  public MutableLiveData<Boolean> getStockNotificationsEnabledLive() {
    return dueSoonNotificationsEnabledLive;
  }

  public void setStockNotificationsEnabled(boolean enabled) {
    if(dueSoonNotificationsEnabledLive == null) {
      return;
    }
    dueSoonNotificationsEnabledLive.setValue(enabled);
    reminderUtil.setReminderEnabled(ReminderUtil.STOCK_TYPE, enabled);
  }

  public String getStockNotificationsTime() {
    return sharedPrefs.getString(
        NOTIFICATIONS.STOCK_TIME, SETTINGS_DEFAULT.NOTIFICATIONS.STOCK_TIME
    );
  }

  public MutableLiveData<String> getStockNotificationsTimeTextLive() {
    return dueSoonNotificationsTimeTextLive;
  }

  public void setStockNotificationsTime(String text) {
    sharedPrefs.edit().putString(NOTIFICATIONS.STOCK_TIME, text).apply();
    dueSoonNotificationsTimeTextLive.setValue(text);
    setStockNotificationsEnabled(reminderUtil.hasPermission());
  }

  public boolean getChoresNotificationsEnabled() {
    boolean isActivated = sharedPrefs.getBoolean(
        NOTIFICATIONS.CHORES_ENABLE, SETTINGS_DEFAULT.NOTIFICATIONS.CHORES_ENABLE
    );
    if (isActivated && !reminderUtil.hasPermission()) {
      isActivated = false;
      setChoresNotificationsEnabled(false);
    }
    return isActivated;
  }

  public MutableLiveData<Boolean> getChoresNotificationsEnabledLive() {
    return choresNotificationsEnabledLive;
  }

  public void setChoresNotificationsEnabled(boolean enabled) {
    if(choresNotificationsEnabledLive == null) {
      return;
    }
    choresNotificationsEnabledLive.setValue(enabled);
    reminderUtil.setReminderEnabled(ReminderUtil.CHORES_TYPE, enabled);
  }

  public String getChoresNotificationsTime() {
    return sharedPrefs.getString(
        NOTIFICATIONS.CHORES_TIME, SETTINGS_DEFAULT.NOTIFICATIONS.CHORES_TIME
    );
  }

  public MutableLiveData<String> getChoresNotificationsTimeTextLive() {
    return choresNotificationsTimeTextLive;
  }

  public void setChoresNotificationsTime(String text) {
    sharedPrefs.edit().putString(NOTIFICATIONS.CHORES_TIME, text).apply();
    choresNotificationsTimeTextLive.setValue(text);
    setChoresNotificationsEnabled(reminderUtil.hasPermission());
  }

  public MutableLiveData<Boolean> getDisplayHelpForNotificationsLive() {
    return displayHelpForNotificationsLive;
  }

  public void toggleDisplayHelpForNotifications() {
    displayHelpForNotificationsLive.setValue(
        Boolean.FALSE.equals(displayHelpForNotificationsLive.getValue()));
  }

  public ArrayList<String> getSupportedVersions() {
    return new ArrayList<>(Arrays.asList(
        getApplication().getResources().getStringArray(R.array.compatible_grocy_versions)
    ));
  }

  public boolean isFeatureEnabled(String pref) {
    if (pref == null) {
      return true;
    }
    return sharedPrefs.getBoolean(pref, true);
  }

  public void clearAllTables() {
    repository.clearAllTables();
  }

  public void clearServerRelatedSharedPreferences() {
    PrefsUtil.clearServerRelatedSharedPreferences(sharedPrefs);
  }

  @Override
  protected void onCleared() {
    dlHelper.destroy();
    super.onCleared();
  }
}
