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

package xyz.zedler.patrick.grocy.viewmodel;

import android.app.Application;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.api.GrocyApi.ENTITY;
import xyz.zedler.patrick.grocy.form.FormDataMasterProductCatOptional;
import xyz.zedler.patrick.grocy.fragment.MasterProductCatOptionalFragmentArgs;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductBarcode;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.repository.MasterProductRepository;
import xyz.zedler.patrick.grocy.util.GrocycodeUtil;
import xyz.zedler.patrick.grocy.util.GrocycodeUtil.Grocycode;
import xyz.zedler.patrick.grocy.util.PictureUtil;

public class MasterProductCatOptionalViewModel extends BaseViewModel {

  private static final String TAG = MasterProductCatOptionalViewModel.class.getSimpleName();

  private final DownloadHelper dlHelper;
  private final GrocyApi grocyApi;
  private final MasterProductRepository repository;
  private final SharedPreferences sharedPrefs;
  private final FormDataMasterProductCatOptional formData;
  private final MasterProductCatOptionalFragmentArgs args;

  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;

  private List<Product> products;
  private List<ProductGroup> productGroups;
  private List<ProductBarcode> barcodes;

  private Runnable queueEmptyAction;
  private final boolean isActionEdit;
  private String currentFilePath;

  public MasterProductCatOptionalViewModel(
      @NonNull Application application,
      @NonNull MasterProductCatOptionalFragmentArgs startupArgs
  ) {
    super(application);

    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue, getOfflineLive());
    grocyApi = new GrocyApi(application);
    repository = new MasterProductRepository(application);
    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    formData = new FormDataMasterProductCatOptional(
        application,
        sharedPrefs,
        getBeginnerModeEnabled()
    );
    args = startupArgs;
    isActionEdit = startupArgs.getAction().equals(Constants.ACTION.EDIT);
    infoFullscreenLive = new MutableLiveData<>();
  }

  public FormDataMasterProductCatOptional getFormData() {
    return formData;
  }

  public boolean isActionEdit() {
    return isActionEdit;
  }

  public Product getFilledProduct() {
    return args.getProduct() != null ? formData.fillProduct(args.getProduct()) : null;
  }

  public void loadFromDatabase(boolean downloadAfterLoading) {
    repository.loadFromDatabase(data -> {
      this.products = data.getProducts();
      this.productGroups = data.getProductGroups();
      this.barcodes = data.getBarcodes();
      formData.getProductsLive().setValue(products);
      formData.getProductGroupsLive().setValue(productGroups);
      if (downloadAfterLoading) {
        downloadData(false);
      } else {
        if (queueEmptyAction != null) {
          queueEmptyAction.run();
          queueEmptyAction = null;
        }
        formData.fillWithProductIfNecessary(args.getProduct());
      }
    }, error -> onError(error, TAG));
  }

  public void downloadData(boolean forceUpdate) {
    dlHelper.updateData(
        updated -> {
          if (updated) {
            loadFromDatabase(false);
          } else {
            if (queueEmptyAction != null) {
              queueEmptyAction.run();
              queueEmptyAction = null;
            }
            formData.fillWithProductIfNecessary(args.getProduct());
          }
        }, error -> onError(error, null),
        forceUpdate,
        false,
        Product.class,
        ProductBarcode.class,
        ProductGroup.class
    );
  }

  public void onBarcodeRecognized(String barcode) {
    Product product;
    Grocycode grocycode = GrocycodeUtil.getGrocycode(barcode);
    if (grocycode != null && grocycode.isProduct()) {
      product = Product.getProductFromId(products, grocycode.getObjectId());
      if (product == null) {
        showMessage(R.string.msg_not_found);
      } else {
        formData.getParentProductLive().setValue(product);
      }
    } else if (grocycode != null) {
      showMessage(R.string.error_wrong_grocycode_type);
    } else {
      product = Product.getProductFromBarcode(products, barcodes, barcode);
      if (product != null) {
        formData.getParentProductLive().setValue(product);
      } else {
        showMessage(getString(R.string.error_barcode_not_linked));
      }
    }
  }

  public void pasteFromClipboard() {
    if (isDemoInstance()) {
      showMessage(R.string.error_picture_uploads_forbidden);
      return;
    }
    ClipboardManager clipboard = (ClipboardManager) getApplication()
        .getSystemService(Context.CLIPBOARD_SERVICE);
    if (clipboard == null) {
      showMessage(R.string.error_clipboard_no_image);
      return;
    }
    if (!clipboard.hasPrimaryClip()) {
      showMessage(R.string.error_clipboard_no_image);
      return;
    }
    ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
    if (item.getUri() == null) {
      showMessage(R.string.error_clipboard_no_image);
      return;
    }
    try {
      InputStream imageStream = getApplication().getContentResolver().openInputStream(item.getUri());
      Bitmap bitmap = BitmapFactory.decodeStream(imageStream);
      scaleAndUploadBitmap(null, bitmap);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      showMessage(R.string.error_clipboard_no_image);
    }
  }

  public File createImageFile() throws IOException {
    File storageDir = getApplication().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
    File image = PictureUtil.createImageFile(storageDir);
    currentFilePath = image.getAbsolutePath();
    return image;
  }

  public void scaleAndUploadBitmap(@Nullable String filePath, @Nullable Bitmap image) {
    if (filePath == null && image == null) {
      showErrorMessage();
      return;
    }
    isLoadingLive.setValue(true);
    ExecutorService executor = Executors.newSingleThreadExecutor();
    executor.execute(() -> {
      Bitmap scaledBitmap = filePath != null
          ? PictureUtil.scaleBitmap(filePath)
          : PictureUtil.scaleBitmap(image);
      byte[] imageArray = PictureUtil.convertBitmapToByteArray(scaledBitmap);
      new Handler(Looper.getMainLooper()).post(() -> {
        uploadPicture(imageArray);
        executor.shutdown();
      });
    });
  }

  public void uploadPicture(byte[] pictureData) {
    if (pictureData == null) {
      showErrorMessage();
      isLoadingLive.setValue(false);
      return;
    }
    String filename = PictureUtil.createImageFilename();
    dlHelper.putFile(
        grocyApi.getProductPicture(filename),
        pictureData,
        () -> {
          deleteCurrentPicture(filename);
          formData.getPictureFilenameLive().setValue(filename);
        },
        error -> {
          isLoadingLive.setValue(false);
          showNetworkErrorMessage(error);
        }
    );
  }

  public void deleteCurrentPicture(String newFilename) {
    if (isActionEdit()) {
      JSONObject jsonObject = new JSONObject();
      try {
        jsonObject.put("picture_file_name", newFilename != null ? newFilename : "");
        dlHelper.put(
            grocyApi.getObject(ENTITY.PRODUCTS, args.getProduct().getId()),
            jsonObject,
            response -> {},
            volleyError -> {}
        );
      } catch (JSONException ignored) {}
    }
    String lastFilename = formData.getPictureFilenameLive().getValue();
    if (lastFilename != null && !lastFilename.isBlank()) {
      dlHelper.delete(
          grocyApi.getProductPicture(lastFilename),
          response -> {
            isLoadingLive.setValue(false);
            formData.getPictureFilenameLive().setValue("");
          },
          volleyError -> {
            isLoadingLive.setValue(false);
            showNetworkErrorMessage(volleyError);
            formData.getPictureFilenameLive().setValue(lastFilename);
          }
      );
    } else if (lastFilename != null && lastFilename.isBlank()) {
      isLoadingLive.setValue(false);
      formData.getPictureFilenameLive().setValue("");
    } else {
      isLoadingLive.setValue(false);
    }
  }

  public String getCurrentFilePath() {
    return currentFilePath;
  }

  public void setCurrentFilePath(String currentFilePath) {
    this.currentFilePath = currentFilePath;
  }

  public String getEnergyUnit() {
    return sharedPrefs.getString(PREF.ENERGY_UNIT, "kcal");
  }

  @NonNull
  public MutableLiveData<Boolean> getIsLoadingLive() {
    return isLoadingLive;
  }

  @NonNull
  public MutableLiveData<InfoFullscreen> getInfoFullscreenLive() {
    return infoFullscreenLive;
  }

  public void setQueueEmptyAction(Runnable queueEmptyAction) {
    this.queueEmptyAction = queueEmptyAction;
  }

  @Override
  protected void onCleared() {
    dlHelper.destroy();
    super.onCleared();
  }

  public static class MasterProductCatOptionalViewModelFactory implements
      ViewModelProvider.Factory {

    private final Application application;
    private final MasterProductCatOptionalFragmentArgs args;

    public MasterProductCatOptionalViewModelFactory(
        Application application,
        MasterProductCatOptionalFragmentArgs args
    ) {
      this.application = application;
      this.args = args;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return (T) new MasterProductCatOptionalViewModel(application, args);
    }
  }
}
