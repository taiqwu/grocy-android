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

package xyz.zedler.patrick.grocy.model;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.database.Converters;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnErrorListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnMultiTypeErrorListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnObjectsResponseListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnStringResponseListener;
import xyz.zedler.patrick.grocy.web.NetworkQueue.QueueItem;

@Entity(tableName = "product_group_table")
public class ProductGroup extends GroupedListItem implements Parcelable {

  @PrimaryKey
  @ColumnInfo(name = "id")
  @SerializedName("id")
  private int id;

  @ColumnInfo(name = "name")
  @SerializedName("name")
  private String name;

  @ColumnInfo(name = "description")
  @SerializedName("description")
  private String description;

  @ColumnInfo(name = "userfields")
  @SerializedName("userfields")
  private Map<String, String> userfields;

  @ColumnInfo(name = "row_created_timestamp")
  @SerializedName("row_created_timestamp")
  private String rowCreatedTimestamp;

  /**
   * First element in bottomSheet selection: NONE (id = null)
   */
  @Ignore
  public ProductGroup(int id, String name) {
    this.id = id;
    this.name = name;
  }

  public ProductGroup() {
  }  // for Room

  public ProductGroup(Parcel parcel) {
    id = parcel.readInt();
    name = parcel.readString();
    description = parcel.readString();
    userfields = Converters.stringToMap(parcel.readString());
    rowCreatedTimestamp = parcel.readString();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(id);
    dest.writeString(name);
    dest.writeString(description);
    dest.writeString(Converters.mapToString(userfields));
    dest.writeString(rowCreatedTimestamp);
  }

  public static final Creator<ProductGroup> CREATOR = new Creator<>() {

    @Override
    public ProductGroup createFromParcel(Parcel in) {
      return new ProductGroup(in);
    }

    @Override
    public ProductGroup[] newArray(int size) {
      return new ProductGroup[size];
    }
  };

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Map<String, String> getUserfields() {
    return userfields;
  }

  public void setUserfields(Map<String, String> userfields) {
    this.userfields = userfields;
  }

  public String getRowCreatedTimestamp() {
    return rowCreatedTimestamp;
  }

  public void setRowCreatedTimestamp(String rowCreatedTimestamp) {
    this.rowCreatedTimestamp = rowCreatedTimestamp;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProductGroup that = (ProductGroup) o;
    return id == that.id &&
        Objects.equals(name, that.name) &&
        Objects.equals(description, that.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, description);
  }

  public static ProductGroup getFromId(List<ProductGroup> productGroups, int id) {
    for (ProductGroup productGroup : productGroups) {
      if (productGroup.getId() == id) {
        return productGroup;
      }
    }
    return null;
  }

  @NonNull
  @Override
  public String toString() {
    return "ProductGroup(" + name + ')';
  }

  public static QueueItem getProductGroups(
      DownloadHelper dlHelper,
      OnObjectsResponseListener<ProductGroup> onResponseListener,
      OnErrorListener onErrorListener
  ) {
    return new QueueItem() {
      @Override
      public void perform(
          @Nullable OnStringResponseListener responseListener,
          @Nullable OnMultiTypeErrorListener errorListener,
          @Nullable String uuid
      ) {
        dlHelper.get(
            dlHelper.grocyApi.getObjects(GrocyApi.ENTITY.PRODUCT_GROUPS),
            uuid,
            response -> {
              Type type = new TypeToken<List<ProductGroup>>() {
              }.getType();
              ArrayList<ProductGroup> productGroups = dlHelper.gson.fromJson(response, type);
              if (dlHelper.debug) {
                Log.i(dlHelper.tag, "download ProductGroups: " + productGroups);
              }
              if (onResponseListener != null) {
                onResponseListener.onResponse(productGroups);
              }
              if (responseListener != null) {
                responseListener.onResponse(response);
              }
            },
            error -> {
              if (onErrorListener != null) {
                onErrorListener.onError(error);
              }
              if (errorListener != null) {
                errorListener.onError(error);
              }
            }
        );
      }
    };
  }

  @SuppressLint("CheckResult")
  public static QueueItem updateProductGroups(
      DownloadHelper dlHelper,
      String dbChangedTime,
      boolean forceUpdate,
      OnObjectsResponseListener<ProductGroup> onResponseListener
  ) {
    String lastTime = !forceUpdate ? dlHelper.sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_PRODUCT_GROUPS, null
    ) : null;
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return new QueueItem() {
        @Override
        public void perform(
            @Nullable OnStringResponseListener responseListener,
            @Nullable OnMultiTypeErrorListener errorListener,
            @Nullable String uuid
        ) {
          dlHelper.get(
              dlHelper.grocyApi.getObjects(GrocyApi.ENTITY.PRODUCT_GROUPS),
              uuid,
              response -> {
                Type type = new TypeToken<List<ProductGroup>>() {
                }.getType();
                ArrayList<ProductGroup> productGroups = dlHelper.gson.fromJson(response, type);
                if (dlHelper.debug) {
                  Log.i(dlHelper.tag, "download ProductGroups: " + productGroups);
                }
                Single.fromCallable(() -> {
                      dlHelper.appDatabase.productGroupDao()
                          .deleteProductGroups().blockingSubscribe();
                      dlHelper.appDatabase.productGroupDao()
                          .insertProductGroups(productGroups).blockingSubscribe();
                      dlHelper.sharedPrefs.edit()
                          .putString(PREF.DB_LAST_TIME_PRODUCT_GROUPS, dbChangedTime).apply();
                      return true;
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      if (onResponseListener != null) {
                        onResponseListener.onResponse(productGroups);
                      }
                      if (responseListener != null) {
                        responseListener.onResponse(response);
                      }
                    })
                    .subscribe(ignored -> {}, throwable -> {
                      if (errorListener != null) {
                        errorListener.onError(throwable);
                      }
                    });
              },
              error -> {
                if (errorListener != null) {
                  errorListener.onError(error);
                }
              }
          );
        }
      };
    } else {
      if (dlHelper.debug) {
        Log.i(dlHelper.tag, "downloadData: skipped ProductGroups download");
      }
      return null;
    }
  }
}
