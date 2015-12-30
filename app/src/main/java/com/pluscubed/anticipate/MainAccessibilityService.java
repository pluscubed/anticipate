package com.pluscubed.anticipate;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.customtabs.CustomTabsCallback;
import android.support.customtabs.CustomTabsService;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.pluscubed.anticipate.customtabs.util.CustomTabActivityHelper;
import com.pluscubed.anticipate.perapp.AppInfo;
import com.pluscubed.anticipate.perapp.DbUtil;
import com.pluscubed.anticipate.util.PrefUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rx.Observable;
import rx.functions.Func1;

public class MainAccessibilityService extends AccessibilityService {

    public static final String TAG = "Accesibility";
    public static final String LINK_REG_EX = "((?:[a-z][\\w-]+:(?:\\/{1,3}|[a-z0-9%])|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}/)(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\))+(?:\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:'\".,<>?«»“”‘’]))";

    private static MainAccessibilityService sSharedService;
    private CustomTabActivityHelper mCustomTabActivityHelper;

    private List<String> mPerAppList;
    private boolean mBlacklistMode;

    public static MainAccessibilityService get() {
        return sSharedService;
    }

    public CustomTabActivityHelper getCustomTabActivityHelper() {
        return mCustomTabActivityHelper;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        sSharedService = this;

        mCustomTabActivityHelper = new CustomTabActivityHelper();
        mCustomTabActivityHelper.setConnectionCallback(null);
        mCustomTabActivityHelper.bindCustomTabsService(this, new CustomTabsCallback() {
            @Override
            public void onNavigationEvent(int navigationEvent, Bundle extras) {
                super.onNavigationEvent(navigationEvent, extras);
            }

            @Override
            public void extraCallback(String callbackName, Bundle args) {
                super.extraCallback(callbackName, args);
            }
        });

        DbUtil.initializeBlacklist(this);

        updateBlackWhitelist();

    }

    @Override
    public boolean onUnbind(Intent intent) {
        sSharedService = null;
        mCustomTabActivityHelper.unbindCustomTabsService(this);

        return super.onUnbind(intent);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        sSharedService = this;

        if (BuildConfig.DEBUG) {
            Log.i(TAG, "====onAccessibilityEvent===");
            Log.i(TAG, "=type: " + AccessibilityEvent.eventTypeToString(event.getEventType()));
        }


        CharSequence packageName = event.getPackageName();

        if (packageName != null) {
            String appId = packageName.toString();

            if (BuildConfig.DEBUG)
                Log.i(TAG, "=appId: " + appId);

            if ((mBlacklistMode && mPerAppList.contains(appId)) ||
                    (!mBlacklistMode && !mPerAppList.contains(appId))) {
                if (BuildConfig.DEBUG)
                    Log.i(TAG, "=excluded");
                return;
            }
        }


        String allText = getAllText(getRootInActiveWindow());

        Pattern pattern = Pattern.compile(LINK_REG_EX);
        Matcher matcher = pattern.matcher(allText);


        Uri top = null;
        String list = "";
        List<Bundle> possibleUrls = new ArrayList<>();

        while (matcher.find()) {
            String url = matcher.group(0);

            Uri uri = Uri.parse(url);

            if (top == null) {
                top = uri;

                list += uri;
            } else {
                Bundle bundle = new Bundle();
                bundle.putParcelable(CustomTabsService.KEY_URL, uri);

                list += "\n" + uri;
            }

            if (BuildConfig.DEBUG)
                Log.i(TAG, "Preload URL: " + url);
        }


        if (top != null) {
            boolean success = mCustomTabActivityHelper.mayLaunchUrl(top, null, possibleUrls);

            if (success && FloatingWindowService.get() != null) {
                FloatingWindowService.get().setText(list);
            }

            if (BuildConfig.DEBUG)
                Log.i(TAG, "Preload URL: " + success);
        }


    }

    public void updateBlackWhitelist() {
        mPerAppList = DbUtil.getPerAppListApps(this).flatMapObservable(new Func1<List<AppInfo>, Observable<AppInfo>>() {
            @Override
            public Observable<AppInfo> call(List<AppInfo> appInfos) {
                return Observable.from(appInfos);
            }
        }).map(new Func1<AppInfo, String>() {
            @Override
            public String call(AppInfo appInfo) {
                return appInfo.packageName;
            }
        }).toList().toBlocking().first();

        mBlacklistMode = PrefUtils.isBlacklistMode(this);
    }

    private String getAllText(AccessibilityNodeInfo source) {
        String string = "";

        if (source == null) {
            return string;
        }

        if (source.getText() != null) {
            String text = source.getText().toString();

            text = text.replace("\n", " ");

            string += text + " ";

            if (BuildConfig.DEBUG)
                Log.i(TAG, "Text: " + text);
        }

        for (int i = 0; i < source.getChildCount(); i++) {
            string += getAllText(source.getChild(i));
        }

        source.recycle();

        return string;
    }

    @Override
    public void onInterrupt() {

    }

}
