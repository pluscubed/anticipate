package com.pluscubed.anticipate.filter;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;

import com.afollestad.inquiry.Inquiry;
import com.pluscubed.anticipate.customtabs.util.CustomTabsHelper;
import com.pluscubed.anticipate.util.PrefUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import rx.Observable;
import rx.Single;
import rx.SingleSubscriber;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class DbUtil {

    public static final String TABLE_BLACKLISTED_APPS = "BlacklistedApps";
    public static final String TABLE_WHITELISTED_APPS = "WhitelistedApps";

    static final String[] DEFAULT_BLACKLISTED_APPS =
            {"com.android.systemui", "com.google.android.googlequicksearchbox",
                    "com.teslacoilsw.launcher", "com.actionlauncher.playstore",
                    CustomTabsHelper.STABLE_PACKAGE, CustomTabsHelper.BETA_PACKAGE,
                    CustomTabsHelper.DEV_PACKAGE, CustomTabsHelper.LOCAL_PACKAGE};

    public static void initializeBlacklist(Context context) {
        if (PrefUtils.isFirstRun(context)) {

            AppInfo[] defaultBlacklistApps = new AppInfo[DEFAULT_BLACKLISTED_APPS.length];
            for (int i = 0; i < DEFAULT_BLACKLISTED_APPS.length; i++) {
                String packageName = DEFAULT_BLACKLISTED_APPS[i];
                AppInfo appInfo = new AppInfo();
                appInfo.packageName = packageName;
                defaultBlacklistApps[i] = appInfo;
            }

            Inquiry.get().insertInto(TABLE_BLACKLISTED_APPS, AppInfo.class)
                    .values(defaultBlacklistApps)
                    .run();

            PrefUtils.setFirstRun(context, false);
        }
    }

    /**
     * Returns sorted list of AppInfos.
     * <p/>
     * AppInfo's drawable and id are null
     */
    @NonNull
    public static Single<List<AppInfo>> getPerAppListApps(final Context context) {
        return Single.create(new Single.OnSubscribe<List<AppInfo>>() {
            @Override
            public void call(SingleSubscriber<? super List<AppInfo>> singleSubscriber) {
                String table = PrefUtils.isBlacklistMode(context) ? TABLE_BLACKLISTED_APPS : TABLE_WHITELISTED_APPS;

                AppInfo[] all = Inquiry.get().selectFrom(table, AppInfo.class)
                        .all();
                if (all != null) {
                    singleSubscriber.onSuccess(new ArrayList<>(Arrays.asList(all)));
                } else {
                    singleSubscriber.onSuccess(new ArrayList<AppInfo>());
                }
            }
        }).subscribeOn(Schedulers.io())
                .flatMapObservable(new Func1<List<AppInfo>, Observable<AppInfo>>() {
                    @Override
                    public Observable<AppInfo> call(List<AppInfo> appInfos) {
                        return Observable.from(appInfos);
                    }
                }).flatMap(new Func1<AppInfo, Observable<AppInfo>>() {
                    @Override
                    public Observable<AppInfo> call(AppInfo appInfo) {
                        try {
                            final ApplicationInfo info = context.getPackageManager().getApplicationInfo(appInfo.packageName, 0);
                            appInfo.name = info.loadLabel(context.getPackageManager()).toString();

                            return Observable.just(appInfo);
                        } catch (PackageManager.NameNotFoundException e) {
                            e.printStackTrace();
                            return Observable.empty();
                        }
                    }
                }).toSortedList().toSingle();
    }

    /**
     * Returns sorted list of AppInfos.
     * <p/>
     * AppInfo's drawable and id are null
     */
    public static Single<List<AppInfo>> getInstalledApps(final Context context) {
        return Single.create(new Single.OnSubscribe<List<ApplicationInfo>>() {
            @Override
            public void call(SingleSubscriber<? super List<ApplicationInfo>> singleSubscriber) {
                singleSubscriber.onSuccess(context.getPackageManager().getInstalledApplications(0));
            }
        }).subscribeOn(Schedulers.io())
                .flatMapObservable(new Func1<List<ApplicationInfo>, Observable<ApplicationInfo>>() {
                    @Override
                    public Observable<ApplicationInfo> call(List<ApplicationInfo> appInfos) {
                        return Observable.from(appInfos);
                    }
                })
                .map(new Func1<ApplicationInfo, AppInfo>() {
                    @Override
                    public AppInfo call(ApplicationInfo applicationInfo) {
                        AppInfo appInfo = new AppInfo();
                        appInfo.packageName = applicationInfo.packageName;
                        appInfo.name = applicationInfo.loadLabel(context.getPackageManager()).toString();
                        return appInfo;
                    }
                })
                .toSortedList().toSingle();
    }

    static void insertApp(AppInfo appInfo, String tableName) {
        Inquiry.get()
                .insertInto(tableName, AppInfo.class)
                .values(appInfo)
                .run();
    }

    static void deleteApp(AppInfo appInfo, String tableName) {
        Inquiry.get()
                .deleteFrom(tableName, AppInfo.class)
                .where("package_name = ?", appInfo.packageName)
                .run();
    }
}
