package com.pluscubed.anticipate;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsIntent;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.pluscubed.anticipate.customtabsshared.CustomTabsHelper;

public class CustomTabDummyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent() != null && getIntent().getData() != null) {

            MainAccessibilityService service = MainAccessibilityService.get();

            CustomTabsIntent.Builder builder;
            if (service != null) {
                service.getCustomTabActivityHelper().mayLaunchUrl(getIntent().getData(), null, null);

                builder = new CustomTabsIntent.Builder(service.getCustomTabActivityHelper().getSession());

            } else {
                builder = new CustomTabsIntent.Builder();

                /*Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);*/
                Toast.makeText(this, R.string.accessibility_disabled, Toast.LENGTH_LONG).show();
               /* startActivity(intent);*/
            }

            CustomTabsIntent customTabsIntent = builder
                    .enableUrlBarHiding()
                    .setShowTitle(true)
                    .build();
            CustomTabsHelper.addKeepAliveExtra(this, customTabsIntent.intent);
            CustomTabActivityHelper.openCustomTab(
                    this, customTabsIntent, getIntent().getData(), new CustomTabActivityHelper.CustomTabFallback() {
                        @Override
                        public void openUri(Context activity, Uri uri) {
                            openUrlStandard(activity, uri);
                        }
                    });
        } else {
            Toast.makeText(this, "Launch error", Toast.LENGTH_SHORT).show();
        }

        finish();
    }

    void openUrlStandard(Context activity, Uri uri) {
        Intent open = new Intent(Intent.ACTION_VIEW);
        open.setData(uri);
        activity.startActivity(Intent.createChooser(open, "Open with:"));
    }
}
