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
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListUpdateCallback;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashMap;
import xyz.zedler.patrick.grocy.Constants.DATE;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.databinding.RowChoreEntryBinding;
import xyz.zedler.patrick.grocy.model.Chore;
import xyz.zedler.patrick.grocy.model.ChoreEntry;
import xyz.zedler.patrick.grocy.model.User;
import xyz.zedler.patrick.grocy.util.DateUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.ResUtil;

public class ChoreEntryAdapter extends
    RecyclerView.Adapter<ChoreEntryAdapter.ViewHolder> {

  private final static String TAG = ChoreEntryAdapter.class.getSimpleName();
  private final static boolean DEBUG = false;

  private Context context;
  private final LinearLayoutManager linearLayoutManager;
  private final ArrayList<ChoreEntry> choreEntries;
  private final HashMap<Integer, Chore> choreHashMap;
  private final HashMap<Integer, User> usersHashMap;
  private final ChoreEntryAdapterListener listener;
  private final DateUtil dateUtil;
  private String sortMode;
  private boolean sortAscending;

  public ChoreEntryAdapter(
      Context context,
      LinearLayoutManager linearLayoutManager,
      ChoreEntryAdapterListener listener
  ) {
    this.context = context;
    this.linearLayoutManager = linearLayoutManager;
    this.choreEntries = new ArrayList<>();
    this.choreHashMap = new HashMap<>();
    this.usersHashMap = new HashMap<>();
    this.listener = listener;
    this.dateUtil = new DateUtil(context);
  }

  @Override
  public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
    super.onDetachedFromRecyclerView(recyclerView);
    this.context = null;
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    public ViewHolder(View view) {
      super(view);
    }
  }

  public static class TaskViewHolder extends ViewHolder {

    private final RowChoreEntryBinding binding;

    public TaskViewHolder(RowChoreEntryBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new TaskViewHolder(RowChoreEntryBinding.inflate(
        LayoutInflater.from(parent.getContext()),
        parent,
        false
    ));
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int positionDoNotUse) {

    int position = viewHolder.getAdapterPosition();

    ChoreEntry choreEntry = choreEntries.get(position);
    TaskViewHolder holder = (TaskViewHolder) viewHolder;

    // NAME

    holder.binding.title.setText(choreEntry.getChoreName());

    // DUE DATE

    String date = choreEntry.getNextEstimatedExecutionTime();
    Integer days = null;
    boolean colorDays = false;
    if (date != null && !date.isEmpty() && !date.equals(DATE.NEVER_OVERDUE_WITH_TIME)) {
      days = DateUtil.getDaysFromNow(date);
    }

    if (days != null) {
      holder.binding.days.setVisibility(View.VISIBLE);
      holder.binding.daysHuman.setVisibility(View.VISIBLE);
      if (choreEntry.getTrackDateOnlyBoolean()) {
        holder.binding.days.setText(dateUtil.getLocalizedDate(date, DateUtil.FORMAT_SHORT));
      } else {
        holder.binding.days.setText(dateUtil.getLocalizedDate(date, DateUtil.FORMAT_SHORT_WITH_TIME));
      }
      holder.binding.daysHuman.setText(dateUtil.getHumanForDaysFromNow(date));
      if (days <= 5) {
        colorDays = true;
      }
    } else {
      holder.binding.days.setVisibility(View.GONE);
      holder.binding.daysHuman.setVisibility(View.GONE);
    }

    if (colorDays) {
      holder.binding.days.setTypeface(
          ResourcesCompat.getFont(context, R.font.jost_medium)
      );

      int color;
      if (days < 0) {
        color = ResUtil.getColor(context, R.attr.colorError);
      } else if (days == 0) {
        color = ResUtil.getColor(context, R.attr.colorCustomBlue);
      } else {
        color = ResUtil.getColor(context, R.attr.colorCustomYellow);
      }
      holder.binding.days.setTextColor(color);
      holder.binding.daysHuman.setTextColor(color);
    } else {
      holder.binding.days.setTypeface(
          ResourcesCompat.getFont(context, R.font.jost_book)
      );
      holder.binding.days.setTextColor(
          ResUtil.getColor(context, R.attr.colorOnSurfaceVariant)
      );
      holder.binding.daysHuman.setTextColor(
          ResUtil.getColor(context, R.attr.colorOnSurfaceVariant)
      );
    }

    // RESCHEDULED

    Chore chore = choreHashMap.get(choreEntry.getChoreId());
    if (chore != null && chore.getRescheduledDate() != null
        && !chore.getRescheduledDate().isEmpty()) {
      holder.binding.imageReschedule.setVisibility(View.VISIBLE);
    } else {
      holder.binding.imageReschedule.setVisibility(View.GONE);
    }

    // USER

    User user = NumUtil.isStringInt(choreEntry.getNextExecutionAssignedToUserId())
        ? usersHashMap.get(Integer.parseInt(choreEntry.getNextExecutionAssignedToUserId())) : null;
    if (user != null) {
      holder.binding.user.setText(user.getDisplayName());
      holder.binding.user.setVisibility(View.VISIBLE);
    } else {
      holder.binding.user.setVisibility(View.GONE);
    }

    // REASSIGNED

    if (chore != null && NumUtil.isStringInt(chore.getRescheduledNextExecutionAssignedToUserId())) {
      holder.binding.imageReassign.setVisibility(View.VISIBLE);
    } else {
      holder.binding.imageReassign.setVisibility(View.GONE);
    }

    // CONTAINER

    holder.binding.linearContainer.setOnClickListener(
        view -> listener.onItemRowClicked(choreEntry)
    );
  }

  @Override
  public int getItemCount() {
    return choreEntries.size();
  }

  public interface ChoreEntryAdapterListener {

    void onItemRowClicked(ChoreEntry choreEntry);
  }

  public ChoreEntry getEntryForPos(int position) {
    if (position < 0 || position >= choreEntries.size()) {
      return null;
    }
    return choreEntries.get(position);
  }

  public void updateData(
      ArrayList<ChoreEntry> newList,
      HashMap<Integer, Chore> choreHashMap,
      HashMap<Integer, User> usersHashMap,
      String sortMode,
      boolean sortAscending,
      Runnable onListFilled
  ) {
    ChoreEntryAdapter.DiffCallback diffCallback = new ChoreEntryAdapter.DiffCallback(
        this.choreEntries,
        newList,
        this.choreHashMap,
        choreHashMap,
        this.usersHashMap,
        usersHashMap,
        this.sortMode,
        sortMode,
        this.sortAscending,
        sortAscending
    );

    if (onListFilled != null && !newList.isEmpty() && choreEntries.isEmpty()) {
      onListFilled.run();
    }

    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
    this.choreEntries.clear();
    this.choreEntries.addAll(newList);
    this.choreHashMap.clear();
    this.choreHashMap.putAll(choreHashMap);
    this.usersHashMap.clear();
    this.usersHashMap.putAll(usersHashMap);
    this.sortMode = sortMode;
    this.sortAscending = sortAscending;
    diffResult.dispatchUpdatesTo(new AdapterListUpdateCallback(this, linearLayoutManager));
  }

  static class DiffCallback extends DiffUtil.Callback {

    ArrayList<ChoreEntry> oldItems;
    ArrayList<ChoreEntry> newItems;
    HashMap<Integer, Chore> choreHashMapOld;
    HashMap<Integer, Chore> choreHashMapNew;
    HashMap<Integer, User> usersHashMapOld;
    HashMap<Integer, User> usersHashMapNew;
    String sortModeOld;
    String sortModeNew;
    boolean sortAscendingOld;
    boolean sortAscendingNew;

    public DiffCallback(
        ArrayList<ChoreEntry> oldItems,
        ArrayList<ChoreEntry> newItems,
        HashMap<Integer, Chore> choreHashMapOld,
        HashMap<Integer, Chore> choreHashMapNew,
        HashMap<Integer, User> usersHashMapOld,
        HashMap<Integer, User> usersHashMapNew,
        String sortModeOld,
        String sortModeNew,
        boolean sortAscendingOld,
        boolean sortAscendingNew
    ) {
      this.newItems = newItems;
      this.oldItems = oldItems;
      this.choreHashMapOld = choreHashMapOld;
      this.choreHashMapNew = choreHashMapNew;
      this.usersHashMapOld = usersHashMapOld;
      this.usersHashMapNew = usersHashMapNew;
      this.sortModeOld = sortModeOld;
      this.sortModeNew = sortModeNew;
      this.sortAscendingOld = sortAscendingOld;
      this.sortAscendingNew = sortAscendingNew;
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
      ChoreEntry newItem = newItems.get(newItemPos);
      ChoreEntry oldItem = oldItems.get(oldItemPos);

      Chore choreOld = choreHashMapOld.get(oldItem.getChoreId());
      Chore choreNew = choreHashMapNew.get(newItem.getChoreId());
      if (choreOld == null && choreNew != null
          || choreOld != null && choreNew == null
          || choreOld != null && !choreOld.equals(choreNew)) {
        return false;
      }

      User userOld = NumUtil.isStringInt(oldItem.getNextExecutionAssignedToUserId())
          ? usersHashMapOld.get(Integer.parseInt(oldItem.getNextExecutionAssignedToUserId())) : null;
      User userNew = NumUtil.isStringInt(newItem.getNextExecutionAssignedToUserId())
          ? usersHashMapNew.get(Integer.parseInt(newItem.getNextExecutionAssignedToUserId())) : null;
      if (userOld == null && userNew != null
          || userOld != null && userNew == null
          || userOld != null && !userOld.equals(userNew)) {
        return false;
      }

      if (!compareContent) {
        return newItem.getId() == oldItem.getId();
      }

      return newItem.equals(oldItem);
    }
  }

  /**
   * Custom ListUpdateCallback that prevents RecyclerView from scrolling down if top item is moved.
   */
  public static final class AdapterListUpdateCallback implements ListUpdateCallback {

    @NonNull
    private final ChoreEntryAdapter mAdapter;
    private final LinearLayoutManager linearLayoutManager;

    public AdapterListUpdateCallback(
        @NonNull ChoreEntryAdapter adapter,
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
