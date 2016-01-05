package com.pluscubed.anticipate.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;

import com.pluscubed.anticipate.R;

public class PrefUtils {
    public static final String PREF_BLACKLIST = "pref_blacklist";
    public static final String PREF_FIRSTRUN = "pref_firstrun";

    public static final String PREF_DYNAMIC_TOOLBAR = "pref_dynamic_toolbar";
    public static final String PREF_DEFAULT_COLOR = "pref_default_toolbar_color";


    public static void setBlacklistMode(Context context, boolean blacklist) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putBoolean(PREF_BLACKLIST, blacklist).apply();
    }

    public static boolean isBlacklistMode(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean(PREF_BLACKLIST, true);
    }

    public static void setFirstRun(Context context, boolean firstRun) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putBoolean(PREF_FIRSTRUN, firstRun).apply();
    }

    public static boolean isFirstRun(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean(PREF_FIRSTRUN, true);
    }

    public static void setDynamicToolbar(Context context, boolean dynamicToolbar) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putBoolean(PREF_DYNAMIC_TOOLBAR, dynamicToolbar).apply();
    }

    public static boolean isDynamicToolbar(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean(PREF_DYNAMIC_TOOLBAR, true);
    }

    public static void setDefaultToolbarColor(Context context, @ColorInt int defaultToolbarColor) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putInt(PREF_DEFAULT_COLOR, defaultToolbarColor).apply();
    }

    @ColorInt
    public static int getDefaultToolbarColor(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getInt(PREF_DEFAULT_COLOR, ContextCompat.getColor(context, R.color.colorPrimary));
    }
}
