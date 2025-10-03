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
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.ACTION;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentTaskEntryEditBinding;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.model.TaskCategory;
import xyz.zedler.patrick.grocy.model.User;
import xyz.zedler.patrick.grocy.util.ViewUtil;
import xyz.zedler.patrick.grocy.viewmodel.TaskEntryEditViewModel;
import xyz.zedler.patrick.grocy.viewmodel.TaskEntryEditViewModel.TaskEntryEditViewModelFactory;

public class TaskEntryEditFragment extends BaseFragment {

  private final static String TAG = TaskEntryEditFragment.class.getSimpleName();

  private MainActivity activity;
  private FragmentTaskEntryEditBinding binding;
  private TaskEntryEditViewModel viewModel;
  private InfoFullscreenHelper infoFullscreenHelper;
  private SystemBarBehavior systemBarBehavior;

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup group, Bundle state) {
    binding = FragmentTaskEntryEditBinding.inflate(inflater, group, false);
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) requireActivity();
    TaskEntryEditFragmentArgs args = TaskEntryEditFragmentArgs
        .fromBundle(requireArguments());
    viewModel = new ViewModelProvider(
        this,
        new TaskEntryEditViewModelFactory(activity.getApplication(), args)
    ).get(TaskEntryEditViewModel.class);
    binding.setActivity(activity);
    binding.setViewModel(viewModel);
    binding.setFormData(viewModel.getFormData());
    binding.setFragment(this);
    binding.setLifecycleOwner(getViewLifecycleOwner());

    systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBar);
    systemBarBehavior.setContainer(binding.swipe);
    systemBarBehavior.setScroll(binding.scroll, binding.constraint);
    systemBarBehavior.setUp();
    activity.setSystemBarBehavior(systemBarBehavior);

    binding.toolbar.setNavigationOnClickListener(v -> activity.navUtil.navigateUp());

    viewModel.getEventHandler().observeEvent(getViewLifecycleOwner(), event -> {
      if (event.getType() == Event.SNACKBAR_MESSAGE) {
        activity.showSnackbar(
            ((SnackbarMessage) event).getSnackbar(activity.binding.coordinatorMain)
        );
      } else if (event.getType() == Event.NAVIGATE_UP) {
        activity.navUtil.navigateUp();
      } else if (event.getType() == Event.BOTTOM_SHEET) {
        BottomSheetEvent bottomSheetEvent = (BottomSheetEvent) event;
        activity.showBottomSheet(bottomSheetEvent.getBottomSheet(), event.getBundle());
      }
    });

    infoFullscreenHelper = new InfoFullscreenHelper(binding.container);
    viewModel.getInfoFullscreenLive().observe(
        getViewLifecycleOwner(),
        infoFullscreen -> infoFullscreenHelper.setInfo(infoFullscreen)
    );

    viewModel.getOfflineLive().observe(getViewLifecycleOwner(), offline -> {
      InfoFullscreen infoFullscreen = offline ? new InfoFullscreen(
          InfoFullscreen.ERROR_OFFLINE,
          () -> updateConnectivity(true)
      ) : null;
      viewModel.getInfoFullscreenLive().setValue(infoFullscreen);
    });

    viewModel.getFormData().getUseMultilineDescriptionLive().observe(getViewLifecycleOwner(), multi -> {
      if(multi) {
        binding.editTextDescription.setInputType(InputType.TYPE_CLASS_TEXT
            | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        binding.editTextDescription.setImeOptions(EditorInfo.IME_ACTION_UNSPECIFIED);
      } else {
        binding.editTextDescription.setInputType(InputType.TYPE_CLASS_TEXT
            | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        binding.editTextDescription.setImeOptions(EditorInfo.IME_ACTION_DONE);
      }
      if(binding.editTextDescription.isFocused()) {
        activity.hideKeyboard();
        if (binding.editTextDescription.getText() != null) {
          binding.editTextDescription.setSelection(binding.editTextDescription.getText().length());
        }
        binding.editTextDescription.clearFocus();
        activity.showKeyboard(binding.editTextDescription);
      }
    });

    ViewUtil.setTooltipText(binding.buttonDeleteDueDate, R.string.action_delete);

    if (savedInstanceState == null && !args.getAction().equals(ACTION.EDIT)) {
      if (binding.editTextName.getText() == null || binding.editTextName.getText().length() == 0) {
        activity.showKeyboard(binding.editTextName);
      }
    }

    if (savedInstanceState == null) {
      viewModel.loadFromDatabase(true);
    }

    // UPDATE UI

    activity.getScrollBehavior().setNestedOverScrollFixEnabled(true);
    activity.getScrollBehavior().setUpScroll(
        binding.appBar, false, binding.scroll, true
    );
    activity.getScrollBehavior().setBottomBarVisibility(true);
    activity.updateBottomAppBar(
        true,
        viewModel.isActionEdit()
            ? R.menu.menu_task_entry_edit_edit
            : R.menu.menu_task_entry_edit_create,
        this::onMenuItemClick
    );
    activity.updateFab(
        R.drawable.ic_round_backup,
        R.string.action_save,
        Constants.FAB.TAG.SAVE,
        savedInstanceState == null,
        () -> {
          if (!viewModel.getFormData().isNameValid()) {
            clearInputFocus();
            activity.showKeyboard(binding.editTextName);
          } else {
            viewModel.saveEntry();
          }
        }
    );
  }

  public void clearInputFocus() {
    activity.hideKeyboard();
    binding.dummyFocusView.requestFocus();
    binding.textInputName.clearFocus();
    binding.textInputDescription.clearFocus();
  }

  @Override
  public void selectDueDate(String dueDate) {
    viewModel.getFormData().getDueDateLive().setValue(dueDate);
  }

  @Override
  public void selectTaskCategory(TaskCategory taskCategory) {
    viewModel.getFormData().getTaskCategoryLive().setValue(taskCategory);
  }

  @Override
  public void selectUser(User user) {
    viewModel.getFormData().getUserLive().setValue(user);
  }

  private boolean onMenuItemClick(MenuItem item) {
    if (item.getItemId() == R.id.action_delete) {
      ViewUtil.startIcon(item);
      viewModel.deleteEntry();
      return true;
    } else if (item.getItemId() == R.id.action_clear_form) {
      clearInputFocus();
      viewModel.getFormData().clearForm();
      return true;
    }
    return false;
  }

  @Override
  public void updateConnectivity(boolean isOnline) {
    if (!isOnline == viewModel.isOffline()) {
      return;
    }
    viewModel.downloadData(false);
    systemBarBehavior.refresh();
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
