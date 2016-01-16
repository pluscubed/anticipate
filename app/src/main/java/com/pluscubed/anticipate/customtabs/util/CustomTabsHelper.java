// Copyright 2015 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.pluscubed.anticipate.customtabs.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;

import com.pluscubed.anticipate.filter.AppInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for Custom Tabs.
 */
public class CustomTabsHelper {
    public static final String STABLE_PACKAGE = "com.android.chrome";
    public static final String BETA_PACKAGE = "com.chrome.beta";
    public static final String DEV_PACKAGE = "com.chrome.dev";
    public static final String LOCAL_PACKAGE = "com.google.android.apps.chrome";

    private static final String TAG = "CustomTabsHelper";
    private static final String EXTRA_CUSTOM_TABS_KEEP_ALIVE =
            "android.support.customtabs.extra.KEEP_ALIVE";
    private static final String ACTION_CUSTOM_TABS_CONNECTION =
            "android.support.customtabs.action.CustomTabsService";

    private CustomTabsHelper() {
    }

    public static void addKeepAliveExtra(Context context, Intent intent) {
        Intent keepAliveIntent = new Intent().setClassName(
                context.getPackageName(), KeepAliveService.class.getCanonicalName());
        intent.putExtra(EXTRA_CUSTOM_TABS_KEEP_ALIVE, keepAliveIntent);
    }

    public static List<String> getCustomTabsSupportedPackages(Context context) {
        PackageManager pm = context.getPackageManager();
        // Get default VIEW intent handler.
        Intent activityIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.example.com"));

        // Get all apps that can handle VIEW intents.
        List<ResolveInfo> resolvedActivityList;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            resolvedActivityList = pm.queryIntentActivities(activityIntent, PackageManager.MATCH_ALL);
        } else {
            resolvedActivityList = pm.queryIntentActivities(activityIntent, PackageManager.MATCH_DEFAULT_ONLY);
        }
        List<String> packagesSupportingCustomTabs = new ArrayList<>();
        for (ResolveInfo info : resolvedActivityList) {
            Intent serviceIntent = new Intent();
            serviceIntent.setAction(ACTION_CUSTOM_TABS_CONNECTION);
            serviceIntent.setPackage(info.activityInfo.packageName);
            if (pm.resolveService(serviceIntent, 0) != null) {
                packagesSupportingCustomTabs.add(info.activityInfo.packageName);
            }
        }

        return packagesSupportingCustomTabs;
    }

    public static String getDefaultPackageFromAppInfos(List<AppInfo> list) {
        List<String> packageNames = new ArrayList<>();
        for (AppInfo info : list) {
            packageNames.add(info.packageName);
        }
        return getDefaultPackage(packageNames);
    }

    public static String getDefaultPackage(List<String> packageNames) {
        if (packageNames.contains(STABLE_PACKAGE)) {
            return STABLE_PACKAGE;
        } else if (packageNames.contains(BETA_PACKAGE)) {
            return BETA_PACKAGE;
        } else if (packageNames.contains(DEV_PACKAGE)) {
            return DEV_PACKAGE;
        } else if (packageNames.contains(LOCAL_PACKAGE)) {
            return LOCAL_PACKAGE;
        } else if (packageNames.size() > 0) {
            return packageNames.get(0);
        } else {
            return "";
        }
    }


    /**
     * @return All possible chrome package names that provide custom tabs feature.
     */
    public static String[] getPackages() {
        return new String[]{"", STABLE_PACKAGE, BETA_PACKAGE, DEV_PACKAGE, LOCAL_PACKAGE};
    }
}
