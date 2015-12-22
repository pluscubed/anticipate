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

import com.pluscubed.anticipate.customtabsshared.CustomTabsHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainAccessibilityService extends AccessibilityService {

    public static final String TAG = "Accesibility";
    public static final String LINK_REG_EX = "((?:[a-z][\\w-]+:(?:\\/{1,3}|[a-z0-9%])|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}/)(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\))+(?:\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:'\".,<>?«»“”‘’]))";

    private static final int URL_PRELOAD_TIMEOUT = 10000;
    private static MainAccessibilityService sSharedService;
    private CustomTabActivityHelper mCustomTabActivityHelper;

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

        });
    }

    @Override
    public boolean onUnbind(Intent intent) {
        sSharedService = null;
        mCustomTabActivityHelper.unbindCustomTabsService(this);

        return super.onUnbind(intent);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "====onAccessibilityEvent===");
            Log.i(TAG, "=type: " + AccessibilityEvent.eventTypeToString(event.getEventType()));
        }

        if (MainService.get() != null) {
            MainService.get().clear();
        }

        CharSequence packageName = event.getPackageName();

        if (packageName != null) {
            String appId = packageName.toString();

            if (appId.equals("com.google.android.googlequicksearchbox") || appId.equals(CustomTabsHelper.STABLE_PACKAGE)) {
                return;
            }

            if (BuildConfig.DEBUG)
                Log.i(TAG, "=appId: " + appId);
        }


        String allText = getAllText(getRootInActiveWindow());

        Pattern pattern = Pattern.compile(LINK_REG_EX);
        Matcher matcher = pattern.matcher(allText);


        Uri top = null;
        List<Bundle> possibleUrls = new ArrayList<>();

        while (matcher.find()) {
            String url = matcher.group(0);

            if (MainService.get() != null) {
                MainService.get().addUrl(url);
            }

            Uri uri = Uri.parse(url);

            if (top == null) {
                top = uri;
            } else {
                Bundle bundle = new Bundle();
                bundle.putParcelable(CustomTabsService.KEY_URL, uri);
            }

            if (BuildConfig.DEBUG)
                Log.i(TAG, "Preload URL: " + url);
        }

        if (top != null) {
            boolean success = mCustomTabActivityHelper.mayLaunchUrl(top, null, possibleUrls);

            if (BuildConfig.DEBUG)
                Log.i(TAG, "Preload URL: " + success);
        }


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

        return string;
    }

    @Override
    public void onInterrupt() {

    }

}
