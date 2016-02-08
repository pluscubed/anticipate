package com.pluscubed.anticipate;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.customtabs.CustomTabsCallback;
import android.support.customtabs.CustomTabsService;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.pluscubed.anticipate.customtabs.util.CustomTabConnectionHelper;
import com.pluscubed.anticipate.filter.AppInfo;
import com.pluscubed.anticipate.filter.DbUtil;
import com.pluscubed.anticipate.quickswitch.QuickSwitchService;
import com.pluscubed.anticipate.util.LimitedQueue;
import com.pluscubed.anticipate.util.PrefUtils;
import com.pluscubed.anticipate.util.Utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rx.Observable;
import rx.SingleSubscriber;
import rx.functions.Func1;

public class MainAccessibilityService extends AccessibilityService {

    public static final String TAG = "Accesibility";
    public static final String LINK_REG_EX = "((?:[a-z][\\w-]+:(?:\\/{1,3}|[a-z0-9%])|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}/)(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\))+(?:\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:'\".,<>?«»“”‘’]))";

    private static MainAccessibilityService sSharedService;
    List<String> mFilterList;
    boolean mBlacklistMode;
    LimitedQueue<AppUsageEntry> mAppsUsed;

    Handler mMainHandler;
    AccessibilityServiceInfo mServiceInfo;
    boolean mPendingPageLoadStart;
    boolean mPendingLoadFoundApp;
    private CustomTabConnectionHelper mCustomTabConnectionHelper;

    public static MainAccessibilityService get() {
        return sSharedService;
    }

    public static void updateFilterList() {
        if (MainAccessibilityService.get() != null) {
            MainAccessibilityService.get().updateBlackWhitelistInternal();
        }
    }

    static void log(String info) {
        if (BuildConfig.DEBUG)
            Log.i(TAG, info);
    }

    public CustomTabConnectionHelper getCustomTabConnectionHelper() {
        return mCustomTabConnectionHelper;
    }

    public void pendLoadStart() {
        mPendingPageLoadStart = true;
        mServiceInfo.notificationTimeout = 50;
        setServiceInfo(mServiceInfo);
    }

    public void cancelPendingLoad() {
        mPendingPageLoadStart = false;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        sSharedService = this;

        mMainHandler = new Handler(getMainLooper());

        mAppsUsed = new LimitedQueue<>(10);

        mCustomTabConnectionHelper = new CustomTabConnectionHelper();
        mCustomTabConnectionHelper.setCustomTabsCallback(new TabsNavigationCallback());
        mCustomTabConnectionHelper.bindCustomTabsService(this);

        DbUtil.initializeBlacklist(this);
        updateFilterList();

        Utils.notifyChangelog(this);

        mServiceInfo = getServiceInfo();
    }


    @Override
    public boolean onUnbind(Intent intent) {
        sSharedService = null;
        mCustomTabConnectionHelper.unbindCustomTabsService(this);

        return super.onUnbind(intent);
    }

    public int getLastAppPrimaryColor() {
        //http://stackoverflow.com/questions/27121919/is-it-possible-to-get-another-applications-primary-color-like-lollipops-recent

        int defaultToolbarColor = PrefUtils.getDefaultToolbarColor(this);

        PackageManager pm = getPackageManager();

        String firstViablePackageName = null;
        int[] attrs = null;
        Resources res = null;

        for (Iterator<AppUsageEntry> iterator = mAppsUsed.descendingIterator(); iterator.hasNext(); ) {
            AppUsageEntry entry = iterator.next();

            if (!entry.packageName.equals(PrefUtils.getChromeApp(MainAccessibilityService.this)) &&
                    !entry.packageName.startsWith("com.android.systemui") &&
                    !entry.packageName.startsWith("android")) {

                if (firstViablePackageName == null) {
                    firstViablePackageName = entry.packageName;

                    // Retrieve the Resources from the app
                    try {
                        res = pm.getResourcesForApplication(firstViablePackageName);
                    } catch (PackageManager.NameNotFoundException e) {
                        return defaultToolbarColor;
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        attrs = new int[]{
                                res.getIdentifier("colorPrimary", "attr", firstViablePackageName),
                                android.R.attr.colorPrimary
                        };
                    } else {
                        attrs = new int[]{
                                res.getIdentifier("colorPrimary", "attr", firstViablePackageName)
                        };
                    }

                }

                try {
                    if (firstViablePackageName != null && firstViablePackageName.equals(entry.packageName)) {
                        final Resources.Theme theme = res.newTheme();
                        ComponentName cn = ComponentName.unflattenFromString(firstViablePackageName + "/" + entry.activityName);
                        theme.applyStyle(pm.getActivityInfo(cn, 0).theme, false);

                        // Obtain the colorPrimary color from the attrs
                        TypedArray a = theme.obtainStyledAttributes(attrs);
                        int fallback = defaultToolbarColor;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            fallback = a.getColor(1, defaultToolbarColor);
                        }
                        int colorPrimary = a.getColor(0, fallback);
                        a.recycle();

                        if (colorPrimary == ContextCompat.getColor(this, R.color.primary_material_light)
                                || colorPrimary == ContextCompat.getColor(this, R.color.primary_material_dark)) {
                            //Assume the primary color was left as default, therefore use user default color
                            colorPrimary = defaultToolbarColor;
                        }

                        return colorPrimary;
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    //ignore, continue
                }
            }
        }

        return defaultToolbarColor;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        sSharedService = this;

        log("====onAccessibilityEvent===");
        log("=type: " + AccessibilityEvent.eventTypeToString(event.getEventType()));

        if (mFilterList == null) {
            return;
        }

        CharSequence packageName = event.getPackageName();

        if (packageName != null) {
            String appId = packageName.toString();
            log("=appId: " + appId);

            if (!mPendingLoadFoundApp && mPendingPageLoadStart && appId.equals(PrefUtils.getChromeApp(this))) {
                mPendingLoadFoundApp = true;
                mMainHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mPendingPageLoadStart) {
                            onPageLoadStarted();

                            if (BuildConfig.DEBUG) {
                                Toast.makeText(MainAccessibilityService.this, "accessibility detected Chrome - load started", Toast.LENGTH_LONG).show();
                            }
                        }
                        mPendingLoadFoundApp = false;
                    }
                }, 200);
            }

            CharSequence className = event.getClassName();
            if (className != null && event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                String activityName = className.toString();
                log("=activityName: " + className);

                boolean redundant = false;
                if (mAppsUsed.size() > 0) {
                    String lastPackageName = mAppsUsed.peekLast().packageName;
                    redundant = appId.equals(lastPackageName);
                }
                if (!redundant) {
                    AppUsageEntry entry = new AppUsageEntry();
                    entry.activityName = activityName;
                    entry.packageName = appId;

                    mAppsUsed.add(entry);
                }
            }


            if ((mBlacklistMode && mFilterList.contains(appId)) ||
                    (!mBlacklistMode && !mFilterList.contains(appId))) {
                log("=excluded");
                return;
            }
        }

        String allText = "";
        try {
            allText = getAllText(getRootInActiveWindow(), 0);
        } catch (SecurityException ignored) {
        }


        Log.v(TAG, allText);

        Pattern pattern = Pattern.compile(LINK_REG_EX);
        Matcher matcher = pattern.matcher(allText);


        Uri firstUrl = null;
        String floatingWindowText = "";
        List<Bundle> possibleUrls = new ArrayList<>();

        while (matcher.find()) {
            String url = matcher.group(0);

            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://" + url;
            }

            Uri uri = Uri.parse(url);

            if (firstUrl == null) {
                firstUrl = uri;

                floatingWindowText += uri;
            } else {
                Bundle bundle = new Bundle();
                bundle.putParcelable(CustomTabsService.KEY_URL, uri);
                possibleUrls.add(bundle);

                floatingWindowText += "\n" + uri;
            }

            log("Preload URL: " + url);
        }


        if (firstUrl != null) {
            boolean success = mCustomTabConnectionHelper.mayLaunchUrl(firstUrl, null, possibleUrls);

            if (FloatingMonitorService.get() != null) {
                FloatingMonitorService.get().setText(floatingWindowText);
            }

            log("Preload URL: " + success);
        }


    }

    void updateBlackWhitelistInternal() {
        DbUtil.getPerAppListApps(this).flatMapObservable(new Func1<List<AppInfo>, Observable<AppInfo>>() {
            @Override
            public Observable<AppInfo> call(List<AppInfo> appInfos) {
                return Observable.from(appInfos);
            }
        }).map(new Func1<AppInfo, String>() {
            @Override
            public String call(AppInfo appInfo) {
                return appInfo.packageName;
            }
        }).toList().toSingle()
                .subscribe(new SingleSubscriber<List<String>>() {

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onSuccess(List<String> strings) {
                        mFilterList = strings;
                        mBlacklistMode = PrefUtils.isBlacklistMode(MainAccessibilityService.this);
                    }
                });


    }

    private String getAllText(AccessibilityNodeInfo source, int depth) {
        if (depth > 50) {
            return "";
        }

        String string = "";

        if (source == null) {
            return string;
        }

        if (source.getText() != null) {
            String text = source.getText().toString();
            text = text.replace("\n", " ");
            string += text + " ";

            log("Text: " + text);
        }

        for (int i = 0; i < source.getChildCount(); i++) {
            try {
                string += getAllText(source.getChild(i), depth + 1);
            } catch (SecurityException ignored) {
            }
        }

        source.recycle();

        return string;
    }

    @Override
    public void onInterrupt() {

    }

    void onPageLoadStarted() {
        if (mPendingPageLoadStart) {
            BrowserLauncherActivity.moveInstanceToBack();
            mPendingPageLoadStart = false;
            mPendingLoadFoundApp = false;

            mServiceInfo.notificationTimeout = 300;
            setServiceInfo(mServiceInfo);
        }
    }

    private class AppUsageEntry {
        String packageName;
        String activityName;

        AppUsageEntry() {
        }
    }

    private class TabsNavigationCallback extends CustomTabsCallback {
        TabsNavigationCallback() {
        }

        @Override
        public void onNavigationEvent(int navigationEvent, Bundle extras) {
            super.onNavigationEvent(navigationEvent, extras);

            switch (navigationEvent) {
                case TAB_SHOWN:
                    break;
                case TAB_HIDDEN:
                    Intent checkBubblesIntent = new Intent(MainAccessibilityService.this, QuickSwitchService.class);
                    checkBubblesIntent.putExtra(QuickSwitchService.EXTRA_CHECK_BUBBLES, true);
                    startService(checkBubblesIntent);

                    break;
                case NAVIGATION_FINISHED:
                    Intent intent = new Intent(MainAccessibilityService.this, QuickSwitchService.class);
                    intent.putExtra(QuickSwitchService.EXTRA_FINISH_LOADING, true);
                    startService(intent);

                    break;
                case NAVIGATION_STARTED:
                    onPageLoadStarted();
                    break;
            }
        }
    }
}
