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

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import java.util.ArrayList;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.QuantityUnitAdapter;
import xyz.zedler.patrick.grocy.databinding.FragmentBottomsheetListSelectionBinding;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.util.UiUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;

public class QuantityUnitsBottomSheet extends BaseBottomSheetDialogFragment
    implements QuantityUnitAdapter.QuantityUnitAdapterListener {

  private final static String TAG = QuantityUnitsBottomSheet.class.getSimpleName();

  private FragmentBottomsheetListSelectionBinding binding;
  private MainActivity activity;
  private ArrayList<QuantityUnit> quantityUnits;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentBottomsheetListSelectionBinding.inflate(
        inflater, container, false
    );

    activity = (MainActivity) requireActivity();
    Bundle bundle = requireArguments();

    ArrayList<QuantityUnit> qUsArg = bundle.getParcelableArrayList(ARGUMENT.QUANTITY_UNITS);
    assert qUsArg != null;
    quantityUnits = new ArrayList<>(qUsArg);

    SortUtil.sortQuantityUnitsByName(quantityUnits, true);
    if (bundle.getBoolean(ARGUMENT.DISPLAY_EMPTY_OPTION, false)) {
      quantityUnits.add(
          0,
          new QuantityUnit(-1, getString(R.string.subtitle_none_selected)));
    }
    boolean hasOptions = false;
    if (bundle.getBoolean(ARGUMENT.DISPLAY_NEW_OPTION, false)) {
      hasOptions = true;
      binding.buttonListSelectionNew.setVisibility(View.VISIBLE);
      binding.buttonListSelectionNew.setOnClickListener(v -> {
        dismiss();
        activity.getCurrentFragment().createQuantityUnit(bundle);
      });
    }
    int selected = bundle.getInt(Constants.ARGUMENT.SELECTED_ID, -1);

    binding.textListSelectionTitle.setText(activity.getString(R.string.property_quantity_units));
    if (!hasOptions) {
      ViewUtil.centerText(binding.textListSelectionTitle);
    }

    binding.recyclerListSelection.setLayoutManager(
        new LinearLayoutManager(
            activity,
            LinearLayoutManager.VERTICAL,
            false
        )
    );
    binding.recyclerListSelection.setItemAnimator(new DefaultItemAnimator());
    binding.recyclerListSelection.setAdapter(
        new QuantityUnitAdapter(quantityUnits, selected, this)
    );

    return binding.getRoot();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    binding = null;
  }

  @Override
  public void onItemRowClicked(int position) {
    activity.getCurrentFragment().selectQuantityUnit(quantityUnits.get(position));
    activity.getCurrentFragment().selectQuantityUnit(
        quantityUnits.get(position), requireArguments()
    );
    dismiss();
  }

  @Override
  public void onDismiss(@NonNull DialogInterface dialog) {
    activity.getCurrentFragment().onBottomSheetDismissed();
    super.onDismiss(dialog);
  }

  @Override
  public void applyBottomInset(int bottom) {
    binding.recyclerListSelection.setPadding(
        binding.recyclerListSelection.getPaddingLeft(),
        binding.recyclerListSelection.getPaddingTop(),
        binding.recyclerListSelection.getPaddingRight(),
        UiUtil.dpToPx(activity, 8) + bottom
    );
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
