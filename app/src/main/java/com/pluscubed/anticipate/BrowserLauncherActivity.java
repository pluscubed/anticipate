package com.pluscubed.anticipate;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.Log;
import android.widget.Toast;

import com.afollestad.inquiry.Inquiry;
import com.crashlytics.android.Crashlytics;
import com.pluscubed.anticipate.customtabs.util.CustomTabConnectionHelper;
import com.pluscubed.anticipate.quickswitch.QuickSwitchService;
import com.pluscubed.anticipate.toolbarcolor.WebsiteService;
import com.pluscubed.anticipate.toolbarcolor.WebsiteToolbarDbUtil;
import com.pluscubed.anticipate.util.AnimationStyle;
import com.pluscubed.anticipate.util.ColorUtils;
import com.pluscubed.anticipate.util.PrefUtils;
import com.pluscubed.anticipate.util.Utils;

public class BrowserLauncherActivity extends Activity {

    public static final int BROWSER_SHORTCUT = 4;
    public static final String EXTRA_MINIMIZE = "com.pluscubed.anticipate.EXTRA_MINIMIZE";
    public static final String EXTRA_ADD_QUEUE = "com.pluscubed.anticipate.EXTRA_ADD_QUEUE";
    public static final int REQUEST_CODE = 23;
    private static final String TAG = "CustomTabDummyActivity";
    static BrowserLauncherActivity sPendLoadInstance;
    String mHost;
    int mToolbarColor;

    public static void moveInstanceToBack() {
        if (sPendLoadInstance != null) {
            Log.i(TAG, "moveInstanceToBack");
            sPendLoadInstance.onPageLoadStarted();
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Inquiry.init(getApplicationContext(), App.DB, 1);

        if (PrefUtils.isFirstRun(this)) {
            PrefUtils.setVersionCode(this, BuildConfig.VERSION_CODE);
        }
        Utils.notifyChangelog(this);

        //Launched by bubble
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            finish();
            return;
        }

        if (getIntent().getBooleanExtra(EXTRA_MINIMIZE, false)) {
            onPageLoadStarted();
            checkBubbles();
            return;
        }

        final Uri uri = getIntent().getData();
        if (uri == null) {
            Toast.makeText(this, "Launch error", Toast.LENGTH_SHORT).show();
            return;
        }

        final MainAccessibilityService accessibilityService = MainAccessibilityService.get();
        final QuickSwitchService quickSwitchService = QuickSwitchService.get();

        final CustomTabConnectionHelper customTabConnectionHelper =
                accessibilityService != null ? accessibilityService.getCustomTabConnectionHelper()
                        : quickSwitchService != null ? quickSwitchService.getCustomTabConnectionHelper()
                        : null;


        final CustomTabsIntent.Builder builder;
        if (customTabConnectionHelper != null) {
            customTabConnectionHelper.mayLaunchUrl(uri, null, null);
            builder = new CustomTabsIntent.Builder(customTabConnectionHelper.getSession());
        } else {
            builder = new CustomTabsIntent.Builder();
            if (!MainActivity.isAccessibilityServiceEnabled(this)) {
                if (!PrefUtils.isAccessibilityOffWarned(this)) {
                    Toast.makeText(this, R.string.accessibility_disabled, Toast.LENGTH_LONG).show();
                    PrefUtils.setAccessibilityOffWarned(this);
                }
            } else {
                Toast.makeText(this, R.string.accessibility_service_not_active, Toast.LENGTH_LONG).show();
            }
        }

        mHost = uri.toString();
        mHost = Utils.getHost(mHost);

        if (PrefUtils.isDynamicToolbar(this)) {
            mToolbarColor = WebsiteToolbarDbUtil.getColor(mHost);
            if (mToolbarColor == WebsiteToolbarDbUtil.NOT_FOUND) {
                Intent serviceIntent = new Intent(BrowserLauncherActivity.this, WebsiteService.class);
                serviceIntent.putExtra(WebsiteService.EXTRA_HOST, mHost);
                startService(serviceIntent);

                mToolbarColor = -4;
            } else if (mToolbarColor == WebsiteToolbarDbUtil.NO_COLOR) {
                mToolbarColor = -4;
            }
        } else {
            mToolbarColor = -4;
        }
        if (mToolbarColor == -4) {
            if (PrefUtils.isDynamicAppBasedToolbar(this) && accessibilityService != null) {
                mToolbarColor = accessibilityService.getLastAppPrimaryColor();
            } else {
                mToolbarColor = PrefUtils.getDefaultToolbarColor(this);
            }
        }

        builder.setToolbarColor(mToolbarColor);

        boolean isLightToolbar = !ColorUtils.shouldUseLightForegroundOnBackground(mToolbarColor);

        Intent shareIntent = new Intent(this, ShareBroadcastReceiver.class);
        PendingIntent sharePending = PendingIntent.getBroadcast(this, 0, shareIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Intent settingsIntent = new Intent(this, MainActivity.class);
        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent settingsPending = PendingIntent.getActivity(this, 0, settingsIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Intent minimizeToQuickSwitch = new Intent(this, getClass());
        minimizeToQuickSwitch.putExtra(EXTRA_MINIMIZE, true);
        minimizeToQuickSwitch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        minimizeToQuickSwitch.setData(uri);
        PendingIntent minimizePending = PendingIntent.getActivity(this, 0, minimizeToQuickSwitch, PendingIntent.FLAG_CANCEL_CURRENT);

        String chromeAppPackageName = PrefUtils.getChromeApp(this);
        Intent openInChrome = new Intent();
        openInChrome.setPackage(chromeAppPackageName);
        openInChrome.setData(uri);
        openInChrome.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent openInChromePending = PendingIntent.getActivity(this, 0, openInChrome, PendingIntent.FLAG_CANCEL_CURRENT);

        String chromeApp = null;
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(chromeAppPackageName, 0);

            if (packageInfo.versionCode >= 261900501) {
                //If newer than 1/14 Chrome Dev release
                ApplicationInfo applicationInfo = packageInfo.applicationInfo;
                chromeApp = (String) applicationInfo.loadLabel(getPackageManager());
            }

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        builder.enableUrlBarHiding()
                .setShowTitle(true)
                .addMenuItem(getString(R.string.share), sharePending)
                .addMenuItem(getString(R.string.anticipate_settings), settingsPending)
                .setCloseButtonIcon(getToolbarIcon(R.drawable.ic_arrow_back_white_24dp, isLightToolbar))
                    /*.addActionBarItem(BROWSER_SHORTCUT,
                            Utils.drawableToBitmap(getPackageManager().getApplicationIcon(PrefUtils.getChromeApp(this))),
                            getString(R.string.share),
                            sharePending)*/;

        if (chromeApp != null) {
            builder.addMenuItem(String.format("Open in %s", chromeApp), openInChromePending);
        }

        boolean bubbleAvailable = PrefUtils.isQuickSwitch(this) &&
                (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this));
        if (bubbleAvailable) {
            builder.setActionButton(Utils.drawableToBitmap(this, R.drawable.ic_swap_vertical_circle_black_24dp), getString(R.string.minimize_to_bubble), minimizePending, true);
        }

        int animationStyleId = PrefUtils.getAnimationStyle(this);
        AnimationStyle animationStyle = AnimationStyle.valueWithId(animationStyleId);
        if (animationStyle != null) {
            switch (animationStyle) {
                case BOTTOM:
                    builder.setStartAnimations(this, R.anim.slide_in_bottom, R.anim.fade_out);
                    builder.setExitAnimations(this, R.anim.fade_in, R.anim.slide_out_bottom);
                    overridePendingTransition(R.anim.slide_in_bottom, R.anim.fade_out);
                    break;
                case RIGHT:
                    builder.setStartAnimations(this, R.anim.slide_in_right, R.anim.fade_out);
                    builder.setExitAnimations(this, R.anim.fade_in, R.anim.slide_out_right);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
                    break;
                case LOLLIPOP:
                    builder.setStartAnimations(this, R.anim.activity_slide_in, R.anim.fade_out);
                    builder.setExitAnimations(this, R.anim.fade_in, R.anim.activity_slide_out);
                    overridePendingTransition(R.anim.activity_slide_in, R.anim.fade_out);
                    break;
            }
        }

        CustomTabsIntent customTabsIntent = builder.build();
        customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);


        boolean openBubble = bubbleAvailable && getIntent().getBooleanExtra(EXTRA_ADD_QUEUE, true);


        if (openBubble) {
            customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            overridePendingTransition(0, 0);
        }

        openCustomTab(customTabsIntent, uri);


        if (openBubble) {
            sPendLoadInstance = this;

            Intent intent = new Intent(this, QuickSwitchService.class);
            intent.setData(uri);
            startService(intent);

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (sPendLoadInstance != null) {
                        if (BuildConfig.DEBUG)
                            Toast.makeText(BrowserLauncherActivity.this,
                                    "Not properly launching",
                                    Toast.LENGTH_LONG).show();

                        if (customTabConnectionHelper != null && accessibilityService != null) {
                            customTabConnectionHelper.unbindCustomTabsService(accessibilityService);
                            customTabConnectionHelper.bindCustomTabsService(accessibilityService);
                        } else if (customTabConnectionHelper != null) {
                            customTabConnectionHelper.unbindCustomTabsService(quickSwitchService);
                            customTabConnectionHelper.bindCustomTabsService(quickSwitchService);
                        }

                        BrowserLauncherActivity.this.onPageLoadStarted();
                    }
                }
            }, 800);
        } else {
            finish();
        }

        if (bubbleAvailable) {
            checkBubbles();
        }
    }

    private void openCustomTab(CustomTabsIntent intent, Uri uri) {
        try {
            CustomTabConnectionHelper.openCustomTab(
                    BrowserLauncherActivity.this, intent, uri,
                    new CustomTabConnectionHelper.CustomTabFallback() {
                        @Override
                        public void openUri(Context activity, Uri uri) {
                            fallbackLaunch(getString(R.string.unable_to_launch));
                        }
                    },
                    REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            Crashlytics.logException(e);

            fallbackLaunch(getString(R.string.unable_to_launch_browser_error));
        }
    }

    void onPageLoadStarted() {
        sPendLoadInstance = null;

        moveTaskToBack(true);
        finish();

        /*if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            ActivityManager.TaskDescription taskDescription = new ActivityManager.TaskDescription(mHost,  null, mToolbarColor);
            setTaskDescription(taskDescription);
        }*/
    }

    void fallbackLaunch(String error) {
        Toast.makeText(BrowserLauncherActivity.this, error, Toast.LENGTH_LONG).show();

        Intent intent = new Intent(BrowserLauncherActivity.this, MainActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void checkBubbles() {
        Intent checkBubblesIntent = new Intent(this, QuickSwitchService.class);
        checkBubblesIntent.putExtra(QuickSwitchService.EXTRA_CHECK_BUBBLES, true);
        startService(checkBubblesIntent);
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();

        sPendLoadInstance = null;
    }

    private Bitmap getToolbarIcon(@DrawableRes int resId, boolean lightToolbar) {
        Drawable drawable = ContextCompat.getDrawable(getApplicationContext(), resId);
        Drawable wrappedDrawable = DrawableCompat.wrap(drawable);
        if (lightToolbar) {
            DrawableCompat.setTintMode(wrappedDrawable, PorterDuff.Mode.SRC_IN);
            DrawableCompat.setTint(wrappedDrawable, ContextCompat.getColor(this, R.color.toolbar_light_icon));
        }
        return Utils.drawableToBitmap(wrappedDrawable);
    }

    public static class ShareBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, intent.getDataString());
            Intent chooserIntent = Intent.createChooser(shareIntent, context.getString(R.string.share));
            chooserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(chooserIntent);
        }
    }
}
