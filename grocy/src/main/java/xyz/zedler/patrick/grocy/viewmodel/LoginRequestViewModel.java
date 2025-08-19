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
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import com.android.volley.AuthFailureError;
import com.android.volley.NoConnectionError;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import dev.gustavoavila.websocketclient.WebSocketClient;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.fragment.LoginRequestFragmentArgs;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.CompatibilityBottomSheet;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnErrorListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnMultiTypeErrorListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnStringResponseListener;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.util.ConfigUtil;
import xyz.zedler.patrick.grocy.util.PrefsUtil;
import xyz.zedler.patrick.grocy.web.NetworkQueue.QueueItem;

public class LoginRequestViewModel extends BaseViewModel {

  private static final String TAG = LoginRequestViewModel.class.getSimpleName();

  private final SharedPreferences sharedPrefs;
  private final SharedPreferences sharedPrefsPrivate;
  private final DownloadHelper dlHelper;
  private WebSocketClient webSocketClient;

  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
  private final MutableLiveData<Boolean> loginErrorOccurred;
  private final MutableLiveData<String> loginErrorMsg;
  private final MutableLiveData<String> loginErrorExactMsg;
  private final MutableLiveData<String> loginErrorHassMsg;
  private final MutableLiveData<String> loginErrorHassLog;

  private final String serverUrl;
  private final String homeAssistantServerUrl;
  private final String homeAssistantLongLivedToken;
  private final String apiKey;
  private final boolean useHassLoginFlow;
  private final boolean debug;

  public LoginRequestViewModel(@NonNull Application application, LoginRequestFragmentArgs args) {
    super(application);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    sharedPrefsPrivate = getApplication().getSharedPreferences(
        Constants.PREF.CREDENTIALS,
        Context.MODE_PRIVATE
    );
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);

    serverUrl = args.getGrocyServerUrl();
    homeAssistantServerUrl = args.getHomeAssistantServerUrl();
    homeAssistantLongLivedToken = args.getHomeAssistantToken();
    apiKey = args.getGrocyApiKey();
    useHassLoginFlow = args.getHomeAssistantServerUrl() != null;

    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(
        getApplication(),
        serverUrl,
        apiKey,
        TAG,
        isLoadingLive::setValue
    );

    infoFullscreenLive = new MutableLiveData<>();
    loginErrorOccurred = new MutableLiveData<>(false);
    loginErrorMsg = new MutableLiveData<>();
    loginErrorExactMsg = new MutableLiveData<>();
    loginErrorHassMsg = new MutableLiveData<>();
    loginErrorHassLog = new MutableLiveData<>();
  }

  public void login(boolean checkVersion) {
    loginErrorOccurred.setValue(false);
    loginErrorMsg.setValue(null);
    loginErrorExactMsg.setValue(null);
    loginErrorHassMsg.setValue(null);

    if (useHassLoginFlow
        && sharedPrefs.getString(PREF.HOME_ASSISTANT_INGRESS_SESSION_KEY, null) == null) {
      appendHassLog("Home Assistant needs authentication over WebSocket.\n");
      createWebSocketClient();
      return;
    }

    appendHassLog("Sending test request to grocy...");
    getSystemInfo(dlHelper, response -> {
          if (!response.contains("grocy_version")) {
            appendHassLog(" Error.\n");
            loginErrorOccurred.setValue(true);
            loginErrorMsg.setValue(getString(R.string.error_not_grocy_instance));
            return;
          }
          try {
            String grocyVersion = new JSONObject(response)
                .getJSONObject("grocy_version")
                .getString("Version");
            ArrayList<String> supportedVersions = new ArrayList<>(
                Arrays.asList(getResources()
                    .getStringArray(R.array.compatible_grocy_versions))
            );
            appendHassLog(" Success.");
            if (checkVersion && !supportedVersions.contains(grocyVersion)) {
              showCompatibilityBottomSheet(supportedVersions, grocyVersion);
              return;
            }
          } catch (JSONException e) {
            Log.e(TAG, "requestLogin: " + e);
            appendHassLog(" Error.\nFailed to parse system info response.\n");
            loginErrorOccurred.setValue(true);
            loginErrorMsg.setValue(getString(R.string.error_not_grocy_instance));
            return;
          }

          if (debug) {
            Log.i(TAG, "requestLogin: successfully logged in");
          }
          sharedPrefs.edit()
              .putString(Constants.PREF.SERVER_URL, serverUrl)
              .putString(Constants.PREF.API_KEY, apiKey)
              .apply();
          if (useHassLoginFlow) {
            sharedPrefs.edit().putString(
                Constants.PREF.HOME_ASSISTANT_SERVER_URL,
                homeAssistantServerUrl
            ).putString(
                Constants.PREF.HOME_ASSISTANT_LONG_LIVED_TOKEN,
                homeAssistantLongLivedToken
            ).apply();
          }
          if (!isDemoServer()) {
            sharedPrefsPrivate.edit()
                .putString(
                    Constants.PREF.SERVER_URL,
                    useHassLoginFlow ? homeAssistantServerUrl : serverUrl
                ).putString(Constants.PREF.API_KEY, apiKey)
                .apply();
          }
          // TODO: Feature needs migrations for database
          /*Server server = new Server();
          server.setGrocyServerUrl(serverUrl);
          server.setGrocyApiKey(apiKey);
          server.setHomeAssistantServerUrl(homeAssistantServerUrl);
          server.setHomeAssistantToken(homeAssistantLongLivedToken);
          appDatabase.serverDao().insertServer(server)
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .doFinally(this::loadInfoAndFinish)
              .subscribe();*/
          loadInfoAndFinish();
        },
        error -> {
          Log.e(TAG, "requestLogin: VolleyError: " + error);
          loginErrorOccurred.setValue(true);
          if (error instanceof AuthFailureError) {
            loginErrorExactMsg.setValue(error.toString());
            loginErrorMsg.setValue(getString(R.string.error_api_not_working));
            loginErrorHassMsg.setValue("Please check the grocy API key on the previous page. ");
          } else if (error instanceof NoConnectionError) {
            if (error.toString().contains("SSLHandshakeException")) {
              showMessage("SSLHandshakeException");
              loginErrorMsg.setValue(getString(R.string.error_handshake));
              loginErrorExactMsg.setValue(getString(R.string.error_handshake_description));
            } else if (error.toString().contains("Invalid host")) {
              loginErrorMsg.setValue(getString(R.string.error_invalid_url));
              loginErrorExactMsg.setValue("Please check the server URL:\n" + serverUrl);
            } else {
              loginErrorMsg.setValue(getString(R.string.error_failed_to_connect_to));
              loginErrorExactMsg
                  .setValue("Server URL: " + serverUrl + "\n\nError: " + error);
            }
          } else if (error instanceof ServerError && error.networkResponse != null) {
            int code = error.networkResponse.statusCode;
            if (code == 404) {
              loginErrorMsg.setValue(getString(R.string.error_not_grocy_instance));
              loginErrorExactMsg.setValue("Server URL: " + serverUrl + "\n\nResponse code: " + code);
              loginErrorHassMsg.setValue("If you use grocy with Home Assistant, please check if you have chosen Home Assistant mode on the previous page.");
            } else {
              loginErrorMsg.setValue(getString(R.string.error_unexpected_response_code));
              loginErrorExactMsg.setValue("Response code: " + code);
              if (useHassLoginFlow && code == 503) {
                loginErrorHassMsg.setValue("The ingress proxy identifier may be wrong. Please check it on the previous page. It should be a longer string like \"s65bor48v40w3r0m8v-cn945mwdj5icjvwsd43cfnm3\" and not \"gs6h7m3o_grocy\".");
              } else if (useHassLoginFlow && code == 401) {
                loginErrorHassMsg.setValue("Additional info: long-lived access token may be invalid for Home Assistant. Please check it on the previous page.");
              }
            }
          } else if (error instanceof ServerError) {
            loginErrorMsg.setValue(getString(R.string.error_unexpected_response));
            loginErrorExactMsg.setValue(error.toString());
          } else if (error instanceof TimeoutError) {
            loginErrorMsg.setValue(getString(R.string.error_timeout));
            loginErrorExactMsg.setValue(error.toString());
          } else {
            loginErrorMsg.setValue(getString(R.string.error_undefined));
            loginErrorExactMsg.setValue(error.toString());
          }
        }
    ).perform(dlHelper.getUuid());
  }

  private void loadInfoAndFinish() {
    ConfigUtil.loadInfo(
        dlHelper,
        new GrocyApi(getApplication()),
        sharedPrefs,
        () -> sendEvent(Event.LOGIN_SUCCESS),
        error -> sendEvent(Event.LOGIN_SUCCESS)
    );
  }

  public void createWebSocketClient() {
    if (homeAssistantLongLivedToken == null || homeAssistantLongLivedToken.isEmpty()) {
      return;
    }
    if (webSocketClient != null) {
      appendHassLog("Closing previous WebSocket client...");
      webSocketClient.close(0, 0, "recreate websocket client");
      appendHassLog(" Done.\n");
    }

    URI uri;
    try {
      String hassWebSocketUrl = homeAssistantServerUrl
          .replaceFirst("https", "wss")
          .replaceFirst("http", "ws");
      uri = new URI(hassWebSocketUrl + "/api/websocket");
    }
    catch (URISyntaxException e) {
      appendHassLog("Error while creating WebSocket URI.\n");
      e.printStackTrace();
      return;
    }

    appendHassLog("Creating WebSocket client...");
    webSocketClient = new WebSocketClient(uri) {
      @Override
      public void onOpen() {
        appendHassLog("WebSocket connection is now open.\n");
      }
      @Override
      public void onPingReceived(byte[] data) {}
      @Override
      public void onPongReceived(byte[] data) {}
      @Override
      public void onBinaryReceived(byte[] data) {}

      @Override
      public void onTextReceived(String message) {
        if (debug) Log.i(TAG, "createWebSocketClient: onTextReceived: " + message);
        if (message.contains("auth_required")) {
          appendHassLog("Received auth_required message. Sending HA token...");
          try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", "auth");
            jsonObject.put("access_token", homeAssistantLongLivedToken);
            webSocketClient.send(jsonObject.toString());
            appendHassLog(" Done.\n");
          } catch (JSONException e) {
            Log.e(TAG, "createWebSocketClient: onTextReceived: " + e);
            appendHassLog("Error: " + e + "\n");
          }
        } else if (message.contains("auth_ok")) {
          appendHassLog("Authentication was successful. Requesting session token...");
          try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", "supervisor/api");
            jsonObject.put("endpoint", "/ingress/session");
            jsonObject.put("method", "post");
            jsonObject.put("id", 1);
            webSocketClient.send(jsonObject.toString());
            appendHassLog(" Done.\n");
          } catch (JSONException e) {
            Log.e(TAG, "createWebSocketClient: onTextReceived: " + e);
            appendHassLog("Error: " + e + "\n");
          }
        } else if (message.contains("auth_invalid")) {
          appendHassLog("Authentication failed.\n");
          loginErrorOccurred.postValue(true);
          loginErrorHassMsg.postValue("Please check the Home Assistant long-lived token on the previous page.");
        } else if (message.contains("result")) {
          appendHassLog("Received result. Extracting session token...");
          try {
            JSONObject jsonObject = new JSONObject(message);
            if (jsonObject.getBoolean("success")) {
              sharedPrefs.edit().putString(
                  PREF.HOME_ASSISTANT_INGRESS_SESSION_KEY,
                  jsonObject.getJSONObject("result").getString("session")
              ).apply();
              appendHassLog(" Success.\n");
              new Handler(getApplication().getMainLooper()).post(() -> login(true));
            } else {
              appendHassLog(" Error: " + message + "\n");
              Log.e(TAG, "createWebSocketClient: onTextReceived: " + message);
            }
          } catch (JSONException e) {
            appendHassLog(" Error: " + e + "\n");
            Log.e(TAG, "createWebSocketClient: onTextReceived: " + e);
          }
        }
      }

      @Override
      public void onException(Exception e) {
        appendHassLog("Error: " + e + "\n");
        Log.e(TAG, "createWebSocketClient: onException: " + e.getMessage());
      }

      @Override
      public void onCloseReceived(int reason, String description) {
        appendHassLog("Connection was closed.");
        if (debug) Log.i(TAG, "createWebSocketClient: onCloseReceived: " + description);
        new Handler().postDelayed(() -> {
          appendHassLog(" Reconnecting...\n");
          webSocketClient.connect();
        }, 1000);
      }
    };
    appendHassLog(" Done.\n");

    webSocketClient.setConnectTimeout(10000);
    webSocketClient.setReadTimeout(60000);
    webSocketClient.enableAutomaticReconnection(5000);
    webSocketClient.connect();
  }

  private void showCompatibilityBottomSheet(
      ArrayList<String> supportedVersions,
      String grocyVersion
  ) {
    sharedPrefs.edit().remove(Constants.PREF.VERSION_COMPATIBILITY_IGNORED).apply();
    Bundle bundle = new Bundle();
    bundle.putString(Constants.ARGUMENT.VERSION, grocyVersion);
    bundle.putStringArrayList(Constants.ARGUMENT.SUPPORTED_VERSIONS, supportedVersions);
    showBottomSheet(new CompatibilityBottomSheet(), bundle);
  }

  public MutableLiveData<Boolean> getLoginErrorOccurred() {
    return loginErrorOccurred;
  }

  public MutableLiveData<String> getLoginErrorMsg() {
    return loginErrorMsg;
  }

  public MutableLiveData<String> getLoginErrorExactMsg() {
    return loginErrorExactMsg;
  }

  public MutableLiveData<String> getLoginErrorHassMsg() {
    return loginErrorHassMsg;
  }

  public MutableLiveData<String> getLoginErrorHassLog() {
    return loginErrorHassLog;
  }

  public boolean isUseHassLoginFlow() {
    return useHassLoginFlow;
  }

  private void appendHassLog(String append) {
    String log = loginErrorHassLog.getValue();
    if (log == null) log = "";
    log += append;
    if (Looper.myLooper() == Looper.getMainLooper()) {
      loginErrorHassLog.setValue(log);
    } else {
      loginErrorHassLog.postValue(log);
    }
  }

  public void clearHassData() {
    sharedPrefs.edit().putString(PREF.HOME_ASSISTANT_INGRESS_SESSION_KEY, null).apply();
    loginErrorHassLog.setValue("");
  }

  private boolean isDemoServer() {
    return serverUrl.contains("demo.grocy.info");
  }

  @NonNull
  public MutableLiveData<Boolean> getIsLoadingLive() {
    return isLoadingLive;
  }

  @NonNull
  public MutableLiveData<InfoFullscreen> getInfoFullscreenLive() {
    return infoFullscreenLive;
  }

  @Override
  protected void onCleared() {
    dlHelper.destroy();
    if (webSocketClient != null) {
      webSocketClient.close(0, 0, "viewmodel cleared");
    }
    super.onCleared();
  }

  public static QueueItem getSystemInfo(
      DownloadHelper dlHelper,
      OnStringResponseListener onResponseListener,
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
            dlHelper.grocyApi.getSystemInfo(),
            uuid,
            response -> {
              if (dlHelper.debug) {
                Log.i(dlHelper.tag, "get systemInfo: " + response);
              }
              if (onResponseListener != null) {
                onResponseListener.onResponse(response);
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

  public static class LoginViewModelFactory implements ViewModelProvider.Factory {

    private final Application application;
    private final LoginRequestFragmentArgs args;

    public LoginViewModelFactory(Application application, LoginRequestFragmentArgs args) {
      this.application = application;
      this.args = args;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return (T) new LoginRequestViewModel(application, args);
    }
  }
}
