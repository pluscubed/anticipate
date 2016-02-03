package com.pluscubed.anticipate;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.widget.Toast;

import com.afollestad.inquiry.Inquiry;
import com.crashlytics.android.Crashlytics;
import com.pluscubed.anticipate.customtabs.util.CustomTabConnectionHelper;
import com.pluscubed.anticipate.toolbarcolor.WebsiteService;
import com.pluscubed.anticipate.toolbarcolor.WebsiteToolbarDbUtil;
import com.pluscubed.anticipate.util.AnimationStyle;
import com.pluscubed.anticipate.util.ColorUtils;
import com.pluscubed.anticipate.util.PrefUtils;
import com.pluscubed.anticipate.util.Utils;

public class BrowserLauncherActivity extends Activity {

    public static final int BROWSER_SHORTCUT = 4;
    public static final String EXTRA_ADD_QUEUE = "com.pluscubed.anticipate.EXTRA_ADD_QUEUE";
    private static final String TAG = "CustomTabDummyActivity";

    private static BrowserLauncherActivity sSharedInstance;

    public static void moveToBack() {
        if (sSharedInstance != null) {
            sSharedInstance.moveTaskToBack(true);
            sSharedInstance.finish();
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Launched by bubble
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            finish();
            return;
        }

        Inquiry.init(getApplicationContext(), App.DB, 1);

        sSharedInstance = this;

        final Uri uri = getIntent().getData();
        if (uri != null) {
            MainAccessibilityService service = MainAccessibilityService.get();

            final CustomTabsIntent.Builder builder;
            if (service != null) {
                service.getCustomTabActivityHelper().mayLaunchUrl(uri, null, null);
                builder = new CustomTabsIntent.Builder(service.getCustomTabActivityHelper().getSession());
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

            String host = uri.toString();
            host = Utils.getHost(host);

            int color;
            if (PrefUtils.isDynamicToolbar(this)) {
                color = WebsiteToolbarDbUtil.getColor(host);
                if (color == WebsiteToolbarDbUtil.NOT_FOUND) {
                    Intent serviceIntent = new Intent(BrowserLauncherActivity.this, WebsiteService.class);
                    serviceIntent.putExtra(WebsiteService.EXTRA_HOST, host);
                    startService(serviceIntent);

                    color = -4;
                } else if (color == WebsiteToolbarDbUtil.NO_COLOR) {
                    color = -4;
                }
            } else {
                color = -4;
            }
            if (color == -4) {
                if (PrefUtils.isDynamicAppBasedToolbar(this) && service != null) {
                    color = service.getLastAppPrimaryColor();
                } else {
                    color = PrefUtils.getDefaultToolbarColor(this);
                }
            }

            builder.setToolbarColor(color);


            boolean isLightToolbar = !ColorUtils.shouldUseLightForegroundOnBackground(color);

            Intent shareIntent = new Intent(this, ShareBroadcastReceiver.class);
            PendingIntent shareBroadcast = PendingIntent.getBroadcast(this, 0, shareIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            builder.enableUrlBarHiding()
                    .setShowTitle(true)
                    .setActionButton(
                            Utils.drawableToBitmap(this, R.drawable.ic_share_black_24dp),
                            getString(R.string.share),
                            shareBroadcast,
                            true)
                    .setCloseButtonIcon(
                            getToolbarIcon(R.drawable.ic_arrow_back_white_24dp, isLightToolbar))
                    /*.addActionBarItem(BROWSER_SHORTCUT,
                            Utils.drawableToBitmap(getPackageManager().getApplicationIcon(PrefUtils.getChromeApp(this))),
                            getString(R.string.share),
                            shareBroadcast)*/;

            int animationStyleId = PrefUtils.getAnimationStyle(this);
            AnimationStyle animationStyle = AnimationStyle.valueWithId(animationStyleId);
            if (animationStyle != null) {
                switch (animationStyle) {
                    case BOTTOM:
                        builder.setStartAnimations(this, R.anim.slide_in_bottom, R.anim.fade_out);
                        builder.setExitAnimations(this, R.anim.fade_in, R.anim.slide_out_bottom);
                        break;
                    case RIGHT:
                        builder.setStartAnimations(this, R.anim.slide_in_right, R.anim.fade_out);
                        builder.setExitAnimations(this, R.anim.fade_in, R.anim.slide_out_right);
                        break;
                    case LOLLIPOP:
                        builder.setStartAnimations(this, R.anim.activity_slide_in, R.anim.fade_out);
                        builder.setExitAnimations(this, R.anim.fade_in, R.anim.activity_slide_out);
                        break;
                }
            }

            CustomTabsIntent customTabsIntent = builder.build();
            customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            try {
                CustomTabConnectionHelper.openCustomTab(
                        BrowserLauncherActivity.this, customTabsIntent, uri,
                        new CustomTabConnectionHelper.CustomTabFallback() {
                            @Override
                            public void openUri(Context activity, Uri uri) {
                                fallbackLaunch(getString(R.string.unable_to_launch));
                            }
                        });
            } catch (ActivityNotFoundException e) {
                Crashlytics.logException(e);

                fallbackLaunch(getString(R.string.unable_to_launch_browser_error));
            }

            boolean addToQueue = PrefUtils.isQuickSwitch(this) &&
                    service != null && getIntent().getBooleanExtra(EXTRA_ADD_QUEUE, true);
            if (addToQueue) {
                service.addQueuedWebsite(uri.toString());
            } else {
                finish();
            }
        } else {
            Toast.makeText(this, "Launch error", Toast.LENGTH_SHORT).show();
        }
    }

    void fallbackLaunch(String error) {
        Toast.makeText(BrowserLauncherActivity.this, error, Toast.LENGTH_LONG).show();

        Intent intent = new Intent(BrowserLauncherActivity.this, MainActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        sSharedInstance = null;
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
