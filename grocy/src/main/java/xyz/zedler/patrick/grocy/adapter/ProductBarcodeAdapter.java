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

package xyz.zedler.patrick.grocy.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.databinding.RowProductBarcodeBinding;
import xyz.zedler.patrick.grocy.model.ProductBarcode;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.util.NumUtil;

public class ProductBarcodeAdapter extends RecyclerView.Adapter<ProductBarcodeAdapter.ViewHolder> {

  private final static String TAG = ProductBarcodeAdapter.class.getSimpleName();

  private final ArrayList<ProductBarcode> productBarcodes;
  private final ProductBarcodeAdapterListener listener;
  private final List<QuantityUnit> quantityUnits;
  private final List<Store> stores;
  private final int maxDecimalPlacesAmount;

  public ProductBarcodeAdapter(
      Context context,
      ProductBarcodeAdapterListener listener
  ) {
    this.productBarcodes = new ArrayList<>();
    this.listener = listener;
    this.quantityUnits = new ArrayList<>();
    this.stores = new ArrayList<>();
    maxDecimalPlacesAmount = PreferenceManager.getDefaultSharedPreferences(context).getInt(
        STOCK.DECIMAL_PLACES_AMOUNT,
        SETTINGS_DEFAULT.STOCK.DECIMAL_PLACES_AMOUNT
    );
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    private final RowProductBarcodeBinding binding;

    public ViewHolder(RowProductBarcodeBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }

  @NonNull
  @Override
  public ProductBarcodeAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
      int viewType) {
    RowProductBarcodeBinding binding = RowProductBarcodeBinding.inflate(
        LayoutInflater.from(parent.getContext()),
        parent,
        false
    );
    return new ViewHolder(binding);
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public void onBindViewHolder(
      @NonNull final ProductBarcodeAdapter.ViewHolder holder,
      int position
  ) {
    ProductBarcode productBarcode = productBarcodes.get(holder.getAdapterPosition());

    holder.binding.barcode.setText(productBarcode.getBarcode());

    if (NumUtil.isStringDouble(productBarcode.getAmount())) {
      String amountStr = holder.binding.amount.getContext().getString(
          R.string.subtitle_barcode_amount,
          NumUtil.trimAmount(NumUtil.toDouble(productBarcode.getAmount()), maxDecimalPlacesAmount)
      );
      holder.binding.amount.setText(amountStr);
      holder.binding.amount.setVisibility(View.VISIBLE);
    } else {
      holder.binding.amount.setVisibility(View.GONE);
    }

    if (productBarcode.getNote() != null && !productBarcode.getNote().trim().isEmpty()) {
      holder.binding.note.setText(productBarcode.getNote());
      holder.binding.note.setVisibility(View.VISIBLE);
    } else {
      holder.binding.note.setVisibility(View.GONE);
    }

    if (productBarcode.getQuIdInt() != -1){
      for (QuantityUnit qU : quantityUnits) {
        if (qU.getId() == productBarcode.getQuIdInt()){
          String qUnitStr = holder.binding.amount.getContext().getString(
              R.string.subtitle_barcode_unit,
              qU.getName()
          );
          holder.binding.quantityUnit.setText(qUnitStr);
          holder.binding.quantityUnit.setVisibility(View.VISIBLE);
        }
      }
    } else {
      holder.binding.quantityUnit.setVisibility(View.GONE);
    }
    if (productBarcode.getStoreIdInt() != -1){
      for (Store store : stores) {
        if (store.getId() == productBarcode.getStoreIdInt()){
          //TODO create dedicated string
          String storeStr = holder.binding.store.getContext().getString(R.string.property_store)+": "+store.getName();
          holder.binding.store.setText(storeStr);
          holder.binding.store.setVisibility(View.VISIBLE);
        }
      }
    } else {
      holder.binding.store.setVisibility(View.GONE);
    }

    holder.binding.container.setOnClickListener(
        view -> listener.onItemRowClicked(productBarcode)
    );
  }

  @Override
  public int getItemCount() {
    return productBarcodes.size();
  }

  public void updateData(
      List<ProductBarcode> productBarcodesNew,
      List<QuantityUnit> quantityUnits,
      List<Store> stores,
      Runnable onListFilled
  ) {
    DiffCallback diffCallback = new DiffCallback(
        this.productBarcodes,
        productBarcodesNew,
        this.quantityUnits,
        quantityUnits,
        this.stores,
        stores
    );

    if (onListFilled != null && !productBarcodesNew.isEmpty() && productBarcodes.isEmpty()) {
      onListFilled.run();
    }

    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
    this.productBarcodes.clear();
    this.productBarcodes.addAll(productBarcodesNew);
    this.quantityUnits.clear();
    this.quantityUnits.addAll(quantityUnits);
    this.stores.clear();
    this.stores.addAll(stores);
    diffResult.dispatchUpdatesTo(this);
  }

  static class DiffCallback extends DiffUtil.Callback {

    List<ProductBarcode> oldItems;
    List<ProductBarcode> newItems;
    List<QuantityUnit> oldQuantityUnits;
    List<QuantityUnit> newQuantityUnits;
    List<Store> oldStores;
    List<Store> newStores;

    public DiffCallback(
        List<ProductBarcode> oldItems,
        List<ProductBarcode> newItems,
        List<QuantityUnit> oldQuantityUnits,
        List<QuantityUnit> newQuantityUnits,
        List<Store> oldStores,
        List<Store> newStores
    ) {
      this.newItems = newItems;
      this.oldItems = oldItems;
      this.newQuantityUnits = newQuantityUnits;
      this.oldQuantityUnits = oldQuantityUnits;
      this.newStores = newStores;
      this.oldStores = oldStores;
    }

    @Override
    public int getOldListSize() {
      return oldItems.size();
    }

    @Override
    public int getNewListSize() {
      return newItems.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
      return compare(oldItemPosition, newItemPosition, false);
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
      return compare(oldItemPosition, newItemPosition, true);
    }

    private boolean compare(int oldItemPos, int newItemPos, boolean compareContent) {
      ProductBarcode newItem = newItems.get(newItemPos);
      ProductBarcode oldItem = oldItems.get(oldItemPos);
      if (!compareContent) {
        return newItem.getId() == oldItem.getId();
      }
      if (!newQuantityUnits.equals(oldQuantityUnits) || !newStores.equals(oldStores)) {
        return false;
      }

      return newItem.equals(oldItem);
    }
  }

  public interface ProductBarcodeAdapterListener {

    void onItemRowClicked(ProductBarcode productBarcode);
  }
}
