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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.ShoppingListItemAdapter;
import xyz.zedler.patrick.grocy.behavior.AppBarBehavior;
import xyz.zedler.patrick.grocy.behavior.SwipeBehavior;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentShoppingListBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ShoppingListClearBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ShoppingListItemBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ShoppingListsBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.TextEditBottomSheet;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.GroupedListItem;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PluralUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;
import xyz.zedler.patrick.grocy.viewmodel.ShoppingListViewModel;

public class ShoppingListFragment extends BaseFragment implements
    ShoppingListItemAdapter.ShoppingListItemAdapterListener {

  private final static String TAG = ShoppingListFragment.class.getSimpleName();

  private MainActivity activity;
  private SharedPreferences sharedPrefs;
  private ShoppingListViewModel viewModel;
  private AppBarBehavior appBarBehavior;
  private ClickUtil clickUtil;
  private SwipeBehavior swipeBehavior;
  private FragmentShoppingListBinding binding;
  private InfoFullscreenHelper infoFullscreenHelper;
  private PluralUtil pluralUtil;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentShoppingListBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();

    if (infoFullscreenHelper != null) {
      infoFullscreenHelper.destroyInstance();
      infoFullscreenHelper = null;
    }
    if (binding != null) {
      binding.recycler.animate().cancel();
      binding.recycler.setAdapter(null);
      binding = null;
    }
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) requireActivity();
    viewModel = new ViewModelProvider(this).get(ShoppingListViewModel.class);
    binding.setViewModel(viewModel);
    binding.setActivity(activity);
    binding.setFragment(this);
    binding.setLifecycleOwner(getViewLifecycleOwner());

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBar);
    systemBarBehavior.setContainer(binding.swipeShoppingList);
    systemBarBehavior.setRecycler(binding.recycler);
    systemBarBehavior.applyAppBarInsetOnContainer(false);
    systemBarBehavior.applyStatusBarInsetOnContainer(false);
    systemBarBehavior.setUp();
    activity.setSystemBarBehavior(systemBarBehavior);

    binding.toolbar.setNavigationOnClickListener(v -> activity.performOnBackPressed());
    binding.toolbar.setOnClickListener(v -> showShoppingListsBottomSheet());

    infoFullscreenHelper = new InfoFullscreenHelper(binding.frame);
    clickUtil = new ClickUtil();
    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
    pluralUtil = new PluralUtil(activity);

    // APP BAR BEHAVIOR

    appBarBehavior = new AppBarBehavior(
        activity,
        binding.appBarDefault,
        binding.appBarSearch,
        savedInstanceState
    );

    binding.recycler.setLayoutManager(
        new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
    );
    ShoppingListItemAdapter adapter = new ShoppingListItemAdapter(requireContext(), this);
    binding.recycler.setAdapter(adapter);

    if (savedInstanceState == null) {
      viewModel.resetSearch();
    }

    Object forcedSelectedId = getFromThisDestinationNow(Constants.ARGUMENT.SELECTED_ID);
    if (forcedSelectedId != null) {
      viewModel.selectShoppingList((Integer) forcedSelectedId);
      removeForThisDestination(Constants.ARGUMENT.SELECTED_ID);
    }

    viewModel.getInfoFullscreenLive().observe(
        getViewLifecycleOwner(),
        infoFullscreen -> infoFullscreenHelper.setInfo(infoFullscreen)
    );

    viewModel.getSelectedShoppingListIdLive().observe(
        getViewLifecycleOwner(), this::changeAppBarTitle
    );

    viewModel.getFilteredShoppingListItemsLive().observe(getViewLifecycleOwner(), items -> {
      if (items == null) return;
      adapter.updateData(
          requireContext(),
          items,
          viewModel.getProductHashMap(),
          viewModel.getProductNamesHashMap(),
          viewModel.getProductLastPurchasedHashMap(),
          viewModel.getQuantityUnitHashMap(),
          viewModel.getUnitConversions(),
          viewModel.getProductGroupHashMap(),
          viewModel.getStoreHashMap(),
          viewModel.getShoppingListItemAmountsHashMap(),
          viewModel.getMissingProductIds(),
          viewModel.getShoppingListNotes(),
          viewModel.getGroupingMode(),
          viewModel.getActiveFields(),
          () -> binding.recycler.scheduleLayoutAnimation()
      );
    });

    viewModel.getEventHandler().observeEvent(getViewLifecycleOwner(), event -> {
      if (event.getType() == Event.SNACKBAR_MESSAGE) {
        activity.showSnackbar(
            ((SnackbarMessage) event).getSnackbar(activity.binding.coordinatorMain)
        );
      } else if (event.getType() == Event.SCROLL_UP) {
        binding.recycler.scrollToPosition(0);
      } else if (event.getType() == Event.TRANSACTION_SUCCESS) {
        viewModel.downloadData(false, false);
      }
    });

    if (swipeBehavior == null) {
      swipeBehavior = new SwipeBehavior(
          activity,
          swipeStarted -> binding.swipeShoppingList.setEnabled(!swipeStarted)
      ) {
        @Override
        public void instantiateUnderlayButton(
            RecyclerView.ViewHolder viewHolder,
            List<UnderlayButton> underlayButtons
        ) {
          if (viewHolder.getItemViewType() != GroupedListItem.TYPE_ENTRY) return;
          if (!(binding.recycler.getAdapter() instanceof ShoppingListItemAdapter)) return;
          int position = viewHolder.getAdapterPosition();
          GroupedListItem item = ((ShoppingListItemAdapter) binding.recycler.getAdapter())
              .getGroupedListItemForPos(position);
          if (!(item instanceof ShoppingListItem)) {
            return;
          }
          underlayButtons.add(new SwipeBehavior.UnderlayButton(
              activity,
              R.drawable.ic_round_done,
              pos -> {
                GroupedListItem item1 = ((ShoppingListItemAdapter) binding.recycler.getAdapter())
                    .getGroupedListItemForPos(position);
                if (!(item1 instanceof ShoppingListItem)) {
                  return;
                }
                viewModel.toggleDoneStatus((ShoppingListItem) item1);
              }
          ));
        }
      };
    }
    swipeBehavior.attachToRecyclerView(binding.recycler);

    hideDisabledFeatures();

    if (savedInstanceState == null) {
      viewModel.loadFromDatabase(true);
    }

    // UPDATE UI

    activity.getScrollBehavior().setNestedOverScrollFixEnabled(true);
    activity.getScrollBehavior().setUpScroll(
        binding.appBar, false, binding.recycler, true, true
    );
    activity.getScrollBehavior().setBottomBarVisibility(true);
    activity.updateBottomAppBar(
        true,
        R.menu.menu_shopping_list,
        getBottomMenuClickListener()
    );
    activity.updateFab(
        R.drawable.ic_round_add_anim,
        R.string.title_entry_new,
        Constants.FAB.TAG.ADD,
        ShoppingListFragmentArgs.fromBundle(requireArguments()).getAnimateStart()
            && savedInstanceState == null,
        this::addItem
    );
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    if (appBarBehavior != null) {
      appBarBehavior.saveInstanceState(outState);
    }
  }

  @Override
  public void selectShoppingList(ShoppingList shoppingList) {
    viewModel.selectShoppingList(shoppingList);
    if (binding != null) {
      binding.recycler.scrollToPosition(0);
    }
  }

  private void changeAppBarTitle(int selectedShoppingListId) {
    ShoppingList shoppingList = viewModel.getShoppingListFromId(selectedShoppingListId);
    if (shoppingList == null) {
      return;
    }
    // change app bar title to shopping list name
    if (Objects.equals(binding.toolbar.getTitle(), shoppingList.getName())) {
      return;
    }
    binding.toolbar.setTitle(shoppingList.getName());
  }

  @Override
  public void toggleDoneStatus(@NonNull ShoppingListItem shoppingListItem) {
    viewModel.toggleDoneStatus(shoppingListItem);
  }

  @Override
  public void editItem(@NonNull ShoppingListItem shoppingListItem) {
    if (showOfflineError()) {
      return;
    }
    activity.navUtil.navigate(
        ShoppingListFragmentDirections
            .actionShoppingListFragmentToShoppingListItemEditFragment(Constants.ACTION.EDIT)
            .setShoppingListItem(shoppingListItem)
    );
  }

  @Override
  public void saveText(Spanned notes) {
    viewModel.saveNotes(notes);
  }

  @Override
  public void purchaseItem(@NonNull ShoppingListItem shoppingListItem) {
    if (showOfflineError()) {
      return;
    }
    activity.navUtil.navigate(
        R.id.purchaseFragment,
        new PurchaseFragmentArgs.Builder()
            .setShoppingListItems(new int[]{shoppingListItem.getId()})
            .setCloseWhenFinished(true).build().toBundle()
    );
  }

  @Override
  public void deleteItem(@NonNull ShoppingListItem shoppingListItem) {
    if (showOfflineError()) {
      return;
    }
    viewModel.deleteItem(shoppingListItem);
  }

  private boolean showOfflineError() {
    if (viewModel.isOffline()) {
      activity.showSnackbar(R.string.error_offline, false);
      return true;
    }
    return false;
  }

  public void addItem() {
    activity.navUtil.navigate(
        ShoppingListFragmentDirections
            .actionShoppingListFragmentToShoppingListItemEditFragment(Constants.ACTION.CREATE)
            .setSelectedShoppingListId(viewModel.getSelectedShoppingListId())
    );
  }

  private void showNotesEditor() {
    Bundle bundle = new Bundle();
    bundle.putString(
        Constants.ARGUMENT.TITLE,
        activity.getString(R.string.action_edit_notes)
    );
    bundle.putString(
        Constants.ARGUMENT.HINT,
        activity.getString(R.string.property_notes)
    );
    ShoppingList shoppingList = viewModel.getSelectedShoppingList();
    if (shoppingList == null) {
      return;
    }
    bundle.putString(Constants.ARGUMENT.HTML, shoppingList.getNotes());
    activity.showBottomSheet(new TextEditBottomSheet(), bundle);
  }

  public void showShoppingListsBottomSheet() {
    Bundle bundle = new Bundle();
    bundle.putBoolean(ARGUMENT.DISPLAY_NEW_OPTION, !isFeatureMultipleListsDisabled()
        && !viewModel.isOffline());
    activity.showBottomSheet(new ShoppingListsBottomSheet(), bundle);
  }

  public Toolbar.OnMenuItemClickListener getBottomMenuClickListener() {
    return item -> {
      if (item.getItemId() == R.id.action_search) {
        ViewUtil.startIcon(item);
        setUpSearch();
        return true;
      } else if (item.getItemId() == R.id.action_shopping_mode) {
        activity.navUtil.navigate(
            ShoppingListFragmentDirections.actionShoppingListFragmentToShoppingModeFragment()
        );
        return true;
      } else if (item.getItemId() == R.id.action_add_missing) {
        ViewUtil.startIcon(item);
        viewModel.addMissingItems();
        return true;
      } else if (item.getItemId() == R.id.action_purchase_all_items) {
        ArrayList<ShoppingListItem> shoppingListItemsSelected
            = viewModel.getFilteredShoppingListItemsLive().getValue();
        if (shoppingListItemsSelected == null) {
          activity.showSnackbar(R.string.error_undefined, false);
          return true;
        }
        if (shoppingListItemsSelected.isEmpty()) {
          activity.showSnackbar(R.string.error_empty_shopping_list, false);
          return true;
        }
        ArrayList<ShoppingListItem> listItems = new ArrayList<>(shoppingListItemsSelected);
        HashMap<Integer, String> productNamesHashMap = viewModel.getProductNamesHashMap();
        if (productNamesHashMap == null) {
          activity.showSnackbar(R.string.error_undefined, false);
          return true;
        }
        SortUtil.sortShoppingListItemsByName(listItems, productNamesHashMap, true);
        int[] array = new int[listItems.size()];
        for (int i = 0; i < array.length; i++) {
          array[i] = listItems.get(i).getId();
        }
        activity.navUtil.navigate(
            R.id.purchaseFragment,
            new PurchaseFragmentArgs.Builder()
                .setShoppingListItems(array)
                .setCloseWhenFinished(true).build().toBundle()
        );
        return true;
      } else if (item.getItemId() == R.id.action_purchase_done_items) {
        ArrayList<ShoppingListItem> shoppingListItemsSelected
            = viewModel.getFilteredShoppingListItemsLive().getValue();
        if (shoppingListItemsSelected == null) {
          activity.showSnackbar(R.string.error_undefined, false);
          return true;
        }
        if (shoppingListItemsSelected.isEmpty()) {
          activity.showSnackbar(R.string.error_empty_shopping_list, false);
          return true;
        }
        ArrayList<ShoppingListItem> listItems = new ArrayList<>(shoppingListItemsSelected);
        HashMap<Integer, String> productNamesHashMap = viewModel.getProductNamesHashMap();
        if (productNamesHashMap == null) {
          activity.showSnackbar(R.string.error_undefined, false);
          return true;
        }
        ArrayList<ShoppingListItem> doneItems = new ArrayList<>();
        for (ShoppingListItem tempItem : listItems) {
          if (!tempItem.isUndone()) doneItems.add(tempItem);
        }
        if (doneItems.isEmpty()) {
          activity.showSnackbar(R.string.error_no_done_items, false);
          return true;
        }
        SortUtil.sortShoppingListItemsByName(doneItems, productNamesHashMap, true);
        int[] array = new int[doneItems.size()];
        for (int i = 0; i < array.length; i++) {
          array[i] = doneItems.get(i).getId();
        }
        activity.navUtil.navigate(
            R.id.purchaseFragment,
            new PurchaseFragmentArgs.Builder()
                .setShoppingListItems(array)
                .setCloseWhenFinished(true).build().toBundle()
        );
        return true;
      } else if (item.getItemId() == R.id.action_shopping_mode) {
        activity.navUtil.navigate(
            ShoppingListFragmentDirections.actionShoppingListFragmentToShoppingModeFragment()
        );
        return true;
      } else if (item.getItemId() == R.id.action_edit_notes) {
        showNotesEditor();
        return true;
      } else if (item.getItemId() == R.id.action_clear) {
        ViewUtil.startIcon(item);
        ShoppingList shoppingList = viewModel.getSelectedShoppingList();
        if (shoppingList == null) {
          activity.showSnackbar(R.string.error_undefined, false);
          return true;
        }
        Bundle bundle = new Bundle();
        bundle.putParcelable(Constants.ARGUMENT.SHOPPING_LIST, shoppingList);
        activity.showBottomSheet(new ShoppingListClearBottomSheet(), bundle);
        return true;
      }
      return false;
    };
  }

  @Override
  public void clearShoppingList(ShoppingList shoppingList, boolean onlyDoneItems) {
    if (onlyDoneItems) {
      viewModel.clearDoneItems(shoppingList);
    } else {
      viewModel.clearAllItems(shoppingList, null);
    }
  }

  @Override
  public void deleteShoppingList(ShoppingList shoppingList) {
    viewModel.safeDeleteShoppingList(shoppingList);
  }

  @Override
  public MutableLiveData<Integer> getSelectedShoppingListIdLive() {
    return viewModel.getSelectedShoppingListIdLive();
  }

  @Override
  public void onItemRowClicked(GroupedListItem groupedListItem) {
    if (clickUtil.isDisabled()) {
      return;
    }
    if (groupedListItem == null) {
      return;
    }
    if (swipeBehavior != null) {
      swipeBehavior.recoverLatestSwipedItem();
    }
    if (groupedListItem.getType(GroupedListItem.CONTEXT_SHOPPING_LIST)
        == GroupedListItem.TYPE_ENTRY) {
      showItemBottomSheet((ShoppingListItem) groupedListItem);
    } else if (!viewModel.isOffline()) {  // Click on bottom notes
      showNotesEditor();
    }
  }

  @Override
  public void updateConnectivity(boolean isOnline) {
    if (!isOnline == viewModel.isOffline()) {
      return;
    }
    viewModel.downloadData(false, false);
  }

  private void hideDisabledFeatures() {
    if (isFeatureMultipleListsDisabled()) {
      binding.buttonShoppingListLists.setVisibility(View.GONE);
      binding.toolbar.setOnClickListener(null);
    }
  }

  private void showItemBottomSheet(ShoppingListItem item) {
    if (item == null) {
      return;
    }
    Bundle bundle = new Bundle();
    Double amountInQuUnit = viewModel.getShoppingListItemAmountsHashMap().get(item.getId());
    Product product = viewModel.getProductHashMap().get(item.getProductIdInt());
    String amountStr;
    if (product != null && amountInQuUnit != null) {
      bundle.putString(Constants.ARGUMENT.PRODUCT_NAME, product.getName());
      QuantityUnit quantityUnit = viewModel.getQuantityUnitHashMap().get(item.getQuIdInt());
      String quStr = pluralUtil.getQuantityUnitPlural(quantityUnit, amountInQuUnit);
      if (quStr != null) {
        amountStr = getString(R.string.subtitle_amount, NumUtil.trimAmount(amountInQuUnit, viewModel.getMaxDecimalPlacesAmount()), quStr);
      } else {
        amountStr = NumUtil.trimAmount(amountInQuUnit, viewModel.getMaxDecimalPlacesAmount());
      }
    } else if (product != null) {
      bundle.putString(Constants.ARGUMENT.PRODUCT_NAME, product.getName());
      QuantityUnit quantityUnit = viewModel.getQuantityUnitHashMap().get(product.getQuIdStockInt());
      String quStr = pluralUtil.getQuantityUnitPlural(quantityUnit, item.getAmountDouble());
      if (quStr != null) {
        amountStr = getString(R.string.subtitle_amount, NumUtil.trimAmount(item.getAmountDouble(), viewModel.getMaxDecimalPlacesAmount()), quStr);
      } else {
        amountStr = NumUtil.trimAmount(item.getAmountDouble(), viewModel.getMaxDecimalPlacesAmount());
      }
    } else {
      amountStr = NumUtil.trimAmount(item.getAmountDouble(), viewModel.getMaxDecimalPlacesAmount());
    }
    bundle.putString(ARGUMENT.AMOUNT, amountStr);
    bundle.putParcelable(Constants.ARGUMENT.SHOPPING_LIST_ITEM, item);
    bundle.putBoolean(Constants.ARGUMENT.SHOW_OFFLINE, viewModel.isOffline());
    activity.showBottomSheet(new ShoppingListItemBottomSheet(), bundle);
  }

  private void setUpSearch() {
    if (!viewModel.isSearchVisible()) {
      appBarBehavior.switchToSecondary();
      binding.editTextShoppingListSearch.setText("");
    }
    binding.textInputShoppingListSearch.requestFocus();
    activity.showKeyboard(binding.editTextShoppingListSearch);

    viewModel.setIsSearchVisible(true);
  }

  @Override
  public boolean isSearchVisible() {
    return viewModel.isSearchVisible();
  }

  @Override
  public void dismissSearch() {
    appBarBehavior.switchToPrimary();
    activity.hideKeyboard();
    binding.editTextShoppingListSearch.setText("");
    viewModel.setIsSearchVisible(false);
  }

  private boolean isFeatureMultipleListsDisabled() {
    return !sharedPrefs.getBoolean(Constants.PREF.FEATURE_MULTIPLE_SHOPPING_LISTS, true);
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}