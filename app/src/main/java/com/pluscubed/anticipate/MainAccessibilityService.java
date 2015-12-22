package com.pluscubed.anticipate;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainAccessibilityService extends AccessibilityService {

    public static final String TAG = "Accesibility";
    public static final String LINK_REG_EX = "((?:[a-z][\\w-]+:(?:\\/{1,3}|[a-z0-9%])|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}/)(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\))+(?:\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:'\".,<>?«»“”‘’]))";

    private static final int URL_PRELOAD_TIMEOUT = 10000;
    private static MainAccessibilityService sSharedService;
    private Map<String, Long> mUrlsToExpire;
    private CustomTabActivityHelper mCustomTabActivityHelper;

    public static MainAccessibilityService getSharedService() {
        return sSharedService;
    }

    public CustomTabActivityHelper getCustomTabActivityHelper() {
        return mCustomTabActivityHelper;
    }

    public void setmCustomTabActivityHelper(CustomTabActivityHelper mCustomTabActivityHelper) {
        this.mCustomTabActivityHelper = mCustomTabActivityHelper;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        sSharedService = this;

        Log.e(TAG, "onServiceConnected:");

        mUrlsToExpire = new HashMap<>();

        mCustomTabActivityHelper = new CustomTabActivityHelper();
        mCustomTabActivityHelper.setConnectionCallback(null);
        mCustomTabActivityHelper.bindCustomTabsService(this);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        sSharedService = null;
        mCustomTabActivityHelper.unbindCustomTabsService(this);

        return super.onUnbind(intent);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        processAllText(getRootInActiveWindow());

        long currentTime = System.currentTimeMillis();


        ArrayList<Uri> urls = new ArrayList<>();
        for (Iterator<String> iterator = mUrlsToExpire.keySet().iterator(); iterator.hasNext(); ) {
            String url = iterator.next();
            if (mUrlsToExpire.get(url) < currentTime) {
                iterator.remove();
                continue;
            }
            urls.add(Uri.parse(url));
        }

        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList("", urls);
        if (urls.size() > 0)
            mCustomTabActivityHelper.mayLaunchUrl(urls.get(0), bundle, null);

        /*for(AccessibilityWindowInfo window: getWindows()){
            processAllText(window.getRoot(), "window - "+ AccessibilityEvent.eventTypeToString(event.getEventType())+": ");
        }*/
    }

    private void processAllText(AccessibilityNodeInfo source) {
        if (source == null) {
            return;
        }

        if (source.getText() != null) {
            String text = source.getText().toString();

            text = text.replace("\n", " ");

            Pattern pattern = Pattern.compile(LINK_REG_EX);
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                String url = matcher.group(0);

                mUrlsToExpire.put(url, System.currentTimeMillis() + URL_PRELOAD_TIMEOUT);


                if (BuildConfig.DEBUG)
                    Log.i(TAG, url);
            }

            Log.i(TAG, text);
        }

        for (int i = 0; i < source.getChildCount(); i++) {
            processAllText(source.getChild(i));
        }
    }

    @Override
    public void onInterrupt() {

    }

}
