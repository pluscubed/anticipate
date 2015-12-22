package com.pluscubed.anticipate;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";
    private Button mEnableServiceButton;
    private ImageView mEnabledImage;
    private Button mSetDefaultButton;
    private ImageView mSetDefaultImage;

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

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);

        mEnableServiceButton = (Button) findViewById(R.id.button_enable_service);
        mEnableServiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }
        });

        mEnabledImage = (ImageView) findViewById(R.id.image_enabled);

        mSetDefaultButton = (Button) findViewById(R.id.button_set_default);
        mSetDefaultButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.google.com"));
                startActivity(intent);
            }
        });

        mSetDefaultImage = (ImageView) findViewById(R.id.image_default);

    }

    @Override
    protected void onResume() {
        super.onResume();

        boolean accessibilityServiceEnabled = isAccessibilityServiceEnabled();
        mEnabledImage.setImageResource(accessibilityServiceEnabled ? R.drawable.ic_done_black_24dp : R.drawable.ic_cross_black_24dp);
        mEnableServiceButton.setVisibility(accessibilityServiceEnabled ? View.GONE : View.VISIBLE);


        boolean isSetAsDefault = isSetAsDefault();

        Drawable drawable = mSetDefaultImage.getDrawable();
        DrawableCompat.setTint(drawable, ContextCompat.getColor(this, isSetAsDefault ? R.color.enabled : R.color.browser));
        mSetDefaultButton.setVisibility(isSetAsDefault ? View.GONE : View.VISIBLE);

    }

    private boolean isSetAsDefault() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://"));
        ResolveInfo resolveInfo = getPackageManager().resolveActivity(browserIntent, PackageManager.MATCH_DEFAULT_ONLY);

        String packageName = resolveInfo.activityInfo.packageName;

        return packageName.equals(BuildConfig.APPLICATION_ID);
    }

    private boolean isAccessibilityServiceEnabled() {
        int accessibilityEnabled = 0;
        final String service = "com.pluscubed.anticipate/com.pluscubed.anticipate.MainAccessibilityService";
        try {
            accessibilityEnabled = Settings.Secure.getInt(getContentResolver(), android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                splitter.setString(settingValue);
                while (splitter.hasNext()) {
                    String accessabilityService = splitter.next();
                    if (accessabilityService.equalsIgnoreCase(service)) {
                        return true;
                    }
                }
            }
        } else {
            Log.v(TAG, "Accessibility is disabled.");
        }

        return false;
    }


}
