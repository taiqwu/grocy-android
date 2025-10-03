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
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.RecipeEditIngredientListEntryAdapter;
import xyz.zedler.patrick.grocy.adapter.RecipeEditIngredientListEntryAdapter.RecipeEditIngredientListEntryAdapterListener;
import xyz.zedler.patrick.grocy.behavior.SwipeBehavior;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentRecipeEditIngredientListBinding;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.RecipePosition;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.viewmodel.RecipeEditIngredientListViewModel;

public class RecipeEditIngredientListFragment extends BaseFragment
        implements RecipeEditIngredientListEntryAdapterListener {

  private final static String TAG = RecipeEditIngredientListFragment.class.getSimpleName();

  private MainActivity activity;
  private FragmentRecipeEditIngredientListBinding binding;
  private RecipeEditIngredientListViewModel viewModel;
  private SwipeBehavior swipeBehavior;
  private InfoFullscreenHelper infoFullscreenHelper;
  private SystemBarBehavior systemBarBehavior;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentRecipeEditIngredientListBinding.inflate(
        inflater, container, false
    );
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    if (binding != null) {
      binding.recycler.animate().cancel();
      binding.recycler.setAdapter(null);
      binding = null;
    }
  }

  @Override
  public void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) requireActivity();
    RecipeEditIngredientListFragmentArgs args = RecipeEditIngredientListFragmentArgs
        .fromBundle(requireArguments());
    viewModel = new ViewModelProvider(this, new RecipeEditIngredientListViewModel
        .RecipeEditIngredientListViewModelFactory(activity.getApplication(), args)
    ).get(RecipeEditIngredientListViewModel.class);
    binding.setActivity(activity);
    binding.setFormData(viewModel.getFormData());
    binding.setViewModel(viewModel);
    binding.setFragment(this);
    binding.setLifecycleOwner(getViewLifecycleOwner());

    systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBar);
    systemBarBehavior.setContainer(binding.swipe);
    systemBarBehavior.setRecycler(binding.recycler);
    systemBarBehavior.setUp();
    activity.setSystemBarBehavior(systemBarBehavior);

    binding.toolbar.setNavigationOnClickListener(v -> activity.navUtil.navigateUp());

    infoFullscreenHelper = new InfoFullscreenHelper(binding.container);

    binding.recycler.setLayoutManager(
            new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
    );
    RecipeEditIngredientListEntryAdapter adapter = new RecipeEditIngredientListEntryAdapter(
        requireContext(),
        (LinearLayoutManager) binding.recycler.getLayoutManager(),
        this
    );
    binding.recycler.setAdapter(adapter);

    if (savedInstanceState == null) {
      binding.recycler.scrollToPosition(0);
    }

    viewModel.getInfoFullscreenLive().observe(
            getViewLifecycleOwner(),
            infoFullscreen -> infoFullscreenHelper.setInfo(infoFullscreen)
    );

    viewModel.getRecipePositionsLive().observe(getViewLifecycleOwner(), items -> {
      if (items == null) {
        return;
      }
      if (items.isEmpty()) {
        InfoFullscreen info = new InfoFullscreen(InfoFullscreen.INFO_EMPTY_INGREDIENTS);
        viewModel.getInfoFullscreenLive().setValue(info);
      } else {
        viewModel.getInfoFullscreenLive().setValue(null);
      }
      adapter.updateData(
          items,
          viewModel.getProducts(),
          viewModel.getQuantityUnitHashMap(),
          viewModel.getUnitConversions(),
          () -> binding.recycler.scheduleLayoutAnimation()
      );
    });

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
          if (!(binding.recycler.getAdapter() instanceof RecipeEditIngredientListEntryAdapter)) return;
          int position = viewHolder.getAdapterPosition();
          RecipePosition entry = ((RecipeEditIngredientListEntryAdapter) binding.recycler.getAdapter())
              .getEntryForPos(position);
          if (entry == null) {
            return;
          }

          underlayButtons.add(new UnderlayButton(
              activity,
              R.drawable.ic_round_delete_anim,
              pos -> {
                RecipePosition entry1 = ((RecipeEditIngredientListEntryAdapter) binding
                    .recycler.getAdapter()).getEntryForPos(position);
                if (entry1 == null) {
                  return;
                }
                swipeBehavior.recoverLatestSwipedItem();
                new Handler().postDelayed(() ->
                    deleteRecipePosition(entry1.getId()), 100);
              }
          ));
        }
      };
    }
    swipeBehavior.attachToRecyclerView(binding.recycler);

    if (savedInstanceState == null) {
      viewModel.loadFromDatabase(true);
    }

    // UPDATE UI

    activity.getScrollBehavior().setNestedOverScrollFixEnabled(true);
    activity.getScrollBehavior().setUpScroll(binding.appBar, false, binding.recycler);
    activity.getScrollBehavior().setBottomBarVisibility(true);
    activity.updateBottomAppBar(true, R.menu.menu_empty);
    activity.updateFab(
        R.drawable.ic_round_add_anim,
        R.string.title_ingredient_new,
        Constants.FAB.TAG.ADD,
        savedInstanceState == null,
        () -> activity.navUtil.navigate(RecipeEditIngredientListFragmentDirections
            .actionRecipeEditIngredientListFragmentToRecipeEditIngredientEditFragment(
                ACTION.CREATE,
                viewModel.getAction()
            )
            .setRecipe(viewModel.getRecipe())
        )
    );
  }

  @Override
  public void onItemRowClicked(RecipePosition recipePosition, int position) {
    activity.navUtil.navigate(
        RecipeEditIngredientListFragmentDirections
            .actionRecipeEditIngredientListFragmentToRecipeEditIngredientEditFragment(
                    ACTION.EDIT,
                    viewModel.getAction()
            )
            .setRecipePosition(recipePosition)
            .setRecipe(viewModel.getRecipe())
    );
  }

  @Override
  public void deleteRecipePosition(int recipePositionId) {
    viewModel.deleteRecipePosition(recipePositionId);
  }

  @Override
  public void updateConnectivity(boolean isOnline) {
    if (!isOnline == viewModel.isOffline()) {
      return;
    }
    viewModel.downloadData(false);
    if (systemBarBehavior != null) {
      systemBarBehavior.refresh();
    }
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
