package com.pluscubed.anticipate;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsIntent;
import android.widget.Toast;

import com.afollestad.inquiry.Inquiry;
import com.pluscubed.anticipate.customtabs.util.CustomTabConnectionHelper;
import com.pluscubed.anticipate.toolbarcolor.WebsiteService;
import com.pluscubed.anticipate.toolbarcolor.WebsiteToolbarDbUtil;
import com.pluscubed.anticipate.util.AnimationStyle;
import com.pluscubed.anticipate.util.PrefUtils;
import com.pluscubed.anticipate.util.Utils;

public class BrowserLauncherActivity extends Activity {

    public static final int BROWSER_SHORTCUT = 4;
    public static final String EXTRA_ADD_QUEUE = "com.pluscubed.anticipate.EXTRA_ADD_QUEUE";
    private static final String TAG = "CustomTabDummyActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Launched by bubble
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            finish();
            return;
        }

        Inquiry.init(getApplicationContext(), App.DB, 1);

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


            Intent shareIntent = new Intent(this, ShareBroadcastReceiver.class);

            PendingIntent shareBroadcast = PendingIntent.getBroadcast(this, 0, shareIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            builder.enableUrlBarHiding()
                    .setShowTitle(true)
                    .setActionButton(Utils.drawableToBitmap(getApplicationContext(), R.drawable.ic_share_black_24dp),
                            getString(R.string.share),
                            shareBroadcast)
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
            builder.setCloseButtonIcon(Utils.drawableToBitmap(getApplicationContext(), R.drawable.ic_arrow_back_white_24dp));

            CustomTabsIntent customTabsIntent = builder.build();
            customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            CustomTabConnectionHelper.openCustomTab(
                    BrowserLauncherActivity.this, customTabsIntent, uri,
                    new CustomTabConnectionHelper.CustomTabFallback() {
                        @Override
                        public void openUri(Context activity, Uri uri) {
                            Toast.makeText(BrowserLauncherActivity.this,
                                    getString(R.string.unable_to_launch),
                                    Toast.LENGTH_LONG).show();

                            Intent intent = new Intent(BrowserLauncherActivity.this, MainActivity.class);
                            intent.setData(uri);
                            startActivity(intent);
                        }
                    });

            boolean addToQueue = service != null && getIntent().getBooleanExtra(EXTRA_ADD_QUEUE, true);
            if (addToQueue) {
                service.addQueuedWebsite(uri.toString());
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        moveTaskToBack(true);
                        finish();
                    }
                }, 400);
            } else {
                finish();
            }
        } else {
            Toast.makeText(this, "Launch error", Toast.LENGTH_SHORT).show();
        }
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
