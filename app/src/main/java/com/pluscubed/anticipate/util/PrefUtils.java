package com.pluscubed.anticipate.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;

import com.pluscubed.anticipate.BuildConfig;
import com.pluscubed.anticipate.R;

public abstract class PrefUtils {
    public static final String PREF_BLACKLIST = "pref_blacklist";
    public static final String PREF_FIRSTRUN = "pref_firstrun";

    public static final String PREF_DYNAMIC_TOOLBAR = "pref_dynamic_toolbar";
    public static final String PREF_DEFAULT_COLOR = "pref_default_toolbar_color";
    public static final String PREF_ANIMATION_STYLE = "pref_animation_style";
    public static final String PREF_VERSION_CODE = "pref_version";
    public static final String PREF_CHROME_APP = "pref_chrome_app";


    public static final String PREF_ACCESSIBILITY_OFF_WARNED = "pref_warned";

    private static SharedPreferences.Editor edit(Context context) {
        return getSharedPreferences(context).edit();
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }


    public static void setBlacklistMode(Context context, boolean blacklist) {
        edit(context).putBoolean(PREF_BLACKLIST, blacklist).apply();
    }

    public static boolean isBlacklistMode(Context context) {
        return getSharedPreferences(context).getBoolean(PREF_BLACKLIST, true);
    }

    public static void setFirstRun(Context context, boolean firstRun) {
        edit(context).putBoolean(PREF_FIRSTRUN, firstRun).apply();
    }

    public static boolean isFirstRun(Context context) {
        return getSharedPreferences(context).getBoolean(PREF_FIRSTRUN, true);
    }

    public static void setDynamicToolbar(Context context, boolean dynamicToolbar) {
        edit(context).putBoolean(PREF_DYNAMIC_TOOLBAR, dynamicToolbar).apply();
    }

    public static boolean isDynamicToolbar(Context context) {
        return getSharedPreferences(context).getBoolean(PREF_DYNAMIC_TOOLBAR, true);
    }

    public static void setDefaultToolbarColor(Context context, @ColorInt int defaultToolbarColor) {
        edit(context).putInt(PREF_DEFAULT_COLOR, defaultToolbarColor).apply();
    }

    @ColorInt
    public static int getDefaultToolbarColor(Context context) {
        return getSharedPreferences(context).getInt(PREF_DEFAULT_COLOR, ContextCompat.getColor(context, R.color.colorPrimary));
    }

    public static void setAnimationStyle(final Context context, final int animationStyle) {
        edit(context).putInt(PREF_ANIMATION_STYLE, animationStyle).apply();
    }

    public static int getAnimationStyle(final Context context) {
        return getSharedPreferences(context).getInt(PREF_ANIMATION_STYLE, 2);
    }

    public static void setVersionCode(Context context, int versionCode) {
        edit(context).putInt(PREF_VERSION_CODE, versionCode).apply();
    }

    @ColorInt
    public static int getVersionCode(Context context) {
        int versionCode = getSharedPreferences(context).getInt(PREF_VERSION_CODE, -1);
        if (versionCode == -1) {
            setVersionCode(context, BuildConfig.VERSION_CODE);
            versionCode = BuildConfig.VERSION_CODE;
        }
        return versionCode;
    }

    public static void setAccessibilityOffWarned(Context context) {
        edit(context).putBoolean(PREF_ACCESSIBILITY_OFF_WARNED, true).apply();
    }

    public static boolean isAccessibilityOffWarned(Context context) {
        return getSharedPreferences(context).getBoolean(PREF_ACCESSIBILITY_OFF_WARNED, false);
    }

    public static void setChromeApp(Context context, String packageName) {
        edit(context).putString(PREF_CHROME_APP, packageName).apply();
    }

    public static String getChromeApp(Context context) {
        return getSharedPreferences(context).getString(PREF_CHROME_APP, "");
    }
}
