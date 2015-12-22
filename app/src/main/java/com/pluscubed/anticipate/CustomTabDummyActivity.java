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


            CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder(
                    MainAccessibilityService.getSharedService().getCustomTabActivityHelper().getSession())
                    .enableUrlBarHiding()
                    .setShowTitle(true)
                    .build();
            CustomTabsHelper.addKeepAliveExtra(this, customTabsIntent.intent);
            CustomTabActivityHelper.openCustomTab(
                    this, customTabsIntent, getIntent().getData(), new CustomTabActivityHelper.CustomTabFallback() {
                        @Override
                        public void openUri(Context activity, Uri uri) {
                            Intent open = new Intent(Intent.ACTION_VIEW);
                            open.setData(uri);
                            activity.startActivity(open);
                        }
                    });
        } else {
            Toast.makeText(this, "Launch error", Toast.LENGTH_SHORT).show();
        }

        finish();
    }
}
