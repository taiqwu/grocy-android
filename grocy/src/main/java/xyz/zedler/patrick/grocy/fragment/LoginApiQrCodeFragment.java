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

package xyz.zedler.patrick.grocy.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentLoginApiQrCodeBinding;
import xyz.zedler.patrick.grocy.scanner.EmbeddedFragmentScanner;
import xyz.zedler.patrick.grocy.scanner.EmbeddedFragmentScanner.BarcodeListener;
import xyz.zedler.patrick.grocy.scanner.EmbeddedFragmentScannerBundle;
import xyz.zedler.patrick.grocy.util.ClickUtil;

public class LoginApiQrCodeFragment extends BaseFragment implements BarcodeListener {

  private final static String TAG = LoginApiQrCodeFragment.class.getSimpleName();

  private static final int SCAN_GROCY_KEY = 0;
  private static final int SCAN_HASS_TOKEN = 1;

  private FragmentLoginApiQrCodeBinding binding;
  private MainActivity activity;
  private LoginApiQrCodeFragmentArgs args;
  private EmbeddedFragmentScanner embeddedFragmentScanner;
  private int pageStatus = SCAN_GROCY_KEY;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentLoginApiQrCodeBinding.inflate(inflater, container, false);
    embeddedFragmentScanner = new EmbeddedFragmentScannerBundle(
        this,
        binding.containerScanner,
        this,
        true,
        false,
        true
    );
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  @Override
  public void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) requireActivity();
    args = LoginApiQrCodeFragmentArgs.fromBundle(requireArguments());
    pageStatus = args.getGrocyApiKey() == null ? SCAN_GROCY_KEY : SCAN_HASS_TOKEN;

    binding.setActivity(activity);
    binding.setFragment(this);
    binding.setClickUtil(new ClickUtil());
    embeddedFragmentScanner.setScannerVisibilityLive(new MutableLiveData<>(isPageForGrocyKey()));

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBar);
    systemBarBehavior.setScroll(binding.scroll, binding.linearContainerScroll);
    systemBarBehavior.setUp();
    activity.setSystemBarBehavior(systemBarBehavior);

    binding.toolbar.setNavigationOnClickListener(v -> activity.navUtil.navigateUp());
    binding.toolbar.setOnMenuItemClickListener(item -> {
      int id = item.getItemId();
      if (id == R.id.action_help) {
        activity.showHelpBottomSheet();
      } else if (id == R.id.action_feedback) {
        activity.showFeedbackBottomSheet();
      } else if (id == R.id.action_website) {
        openGrocyWebsite();
      } else if (id == R.id.action_settings) {
        activity.navUtil.navigateDeepLink(R.string.deep_link_settingsFragment);
      } else if (id == R.id.action_about) {
        activity.navUtil.navigateDeepLink(R.string.deep_link_aboutFragment);
      }
      return true;
    });

    if (!isPageForGrocyKey()) {
      new Handler().postDelayed(() -> new MaterialAlertDialogBuilder(
          requireContext(), R.style.ThemeOverlay_Grocy_AlertDialog
      )
          .setMessage(R.string.msg_qr_code_scan_token)
          .setPositiveButton(R.string.action_scan, (dialog, which) -> {
            embeddedFragmentScanner.setScannerVisibilityLive(new MutableLiveData<>(true));
            dialog.dismiss();
          })
          .create().show(), 100);
    }

    activity.getScrollBehavior().setNestedOverScrollFixEnabled(false);
    activity.getScrollBehavior().setProvideTopScroll(false);
    activity.getScrollBehavior().setCanBottomAppBarBeVisible(false);
    activity.getScrollBehavior().setBottomBarVisibility(false, true, false);
    activity.getScrollBehavior().setUpScroll(
        binding.appBar, false, binding.scroll, false
    );
  }

  @Override
  public void onResume() {
    super.onResume();
    embeddedFragmentScanner.onResume();
  }

  @Override
  public void onPause() {
    embeddedFragmentScanner.onPause();
    super.onPause();
  }

  @Override
  public void onDestroy() {
    if (embeddedFragmentScanner != null) embeddedFragmentScanner.onDestroy();
    super.onDestroy();
  }

  @Override
  public void onBarcodeRecognized(String rawValue) {
    if(pageStatus == SCAN_GROCY_KEY) {
      String[] resultSplit = rawValue.split("\\|");
      if (resultSplit.length != 2) {
        activity.showSnackbar(R.string.error_api_qr_code, true);
        embeddedFragmentScanner.startScannerIfVisible();
        return;
      }
      String apiURL = resultSplit[0];
      String serverURL = apiURL.replaceAll("/api$", "");
      String ingressProxyId = null;
      String serverURLHomeAssistant = null;
      if (serverURL.contains("/api/hassio_ingress/")) {
        String[] serverURLAndIngressProxyId = serverURL.split("/api/hassio_ingress/");
        serverURLHomeAssistant = serverURLAndIngressProxyId[0];
        if (serverURLHomeAssistant != null && serverURLHomeAssistant.isEmpty()) {
          serverURLHomeAssistant = null;
        }
        ingressProxyId = serverURLAndIngressProxyId[1];
      }
      String apiKey = resultSplit[1];

      if (ingressProxyId == null) {
        activity.navUtil.navigate(LoginApiQrCodeFragmentDirections
            .actionLoginApiQrCodeFragmentToLoginRequestFragment(serverURL, apiKey));
      } else { // grocy home assistant add-on used
        activity.navUtil.navigate(LoginApiQrCodeFragmentDirections
            .actionLoginApiQrCodeFragmentSelf()
            .setServerURL(serverURLHomeAssistant)
            .setGrocyIngressProxyId(ingressProxyId)
            .setGrocyApiKey(apiKey));
      }
    } else if (pageStatus == SCAN_HASS_TOKEN) {
      String[] resultSplit = rawValue.split("\\.");
      if (resultSplit.length != 3) {
        activity.showSnackbar(R.string.error_token_qr_code, true);
        Log.e(TAG, "onBarcodeRecognized: not a HASS Token QR code: " + rawValue);
        embeddedFragmentScanner.startScannerIfVisible();
        return;
      }
      activity.navUtil.navigate(LoginApiQrCodeFragmentDirections
          .actionLoginApiQrCodeFragmentToLoginApiFormFragment()
          .setServerUrl(args.getServerURL())
          .setGrocyIngressProxyId(args.getGrocyIngressProxyId())
          .setGrocyApiKey(args.getGrocyApiKey())
          .setHomeAssistantToken(rawValue));
    }
  }

  public void toggleTorch() {
    embeddedFragmentScanner.toggleTorch();
  }

  public void enterDataManually() {
    activity.navUtil.navigate(
        LoginApiQrCodeFragmentDirections.actionLoginApiQrCodeFragmentToLoginApiFormFragment()
    );
  }

  public void openGrocyWebsite() {
    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_grocy))));
  }

  public boolean isPageForGrocyKey() {
    return pageStatus == SCAN_GROCY_KEY;
  }
}
