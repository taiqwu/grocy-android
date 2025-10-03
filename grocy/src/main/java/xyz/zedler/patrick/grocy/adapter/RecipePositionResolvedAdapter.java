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
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListUpdateCallback;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.databinding.RowRecipePositionEntryBinding;
import xyz.zedler.patrick.grocy.databinding.RowRecipePositionGroupBinding;
import xyz.zedler.patrick.grocy.model.GroupHeader;
import xyz.zedler.patrick.grocy.model.GroupedListItem;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversion;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversionResolved;
import xyz.zedler.patrick.grocy.model.Recipe;
import xyz.zedler.patrick.grocy.model.RecipePositionResolved;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PluralUtil;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;
import xyz.zedler.patrick.grocy.viewmodel.RecipeViewModel;

public class RecipePositionResolvedAdapter extends
    RecyclerView.Adapter<RecipePositionResolvedAdapter.ViewHolder> {

  private final static String TAG = RecipePositionResolvedAdapter.class.getSimpleName();
  private final static boolean DEBUG = false;

  private Context context;
  private final LinearLayoutManager linearLayoutManager;
  private Recipe recipe;
  private final List<GroupedListItem> groupedListItems;
  private final List<Product> products;
  private final List<QuantityUnit> quantityUnits;
  private final List<QuantityUnitConversionResolved> quantityUnitConversions;
  private final List<String> activeFields;
  private final RecipePositionsItemAdapterListener listener;

  private final PluralUtil pluralUtil;
  private final int maxDecimalPlacesAmount;
  private final int maxDecimalPlacesPrice;
  private final int colorGreen, colorYellow, colorRed;
  private final String energyUnit;
  private final String currency;

  public RecipePositionResolvedAdapter(
      Context context,
      LinearLayoutManager linearLayoutManager,
      Recipe recipe,
      List<RecipePositionResolved> recipePositions,
      List<Product> products,
      List<QuantityUnit> quantityUnits,
      List<QuantityUnitConversionResolved> quantityUnitConversions,
      List<String> activeFields,
      RecipePositionsItemAdapterListener listener
  ) {
    this.context = context;
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    maxDecimalPlacesAmount = sharedPrefs.getInt(
        STOCK.DECIMAL_PLACES_AMOUNT,
        SETTINGS_DEFAULT.STOCK.DECIMAL_PLACES_AMOUNT
    );
    maxDecimalPlacesPrice = sharedPrefs.getInt(
        STOCK.DECIMAL_PLACES_PRICES_DISPLAY,
        SETTINGS_DEFAULT.STOCK.DECIMAL_PLACES_PRICES_DISPLAY
    );
    energyUnit = sharedPrefs.getString(PREF.ENERGY_UNIT, PREF.ENERGY_UNIT_DEFAULT);
    currency = sharedPrefs.getString(PREF.CURRENCY, "");
    this.linearLayoutManager = linearLayoutManager;
    this.recipe = recipe;
    this.groupedListItems = getGroupedListItems(context, recipePositions);
    this.products = new ArrayList<>(products);
    this.quantityUnits = new ArrayList<>(quantityUnits);
    this.quantityUnitConversions = new ArrayList<>(quantityUnitConversions);
    this.activeFields = new ArrayList<>(activeFields);
    this.listener = listener;
    this.pluralUtil = new PluralUtil(context);

    colorGreen = ResUtil.getColor(context, R.attr.colorCustomGreen);
    colorYellow = ResUtil.getColor(context, R.attr.colorCustomYellow);
    colorRed = ResUtil.getColor(context, R.attr.colorError);
  }

  @Override
  public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
    super.onDetachedFromRecyclerView(recyclerView);
    this.context = null;
  }

  static ArrayList<GroupedListItem> getGroupedListItems(
      Context context,
      List<RecipePositionResolved> recipePositions
  ) {
    HashMap<String, ArrayList<RecipePositionResolved>> recipePositionsGroupedHashMap
        = new HashMap<>();
    ArrayList<RecipePositionResolved> ungroupedItems = new ArrayList<>();
    for (RecipePositionResolved recipePosition : recipePositions) {
      String groupName = recipePosition.getIngredientGroup();
      if (groupName != null && !groupName.isEmpty()) {
        ArrayList<RecipePositionResolved> itemsFromGroup
            = recipePositionsGroupedHashMap.get(groupName);
        if (itemsFromGroup == null) {
          itemsFromGroup = new ArrayList<>();
          recipePositionsGroupedHashMap.put(groupName, itemsFromGroup);
        }
        itemsFromGroup.add(recipePosition);
      } else {
        ungroupedItems.add(recipePosition);
      }
    }
    ArrayList<GroupedListItem> groupedListItems = new ArrayList<>();
    ArrayList<String> groupsSorted = new ArrayList<>(recipePositionsGroupedHashMap.keySet());
    SortUtil.sortStringsByName(groupsSorted, true);
    for (String group : groupsSorted) {
      ArrayList<RecipePositionResolved> itemsFromGroup = recipePositionsGroupedHashMap.get(group);
      if (itemsFromGroup == null) continue;
      GroupHeader groupHeader = new GroupHeader(group);
      groupHeader.setDisplayDivider(!ungroupedItems.isEmpty() || !groupsSorted.get(0).equals(group));
      groupedListItems.add(groupHeader);
      groupedListItems.addAll(itemsFromGroup);
    }
    if (!ungroupedItems.isEmpty()) {
      if (!groupsSorted.isEmpty()) {
        GroupHeader groupHeader = new GroupHeader(context.getString(R.string.property_ungrouped));
        groupHeader.setDisplayDivider(true);
        groupedListItems.add(groupHeader);
      }
      groupedListItems.addAll(ungroupedItems);
    }
    return groupedListItems;
  }

  public List<Product> getMissingProducts() {
    ArrayList<Product> missingProducts = new ArrayList<>();
    for (GroupedListItem groupedListItem : groupedListItems) {
      if (!(groupedListItem instanceof RecipePositionResolved)) continue;
      RecipePositionResolved recipePosition = (RecipePositionResolved) groupedListItem;
      Product product = Product.getProductFromId(products, recipePosition.getProductId());
      if (!recipePosition.getNeedFulfilledBoolean()
          && !recipePosition.getNeedFulfilledWithShoppingListBoolean()) {
        missingProducts.add(product);
      }
    }
    return missingProducts;
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    public ViewHolder(View view) {
      super(view);
    }
  }

  public static class RecipePositionViewHolder extends ViewHolder {

    private final RowRecipePositionEntryBinding binding;

    public RecipePositionViewHolder(RowRecipePositionEntryBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }

  public static class IngredientGroupViewHolder extends ViewHolder {

    private final RowRecipePositionGroupBinding binding;

    public IngredientGroupViewHolder(RowRecipePositionGroupBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }

  @Override
  public int getItemViewType(int position) {
    return GroupedListItem.getType(
        groupedListItems.get(position),
        GroupedListItem.CONTEXT_RECIPE_POSITIONS
    );
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    if (viewType == GroupedListItem.TYPE_HEADER) {
      return new IngredientGroupViewHolder(
          RowRecipePositionGroupBinding.inflate(
              LayoutInflater.from(parent.getContext()),
              parent,
              false
          )
      );
    } else {
      return new RecipePositionViewHolder(RowRecipePositionEntryBinding.inflate(
          LayoutInflater.from(parent.getContext()),
          parent,
          false
      ));
    }
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int positionDoNotUse) {

    GroupedListItem groupedListItem = groupedListItems.get(viewHolder.getAdapterPosition());

    int type = getItemViewType(viewHolder.getAdapterPosition());
    if (type == GroupedListItem.TYPE_HEADER) {
      IngredientGroupViewHolder holder = (IngredientGroupViewHolder) viewHolder;
      if (((GroupHeader) groupedListItem).getGroupName() != null) {
        holder.binding.name.setText(((GroupHeader) groupedListItem).getGroupName());
        holder.binding.name.setVisibility(View.VISIBLE);
      } else {
        holder.binding.name.setVisibility(View.GONE);
      }
      return;
    }

    int position = viewHolder.getAdapterPosition();
    RecipePositionViewHolder holder = (RecipePositionViewHolder) viewHolder;

    RecipePositionResolved recipePosition = (RecipePositionResolved) groupedListItem;
    Product product = Product.getProductFromId(products, recipePosition.getProductId());
    QuantityUnit quantityUnit = QuantityUnit.getFromId(
        quantityUnits, recipePosition.getQuId()
    );
    QuantityUnitConversion conversion = product != null
        ? QuantityUnitConversionResolved.findConversion(
            quantityUnitConversions,
            product.getId(),
            product.getQuIdStockInt(),
            recipePosition.getQuId()
        ) : null;

    // AMOUNT
    double amountRecipeUnit = recipePosition.getRecipeAmount(); // stock unit
    if (conversion != null && !recipePosition.isOnlyCheckSingleUnitInStock()) {
      amountRecipeUnit *= conversion.getFactor();
    }
    String amountString;
    if (recipePosition.getRecipeVariableAmount() == null
        || recipePosition.getRecipeVariableAmount().isEmpty()) {
      amountString = NumUtil.trimAmount(amountRecipeUnit, maxDecimalPlacesAmount);
      holder.binding.variableAmount.setVisibility(View.GONE);
    } else {
      amountString = recipePosition.getRecipeVariableAmount();
      holder.binding.variableAmount.setVisibility(View.VISIBLE);
    }

    if (product != null) {
      holder.binding.ingredient.setText(context.getString(
          R.string.title_ingredient_with_amount,
          amountString,
          pluralUtil.getQuantityUnitPlural(quantityUnit, amountRecipeUnit),
          product.getName()
      ));
    } else {
      holder.binding.ingredient.setText(R.string.error_undefined);
    }

    // FULFILLMENT
    if (recipePosition.isNotCheckStockFulfillment() || !activeFields.contains(
        RecipeViewModel.FIELD_FULFILLMENT)) {
      holder.binding.fulfillment.setVisibility(View.GONE);
    } else {
      holder.binding.fulfillment.setVisibility(View.VISIBLE);

      if (recipePosition.getNeedFulfilledBoolean()) {
        double stockAmount = conversion != null
            ? recipePosition.getStockAmount() * conversion.getFactor()
            : recipePosition.getStockAmount();
        holder.binding.fulfilled.setText(
            context.getString(
                R.string.msg_recipes_enough_in_stock_amount,
                context.getString(
                    R.string.subtitle_amount,
                    NumUtil.trimAmount(stockAmount, maxDecimalPlacesAmount),
                    pluralUtil.getQuantityUnitPlural(quantityUnit, stockAmount)
                )
            )
        );

        holder.binding.imageFulfillment.setImageDrawable(ResourcesCompat.getDrawable(
            context.getResources(),
            R.drawable.ic_round_check_circle_outline,
            null
        ));
        holder.binding.imageFulfillment.setImageTintList(ColorStateList.valueOf(colorGreen));
        holder.binding.missing.setVisibility(View.GONE);
      } else {
        double amountMissing = conversion != null
            ? recipePosition.getMissingAmount() * conversion.getFactor()
            : recipePosition.getMissingAmount();
        double amountShoppingList = conversion != null
            ? recipePosition.getAmountOnShoppingList() * conversion.getFactor()
            : recipePosition.getAmountOnShoppingList();

        holder.binding.fulfilled.setText(R.string.msg_recipes_not_enough);
        holder.binding.imageFulfillment.setImageDrawable(ResourcesCompat.getDrawable(
            context.getResources(),
            amountShoppingList >= amountMissing
                ? R.drawable.ic_round_error_outline
                : R.drawable.ic_round_highlight_off,
            null
        ));
        holder.binding.imageFulfillment.setImageTintList(
            ColorStateList.valueOf(amountShoppingList >= amountMissing ? colorYellow : colorRed)
        );
        holder.binding.missing.setText(
            context.getString(
                R.string.msg_recipes_ingredient_fulfillment_info_list,
                NumUtil.trimAmount(amountMissing, maxDecimalPlacesAmount),
                NumUtil.trimAmount(amountShoppingList, maxDecimalPlacesAmount)
            )
        );
        holder.binding.missing.setVisibility(View.VISIBLE);
      }
    }
    holder.binding.fulfilled.requestLayout();

    // NOTE
    if (recipePosition.getNote() == null || recipePosition.getNote().trim().isEmpty()
        || !activeFields.contains(RecipeViewModel.FIELD_NOTE)) {
      holder.binding.note.setVisibility(View.GONE);
    } else {
      holder.binding.note.setText(recipePosition.getNote());
      holder.binding.note.setVisibility(View.VISIBLE);
    }

    // CALORIES & PRICE
    holder.binding.flexboxLayout.removeAllViews();
    Chip chipCalories = createChip(
        context,
        NumUtil.trimAmount(
            recipePosition.getCalories(), maxDecimalPlacesAmount
        ) + " " + energyUnit
    );
    if (activeFields.contains(RecipeViewModel.FIELD_ENERGY)) {
      holder.binding.flexboxLayout.addView(chipCalories);
    }
    Chip chipPrice = createChip(
        context, context.getString(
            R.string.property_price_with_currency,
            NumUtil.trimPrice(recipePosition.getCosts(), maxDecimalPlacesPrice),
            currency
        )
    );
    if (activeFields.contains(RecipeViewModel.FIELD_PRICE)) {
      holder.binding.flexboxLayout.addView(chipPrice);
    }
    holder.binding.flexboxLayout.setVisibility(
        holder.binding.flexboxLayout.getChildCount() > 0 ? View.VISIBLE : View.GONE
    );

    if (recipePosition.isChecked()) {
      holder.binding.linearRecipePositionContainer.setAlpha(0.5f);
      holder.binding.ingredient.setPaintFlags(
          holder.binding.ingredient.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
      );
    } else {
      holder.binding.linearRecipePositionContainer.setAlpha(1f);
      holder.binding.ingredient.setPaintFlags(
          holder.binding.ingredient.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG
      );
    }

    // CONTAINER
    holder.binding.linearRecipePositionContainer.setBackground(
        ViewUtil.getRippleBgListItemSurface(context)
    );
    holder.binding.linearRecipePositionContainer.setOnClickListener(
        view -> listener.onItemRowClicked(recipePosition, position)
    );
  }

  private static Chip createChip(Context context, String text) {
    @SuppressLint("InflateParams")
    Chip chip = (Chip) LayoutInflater.from(context).inflate(
        R.layout.view_info_chip, null, false
    );
    chip.setText(text);
    chip.setTextColor(ColorStateList.valueOf(ResUtil.getColor(context, R.attr.colorOnSurface)));
    chip.setEnabled(false);
    return chip;
  }

  @Override
  public int getItemCount() {
    return groupedListItems.size();
  }

  public interface RecipePositionsItemAdapterListener {

    void onItemRowClicked(RecipePositionResolved recipePosition, int position);
  }

  public void updateData(
      Recipe recipe,
      List<RecipePositionResolved> newList,
      List<Product> newProducts,
      List<QuantityUnit> newQuantityUnits,
      List<QuantityUnitConversionResolved> newQuantityUnitConversions,
      List<String> newActiveFields
  ) {
    List<GroupedListItem> groupedListItemsNew = getGroupedListItems(
        context, (ArrayList<RecipePositionResolved>) newList
    );
    RecipePositionResolvedAdapter.DiffCallback diffCallback = new RecipePositionResolvedAdapter.DiffCallback(
        this.recipe,
        recipe,
        this.groupedListItems,
        groupedListItemsNew,
        this.products,
        newProducts,
        this.quantityUnits,
        newQuantityUnits,
        this.quantityUnitConversions,
        newQuantityUnitConversions,
        this.activeFields,
        newActiveFields
    );
    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
    this.recipe = recipe;
    this.groupedListItems.clear();
    this.groupedListItems.addAll(groupedListItemsNew);
    this.products.clear();
    this.products.addAll(newProducts);
    this.quantityUnits.clear();
    this.quantityUnits.addAll(newQuantityUnits);
    this.quantityUnitConversions.clear();
    this.quantityUnitConversions.addAll(newQuantityUnitConversions);
    this.activeFields.clear();
    this.activeFields.addAll(newActiveFields);
    diffResult.dispatchUpdatesTo(new AdapterListUpdateCallback(this, linearLayoutManager));
  }

  static class DiffCallback extends DiffUtil.Callback {

    Recipe oldRecipe;
    Recipe newRecipe;
    List<GroupedListItem> oldItems;
    List<GroupedListItem> newItems;
    List<Product> oldProducts;
    List<Product> newProducts;
    List<QuantityUnit> oldQuantityUnits;
    List<QuantityUnit> newQuantityUnits;
    List<QuantityUnitConversionResolved> oldQuantityUnitConversions;
    List<QuantityUnitConversionResolved> newQuantityUnitConversions;
    List<String> oldActiveFields;
    List<String> newActiveFields;

    public DiffCallback(
        Recipe oldRecipe,
        Recipe newRecipe,
        List<GroupedListItem> oldItems,
        List<GroupedListItem> newItems,
        List<Product> oldProducts,
        List<Product> newProducts,
        List<QuantityUnit> oldQuantityUnits,
        List<QuantityUnit> newQuantityUnits,
        List<QuantityUnitConversionResolved> oldQuantityUnitConversions,
        List<QuantityUnitConversionResolved> newQuantityUnitConversions,
        List<String> oldActiveFields,
        List<String> newActiveFields
    ) {
      this.oldRecipe = oldRecipe;
      this.newRecipe = newRecipe;
      this.oldItems = oldItems;
      this.newItems = newItems;
      this.oldProducts = oldProducts;
      this.newProducts = newProducts;
      this.oldQuantityUnits = oldQuantityUnits;
      this.newQuantityUnits = newQuantityUnits;
      this.oldQuantityUnitConversions = oldQuantityUnitConversions;
      this.newQuantityUnitConversions = newQuantityUnitConversions;
      this.oldActiveFields = oldActiveFields;
      this.newActiveFields = newActiveFields;
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
      if (!Objects.equals(newRecipe.getDesiredServings(), oldRecipe.getDesiredServings())) {
        return false;
      }
      int oldItemType = GroupedListItem.getType(
          oldItems.get(oldItemPos),
          GroupedListItem.CONTEXT_RECIPE_POSITIONS
      );
      int newItemType = GroupedListItem.getType(
          newItems.get(newItemPos),
          GroupedListItem.CONTEXT_RECIPE_POSITIONS
      );
      if (oldItemType != newItemType) {
        return false;
      }
      if (oldItemType == GroupedListItem.TYPE_ENTRY) {
        RecipePositionResolved newItem = (RecipePositionResolved) newItems.get(newItemPos);
        RecipePositionResolved oldItem = (RecipePositionResolved) oldItems.get(oldItemPos);
        Product newItemProduct = Product.getProductFromId(newProducts, newItem.getProductId());
        Product oldItemProduct = Product.getProductFromId(oldProducts, oldItem.getProductId());
        QuantityUnit newQuantityUnit = QuantityUnit.getFromId(
            newQuantityUnits, newItem.getQuId()
        );
        QuantityUnit oldQuantityUnit = QuantityUnit.getFromId(
            oldQuantityUnits, oldItem.getQuId()
        );
        QuantityUnitConversion newQuantityUnitConversion = newItemProduct != null
            ? QuantityUnitConversionResolved.findConversion(
            newQuantityUnitConversions,
            newItemProduct.getId(),
            newItemProduct.getQuIdStockInt(),
            newItem.getQuId()
        ) : null;
        QuantityUnitConversion oldQuantityUnitConversion = oldItemProduct != null
            ? QuantityUnitConversionResolved.findConversion(
            oldQuantityUnitConversions,
            oldItemProduct.getId(),
            oldItemProduct.getQuIdStockInt(),
            oldItem.getQuId()
        ) : null;
        if (!compareContent) {
          return newItem.getId() == oldItem.getId();
        }
        if (!oldActiveFields.equals(newActiveFields)) {
          return false;
        }
        if (newItemProduct == null || !newItemProduct.equals(oldItemProduct)) {
          return false;
        }
        if (newQuantityUnit == null || !newQuantityUnit.equals(oldQuantityUnit)) {
          return false;
        }
        if (newQuantityUnitConversion == null
            || !newQuantityUnitConversion.equals(oldQuantityUnitConversion)) {
          return false;
        }
        return newItem.equals(oldItem);
      } else { // Type: header
        GroupHeader newGroup = (GroupHeader) newItems.get(newItemPos);
        GroupHeader oldGroup = (GroupHeader) oldItems.get(oldItemPos);
        return newGroup.equals(oldGroup);
      }
    }
  }

  /**
   * Custom ListUpdateCallback that prevents RecyclerView from scrolling down if top item is moved.
   */
  public static final class AdapterListUpdateCallback implements ListUpdateCallback {

    @NonNull
    private final RecipePositionResolvedAdapter mAdapter;
    private final LinearLayoutManager linearLayoutManager;

    public AdapterListUpdateCallback(
        @NonNull RecipePositionResolvedAdapter adapter,
        LinearLayoutManager linearLayoutManager
    ) {
      this.mAdapter = adapter;
      this.linearLayoutManager = linearLayoutManager;
    }

    @Override
    public void onInserted(int position, int count) {
      mAdapter.notifyItemRangeInserted(position, count);
    }

    @Override
    public void onRemoved(int position, int count) {
      mAdapter.notifyItemRangeRemoved(position, count);
    }

    @Override
    public void onMoved(int fromPosition, int toPosition) {
      // workaround for https://github.com/patzly/grocy-android/issues/439
      // figure out the position of the first visible item
      int firstPos = linearLayoutManager.findFirstCompletelyVisibleItemPosition();
      int offsetTop = 0;
      if(firstPos >= 0) {
        View firstView = linearLayoutManager.findViewByPosition(firstPos);
        if (firstView != null) {
          offsetTop = linearLayoutManager.getDecoratedTop(firstView)
              - linearLayoutManager.getTopDecorationHeight(firstView);
        }
      }

      mAdapter.notifyItemMoved(fromPosition, toPosition);

      // reapply the saved position
      if(firstPos >= 0) {
        linearLayoutManager.scrollToPositionWithOffset(firstPos, offsetTop);
      }
    }

    @Override
    public void onChanged(int position, int count, Object payload) {
      mAdapter.notifyItemRangeChanged(position, count, payload);
    }
  }
}
