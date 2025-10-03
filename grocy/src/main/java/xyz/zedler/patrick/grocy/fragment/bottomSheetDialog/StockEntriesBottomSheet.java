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
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import java.util.ArrayList;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.StockEntrySelectionAdapter;
import xyz.zedler.patrick.grocy.databinding.FragmentBottomsheetStockEntriesBinding;
import xyz.zedler.patrick.grocy.model.StockEntry;
import xyz.zedler.patrick.grocy.util.UiUtil;

public class StockEntriesBottomSheet extends BaseBottomSheetDialogFragment
    implements StockEntrySelectionAdapter.StockEntrySelectionAdapterListener {

  private final static String TAG = StockEntriesBottomSheet.class.getSimpleName();

  private FragmentBottomsheetStockEntriesBinding binding;

  private MainActivity activity;
  private ArrayList<StockEntry> stockEntries;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentBottomsheetStockEntriesBinding.inflate(
        inflater, container, false
    );

    activity = (MainActivity) requireActivity();
    Bundle bundle = requireArguments();

    stockEntries = bundle.getParcelableArrayList(Constants.ARGUMENT.STOCK_ENTRIES);
    String selectedStockId = bundle.getString(Constants.ARGUMENT.SELECTED_ID);

    // Add entry for automatic selection
    stockEntries.add(0, new StockEntry(-1, null));

    binding.recyclerStockEntries.setLayoutManager(
        new LinearLayoutManager(
            activity,
            LinearLayoutManager.VERTICAL,
            false
        )
    );
    binding.recyclerStockEntries.setItemAnimator(new DefaultItemAnimator());
    binding.recyclerStockEntries.setAdapter(
        new StockEntrySelectionAdapter(activity, stockEntries, selectedStockId, this)
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
    StockEntry stockEntry = stockEntries.get(position);
    activity.getCurrentFragment().selectStockEntry(stockEntry.getId() != -1 ? stockEntry : null);
    dismiss();
  }

  @Override
  public void applyBottomInset(int bottom) {
    binding.recyclerStockEntries.setPadding(
        binding.recyclerStockEntries.getPaddingLeft(),
        binding.recyclerStockEntries.getPaddingTop(),
        binding.recyclerStockEntries.getPaddingRight(),
        UiUtil.dpToPx(activity, 8) + bottom
    );
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
