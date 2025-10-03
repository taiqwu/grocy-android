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

package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentBottomsheetShoppingListClearBinding;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.util.UiUtil;

public class ShoppingListClearBottomSheet extends BaseBottomSheetDialogFragment {

  private final static String TAG = ShoppingListClearBottomSheet.class.getSimpleName();

  private MainActivity activity;
  private FragmentBottomsheetShoppingListClearBinding binding;

  private ShoppingList shoppingList;
  private MutableLiveData<Integer> selectionLive;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentBottomsheetShoppingListClearBinding.inflate(
        inflater, container, false
    );
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    setSkipCollapsedInPortrait();
    super.onViewCreated(view, savedInstanceState);

    activity = (MainActivity) requireActivity();
    binding.setBottomsheet(this);
    binding.setLifecycleOwner(getViewLifecycleOwner());

    shoppingList = requireArguments().getParcelable(Constants.ARGUMENT.SHOPPING_LIST);

    selectionLive = new MutableLiveData<>(1);
  }

  public void proceed() {
    assert selectionLive.getValue() != null;
    activity.getCurrentFragment().clearShoppingList(
        shoppingList,
        selectionLive.getValue() == 1
    );
    dismiss();
  }

  public MutableLiveData<Integer> getSelectionLive() {
    return selectionLive;
  }

  public void setSelectionLive(int selection) {
    selectionLive.setValue(selection);
  }

  @Override
  public void applyBottomInset(int bottom) {
    binding.linearContainerScroll.setPadding(
        binding.linearContainerScroll.getPaddingLeft(),
        binding.linearContainerScroll.getPaddingTop(),
        binding.linearContainerScroll.getPaddingRight(),
        UiUtil.dpToPx(activity, 12) + bottom
    );
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
