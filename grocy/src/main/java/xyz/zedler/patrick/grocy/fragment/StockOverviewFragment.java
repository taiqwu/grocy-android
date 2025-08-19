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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.StockOverviewItemAdapter;
import xyz.zedler.patrick.grocy.behavior.AppBarBehavior;
import xyz.zedler.patrick.grocy.behavior.SwipeBehavior;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentStockOverviewBinding;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.GroupedListItem;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.scanner.EmbeddedFragmentScanner;
import xyz.zedler.patrick.grocy.scanner.EmbeddedFragmentScanner.BarcodeListener;
import xyz.zedler.patrick.grocy.scanner.EmbeddedFragmentScannerBundle;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;
import xyz.zedler.patrick.grocy.viewmodel.StockOverviewViewModel;

public class StockOverviewFragment extends BaseFragment implements
    StockOverviewItemAdapter.StockOverviewItemAdapterListener,
    BarcodeListener {

  private final static String TAG = StockOverviewFragment.class.getSimpleName();

  private MainActivity activity;
  private StockOverviewViewModel viewModel;
  private AppBarBehavior appBarBehavior;
  private ClickUtil clickUtil;
  private SwipeBehavior swipeBehavior;
  private FragmentStockOverviewBinding binding;
  private InfoFullscreenHelper infoFullscreenHelper;
  private EmbeddedFragmentScanner embeddedFragmentScanner;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentStockOverviewBinding.inflate(inflater, container, false);
    embeddedFragmentScanner = new EmbeddedFragmentScannerBundle(
        this, binding.containerScanner, this
    );
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
    StockOverviewFragmentArgs args = StockOverviewFragmentArgs.fromBundle(requireArguments());
    viewModel = new ViewModelProvider(this, new StockOverviewViewModel
        .StockOverviewViewModelFactory(activity.getApplication(), args)
    ).get(StockOverviewViewModel.class);
    binding.setViewModel(viewModel);
    binding.setActivity(activity);
    binding.setFragment(this);
    binding.setLifecycleOwner(getViewLifecycleOwner());

    infoFullscreenHelper = new InfoFullscreenHelper(binding.frame);
    clickUtil = new ClickUtil();

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBar);
    systemBarBehavior.setContainer(binding.swipe);
    systemBarBehavior.setRecycler(binding.recycler);
    systemBarBehavior.applyAppBarInsetOnContainer(false);
    systemBarBehavior.applyStatusBarInsetOnContainer(false);
    systemBarBehavior.setUp();
    activity.setSystemBarBehavior(systemBarBehavior);

    binding.toolbarDefault.setNavigationOnClickListener(v -> activity.navUtil.navigateUp());

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

    StockOverviewItemAdapter adapter = new StockOverviewItemAdapter(
        requireContext(),
        this,
        viewModel.isFeatureEnabled(PREF.FEATURE_STOCK_BBD_TRACKING),
        viewModel.isFeatureEnabled(PREF.FEATURE_SHOPPING_LIST),
        viewModel.getDaysExpriringSoon(),
        viewModel.getCurrency()
    );
    binding.recycler.setAdapter(adapter);

    if (savedInstanceState == null) {
      binding.recycler.scrollToPosition(0);
      viewModel.resetSearch();
    }

    viewModel.getInfoFullscreenLive().observe(
        getViewLifecycleOwner(),
        infoFullscreen -> infoFullscreenHelper.setInfo(infoFullscreen)
    );

    viewModel.getFilteredStockItemsLive().observe(getViewLifecycleOwner(), items -> {
      if (items == null) return;
      adapter.updateData(
          requireContext(),
          items,
          viewModel.getShoppingListItemsProductIds(),
          viewModel.getQuantityUnitHashMap(),
          viewModel.getQuantityUnitConversions(),
          viewModel.getProductAveragePriceHashMap(),
          viewModel.getProductLastPurchasedHashMap(),
          viewModel.getProductGroupHashMap(),
          viewModel.getProductHashMap(),
          viewModel.getLocationHashMap(),
          viewModel.getProductIdsMissingItems(),
          viewModel.getUserfieldHashMap(),
          viewModel.getSortMode(),
          viewModel.isSortAscending(),
          viewModel.getGroupingMode(),
          viewModel.getActiveFields(),
          () -> binding.recycler.scheduleLayoutAnimation()
      );
    });

    embeddedFragmentScanner.setScannerVisibilityLive(viewModel.getScannerVisibilityLive());

    viewModel.getEventHandler().observeEvent(getViewLifecycleOwner(), event -> {
      if (event.getType() == Event.SNACKBAR_MESSAGE) {
        activity.showSnackbar(
            ((SnackbarMessage) event).getSnackbar(activity.binding.coordinatorMain)
        );
      } else if (event.getType() == Event.SCROLL_UP) {
        binding.recycler.scrollToPosition(0);
      }
    });

    if (swipeBehavior == null) {
      swipeBehavior = new SwipeBehavior(
          activity,
          swipeStarted -> binding.swipe.setEnabled(!swipeStarted)
      ) {
        @Override
        public void instantiateUnderlayButton(
            RecyclerView.ViewHolder viewHolder,
            List<UnderlayButton> underlayButtons
        ) {
          if (viewHolder.getItemViewType() != GroupedListItem.TYPE_ENTRY) return;
          if (!(binding.recycler.getAdapter() instanceof StockOverviewItemAdapter)) return;
          int position = viewHolder.getBindingAdapterPosition();
          GroupedListItem item = ((StockOverviewItemAdapter) binding.recycler.getAdapter())
              .getGroupedListItemForPos(position);
          if (!(item instanceof StockItem)) {
            return;
          }
          StockItem stockItem = (StockItem) item;
          if (stockItem.getAmountAggregatedDouble() > 0
              && stockItem.getProduct().getEnableTareWeightHandlingInt() == 0
          ) {
            underlayButtons.add(new SwipeBehavior.UnderlayButton(
                activity,
                R.drawable.ic_round_consume_product,
                pos -> {
                  GroupedListItem item1 = ((StockOverviewItemAdapter) binding.recycler.getAdapter())
                      .getGroupedListItemForPos(position);
                  if (!(item1 instanceof StockItem)) {
                    return;
                  }
                  swipeBehavior.recoverLatestSwipedItem();
                  viewModel.performAction(
                      Constants.ACTION.CONSUME,
                      (StockItem) item1
                  );
                }
            ));
          }
          if (stockItem.getAmountAggregatedDouble()
              > stockItem.getAmountOpenedAggregatedDouble()
              && stockItem.getProduct().getEnableTareWeightHandlingInt() == 0
              && viewModel.isFeatureEnabled(Constants.PREF.FEATURE_STOCK_OPENED_TRACKING)
          ) {
            underlayButtons.add(new SwipeBehavior.UnderlayButton(
                activity,
                R.drawable.ic_round_open,
                pos -> {
                  GroupedListItem item1 = ((StockOverviewItemAdapter) binding.recycler.getAdapter())
                      .getGroupedListItemForPos(position);
                  if (!(item1 instanceof StockItem)) {
                    return;
                  }
                  swipeBehavior.recoverLatestSwipedItem();
                  viewModel.performAction(
                      Constants.ACTION.OPEN,
                      (StockItem) item1
                  );
                }
            ));
          }
          if (underlayButtons.isEmpty()) {
            underlayButtons.add(new SwipeBehavior.UnderlayButton(
                activity,
                R.drawable.ic_round_close,
                pos -> swipeBehavior.recoverLatestSwipedItem()
            ));
          }
        }
      };
    }
    swipeBehavior.attachToRecyclerView(binding.recycler);

    hideDisabledFeatures();

    if (savedInstanceState == null || !viewModel.isAlreadyLoadedFromDatabase()) {
      viewModel.loadFromDatabase(true);
    }

    // UPDATE UI

    activity.getScrollBehavior().setNestedOverScrollFixEnabled(true);
    activity.getScrollBehavior().setUpScroll(
        binding.appBar, false, binding.recycler, true, true
    );
    activity.getScrollBehavior().setBottomBarVisibility(true);
    activity.updateBottomAppBar(false, R.menu.menu_stock, this::onMenuItemClick);
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    if (appBarBehavior != null) {
      appBarBehavior.saveInstanceState(outState);
    }
  }

  public void toggleScannerVisibility() {
    viewModel.toggleScannerVisibility();
    if (viewModel.isScannerVisible()) {
      binding.editTextSearch.clearFocus();
      activity.hideKeyboard();
    } else {
      activity.showKeyboard(binding.editTextSearch);
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    embeddedFragmentScanner.onResume();
  }

  @Override
  public void onPause() {
    embeddedFragmentScanner.onPause();
    super.onPause();
  }

  @Override
  public void onDestroy() {
    if (embeddedFragmentScanner != null) embeddedFragmentScanner.onDestroy();
    super.onDestroy();
  }

  @Override
  public void onBarcodeRecognized(String rawValue) {
    viewModel.toggleScannerVisibility();
    binding.editTextSearch.setText(rawValue);
  }

  public void toggleTorch() {
    embeddedFragmentScanner.toggleTorch();
  }

  @Override
  public void performAction(String action, StockItem stockItem) {
    viewModel.performAction(action, stockItem);
  }

  private boolean onMenuItemClick(MenuItem item) {
    if (item.getItemId() == R.id.action_search) {
      ViewUtil.startIcon(item);
      setUpSearch();
      return true;
    } else if (item.getItemId() == R.id.action_stock_journal) {
      activity.navUtil.navigate(
          StockOverviewFragmentDirections.actionStockOverviewFragmentToStockJournalFragment()
      );
      return true;
    } else if (item.getItemId() == R.id.action_stock_entries) {
      activity.navUtil.navigate(
          StockOverviewFragmentDirections.actionStockOverviewFragmentToStockEntriesFragment()
      );
      return true;
    }
    return false;
  }

  @Override
  public void onItemRowClicked(StockItem stockItem) {
    if (clickUtil.isDisabled()) {
      return;
    }
    if (stockItem == null) {
      return;
    }
    if (swipeBehavior != null) {
      swipeBehavior.recoverLatestSwipedItem();
    }
    showProductOverview(stockItem);
  }

  private void showProductOverview(StockItem stockItem) {
    if (stockItem == null) {
      return;
    }
    QuantityUnit quantityUnitStock = viewModel
        .getQuantityUnitFromId(stockItem.getProduct().getQuIdStockInt());
    QuantityUnit quantityUnitPurchase = viewModel
        .getQuantityUnitFromId(stockItem.getProduct().getQuIdPurchaseInt());
    Location location = viewModel.getLocationFromId(stockItem.getProduct().getLocationIdInt());
    if (quantityUnitStock == null || quantityUnitPurchase == null) {
      activity.showSnackbar(R.string.error_undefined, false);
      return;
    }
    activity.navUtil.navigate(StockOverviewFragmentDirections
        .actionStockOverviewFragmentToProductOverviewBottomSheetDialogFragment()
        .setShowActions(true)
        .setStockItem(stockItem)
        .setQuantityUnitStock(quantityUnitStock)
        .setQuantityUnitPurchase(quantityUnitPurchase)
        .setLocation(location));
  }

  @Override
  public void updateConnectivity(boolean isOnline) {
    if (!isOnline == viewModel.isOffline()) {
      return;
    }
    viewModel.downloadData(false);
  }

  private void hideDisabledFeatures() {
  }

  private void setUpSearch() {
    if (!viewModel.isSearchVisible()) {
      appBarBehavior.switchToSecondary();
      binding.editTextSearch.setText("");
    }
    binding.textInputSearch.requestFocus();
    activity.showKeyboard(binding.editTextSearch);

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
    binding.editTextSearch.setText("");
    viewModel.setIsSearchVisible(false);
    if (viewModel.isScannerVisible()) {
      viewModel.toggleScannerVisibility();
    }
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}