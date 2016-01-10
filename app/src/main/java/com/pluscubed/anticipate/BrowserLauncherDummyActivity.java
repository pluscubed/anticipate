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

import com.pluscubed.anticipate.customtabs.util.CustomTabConnectionHelper;
import com.pluscubed.anticipate.customtabs.util.CustomTabsHelper;
import com.pluscubed.anticipate.toolbarcolor.WebsiteService;
import com.pluscubed.anticipate.toolbarcolor.WebsiteToolbarDbUtil;
import com.pluscubed.anticipate.util.PrefUtils;

public class BrowserLauncherDummyActivity extends Activity {

    private static final String TAG = "CustomTabDummyActivity";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
                    Toast.makeText(this, R.string.accessibility_disabled, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, R.string.accessibility_service_not_active, Toast.LENGTH_LONG).show();
                }
            }


            String host = uri.toString().replaceAll(".*\\.(?=.*\\.)", "");

            if (PrefUtils.isDynamicToolbar(this)) {
                int color = WebsiteToolbarDbUtil.getColor(host);
                if (color != -1) {
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

            CustomTabsIntent customTabsIntent = builder
                    .enableUrlBarHiding()
                    .setShowTitle(true)
                    .addMenuItem(getString(R.string.share), PendingIntent.getBroadcast(this, 0, shareIntent, PendingIntent.FLAG_CANCEL_CURRENT))
                    .build();
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
