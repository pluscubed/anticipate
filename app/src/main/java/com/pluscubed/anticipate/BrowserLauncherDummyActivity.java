package com.pluscubed.anticipate;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsIntent;
import android.widget.Toast;

import com.afollestad.inquiry.Inquiry;
import com.pluscubed.anticipate.customtabs.util.CustomTabConnectionHelper;
import com.pluscubed.anticipate.customtabs.util.CustomTabsHelper;
import com.pluscubed.anticipate.toolbarcolor.WebsiteService;
import com.pluscubed.anticipate.toolbarcolor.WebsiteToolbarDbUtil;
import com.pluscubed.anticipate.util.PrefUtils;
import com.pluscubed.anticipate.util.Utils;

public class BrowserLauncherDummyActivity extends Activity {

    public static final int SHARE_ACTION_ID = 0;
    private static final String TAG = "CustomTabDummyActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Inquiry.init(getApplicationContext(), App.DB, 1);

        final Uri uri = getIntent().getData();
        if (getIntent() != null && uri != null) {

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
            if (host.startsWith("http://") || host.startsWith("https://")) {
                host = host.substring(host.indexOf("/") + 2);
            }

            int endOfTld = host.indexOf("/");
            if (endOfTld != -1)
                host = host.substring(0, endOfTld);

            if (PrefUtils.isDynamicToolbar(this)) {
                int color = WebsiteToolbarDbUtil.getColor(host);
                if (color != WebsiteToolbarDbUtil.NO_COLOR && color != WebsiteToolbarDbUtil.NOT_FOUND) {
                    builder.setToolbarColor(color);
                } else {
                    Intent serviceIntent = new Intent(BrowserLauncherDummyActivity.this, WebsiteService.class);
                    serviceIntent.putExtra(WebsiteService.EXTRA_HOST, host);
                    startService(serviceIntent);

                    builder.setToolbarColor(PrefUtils.getDefaultToolbarColor(this));
                }
            } else {
                builder.setToolbarColor(PrefUtils.getDefaultToolbarColor(this));
            }

            Intent shareIntent = new Intent(this, ShareBroadcastReceiver.class);

            PendingIntent shareBroadcast = PendingIntent.getBroadcast(this, 0, shareIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            builder.enableUrlBarHiding()
                    .setShowTitle(true)
                    .setActionButton(Utils.drawableToBitmap(getApplicationContext(), R.drawable.ic_share_black_24dp),
                            getString(R.string.share),
                            shareBroadcast);
                    /*.addActionButton(SHARE_ACTION_ID,
                            mShareButtonBitmap,
                            getString(R.string.share),
                            shareBroadcast)*/
            ;

            if (PrefUtils.getAnimationStyle(this) == 0) {
                builder.setStartAnimations(this, R.anim.slide_in_bottom, R.anim.fade_out);
                builder.setExitAnimations(this, R.anim.fade_in, R.anim.slide_out_bottom);
            } else if (PrefUtils.getAnimationStyle(this) == 1) {
                builder.setStartAnimations(this, R.anim.slide_in_right, R.anim.fade_out);
                builder.setExitAnimations(this, R.anim.fade_in, R.anim.slide_out_right);
            }
            builder.setCloseButtonIcon(Utils.drawableToBitmap(getApplicationContext(), R.drawable.ic_arrow_back_white_24dp));

            CustomTabsIntent customTabsIntent = builder.build();
            CustomTabsHelper.addKeepAliveExtra(BrowserLauncherDummyActivity.this, customTabsIntent.intent);
            CustomTabConnectionHelper.openCustomTab(
                    BrowserLauncherDummyActivity.this, customTabsIntent, uri,
                    new CustomTabConnectionHelper.CustomTabFallback() {
                        @Override
                        public void openUri(Context activity, Uri uri) {
                            Toast.makeText(BrowserLauncherDummyActivity.this,
                                    getString(R.string.unable_to_launch),
                                    Toast.LENGTH_LONG).show();

                            Intent intent = new Intent(BrowserLauncherDummyActivity.this, MainActivity.class);
                            intent.setData(uri);
                            startActivity(intent);
                        }
                    });

        } else {
            Toast.makeText(this, "Launch error", Toast.LENGTH_SHORT).show();
        }

        finish();
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
