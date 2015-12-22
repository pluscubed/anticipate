package com.pluscubed.anticipate;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.customtabs.CustomTabsIntent;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getIntent() != null && getIntent().getData() != null) {
            CustomTabsIntent launch = new CustomTabsIntent.Builder(
                    MainAccessibilityService.getSharedService().getCustomTabActivityHelper().getSession())
                    .enableUrlBarHiding()
                    .setShowTitle(true)
                    .build();
            CustomTabActivityHelper.openCustomTab(
                    this, launch, getIntent().getData(), new CustomTabActivityHelper.CustomTabFallback() {
                        @Override
                        public void openUri(Context activity, Uri uri) {
                            Intent open = new Intent(Intent.ACTION_VIEW);
                            open.setData(uri);
                            activity.startActivity(open);
                        }
                    });
            finish();
        }
    }


}
