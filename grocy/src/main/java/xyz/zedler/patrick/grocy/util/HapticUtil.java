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

package xyz.zedler.patrick.grocy.util;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.provider.Settings;

public class HapticUtil {

  private final Vibrator vibrator;
  private boolean enabled;

  public static final long TICK = 13;
  public static final long CLICK = 20;
  public static final long HEAVY = 50;

  public HapticUtil(Context context) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
      VibratorManager manager =
          (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
      vibrator = manager.getDefaultVibrator();
    } else {
      vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }
    enabled = hasVibrator();
  }

  public void vibrate(long duration) {
    if (!enabled) {
      return;
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
    } else {
      vibrator.vibrate(duration);
    }
  }

  private void vibrate(int effectId) {
    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      vibrator.vibrate(VibrationEffect.createPredefined(effectId));
    }
  }

  public void tick() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      vibrate(VibrationEffect.EFFECT_TICK);
    } else {
      vibrate(TICK);
    }
  }

  public void click() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      vibrate(VibrationEffect.EFFECT_CLICK);
    } else {
      vibrate(CLICK);
    }
  }

  public void doubleClick() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      vibrate(VibrationEffect.EFFECT_DOUBLE_CLICK);
    } else {
      vibrate(CLICK);
      new Handler(Looper.getMainLooper()).postDelayed(() -> vibrate(CLICK), 100);
    }
  }

  public void heavyClick() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      vibrate(VibrationEffect.EFFECT_HEAVY_CLICK);
    } else {
      vibrate(HEAVY);
    }
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled && hasVibrator();
  }

  public boolean hasVibrator() {
    return vibrator.hasVibrator();
  }

  public static boolean areSystemHapticsTurnedOn(Context context) {
    int hapticFeedbackEnabled = Settings.System.getInt(
        context.getContentResolver(), Settings.System.HAPTIC_FEEDBACK_ENABLED, 0
    );
    return hapticFeedbackEnabled != 0;
  }
}
