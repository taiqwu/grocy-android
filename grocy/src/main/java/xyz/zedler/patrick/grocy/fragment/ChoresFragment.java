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
import android.os.Handler;
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
import xyz.zedler.patrick.grocy.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.ChoreEntryAdapter;
import xyz.zedler.patrick.grocy.adapter.ChoreEntryAdapter.ChoreEntryAdapterListener;
import xyz.zedler.patrick.grocy.behavior.AppBarBehavior;
import xyz.zedler.patrick.grocy.behavior.SwipeBehavior;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentChoresBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ChoreEntryBottomSheet;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.Chore;
import xyz.zedler.patrick.grocy.model.ChoreEntry;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.VersionUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;
import xyz.zedler.patrick.grocy.viewmodel.ChoresViewModel;

public class ChoresFragment extends BaseFragment implements ChoreEntryAdapterListener {

  private final static String TAG = ChoresFragment.class.getSimpleName();

  private MainActivity activity;
  private ChoresViewModel viewModel;
  private AppBarBehavior appBarBehavior;
  private ClickUtil clickUtil;
  private SwipeBehavior swipeBehavior;
  private FragmentChoresBinding binding;
  private InfoFullscreenHelper infoFullscreenHelper;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentChoresBinding.inflate(inflater, container, false);
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
    viewModel = new ViewModelProvider(this, new ChoresViewModel.ChoresViewModelFactory(
        activity.getApplication(),
        ChoresFragmentArgs.fromBundle(requireArguments())
    )).get(ChoresViewModel.class);
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
    ChoreEntryAdapter adapter = new ChoreEntryAdapter(
        requireContext(),
        (LinearLayoutManager) binding.recycler.getLayoutManager(),
        this
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

    viewModel.getFilteredChoreEntriesLive().observe(getViewLifecycleOwner(), items -> {
      if (items == null) {
        return;
      }
      if (items.isEmpty()) {
        InfoFullscreen info;
        if (viewModel.isSearchActive()) {
          info = new InfoFullscreen(InfoFullscreen.INFO_NO_SEARCH_RESULTS);
        } else if (viewModel.getFilterChipLiveDataStatus().getData().isActive()) {
          info = new InfoFullscreen(InfoFullscreen.INFO_NO_FILTER_RESULTS);
        } else {
          info = new InfoFullscreen(InfoFullscreen.INFO_EMPTY_CHORES);
        }
        viewModel.getInfoFullscreenLive().setValue(info);
      } else {
        viewModel.getInfoFullscreenLive().setValue(null);
      }
      adapter.updateData(
          items,
          viewModel.getChoreHashMap(),
          viewModel.getUsersHashMap(),
          viewModel.getSortMode(),
          viewModel.isSortAscending(),
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
          if (!(binding.recycler.getAdapter() instanceof ChoreEntryAdapter)) return;
          int position = viewHolder.getAdapterPosition();
          ChoreEntry entry = ((ChoreEntryAdapter) binding.recycler.getAdapter())
              .getEntryForPos(position);
          if (entry == null) {
            return;
          }
          underlayButtons.add(new UnderlayButton(
              activity,
              R.drawable.ic_round_play_arrow,
              pos -> {
                swipeBehavior.recoverLatestSwipedItem();
                ChoreEntry entry1 = ((ChoreEntryAdapter) binding.recycler.getAdapter())
                    .getEntryForPos(position);
                if (entry1 == null) return;
                new Handler().postDelayed(
                    () -> viewModel.executeChore(entry1, entry1.getNextEstimatedExecutionTime(), false),
                    100
                );
              }
          ));
          if (VersionUtil.isGrocyServerMin320(viewModel.getSharedPrefs())
              && !viewModel.hasManualScheduling(entry.getChoreId())) {
            underlayButtons.add(new UnderlayButton(
                activity,
                R.drawable.ic_round_skip_next,
                pos -> {
                  swipeBehavior.recoverLatestSwipedItem();
                  ChoreEntry entry1 = ((ChoreEntryAdapter) binding.recycler.getAdapter())
                      .getEntryForPos(position);
                  if (entry1 == null) return;
                  new Handler().postDelayed(
                      () -> viewModel.executeChore(entry1, entry1.getNextEstimatedExecutionTime(), true),
                      100
                  );
                }
            ));
          }
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
    activity.updateBottomAppBar(false, R.menu.menu_tasks, this::onMenuItemClick);
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    if (appBarBehavior != null) {
      appBarBehavior.saveInstanceState(outState);
    }
  }

  private boolean onMenuItemClick(MenuItem item) {
    if (item.getItemId() == R.id.action_search) {
      ViewUtil.startIcon(item);
      setUpSearch();
      return true;
    }
    return false;
  }

  @Override
  public void onItemRowClicked(ChoreEntry choreEntry) {
    if (clickUtil.isDisabled()) {
      return;
    }
    Chore chore = viewModel.getChoreHashMap().get(choreEntry.getChoreId());
    if (chore == null) {
      viewModel.showErrorMessage();
      return;
    }
    if (swipeBehavior != null) {
      swipeBehavior.recoverLatestSwipedItem();
    }
    Bundle bundle = new Bundle();
    bundle.putParcelable(ARGUMENT.CHORE, chore);
    bundle.putParcelable(ARGUMENT.CHORE_ENTRY, choreEntry);
    activity.showBottomSheet(new ChoreEntryBottomSheet(), bundle);
  }

  @Override
  public void trackNextChoreSchedule(ChoreEntry choreEntry) {
    viewModel.executeChore(choreEntry, choreEntry.getNextEstimatedExecutionTime(), false);
  }

  @Override
  public void skipNextChoreSchedule(ChoreEntry choreEntry) {
    viewModel.executeChore(choreEntry, choreEntry.getNextEstimatedExecutionTime(), true);
  }

  @Override
  public void trackChoreExecutionNow(ChoreEntry choreEntry) {
    viewModel.executeChore(choreEntry, null, false);
  }

  @Override
  public void rescheduleNextExecution(ChoreEntry choreEntry) {
    Chore chore = viewModel.getChoreHashMap().get(choreEntry.getChoreId());
    if (chore == null) {
      viewModel.showErrorMessage();
      return;
    }
    activity.navUtil.navigate(
        ChoresFragmentDirections.actionChoresFragmentToChoreEntryRescheduleFragment(chore)
    );
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
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}