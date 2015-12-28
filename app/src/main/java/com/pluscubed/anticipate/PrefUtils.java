package com.pluscubed.anticipate;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PrefUtils {
    public static final String PREF_BLACKLIST = "pref_blacklist";

    public static void setBlacklistMode(Context context, boolean blacklist) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putBoolean(PREF_BLACKLIST, blacklist).apply();
    }

    public static boolean isBlacklistMode(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean(PREF_BLACKLIST, true);
    }

}
