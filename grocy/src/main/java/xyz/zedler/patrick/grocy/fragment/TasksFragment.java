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
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.ACTION;
import xyz.zedler.patrick.grocy.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.TaskEntryAdapter;
import xyz.zedler.patrick.grocy.behavior.AppBarBehavior;
import xyz.zedler.patrick.grocy.behavior.SwipeBehavior;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentTasksBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.TaskEntryBottomSheet;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.model.Task;
import xyz.zedler.patrick.grocy.model.TaskCategory;
import xyz.zedler.patrick.grocy.model.User;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;
import xyz.zedler.patrick.grocy.viewmodel.TasksViewModel;

public class TasksFragment extends BaseFragment implements
    TaskEntryAdapter.TasksItemAdapterListener {

  private final static String TAG = TasksFragment.class.getSimpleName();

  private MainActivity activity;
  private TasksViewModel viewModel;
  private AppBarBehavior appBarBehavior;
  private ClickUtil clickUtil;
  private SwipeBehavior swipeBehavior;
  private FragmentTasksBinding binding;
  private InfoFullscreenHelper infoFullscreenHelper;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentTasksBinding.inflate(inflater, container, false);
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
    viewModel = new ViewModelProvider(this).get(TasksViewModel.class);
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
    TaskEntryAdapter adapter = new TaskEntryAdapter(
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

    viewModel.getFilteredTasksLive().observe(getViewLifecycleOwner(), items -> {
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
          info = new InfoFullscreen(InfoFullscreen.INFO_EMPTY_TASKS);
        }
        viewModel.getInfoFullscreenLive().setValue(info);
      } else {
        viewModel.getInfoFullscreenLive().setValue(null);
      }
      adapter.updateData(
          items,
          viewModel.getTaskCategoriesHashMap(),
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
          if (!(binding.recycler.getAdapter() instanceof TaskEntryAdapter)) return;
          int position = viewHolder.getAdapterPosition();
          Task task = ((TaskEntryAdapter) binding.recycler.getAdapter())
              .getTaskForPos(position);
          if (task == null) {
            return;
          }
          underlayButtons.add(new UnderlayButton(
              activity,
              R.drawable.ic_round_done,
              pos -> {
                Task task1 = ((TaskEntryAdapter) binding.recycler.getAdapter())
                    .getTaskForPos(position);
                if (task1 == null) {
                  return;
                }
                swipeBehavior.recoverLatestSwipedItem();
                new Handler().postDelayed(() -> viewModel
                    .changeTaskDoneStatus(task1.getId()), 100);
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
    activity.updateBottomAppBar(true, R.menu.menu_tasks, this::onMenuItemClick);
    activity.updateFab(
        R.drawable.ic_round_add_anim,
        R.string.title_task_new,
        Constants.FAB.TAG.ADD,
        savedInstanceState == null,
        () -> activity.navUtil.navigate(
            TasksFragmentDirections.actionTasksFragmentToTaskEntryEditFragment(ACTION.CREATE)
        )
    );
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
  public void onItemRowClicked(Task task) {
    if (clickUtil.isDisabled()) {
      return;
    }
    if (task == null) {
      return;
    }
    if (swipeBehavior != null) {
      swipeBehavior.recoverLatestSwipedItem();
    }
    Bundle bundle = new Bundle();
    bundle.putParcelable(ARGUMENT.TASK, task);
    TaskCategory category = NumUtil.isStringInt(task.getCategoryId())
        ? viewModel.getTaskCategoriesHashMap().get(Integer.parseInt(task.getCategoryId())) : null;
    String categoryText = category != null ? category.getName()
        : getString(R.string.subtitle_uncategorized);
    bundle.putString(ARGUMENT.TASK_CATEGORY, categoryText);
    User user = NumUtil.isStringInt(task.getAssignedToUserId())
        ? viewModel.getUsersHashMap().get(Integer.parseInt(task.getAssignedToUserId())) : null;
    bundle.putString(ARGUMENT.USER, user != null ? user.getDisplayName() : null);
    activity.showBottomSheet(new TaskEntryBottomSheet(), bundle);
  }

  @Override
  public void toggleDoneStatus(Task task) {
    viewModel.changeTaskDoneStatus(task.getId());
  }

  @Override
  public void editTask(Task task) {
    activity.navUtil.navigate(
        TasksFragmentDirections
            .actionTasksFragmentToTaskEntryEditFragment(ACTION.EDIT)
            .setTaskEntry(task)
    );
  }

  @Override
  public void deleteTask(Task task) {
    viewModel.deleteTask(task.getId());
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