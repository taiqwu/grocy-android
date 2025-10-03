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

package xyz.zedler.patrick.grocy.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.res.ResourcesCompat;
import androidx.preference.PreferenceManager;
import com.google.android.material.color.DynamicColors;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.Constants.SETTINGS;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.Constants.THEME;
import xyz.zedler.patrick.grocy.util.UiUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;

public class SplashActivity extends MainActivity {

  @Override
  public void onCreate(Bundle bundle) {
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
      super.onCreate(bundle);

      boolean speedUpStart = sharedPrefs.getBoolean(
          Constants.SETTINGS.BEHAVIOR.SPEED_UP_START,
          Constants.SETTINGS_DEFAULT.BEHAVIOR.SPEED_UP_START
      );

      getSplashScreen().setOnExitAnimationListener(view -> {
        Instant startTime = view.getIconAnimationStart();
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "alpha", 0);
        animator.setDuration(speedUpStart ? 150 : 250);
        animator.setStartDelay(
            startTime != null && !speedUpStart
                ? 900 - startTime.until(Instant.now(), ChronoUnit.MILLIS)
                : 0
        );
        animator.addListener(new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(@NonNull Animator animation, boolean isReverse) {
            view.remove();
          }
        });
        animator.start();
      });
    } else {
      // DARK MODE

      try {
        sharedPrefs.getInt(SETTINGS.APPEARANCE.DARK_MODE, SETTINGS_DEFAULT.APPEARANCE.DARK_MODE);
      } catch (ClassCastException e) {
        sharedPrefs.edit().remove(SETTINGS.APPEARANCE.DARK_MODE).apply();
      }
      int modeNight = sharedPrefs.getInt(
          SETTINGS.APPEARANCE.DARK_MODE, SETTINGS_DEFAULT.APPEARANCE.DARK_MODE);
      int uiMode = getResources().getConfiguration().uiMode;
      switch (modeNight) {
        case AppCompatDelegate.MODE_NIGHT_NO:
          uiMode = Configuration.UI_MODE_NIGHT_NO;
          break;
        case AppCompatDelegate.MODE_NIGHT_YES:
          uiMode = Configuration.UI_MODE_NIGHT_YES;
          break;
      }
      AppCompatDelegate.setDefaultNightMode(modeNight);
      // Apply config to resources
      Resources resBase = getBaseContext().getResources();
      Configuration configBase = resBase.getConfiguration();
      configBase.uiMode = uiMode;
      resBase.updateConfiguration(configBase, resBase.getDisplayMetrics());

      // THEME

      UiUtil.setTheme(this, sharedPrefs);

      if (bundle == null) {
        bundle = new Bundle();
      }
      bundle.putBoolean(ARGUMENT.RUN_AS_SUPER_CLASS, true);
      super.onCreate(bundle);

      new SystemBarBehavior(this).setUp();

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        boolean speedUpStart = sharedPrefs.getBoolean(
            Constants.SETTINGS.BEHAVIOR.SPEED_UP_START,
            Constants.SETTINGS_DEFAULT.BEHAVIOR.SPEED_UP_START
        );
        LayerDrawable splashContent = (LayerDrawable) ResourcesCompat.getDrawable(
            getResources(), R.drawable.splash_content, getTheme()
        );
        getWindow().getDecorView().setBackground(splashContent);
        try {
          if (speedUpStart) {
            new Handler(Looper.getMainLooper()).postDelayed(
                this::startNewMainActivity, 50
            );
          } else {
            assert splashContent != null;
            ViewUtil.startIcon(splashContent.findDrawableByLayerId(R.id.splash_logo));
            new Handler(Looper.getMainLooper()).postDelayed(
                this::startNewMainActivity, 850
            );
          }
        } catch (Exception e) {
          startNewMainActivity();
        }
      } else {
        startNewMainActivity();
      }
    }
  }

  @Override
  protected void attachBaseContext(Context base) {
    if (Build.VERSION.SDK_INT >= 31) {
      super.attachBaseContext(base);
      return;
    }
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(base);
    // Night mode
    try {
      sharedPrefs.getInt(SETTINGS.APPEARANCE.DARK_MODE, SETTINGS_DEFAULT.APPEARANCE.DARK_MODE);
    } catch (ClassCastException e) {
      sharedPrefs.edit().remove(SETTINGS.APPEARANCE.DARK_MODE).apply();
    }
    int modeNight = sharedPrefs.getInt(
        SETTINGS.APPEARANCE.DARK_MODE, SETTINGS_DEFAULT.APPEARANCE.DARK_MODE
    );
    int uiMode = base.getResources().getConfiguration().uiMode;
    switch (modeNight) {
      case AppCompatDelegate.MODE_NIGHT_NO:
        uiMode = Configuration.UI_MODE_NIGHT_NO;
        break;
      case AppCompatDelegate.MODE_NIGHT_YES:
        uiMode = Configuration.UI_MODE_NIGHT_YES;
        break;
    }
    AppCompatDelegate.setDefaultNightMode(modeNight);
    // Apply config to resources
    Resources resources = base.getResources();
    Configuration config = resources.getConfiguration();
    config.uiMode = uiMode;
    resources.updateConfiguration(config, resources.getDisplayMetrics());
    super.attachBaseContext(base.createConfigurationContext(config));
  }

  private void startNewMainActivity() {
    Intent intent = new Intent(this, MainActivity.class);
    intent.addCategory(Intent.CATEGORY_LAUNCHER);
    startActivity(intent);
    overridePendingTransition(0, R.anim.fade_out);
    finish();
  }
}
