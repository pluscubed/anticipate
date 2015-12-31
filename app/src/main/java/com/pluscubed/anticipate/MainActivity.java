package com.pluscubed.anticipate;

import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.Crashlytics;
import com.flipboard.bottomsheet.BottomSheetLayout;
import com.pluscubed.anticipate.customtabs.CustomTabDummyActivity;
import com.pluscubed.anticipate.perapp.DbUtil;
import com.pluscubed.anticipate.perapp.PerAppListActivity;
import com.pluscubed.anticipate.util.PrefUtils;
import com.pluscubed.anticipate.widget.DispatchBackEditText;
import com.pluscubed.anticipate.widget.IntentPickerSheetView;

import java.util.List;

import io.fabric.sdk.android.Fabric;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";

    PopupMenu mTryPopup;
    DispatchBackEditText mTryEditText;
    Button mConfigurePerApp;
    private Button mEnableServiceButton;
    private ImageView mEnabledImage;
    private Button mSetDefaultButton;
    private ImageView mSetDefaultImage;
    private BottomSheetLayout mBottomSheetLayout;

    public static boolean isAccessibilityServiceEnabled(Context context) {
        int accessibilityEnabled = 0;
        final String service = BuildConfig.APPLICATION_ID + "/com.pluscubed.anticipate.MainAccessibilityService";
        try {
            accessibilityEnabled = Settings.Secure.getInt(context.getContentResolver(), android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!BuildConfig.DEBUG) {
            Fabric.with(this, new Crashlytics());
        }

        setContentView(R.layout.activity_main);

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
                onSetDefaultButtonPressed();
            }
        });

        mSetDefaultImage = (ImageView) findViewById(R.id.image_default);


        final Button tryButton = (Button) findViewById(R.id.button_try);
        mTryEditText = (DispatchBackEditText) findViewById(R.id.edittext_try);

        tryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onTryButtonPressed();
            }
        });

        mTryPopup = new PopupMenu(this, tryButton, Gravity.BOTTOM);
        mTryPopup.getMenuInflater().inflate(R.menu.menu_try, mTryPopup.getMenu());
        mTryPopup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                onTrySelectPopupPressed(item, mTryEditText);
                return true;
            }
        });

        mTryEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                return onTryEditTextChanged(actionId, tryButton);
            }
        });

        final Switch floatingWindowSwitch = (Switch) findViewById(R.id.checkbox_preload_window);
        floatingWindowSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onFloatingWindowSwitchChange(floatingWindowSwitch);
            }
        });

        floatingWindowSwitch.setChecked(FloatingWindowService.get() != null);

        Spinner spinner = (Spinner) findViewById(R.id.spinner_per_app_mode);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item,
                new String[]{getString(R.string.blacklist), getString(R.string.whitelist)});
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                PrefUtils.setBlacklistMode(MainActivity.this, position==0);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        spinner.setSelection(PrefUtils.isBlacklistMode(this)?0:1);

        mConfigurePerApp = (Button) findViewById(R.id.button_configure_perapp);
        mConfigurePerApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, PerAppListActivity.class));
            }
        });

        mBottomSheetLayout = (BottomSheetLayout) findViewById(R.id.bottom_sheet);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //TODO: Fix BottomSheetLayout listener
            mBottomSheetLayout.addOnSheetStateChangeListener(new BottomSheetLayout.OnSheetStateChangeListener() {
                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void onSheetStateChanged(BottomSheetLayout.State state) {
                    switch (state) {
                        case EXPANDED:
                        case PEEKED:
                            getWindow().setStatusBarColor(ContextCompat.getColor(MainActivity.this, R.color.colorPrimarySuperDark));
                            break;
                        case HIDDEN:
                            getWindow().setStatusBarColor(ContextCompat.getColor(MainActivity.this, R.color.colorPrimaryDark));
                            break;
                    }
                }
            });
        }

        DbUtil.initializeBlacklist(this);
        MainAccessibilityService.updateBlackWhitelist();

        invalidateStates();

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if(intent.getData()!=null){
            showBottomSheetFromUrlIntent(getViewUrlIntent(intent.getDataString()));
        }
    }

    void onFloatingWindowSwitchChange(Switch floatingWindowSwitch) {
        Intent service = new Intent(MainActivity.this, FloatingWindowService.class);

        if (floatingWindowSwitch.isChecked()) {
            if (FloatingWindowService.get() == null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(MainActivity.this)) {
                    new MaterialDialog.Builder(MainActivity.this)
                            .content(R.string.dialog_draw_overlay)
                            .positiveText(R.string.open_settings)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @TargetApi(Build.VERSION_CODES.M)
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    openSettings(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, BuildConfig.APPLICATION_ID);
                                }
                            })
                            .show();
                    floatingWindowSwitch.setChecked(false);
                } else {
                    startService(service);
                }
            }
        } else {
            stopService(service);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_main_about:
                showAboutDialog();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void showAboutDialog() {
        new MaterialDialog.Builder(this)
                .title(getString(R.string.about_dialog_title, BuildConfig.VERSION_NAME))
                .positiveText(R.string.dismiss)
                .content(Html.fromHtml(getString(R.string.about_body)))
                .iconRes(R.mipmap.ic_launcher)
                .linkColor(ContextCompat.getColor(this, R.color.blue_800))
                .positiveColor(ContextCompat.getColor(this, R.color.blue_800))
                .show();
    }

    boolean onTryEditTextChanged(int actionId, Button tryButton) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            tryButton.callOnClick();

            return true;
        }
        return false;
    }

    void onTrySelectPopupPressed(MenuItem item, EditText tryEditText) {
        String input = tryEditText.getText().toString();

        if (!input.startsWith("http://") && !input.startsWith("https://")) {
            input = "http://" + input;
        }
        final Intent viewUrlIntent = getViewUrlIntent(input);

        if (item.getItemId() == R.id.menu_try_anticipate) {
            viewUrlIntent.setClass(MainActivity.this, CustomTabDummyActivity.class);
            startActivity(viewUrlIntent);
        } else if (item.getItemId() == R.id.menu_try_browser) {
            showBottomSheetFromUrlIntent(viewUrlIntent);

        }


    }

    private void showBottomSheetFromUrlIntent(final Intent viewUrlIntent) {
        IntentPickerSheetView picker = new IntentPickerSheetView(this, viewUrlIntent, R.string.open_with, new IntentPickerSheetView.OnIntentPickedListener() {
            @Override
            public void onIntentPicked(IntentPickerSheetView.ActivityInfo activityInfo) {
                viewUrlIntent.setComponent(activityInfo.componentName);
                startActivity(viewUrlIntent);
            }
        });

        picker.setFilter(new IntentPickerSheetView.Filter() {
            @Override
            public boolean include(IntentPickerSheetView.ActivityInfo info) {
                return !info.componentName.getPackageName().equals(BuildConfig.APPLICATION_ID);
            }
        });

        mBottomSheetLayout.showWithSheetView(picker);
    }

    void onTryButtonPressed() {
        mTryPopup.show();

        clearEditTextFocus();
    }

    void onSetDefaultButtonPressed() {
        final String current = getDefaultBrowserPackage();


        if (!current.equals("android") && packageExists(current)) {
            new MaterialDialog.Builder(MainActivity.this)
                    .content(R.string.dialog_clear_defaults)
                    .positiveText(R.string.open_settings)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            try {
                                //Open the current default browswer App Info page
                                openSettings(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, current);
                            } catch (ActivityNotFoundException ignored) {
                                Toast.makeText(MainActivity.this, R.string.open_settings_failed_clear_deafults, Toast.LENGTH_LONG).show();
                            }
                        }
                    })
                    .show();
        } else {
            promptSetDefault();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        //http://stackoverflow.com/questions/4828636/edittext-clear-focus-on-touch-outside
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                    clearEditTextFocus();
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    void clearEditTextFocus() {
        mTryEditText.clearFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mTryEditText.getWindowToken(), 0);
    }

    void openSettings(String settingsAction, String packageName) {
        Intent intent = new Intent(settingsAction);
        intent.setData(Uri.parse("package:" + packageName));
        startActivity(intent);
    }

    void promptSetDefault() {
        Intent intent = getViewUrlIntent("http://www.google.com");
        startActivity(intent);
    }

    @NonNull
    Intent getViewUrlIntent(String parse) {
        return new Intent(Intent.ACTION_VIEW, Uri.parse(parse));
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            invalidateStates();
        }
    }

    private void invalidateStates() {
        final boolean accessibilityServiceEnabled = isAccessibilityServiceEnabled(this);
        mEnabledImage.setImageResource(accessibilityServiceEnabled ? R.drawable.ic_done_black_24dp : R.drawable.ic_cross_black_24dp);
        mEnableServiceButton.setVisibility(accessibilityServiceEnabled ? View.GONE : View.VISIBLE);


        boolean isSetAsDefault = isSetAsDefault();

        Drawable drawable = mSetDefaultImage.getDrawable();
        DrawableCompat.setTint(drawable, ContextCompat.getColor(this, isSetAsDefault ? R.color.green_500 : R.color.blue_800));
        mSetDefaultButton.setVisibility(isSetAsDefault ? View.GONE : View.VISIBLE);
    }

    public boolean packageExists(String targetPackage) {
        List<ApplicationInfo> packages;
        PackageManager pm;

        pm = getPackageManager();
        packages = pm.getInstalledApplications(0);
        for (ApplicationInfo packageInfo : packages) {
            if (packageInfo.packageName.equals(targetPackage))
                return true;
        }
        return false;
    }

    private boolean isSetAsDefault() {
        String packageName = getDefaultBrowserPackage();

        return packageName.equals(BuildConfig.APPLICATION_ID);
    }

    String getDefaultBrowserPackage() {
        Intent browserIntent = getViewUrlIntent("http://");
        ResolveInfo resolveInfo = getPackageManager().resolveActivity(browserIntent, PackageManager.MATCH_DEFAULT_ONLY);

        return resolveInfo.activityInfo.packageName;
    }


}
