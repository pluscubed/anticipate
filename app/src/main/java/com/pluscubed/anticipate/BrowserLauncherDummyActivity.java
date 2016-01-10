package com.pluscubed.anticipate;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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

    private Bitmap backButtonBitmap;

    private static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap;

        if(drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        cacheBitmaps();

        final Uri uri = getIntent().getData();
        if (getIntent() != null && uri != null) {

            MainAccessibilityService service = MainAccessibilityService.get();

            final CustomTabsIntent.Builder builder;
            if (service != null) {
                service.getCustomTabActivityHelper().mayLaunchUrl(uri, null, null);
                builder = new CustomTabsIntent.Builder(service.getCustomTabActivityHelper().getSession());
            } else {
                builder = new CustomTabsIntent.Builder();
                if (MainActivity.isAccessibilityServiceEnabled(this)) {
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

            builder.enableUrlBarHiding();
            builder.setShowTitle(true);
            builder.addMenuItem(getString(R.string.share), PendingIntent.getBroadcast(this, 0, shareIntent, PendingIntent.FLAG_CANCEL_CURRENT));

            if(PrefUtils.getAnimationStyle(this) == 0) {
                builder.setStartAnimations(this, R.anim.slide_up_right, R.anim.slide_down_left);
                builder.setExitAnimations(this, R.anim.slide_up_left, R.anim.slide_down_right);
            } else if(PrefUtils.getAnimationStyle(this) == 1) {
                builder.setStartAnimations(this, R.anim.slide_in_right, R.anim.slide_out_left);
                builder.setExitAnimations(this, R.anim.slide_in_left, R.anim.slide_out_right);
            }
            builder.setCloseButtonIcon(backButtonBitmap);

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

    private void cacheBitmaps()
    {
        if(backButtonBitmap != null) {
            if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                backButtonBitmap = drawableToBitmap(getDrawable(R.drawable.ic_arrow_back_white_24dp));
            } else {
                backButtonBitmap = drawableToBitmap(getResources().getDrawable(R.drawable.ic_arrow_back_white_24dp));
            }
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
