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

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import com.google.android.material.color.DynamicColors;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.NOTIFICATIONS;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.Constants.THEME;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.notification.BootReceiver;
import xyz.zedler.patrick.grocy.notification.ChoresNotificationReceiver;
import xyz.zedler.patrick.grocy.notification.StockNotificationReceiver;

public class ReminderUtil {

  private static final String TAG = ReminderUtil.class.getSimpleName();

  public final static String STOCK_TYPE = "STOCK";
  public final static String CHORES_TYPE = "CHORES";

  private final Context context;
  private final SharedPreferences sharedPrefs;
  private final AlarmManager alarmManager;
  private final NotificationManager notificationManager;

  public ReminderUtil(Context context) {
    this.context = context;

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    notificationManager = (NotificationManager) context.getSystemService(
        Context.NOTIFICATION_SERVICE
    );
  }

  public void rescheduleReminders() {
    List<String> reminderTypes = Arrays.asList(STOCK_TYPE, CHORES_TYPE);

    for (String reminderType : reminderTypes) {
      switch (reminderType) {
        case STOCK_TYPE:
          setReminderEnabled(reminderType, sharedPrefs.getBoolean(NOTIFICATIONS.STOCK_ENABLE,
              SETTINGS_DEFAULT.NOTIFICATIONS.STOCK_ENABLE));
          break;
        case CHORES_TYPE:
          setReminderEnabled(reminderType, sharedPrefs.getBoolean(NOTIFICATIONS.CHORES_ENABLE,
              SETTINGS_DEFAULT.NOTIFICATIONS.CHORES_ENABLE));
          break;
        default:
          throw new IllegalArgumentException("Unknown reminder type: " + reminderType);
      }
    }
  }

  @SuppressLint("SimpleDateFormat")
  public void scheduleReminder(
      String reminderType,
      int reminderId,
      String time,
      Class<? extends BroadcastReceiver> receiverClass
  ) {
    if (time == null) {
      switch (reminderType) {
        case STOCK_TYPE:
          time = NOTIFICATIONS.STOCK_TIME;
          break;
        case CHORES_TYPE:
          time = NOTIFICATIONS.CHORES_TIME;
          break;
        default:
          throw new IllegalArgumentException("Unknown reminder type: " + reminderType);
      }
    }

    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(System.currentTimeMillis());

    calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time.split(":")[0]));
    calendar.set(Calendar.MINUTE, Integer.parseInt(time.split(":")[1]));
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);

    if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
      calendar.add(Calendar.DATE, 1);
    }

    PendingIntent pendingIntent = PendingIntent.getBroadcast(
        context,
        reminderId,
        new Intent(context, receiverClass),
        getPendingIntentFlags()
    );

    if (notificationManager != null) {
      notificationManager.cancel(reminderId);
    }
    if (alarmManager != null) {
      alarmManager.cancel(pendingIntent);
      alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
    }
  }

  public void scheduleAgainIn10Minutes(
      int reminderId,
      Class<? extends BroadcastReceiver> receiverClass
  ) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(System.currentTimeMillis());
    calendar.add(Calendar.MINUTE, 10);

    PendingIntent pendingIntent = PendingIntent.getBroadcast(
        context,
        reminderId,
        new Intent(context, receiverClass),
        getPendingIntentFlags()
    );

    if (notificationManager != null) {
      notificationManager.cancel(reminderId);
    }
    if (alarmManager != null) {
      alarmManager.cancel(pendingIntent);
      alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
    }
  }

  public void setReminderEnabled(String reminderType, boolean enabled) {
    int reminderId;
    String reminderTime;
    String reminderTimeDefault;
    Class<? extends BroadcastReceiver> receiverClass;

    switch (reminderType) {
      case STOCK_TYPE:
        sharedPrefs.edit().putBoolean(NOTIFICATIONS.STOCK_ENABLE, enabled).apply();
        reminderId = NOTIFICATIONS.STOCK_ID;
        reminderTime = NOTIFICATIONS.STOCK_TIME;
        reminderTimeDefault = SETTINGS_DEFAULT.NOTIFICATIONS.STOCK_TIME;
        receiverClass = StockNotificationReceiver.class;
        break;
      case CHORES_TYPE:
        sharedPrefs.edit().putBoolean(NOTIFICATIONS.CHORES_ENABLE, enabled).apply();
        reminderId = NOTIFICATIONS.CHORES_ID;
        reminderTime = NOTIFICATIONS.CHORES_TIME;
        reminderTimeDefault = SETTINGS_DEFAULT.NOTIFICATIONS.CHORES_TIME;
        receiverClass = ChoresNotificationReceiver.class;
        break;
      default:
        throw new IllegalArgumentException("Unknown reminder type: " + reminderType);
    }

    if (enabled) {
      scheduleReminder(
          reminderType,
          reminderId,
          sharedPrefs.getString(reminderTime, reminderTimeDefault),
          receiverClass
      );
    } else {
      if (notificationManager != null) {
        notificationManager.cancel(reminderId);
      }
      PendingIntent pendingIntent = PendingIntent.getBroadcast(
          context,
          reminderId,
          new Intent(context, receiverClass),
          getPendingIntentFlags()
      );
      if (alarmManager != null && pendingIntent != null) {
        alarmManager.cancel(pendingIntent);
      }
    }
    startOnBootCompleted(enabled);
  }

  public void startOnBootCompleted(boolean enabled) {
    context.getPackageManager().setComponentEnabledSetting(
        new ComponentName(context, BootReceiver.class),
        enabled
            ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
        PackageManager.DONT_KILL_APP
    );
  }

  public boolean hasPermission() {
    if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
      int status = ContextCompat.checkSelfPermission(
          context, Manifest.permission.POST_NOTIFICATIONS
      );
      return status == PackageManager.PERMISSION_GRANTED;
    } else {
      return true;
    }
  }

  public static Notification getNotification(
      Context context,
      String title,
      String text,
      int notificationId,
      String channelId,
      Intent intent
  ) {
    NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId);
    builder
        .setContentTitle(title)
        .setAutoCancel(true)
        .setColor(getColor(context))
        .setSmallIcon(R.drawable.ic_round_grocy_notification)
        .setContentIntent(
            PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                getPendingIntentFlags()
            )
        ).setPriority(NotificationCompat.PRIORITY_DEFAULT);
    if (text != null) {
      builder.setContentText(text);
      builder.setStyle(new NotificationCompat.BigTextStyle().bigText(text));
    }
    return builder.build();
  }

  private static int getPendingIntentFlags() {
    return VERSION.SDK_INT >= VERSION_CODES.M
        ? PendingIntent.FLAG_IMMUTABLE
        : PendingIntent.FLAG_UPDATE_CURRENT;
  }

  private static int getColor(Context context) {
    Context colorContext;
    if (DynamicColors.isDynamicColorAvailable()) {
      colorContext = DynamicColors.wrapContextIfAvailable(context);
    } else {
      int themeResId;
      SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
      String theme = sharedPrefs.getString(
          Constants.SETTINGS.APPEARANCE.THEME, Constants.SETTINGS_DEFAULT.APPEARANCE.THEME
      );
      switch (theme) {
        case THEME.RED:
          themeResId = R.style.Theme_Grocy_Red;
          break;
        case THEME.YELLOW:
          themeResId = R.style.Theme_Grocy_Yellow;
          break;
        case THEME.BLUE:
          themeResId = R.style.Theme_Grocy_Blue;
          break;
        default:
          themeResId = R.style.Theme_Grocy_Green;
          break;
      }
      colorContext = new ContextThemeWrapper(context, themeResId);
    }
    return ResUtil.getSysColor(colorContext, R.attr.colorPrimary);
  }
}

