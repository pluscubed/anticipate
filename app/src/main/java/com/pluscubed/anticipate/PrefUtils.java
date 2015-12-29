package com.pluscubed.anticipate;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.afollestad.inquiry.Inquiry;
import com.pluscubed.anticipate.customtabsshared.CustomTabsHelper;

public class PrefUtils {
    public static final String PREF_BLACKLIST = "pref_blacklist";
    public static final String PREF_FIRSTRUN = "pref_firstrun";

    static final String[] DEFAULT_BLACKLISTED_APPS =
            {"com.android.systemui", "com.google.android.googlequicksearchbox",
                    CustomTabsHelper.STABLE_PACKAGE, CustomTabsHelper.BETA_PACKAGE,
                    CustomTabsHelper.DEV_PACKAGE, CustomTabsHelper.LOCAL_PACKAGE};

    public static void initialize(Context context) {
        if (isFirstRun(context)) {

            AppPackage[] defaultBlacklistApps = new AppPackage[DEFAULT_BLACKLISTED_APPS.length];
            for (int i = 0; i < DEFAULT_BLACKLISTED_APPS.length; i++) {
                String packageName = DEFAULT_BLACKLISTED_APPS[i];
                AppPackage appPackage = new AppPackage();
                appPackage.package_name = packageName;
                defaultBlacklistApps[i] = appPackage;
            }

            Long[] ids = Inquiry.get().insertInto(PerAppListActivity.TABLE_BLACKLISTED_APPS, AppPackage.class)
                    .values(defaultBlacklistApps)
                    .run();

            for (int i = 0; i < ids.length; i++) {
                defaultBlacklistApps[i].id = ids[i];
            }

            setFirstRun(context, false);
        }
    }

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

}
