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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.File;
import java.io.IOException;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.ACTION;
import xyz.zedler.patrick.grocy.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentRecipeEditBinding;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.Recipe;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.scanner.EmbeddedFragmentScanner;
import xyz.zedler.patrick.grocy.scanner.EmbeddedFragmentScannerBundle;
import xyz.zedler.patrick.grocy.util.PictureUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;
import xyz.zedler.patrick.grocy.viewmodel.RecipeEditViewModel;
import xyz.zedler.patrick.grocy.viewmodel.RecipeEditViewModel.RecipeEditViewModelFactory;
import xyz.zedler.patrick.grocy.web.RequestHeaders;

public class RecipeEditFragment extends BaseFragment implements EmbeddedFragmentScanner.BarcodeListener {

  private final static String TAG = RecipeEditFragment.class.getSimpleName();

  private static final String DIALOG_DELETE_SHOWING = "dialog_delete_showing";

  private MainActivity activity;
  private FragmentRecipeEditBinding binding;
  private RecipeEditViewModel viewModel;
  private InfoFullscreenHelper infoFullscreenHelper;
  private EmbeddedFragmentScanner embeddedFragmentScanner;
  private AlertDialog dialogDelete;
  private ActivityResultLauncher<Intent> mActivityResultLauncherTakePicture;

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup group, Bundle state) {
    binding = FragmentRecipeEditBinding.inflate(inflater, group, false);
    embeddedFragmentScanner = new EmbeddedFragmentScannerBundle(
            this,
            binding.containerScanner,
            this
    );
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
    RecipeEditFragmentArgs args = RecipeEditFragmentArgs
        .fromBundle(requireArguments());
    viewModel = new ViewModelProvider(
        this, new RecipeEditViewModelFactory(activity.getApplication(), args)
    ).get(RecipeEditViewModel.class);
    binding.setActivity(activity);
    binding.setViewModel(viewModel);
    binding.setFormData(viewModel.getFormData());
    binding.setFragment(this);
    binding.setLifecycleOwner(getViewLifecycleOwner());

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBar);
    systemBarBehavior.setContainer(binding.swipe);
    systemBarBehavior.setScroll(binding.scroll, binding.constraint);
    systemBarBehavior.setUp();
    activity.setSystemBarBehavior(systemBarBehavior);

    binding.toolbar.setNavigationOnClickListener(v -> activity.navUtil.navigateUp());

    binding.ingredients.setOnClickListener(v -> {
      if (viewModel.isActionEdit()) {
        activity.navUtil.navigate(
            RecipeEditFragmentDirections
                .actionRecipeEditFragmentToRecipeEditIngredientListFragment(viewModel.getAction())
                .setRecipe(viewModel.getRecipe())
        );
      } else {
        activity.showSnackbar(R.string.msg_save_recipe_first, true);
      }
    });
    binding.preparation.setOnClickListener(v -> {
      if (viewModel.getFormData().getPreparationLive().getValue() != null) {
        activity.navUtil.navigate(
            RecipeEditFragmentDirections.actionRecipeEditFragmentToEditorHtmlFragment2().setText(
                viewModel.getFormData().getPreparationLive().getValue()
            )
        );
      } else {
        activity.navUtil.navigate(
            RecipeEditFragmentDirections.actionRecipeEditFragmentToEditorHtmlFragment2()
        );
      }
    });
    Object preparationEdited = getFromThisDestinationNow(ARGUMENT.DESCRIPTION);
    if (preparationEdited != null) {
      removeForThisDestination(ARGUMENT.DESCRIPTION);
      viewModel.getFormData().getPreparationLive().setValue((String) preparationEdited);
      viewModel.getFormData().getPreparationSpannedLive()
          .setValue(Html.fromHtml((String) preparationEdited));
    }

    viewModel.getEventHandler().observeEvent(getViewLifecycleOwner(), event -> {
      if (event.getType() == Event.SNACKBAR_MESSAGE) {
        activity.showSnackbar(
            ((SnackbarMessage) event).getSnackbar(activity.binding.coordinatorMain)
        );
      } else if (event.getType() == Event.NAVIGATE_UP) {
        activity.navUtil.navigateUp();
      } else if (event.getType() == Event.SET_RECIPE_ID) {
        int id = event.getBundle().getInt(Constants.ARGUMENT.RECIPE_ID);
        setForPreviousDestination(Constants.ARGUMENT.RECIPE_ID, id);
      } else if (event.getType() == Event.BOTTOM_SHEET) {
        BottomSheetEvent bottomSheetEvent = (BottomSheetEvent) event;
        activity.showBottomSheet(bottomSheetEvent.getBottomSheet(), event.getBundle());
      } else if (event.getType() == Event.TRANSACTION_SUCCESS) {
        if (args.getAction().equals(ACTION.CREATE) && viewModel.isActionEdit()) {
          activity.updateFab(
              R.drawable.ic_round_save,
              R.string.action_save,
              Constants.FAB.TAG.SAVE,
              savedInstanceState == null,
              () -> {
                if (!viewModel.getFormData().isNameValid()) {
                  clearInputFocus();
                  activity.showKeyboard(binding.editTextName);
                } else {
                  clearInputFocus();
                  viewModel.saveEntry(true);
                }
              }
          );
        }
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

    if (savedInstanceState == null && args.getAction().equals(ACTION.CREATE)) {
      if (viewModel.getFormData().getNameLive().getValue() == null
          || viewModel.getFormData().getNameLive().getValue().length() == 0) {
        activity.showKeyboard(binding.editTextName);
      }
    }

    embeddedFragmentScanner.setScannerVisibilityLive(
            viewModel.getFormData().getScannerVisibilityLive()
    );

    viewModel.getFormData().getPictureFilenameLive().observe(getViewLifecycleOwner(),
        this::loadRecipePicture);

    mActivityResultLauncherTakePicture = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
          if (result.getResultCode() == Activity.RESULT_OK) {
            viewModel.scaleAndUploadBitmap(viewModel.getCurrentFilePath(), null);
          }
        });

    viewModel.getActionEditLive().observe(getViewLifecycleOwner(), isEdit -> activity.updateBottomAppBar(
        true,
        isEdit
            ? R.menu.menu_recipe_edit_edit
            : R.menu.menu_recipe_edit_create,
        this::onMenuItemClick
    ));

    String action = (String) getFromThisDestinationNow(Constants.ARGUMENT.ACTION);
    if (action != null) {
      removeForThisDestination(Constants.ARGUMENT.ACTION);
      switch (action) {
        case ACTION.SAVE_CLOSE:
          new Handler().postDelayed(() -> viewModel.saveEntry(true), 500);
          break;
        case ACTION.SAVE_NOT_CLOSE:
          new Handler().postDelayed(() -> viewModel.saveEntry(false), 500);
          break;
        case ACTION.DELETE:
          new Handler().postDelayed(() -> viewModel.deleteEntry(), 500);
          break;
      }
    }

    if (savedInstanceState == null) {
      viewModel.loadFromDatabase(true);
    } else {
      if (savedInstanceState.getBoolean(DIALOG_DELETE_SHOWING)) {
        new Handler(Looper.getMainLooper()).postDelayed(
            this::showDeleteConfirmationDialog, 1
        );
      }
    }

    // UPDATE UI

    activity.getScrollBehavior().setNestedOverScrollFixEnabled(true);
    activity.getScrollBehavior().setUpScroll(
        binding.appBar, false, binding.scroll, false, false
    );
    activity.getScrollBehavior().setBottomBarVisibility(true);
    activity.updateFab(
        viewModel.isActionEdit() ? R.drawable.ic_round_save : R.drawable.ic_round_save_as,
        viewModel.isActionEdit() ? R.string.action_save : R.string.action_save_not_close,
        viewModel.isActionEdit() ? Constants.FAB.TAG.SAVE : Constants.FAB.TAG.SAVE_NOT_CLOSE,
        savedInstanceState == null,
        () -> {
          if (!viewModel.getFormData().isFormValid()) {
            clearInputFocus();
            activity.showKeyboard(binding.editTextName);
          } else {
            clearInputFocus();
            viewModel.saveEntry(viewModel.isActionEdit());
          }
        }
    );
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean(DIALOG_DELETE_SHOWING, dialogDelete != null && dialogDelete.isShowing());
  }

  public void clearInputFocus() {
    activity.hideKeyboard();
    binding.textInputName.clearFocus();
    binding.scroll.clearFocus();
    binding.dummyFocusView.requestFocus();
  }

  public void clearBaseServingsFieldAndFocusIt() {
    binding.editTextAmount.setText("");
    activity.showKeyboard(binding.editTextAmount);
  }

  public void onItemAutoCompleteClick(AdapterView<?> adapterView, int pos) {
    Product product = (Product) adapterView.getItemAtPosition(pos);

    clearInputFocus();
    if (product == null) {
      return;
    }
    viewModel.setProduct(product);
  }

  public void clearFocusAndCheckProductInput() {
    clearInputFocus();
    viewModel.checkProductInput();
  }

  public void clearFocusAndCheckProductInputExternal() {
    clearInputFocus();
    String input = viewModel.getFormData().getProductProducedNameLive().getValue();
    if (input == null || input.isEmpty()) return;
    viewModel.onBarcodeRecognized(viewModel.getFormData().getProductProducedNameLive().getValue());
  }

  private boolean onMenuItemClick(MenuItem item) {
    if (item.getItemId() == R.id.action_delete) {
      ViewUtil.startIcon(item);
      showDeleteConfirmationDialog();
      return true;
    } else if (item.getItemId() == R.id.action_clear_form) {
      clearInputFocus();
      viewModel.getFormData().clearForm();
      return true;
    } else if (item.getItemId() == R.id.action_save) {
      viewModel.saveEntry(true);
      return true;
    }
    return false;
  }

  public void toggleScannerVisibility() {
    viewModel.getFormData().toggleScannerVisibility();
    if (viewModel.getFormData().isScannerVisible()) {
      clearInputFocus();
    }
  }

  private void loadRecipePicture(String filename) {
    if (filename != null && !filename.isBlank()) {
      GrocyApi grocyApi = new GrocyApi(activity.getApplication());
      PictureUtil.loadPicture(
          binding.picture,
          null,
          null,
          grocyApi.getRecipePictureServeLarge(filename),
          RequestHeaders.getGlideGrocyAuthHeaders(requireContext()),
          true
      );
    } else {
      binding.picture.setVisibility(View.GONE);
    }
  }

  public void dispatchTakePictureIntent() {
    if (viewModel.isDemoInstance()) {
      viewModel.showMessage(R.string.error_picture_uploads_forbidden);
      return;
    }
    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    // Create the File where the photo should go
    File photoFile = null;
    try {
      photoFile = viewModel.createImageFile();
    } catch (IOException ex) {
      viewModel.showErrorMessage();
      viewModel.setCurrentFilePath(null);
    }
    if (photoFile != null) {
      Uri photoURI = FileProvider.getUriForFile(requireContext(),
          requireContext().getPackageName() + ".fileprovider",
          photoFile);
      takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
      mActivityResultLauncherTakePicture.launch(takePictureIntent);
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
    clearInputFocus();
    viewModel.getFormData().toggleScannerVisibility();
    viewModel.onBarcodeRecognized(rawValue);
  }

  private void showDeleteConfirmationDialog() {
    Recipe recipe = viewModel.getRecipe();
    if (recipe == null) {
      activity.showSnackbar(R.string.error_undefined, false);
      return;
    }
    dialogDelete = new MaterialAlertDialogBuilder(
        activity, R.style.ThemeOverlay_Grocy_AlertDialog_Caution
    ).setTitle(R.string.title_confirmation)
        .setMessage(getString(R.string.msg_master_delete, getString(R.string.title_recipe), recipe.getName()))
        .setPositiveButton(R.string.action_delete, (dialog, which) -> {
          performHapticClick();
          viewModel.deleteEntry();
          activity.navUtil.navigateUp();
          activity.navUtil.navigateUp();
        }).setNegativeButton(R.string.action_cancel, (dialog, which) -> performHapticClick())
        .setOnCancelListener(dialog -> performHapticClick())
        .create();
    dialogDelete.show();
  }

  public void toggleTorch() {
    embeddedFragmentScanner.toggleTorch();
  }

  @Override
  public void updateConnectivity(boolean isOnline) {
    if (!isOnline == viewModel.isOffline()) {
      return;
    }
    viewModel.downloadData(false);
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
