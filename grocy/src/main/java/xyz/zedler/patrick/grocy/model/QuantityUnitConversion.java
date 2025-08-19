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
import java.util.Objects;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnMultiTypeErrorListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnObjectsResponseListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnStringResponseListener;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.web.NetworkQueue.QueueItem;

@Entity(tableName = "quantity_unit_conversion_table")
public class QuantityUnitConversion implements Parcelable {

  @PrimaryKey
  @ColumnInfo(name = "id")
  @SerializedName("id")
  private int id;

  @ColumnInfo(name = "from_qu_id")
  @SerializedName("from_qu_id")
  private int fromQuId;

  @ColumnInfo(name = "to_qu_id")
  @SerializedName("to_qu_id")
  private int toQuId;

  @ColumnInfo(name = "factor")
  @SerializedName("factor")
  private double factor;

  @ColumnInfo(name = "product_id")
  @SerializedName("product_id")
  private String productId;

  @ColumnInfo(name = "row_created_timestamp")
  @SerializedName("row_created_timestamp")
  private String rowCreatedTimestamp;

  public QuantityUnitConversion() {
  }

  @Ignore
  public QuantityUnitConversion(Parcel parcel) {
    id = parcel.readInt();
    fromQuId = parcel.readInt();
    toQuId = parcel.readInt();
    factor = parcel.readDouble();
    productId = parcel.readString();
    rowCreatedTimestamp = parcel.readString();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(id);
    dest.writeInt(fromQuId);
    dest.writeInt(toQuId);
    dest.writeDouble(factor);
    dest.writeString(productId);
    dest.writeString(rowCreatedTimestamp);
  }

  public static final Creator<QuantityUnitConversion> CREATOR = new Creator<>() {

    @Override
    public QuantityUnitConversion createFromParcel(Parcel in) {
      return new QuantityUnitConversion(in);
    }

    @Override
    public QuantityUnitConversion[] newArray(int size) {
      return new QuantityUnitConversion[size];
    }
  };

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public int getFromQuId() {
    return fromQuId;
  }

  public void setFromQuId(int fromQuId) {
    this.fromQuId = fromQuId;
  }

  public int getToQuId() {
    return toQuId;
  }

  public void setToQuId(int toQuId) {
    this.toQuId = toQuId;
  }

  public double getFactor() {
    return factor;
  }

  public void setFactor(double factor) {
    this.factor = factor;
  }

  public String getProductId() {
    return productId;
  }

  public int getProductIdInt() {
    return NumUtil.isStringInt(productId) ? Integer.parseInt(productId) : -1;
  }

  public void setProductId(String productId) {
    this.productId = productId;
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
    QuantityUnitConversion that = (QuantityUnitConversion) o;
    return id == that.id &&
        fromQuId == that.fromQuId &&
        toQuId == that.toQuId &&
        Double.compare(that.factor, factor) == 0 &&
        Objects.equals(productId, that.productId) &&
        Objects.equals(rowCreatedTimestamp, that.rowCreatedTimestamp);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, fromQuId, toQuId, factor, productId, rowCreatedTimestamp);
  }

  public static List<QuantityUnitConversion> getFromTargetUnit(List<QuantityUnitConversion> quantityUnitConversions, int toQuId) {
    if (quantityUnitConversions == null) return null;
    ArrayList<QuantityUnitConversion> result = new ArrayList<>();
    for (QuantityUnitConversion quantityUnitConversion : quantityUnitConversions) {
      if (quantityUnitConversion.getToQuId() == toQuId) {
        result.add(quantityUnitConversion);
      }
    }
    return result;
  }

  public static QuantityUnitConversion getFromTwoUnits(
      List<QuantityUnitConversion> quantityUnitConversions,
      int fromQuId,
      int toQuId,
      int productId
  ) {
    if (quantityUnitConversions == null) return null;
    QuantityUnitConversion tempConversion = null;
    for (QuantityUnitConversion quantityUnitConversion : quantityUnitConversions) {
      if (quantityUnitConversion.getFromQuId() == fromQuId && quantityUnitConversion.getToQuId() == toQuId) {
        if (NumUtil.isStringInt(quantityUnitConversion.getProductId()) && quantityUnitConversion.getProductIdInt() == productId) {
          return quantityUnitConversion;
        }
        if (!NumUtil.isStringInt(quantityUnitConversion.getProductId())) {
          tempConversion = quantityUnitConversion;
        }
      }
    }
    return tempConversion;
  }

  public static ArrayList<QuantityUnitConversion> getQuantityUnitConversionsForRecipePositions(
      List<QuantityUnitConversion> quantityUnitConversions,
      List<RecipePosition> recipePositions
  ) {
    ArrayList<QuantityUnitConversion> result = new ArrayList<>();
    for (RecipePosition recipePosition : recipePositions) {
      List<QuantityUnitConversion> conversionsPosition = getFromTargetUnit(quantityUnitConversions, recipePosition.getQuantityUnitId());
      result.addAll(conversionsPosition);
    }
    return result;
  }

  public static JSONObject getJsonFromConversion(QuantityUnitConversion conversion, boolean debug,
      String TAG) {
    JSONObject json = new JSONObject();
    try {
      json.put("product_id", conversion.getProductIdInt());
      json.put("from_qu_id", conversion.getFromQuId());
      json.put("to_qu_id", conversion.getToQuId());
      json.put("factor", conversion.getFactor());
    } catch (JSONException e) {
      if (debug) {
        Log.e(TAG, "getJsonFromProductBarcode: " + e);
      }
    }
    return json;
  }

  public JSONObject getJsonFromConversion(boolean debug, String TAG) {
    return getJsonFromConversion(this, debug, TAG);
  }

  @NonNull
  @Override
  public String toString() {
    return "QuantityUnitConversion(" + productId + ", " + fromQuId + ", " + toQuId + ", " + factor + ")";
  }

  @SuppressLint("CheckResult")
  public static QueueItem updateQuantityUnitConversions(
      DownloadHelper dlHelper,
      String dbChangedTime,
      boolean forceUpdate,
      OnObjectsResponseListener<QuantityUnitConversion> onResponseListener
  ) {
    String lastTime = !forceUpdate ? dlHelper.sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_QUANTITY_UNIT_CONVERSIONS, null
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
              dlHelper.grocyApi.getObjects(GrocyApi.ENTITY.QUANTITY_UNIT_CONVERSIONS),
              uuid,
              response -> {
                Type type = new TypeToken<List<QuantityUnitConversion>>() {
                }.getType();
                List<QuantityUnitConversion> conversions
                    = dlHelper.gson.fromJson(response, type);
                if (dlHelper.debug) {
                  Log.i(dlHelper.tag, "download QuantityUnitConversions: "
                      + conversions);
                }
                Single.fromCallable(() -> {
                  dlHelper.appDatabase.quantityUnitConversionDao()
                      .deleteConversions().blockingSubscribe();
                  dlHelper.appDatabase.quantityUnitConversionDao()
                      .insertConversions(conversions).blockingSubscribe();
                  dlHelper.sharedPrefs.edit()
                      .putString(PREF.DB_LAST_TIME_QUANTITY_UNIT_CONVERSIONS, dbChangedTime).apply();
                  return true;
                })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      if (onResponseListener != null) {
                        onResponseListener.onResponse(conversions);
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
        Log.i(dlHelper.tag, "downloadData: skipped QuantityUnitConversions download");
      }
      return null;
    }
  }
}
