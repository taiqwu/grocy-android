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
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.CursorWindow;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.DrawableRes;
import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar.OnMenuItemClickListener;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.preference.PreferenceManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.Snackbar;
import java.lang.reflect.Field;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.Constants.SETTINGS;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.BEHAVIOR;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.NETWORK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.NavigationMainDirections;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.behavior.BottomScrollBehavior;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.database.AppDatabase;
import xyz.zedler.patrick.grocy.databinding.ActivityMainBinding;
import xyz.zedler.patrick.grocy.fragment.BaseFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.FeedbackBottomSheet;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.ConfigUtil;
import xyz.zedler.patrick.grocy.util.HapticUtil;
import xyz.zedler.patrick.grocy.util.LocaleUtil;
import xyz.zedler.patrick.grocy.util.NavUtil;
import xyz.zedler.patrick.grocy.util.NetUtil;
import xyz.zedler.patrick.grocy.util.PrefsUtil;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.util.ShortcutUtil;
import xyz.zedler.patrick.grocy.util.UiUtil;
import xyz.zedler.patrick.grocy.util.VersionUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;
import xyz.zedler.patrick.grocy.web.OrbotHelper;

public class MainActivity extends AppCompatActivity {

  private final static String TAG = MainActivity.class.getSimpleName();

  public ActivityMainBinding binding;
  public NavUtil navUtil;
  public NetUtil netUtil;
  public HapticUtil hapticUtil;
  private SharedPreferences sharedPrefs;
  private FragmentManager fragmentManager;
  private GrocyApi grocyApi;
  private ClickUtil clickUtil;
  private BroadcastReceiver networkReceiver;
  private BottomScrollBehavior scrollBehavior;
  private UiUtil uiUtil;
  private boolean runAsSuperClass;
  private boolean debug;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    runAsSuperClass = savedInstanceState != null
        && savedInstanceState.getBoolean(ARGUMENT.RUN_AS_SUPER_CLASS, false);

    if (runAsSuperClass) {
      super.onCreate(savedInstanceState);
      return;
    }

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    PrefsUtil.migratePrefs(sharedPrefs);
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);

    // DARK MODE AND THEME

    // this has to be placed before super.onCreate(savedInstanceState);
    // https://stackoverflow.com/a/53356918
    int modeNight = PrefsUtil.getModeNight(sharedPrefs);
    AppCompatDelegate.setDefaultNightMode(modeNight);
    ResUtil.applyConfigToResources(this, modeNight);

    // COLOR

    UiUtil.setTheme(this, sharedPrefs);
    UiUtil.applyColorHarmonization(this);

    Bundle bundleInstanceState = getIntent().getBundleExtra(ARGUMENT.INSTANCE_STATE);
    super.onCreate(bundleInstanceState != null ? bundleInstanceState : savedInstanceState);

    // UTILS

    clickUtil = new ClickUtil();
    hapticUtil = new HapticUtil(this);
    hapticUtil.setEnabled(PrefsUtil.areHapticsEnabled(sharedPrefs, this));
    netUtil = new NetUtil(this, sharedPrefs, debug, TAG);
    netUtil.insertConscrypt();
    netUtil.createWebSocketClient();

    // LANGUAGE

    LocaleUtil.setLocalizedGrocyDemoInstance(this, sharedPrefs);  // set localized demo instance
    ShortcutUtil.refreshShortcuts(this);  // refresh shortcut language

    // DATABASE

    // Workaround for issue #698
    // https://github.com/andpor/react-native-sqlite-storage/issues/364#issuecomment-526423153
    try {
      @SuppressLint("PrivateApi")
      Field field = CursorWindow.class.getDeclaredField("sCursorWindowSize");
      field.setAccessible(true);
      field.set(null, 10 * 1024 * 1024); // 10MB is the new size
    } catch (Exception e) {
      Log.e(TAG, "onCreate: " + e);
    }

    // WEB

    networkReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        Fragment navHostFragment = fragmentManager.findFragmentById(R.id.fragment_main_nav_host);
        assert navHostFragment != null;
        if (navHostFragment.getChildFragmentManager().getFragments().isEmpty()) {
          return;
        }
        getCurrentFragment().updateConnectivity(netUtil.isOnline());
      }
    };
    ContextCompat.registerReceiver(
        this,
        networkReceiver,
        new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION),
        ContextCompat.RECEIVER_EXPORTED
    );

    boolean useTor = sharedPrefs.getBoolean(NETWORK.TOR, SETTINGS_DEFAULT.NETWORK.TOR);
    if (useTor && !OrbotHelper.get(this).init()) {
      OrbotHelper.get(this).installOrbot(this);
    }

    // API
    updateGrocyApi();

    // VIEWS
    binding = ActivityMainBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    // NAVIGATION
    fragmentManager = getSupportFragmentManager();
    navUtil = new NavUtil(this, (controller, dest, args) -> {
      if (PrefsUtil.isServerUrlEmpty(sharedPrefs)) {
        binding.fabMain.hide();
      }
    }, sharedPrefs, TAG);
    navUtil.updateStartDestination();

    OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
      @Override
      public void handleOnBackPressed() {
        BaseFragment currentFragment = getCurrentFragment();
        if (currentFragment.isSearchVisible()) {
          currentFragment.dismissSearch();
        } else {
          boolean handled = currentFragment.onBackPressed();
          if (!handled) {
            setEnabled(false);
            getOnBackPressedDispatcher().onBackPressed();
            setEnabled(true);
          }
          if (!PrefsUtil.isServerUrlEmpty(sharedPrefs)) {
            binding.bottomAppBar.performShow();
          }
        }
        hideKeyboard();
      }
    };
    getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);

    // BOTTOM APP BAR

    uiUtil = new UiUtil(this);
    uiUtil.setUpBottomAppBar();
    updateBottomNavigationMenuButton();

    scrollBehavior = new BottomScrollBehavior(
        this,
        binding.bottomAppBar, binding.fabMain, binding.fabMainScroll,
        binding.anchor, binding.anchorMaxBottom
    );

    // UPDATE CONFIG | CHECK GROCY COMPATIBILITY
    if (!PrefsUtil.isServerUrlEmpty(sharedPrefs)) {
      ConfigUtil.loadInfo(
          new DownloadHelper(this, TAG),
          grocyApi,
          sharedPrefs,
          () -> VersionUtil.showCompatibilityBottomSheetIfNecessary(this, sharedPrefs),
          null
      );
    }

    if (VersionUtil.isAppUpdated(sharedPrefs)) {
      // Show changelog if app was updated
      VersionUtil.showChangelogBottomSheet(this);
      PrefsUtil.clearCachingRelatedSharedPreferences(sharedPrefs);
    } else {
      // Check if database scheme was updated and clear caching data if necessary
      AppDatabase.getAppDatabase(getApplication()).getVersion(version -> {
        if (VersionUtil.isDatabaseUpdated(sharedPrefs, version)) {
          PrefsUtil.clearCachingRelatedSharedPreferences(sharedPrefs);
        }
      });
    }
  }

  @Override
  protected void onDestroy() {
    if (networkReceiver != null) {
      unregisterReceiver(networkReceiver);
    }
    if (netUtil != null) {
      netUtil.closeWebSocketClient("fragment destroyed");
    }
    super.onDestroy();
  }

  @Override
  protected void onPause() {
    if (netUtil != null) {
      netUtil.cancelHassSessionTimer();
    }
    super.onPause();
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (runAsSuperClass) {
      return;
    }
    netUtil.createWebSocketClient();
    netUtil.resetHassSessionTimer();
    if (!sharedPrefs.contains(Constants.SETTINGS.BEHAVIOR.HAPTIC)) {
      hapticUtil.setEnabled(HapticUtil.areSystemHapticsTurnedOn(this));
    }
  }

  @Override
  protected void attachBaseContext(Context base) {
    if (runAsSuperClass) {
      super.attachBaseContext(base);
    } else {
      SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(base);
      // Night mode
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
  }

  public BottomScrollBehavior getScrollBehavior() {
    return scrollBehavior;
  }

  public void setSystemBarBehavior(SystemBarBehavior systemBarBehavior) {
    // IME ANIMATION
    uiUtil.setupImeAnimation(systemBarBehavior, scrollBehavior);
  }

  public void updateBottomAppBar(
      boolean showFab,
      @MenuRes int newMenuId,
      @Nullable OnMenuItemClickListener onMenuItemClickListener
  ) {
    uiUtil.updateBottomAppBar(sharedPrefs, showFab, newMenuId, onMenuItemClickListener);
  }

  public void updateBottomAppBar(boolean showFab, @MenuRes int newMenuId) {
    updateBottomAppBar(showFab, newMenuId, null);
  }

  public void updateFab(
      @DrawableRes int resId,
      @StringRes int tooltipStringId,
      String tag,
      boolean animated,
      Runnable onClick
  ) {
    updateFab(
        ContextCompat.getDrawable(this, resId),
        tooltipStringId,
        tag,
        animated,
        onClick,
        null
    );
  }

  public void updateFab(
      @DrawableRes int resId,
      @StringRes int tooltipStringId,
      String tag,
      boolean animated,
      Runnable onClick,
      Runnable onLongClick
  ) {
    updateFab(
        ContextCompat.getDrawable(this, resId),
        tooltipStringId,
        tag,
        animated,
        onClick,
        onLongClick
    );
  }

  public void updateFab(
      Drawable icon,
      @StringRes int tooltipStringId,
      String tag,
      boolean animated,
      Runnable onClick,
      Runnable onLongClick
  ) {
    replaceFabIcon(icon, tag, animated);
    binding.fabMain.setOnClickListener(v -> {
      Drawable drawable = binding.fabMain.getDrawable();
      if (drawable instanceof AnimationDrawable) {
        ViewUtil.startIcon(drawable);
      }
      onClick.run();
    });
    if (onLongClick != null) {
      binding.fabMain.setOnLongClickListener(v -> {
        Drawable drawable = binding.fabMain.getDrawable();
        if (drawable instanceof AnimationDrawable) {
          ViewUtil.startIcon(drawable);
        }
        onLongClick.run();
        return true;
      });
    } else {
      binding.fabMain.setOnLongClickListener(null);
      ViewUtil.setTooltipText(binding.fabMain, tooltipStringId);
    }
  }

  public void performOnBackPressed() {
    getOnBackPressedDispatcher().onBackPressed();
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    try {
      BaseFragment currentFragment = getCurrentFragment();
      return currentFragment.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    } catch (Exception e) {
      Log.e(TAG, "onKeyDown: fragmentManager or currentFragment is null");
      return false;
    }
  }

  @Override
  public boolean onKeyUp(int keyCode, KeyEvent event) {
    try {
      BaseFragment currentFragment = getCurrentFragment();
      return currentFragment.onKeyUp(keyCode, event) || super.onKeyUp(keyCode, event);
    } catch (Exception e) {
      Log.e(TAG, "onKeyUp: fragmentManager or currentFragment is null");
      return false;
    }
  }

  public boolean isOnline() {
    return netUtil.isOnline();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    getCurrentFragment().getActivityResult(requestCode, resultCode, data);
  }

  public Snackbar getSnackbar(String msg, boolean showLong) {
    return Snackbar.make(
        binding.coordinatorMain, msg, showLong ? Snackbar.LENGTH_LONG : Snackbar.LENGTH_SHORT
    );
  }

  public Snackbar getSnackbar(@StringRes int resId, boolean showLong) {
    return getSnackbar(getString(resId), showLong);
  }

  public void showSnackbar(Snackbar snackbar) {
    snackbar.setAnchorView(binding.anchor);
    snackbar.setAnchorViewLayoutListenerEnabled(true);
    snackbar.show();
  }

  public void showSnackbar(String msg, boolean showLong) {
    showSnackbar(getSnackbar(msg, showLong));
  }

  public void showSnackbar(@StringRes int resId, boolean showLong) {
    showSnackbar(getSnackbar(resId, showLong));
  }

  public void showToast(@StringRes int resId, boolean showLong) {
    Toast toast = Toast.makeText(
        this, resId, showLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT
    );
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
      LinearLayout linearLayout = (LinearLayout) toast.getView();
      if (linearLayout != null) {
        TextView textView = (TextView) linearLayout.getChildAt(0);
        if (textView != null) {
          textView.setTypeface(ResourcesCompat.getFont(this, R.font.jost_book));
        }
      }
    }
    toast.show();
  }

  public void showBottomSheet(BottomSheetDialogFragment bottomSheet) {
    String tag = bottomSheet.toString();
    bottomSheet.show(fragmentManager, tag);
    if (debug) {
      Log.i(TAG, "showBottomSheet: " + bottomSheet);
    }
  }

  public void showBottomSheet(BottomSheetDialogFragment bottomSheet, Bundle bundle) {
    bottomSheet.setArguments(bundle);
    showBottomSheet(bottomSheet);
  }

  public void showTextBottomSheet(@RawRes int file, @StringRes int title, @StringRes int link) {
    NavigationMainDirections.ActionGlobalTextDialog action
        = NavigationMainDirections.actionGlobalTextDialog();
    action.setTitle(title);
    action.setFile(file);
    if (link != 0) {
      action.setLink(link);
    }
    navUtil.navigate(action);
  }

  public void showHelpBottomSheet() {
    showTextBottomSheet(R.raw.help, R.string.title_help, 0);
  }

  public void showFeedbackBottomSheet() {
    showBottomSheet(new FeedbackBottomSheet());
  }

  public SharedPreferences getSharedPrefs() {
    if (sharedPrefs != null) {
      return sharedPrefs;
    } else {
      return PreferenceManager.getDefaultSharedPreferences(this);
    }
  }

  public void showKeyboard(EditText editText) {
    editText.requestFocus();
    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    new Handler().postDelayed(
        () -> imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT),
        50
    );
  }

  public void hideKeyboard() {
    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.hideSoftInputFromWindow(findViewById(android.R.id.content).getWindowToken(), 0);
  }

  public GrocyApi getGrocyApi() {
    return grocyApi;
  }

  public void updateGrocyApi() {
    grocyApi = new GrocyApi(getApplication());
  }

  @NonNull
  public BaseFragment getCurrentFragment() {
    return navUtil.getCurrentFragment();
  }

  private void replaceFabIcon(Drawable icon, String tag, boolean animated) {
    if (!tag.equals(binding.fabMain.getTag())) {
      if (animated) {
        int duration = 400;
        ValueAnimator animOut = ValueAnimator.ofInt(binding.fabMain.getImageAlpha(), 0);
        animOut.addUpdateListener(
            animation -> binding.fabMain.setImageAlpha(
                (int) animation.getAnimatedValue()
            )
        );
        animOut.setDuration(duration / 2);
        animOut.setInterpolator(new FastOutSlowInInterpolator());
        animOut.addListener(new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(Animator animation) {
            binding.fabMain.setImageDrawable(icon);
            binding.fabMain.setTag(tag);
            ValueAnimator animIn = ValueAnimator.ofInt(0, 255);
            animIn.addUpdateListener(
                anim -> binding.fabMain.setImageAlpha(
                    (int) (anim.getAnimatedValue())
                )
            );
            animIn.setDuration(duration / 2);
            animIn.setInterpolator(new FastOutSlowInInterpolator());
            animIn.start();
          }
        });
        animOut.start();
      } else {
        binding.fabMain.setImageDrawable(icon);
        binding.fabMain.setTag(tag);
      }
    } else {
      if (debug) {
        Log.i(TAG, "replaceFabIcon: not replaced, tags are identical");
      }
    }
  }

  public void updateBottomNavigationMenuButton() {
    if (sharedPrefs.getBoolean(BEHAVIOR.SHOW_MAIN_MENU_BUTTON,
        SETTINGS_DEFAULT.BEHAVIOR.SHOW_MAIN_MENU_BUTTON)) {
      binding.bottomAppBar.setNavigationIcon(
          AppCompatResources.getDrawable(this, R.drawable.ic_round_menu_anim)
      );
      binding.bottomAppBar.setNavigationOnClickListener(v -> {
        if (clickUtil.isDisabled()) {
          return;
        }
        ViewUtil.startIcon(binding.bottomAppBar.getNavigationIcon());
        navUtil.navigate(NavigationMainDirections.actionGlobalDrawerBottomSheetDialogFragment());
      });
    } else {
      binding.bottomAppBar.setNavigationIcon(null);
    }
  }

  public boolean hasBottomNavigationIcon() {
    return binding.bottomAppBar.getNavigationIcon() != null;
  }

  public void startIconAnimation(View view, boolean hasFocus) {
    if (!hasFocus) {
      return;
    }
    ViewUtil.startIcon(view);
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent event) {
    boolean dispatch = getCurrentFragment().dispatchTouchEvent(event);
    return dispatch || super.dispatchTouchEvent(event);
  }


  public void setHapticEnabled(boolean enabled) {
    hapticUtil.setEnabled(enabled);
  }

  public void saveInstanceState(Bundle outState) {
    onSaveInstanceState(outState);
  }
}