package com.pluscubed.anticipate.customtabs;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsIntent;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.pluscubed.anticipate.MainAccessibilityService;
import com.pluscubed.anticipate.MainActivity;
import com.pluscubed.anticipate.R;
import com.pluscubed.anticipate.customtabs.util.CustomTabActivityHelper;
import com.pluscubed.anticipate.customtabs.util.CustomTabsHelper;

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
                if (!MainActivity.isAccessibilityServiceEnabled(this)) {
                    Toast.makeText(this, R.string.accessibility_disabled, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Accessibility service not active", Toast.LENGTH_LONG).show();
                }
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
                            Toast.makeText(CustomTabDummyActivity.this,
                                    getString(R.string.unable_to_launch),
                                    Toast.LENGTH_LONG).show();

                            Intent intent = new Intent(CustomTabDummyActivity.this, MainActivity.class);
                            intent.setData(getIntent().getData());
                            startActivity(intent);
                        }
                    });
        } else {
            Toast.makeText(this, "Launch error", Toast.LENGTH_SHORT).show();
        }

        finish();
    }
}
