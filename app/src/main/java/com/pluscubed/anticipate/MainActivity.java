package com.pluscubed.anticipate;

import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.SwitchCompat;
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
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.color.ColorChooserDialog;
import com.crashlytics.android.Crashlytics;
import com.flipboard.bottomsheet.BottomSheetLayout;
import com.pluscubed.anticipate.changelog.ChangelogDialog;
import com.pluscubed.anticipate.customtabs.util.CustomTabConnectionHelper;
import com.pluscubed.anticipate.customtabs.util.CustomTabsHelper;
import com.pluscubed.anticipate.filter.AppInfo;
import com.pluscubed.anticipate.filter.DbUtil;
import com.pluscubed.anticipate.filter.FilterListActivity;
import com.pluscubed.anticipate.util.AnimationStyle;
import com.pluscubed.anticipate.util.PrefUtils;
import com.pluscubed.anticipate.util.Utils;
import com.pluscubed.anticipate.widget.DispatchBackEditText;
import com.pluscubed.anticipate.widget.IntentPickerSheetView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ColorChooserDialog.ColorCallback {

    public static final String TAG = "MainActivity";
    public static final String EXTRA_SHOW_CHANGELOG = "com.pluscubed.anticipate.EXTRA_SHOW_CHANGELOG";

    PopupMenu mTryPopup;
    DispatchBackEditText mTryEditText;
    Button mConfigurePerApp;
    SwitchCompat mPageBasedToolbarSwitch;
    SwitchCompat mQuickSwitch;
    private Button mEnableServiceButton;
    private ImageView mEnabledImage;
    private Button mSetDefaultButton;
    private ImageView mSetDefaultImage;
    private BottomSheetLayout mBottomSheetLayout;
    private View mDefaultToolbarColorView;

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
                    String accessibilityService = splitter.next();
                    if (accessibilityService.equalsIgnoreCase(service)) {
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

        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);

        //SET ENABLE SERVICE
        mEnableServiceButton = (Button) findViewById(R.id.button_enable_service);
        mEnableServiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                } catch (ActivityNotFoundException e) {
                    Crashlytics.logException(e);
                    Toast.makeText(MainActivity.this, R.string.open_settings_failed_accessibility, Toast.LENGTH_LONG).show();
                }
            }
        });

        mEnabledImage = (ImageView) findViewById(R.id.image_enabled);

        //SET DEFAULT
        mSetDefaultButton = (Button) findViewById(R.id.button_set_default);
        mSetDefaultButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSetDefaultButtonPressed();
            }
        });

        mSetDefaultImage = (ImageView) findViewById(R.id.image_default);


        //TRY
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

        //PER APP FILTER
        final Spinner spinner = (Spinner) findViewById(R.id.spinner_per_app_mode);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.spinner_dropdown,
                new String[]{getString(R.string.blacklist), getString(R.string.whitelist)}) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view;

                if (convertView == null) {
                    view = getLayoutInflater().inflate(R.layout.spinner_dropdown_icon, parent, false);
                } else {
                    view = convertView;
                }

                TextView text = (TextView) view.findViewById(android.R.id.text1);
                ImageView imageView = (ImageView) view.findViewById(R.id.image_app);

                text.setText(getItem(position));
                switch (position) {
                    case 0:
                        imageView.setImageResource(R.drawable.ic_remove_circle_black_24dp);
                        break;
                    case 1:
                        imageView.setImageResource(R.drawable.ic_remove_circle_outline_black_24dp);
                        break;
                }

                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                return getView(position, convertView, parent);
            }
        };
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                PrefUtils.setBlacklistMode(MainActivity.this, position == 0);
                spinner.setDropDownVerticalOffset(Utils.dp2px(MainActivity.this, spinner.getSelectedItemPosition() * -48));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        spinner.setSelection(PrefUtils.isBlacklistMode(this) ? 0 : 1);
        spinner.setDropDownVerticalOffset(Utils.dp2px(this, spinner.getSelectedItemPosition() * -48));

        mConfigurePerApp = (Button) findViewById(R.id.button_configure_perapp);
        mConfigurePerApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, FilterListActivity.class));
            }
        });

        //ANIMATION
        setupAnimationStyle();

        //CHROME APP

        List<String> customTabPackages = CustomTabsHelper.getCustomTabsSupportedPackages(this);
        final List<AppInfo> packages = new ArrayList<>();
        for (String packageName : customTabPackages) {
            AppInfo appInfo = new AppInfo();
            appInfo.packageName = packageName;
            try {
                appInfo.name = getPackageManager().getApplicationInfo(packageName, 0).loadLabel(getPackageManager()).toString();
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            packages.add(appInfo);
        }
        Collections.sort(packages);

        final Spinner spinnerChrome = (Spinner) findViewById(R.id.spinner_browser);
        final Button installChrome = (Button) findViewById(R.id.button_install_chrome);

        installChrome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + CustomTabsHelper.STABLE_PACKAGE)));
                } catch (android.content.ActivityNotFoundException anfe) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + CustomTabsHelper.STABLE_PACKAGE)));
                }
            }
        });

        if (packages.size() > 0) {
            spinnerChrome.setVisibility(View.VISIBLE);
            installChrome.setVisibility(View.GONE);

            ArrayAdapter<AppInfo> adapterChrome = new ArrayAdapter<AppInfo>(this, R.layout.spinner_dropdown_icon, packages) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View view;

                    if (convertView == null) {
                        view = getLayoutInflater().inflate(R.layout.spinner_dropdown_icon, parent, false);
                    } else {
                        view = convertView;
                    }

                    TextView text = (TextView) view.findViewById(android.R.id.text1);
                    ImageView imageView = (ImageView) view.findViewById(R.id.image_app);

                    AppInfo appInfo = getItem(position);

                    ApplicationInfo applicationInfo;
                    try {
                        applicationInfo = getPackageManager().getApplicationInfo(appInfo.packageName, 0);

                        imageView.setImageDrawable(applicationInfo.loadIcon(getPackageManager()));
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                        imageView.setImageDrawable(null);
                    }
                    text.setText(appInfo.name);

                    return view;
                }

                @Override
                public View getDropDownView(int position, View convertView, ViewGroup parent) {
                    return getView(position, convertView, parent);
                }
            };
            spinnerChrome.setAdapter(adapterChrome);
            spinnerChrome.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    PrefUtils.setChromeApp(MainActivity.this, packages.get(position).packageName);

                    MainAccessibilityService mainAccessibilityService = MainAccessibilityService.get();
                    if (mainAccessibilityService != null) {
                        CustomTabConnectionHelper customTabConnectionHelper = mainAccessibilityService.getCustomTabConnectionHelper();
                        customTabConnectionHelper.unbindCustomTabsService(mainAccessibilityService);
                        customTabConnectionHelper.bindCustomTabsService(mainAccessibilityService);
                    }

                    spinnerChrome.setDropDownVerticalOffset(Utils.dp2px(MainActivity.this, spinnerChrome.getSelectedItemPosition() * -48));
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
            String chromeApp = PrefUtils.getChromeApp(this);
            boolean found = false;
            int defaultIndex = 0;
            String defaultPackage = CustomTabsHelper.getDefaultPackageFromAppInfos(packages);
            for (int i = 0, customTabPackagesSize = packages.size(); i < customTabPackagesSize; i++) {
                String info = packages.get(i).packageName;
                if (info.equals(chromeApp)) {
                    spinnerChrome.setSelection(i);
                    found = true;
                    break;
                }
                if (info.equals(defaultPackage)) {
                    defaultIndex = i;
                }
            }

            //No set app yet or set app isn't installed anymore
            if (!found) {
                spinnerChrome.setSelection(defaultIndex);
            }

            spinnerChrome.setDropDownVerticalOffset(Utils.dp2px(MainActivity.this, spinnerChrome.getSelectedItemPosition() * -48));
        } else {
            spinnerChrome.setVisibility(View.GONE);
            installChrome.setVisibility(View.VISIBLE);
        }

        //QUICK SWITCH BUBBLE
        final ViewGroup quickSwitchLinear = (ViewGroup) findViewById(R.id.linear_quick_switch_bubble);
        mQuickSwitch = (SwitchCompat) quickSwitchLinear.getChildAt(2);

        //state set in invalidateStates

        quickSwitchLinear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    Toast.makeText(MainActivity.this, "This beta feature is currently only available on Android 5.0+. Stay tuned to updates!", Toast.LENGTH_LONG).show();
                    return;
                }

                boolean checked = !mQuickSwitch.isChecked();
                /*if (checked && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    Toast.makeText(MainActivity.this, "Currently on Android 4.x, only one bubble is open at a time - newer load requests override old ones. Stay tuned for updates!", Toast.LENGTH_LONG).show();
                }*/

                if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(MainActivity.this)) {
                    new MaterialDialog.Builder(MainActivity.this)
                            .content(R.string.dialog_draw_overlay)
                            .positiveText(R.string.open_settings)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @TargetApi(Build.VERSION_CODES.M)
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    try {
                                        //Open the current default browswer App Info page
                                        openSettings(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, BuildConfig.APPLICATION_ID);
                                    } catch (ActivityNotFoundException ignored) {
                                        Crashlytics.logException(ignored);
                                        Toast.makeText(MainActivity.this, R.string.open_settings_failed_overlay, Toast.LENGTH_LONG).show();
                                    }
                                }
                            })
                            .show();
                } else {
                    PrefUtils.setQuickSwitch(MainActivity.this, checked);
                    mQuickSwitch.setChecked(checked);
                }
            }
        });

        //DEFAULT TOOLBAR COLOR
        final ViewGroup defaultToolbarLinear = (ViewGroup) findViewById(R.id.linear_default_toolbar_color);
        mDefaultToolbarColorView = defaultToolbarLinear.getChildAt(2);

        defaultToolbarLinear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    new ColorChooserDialog.Builder(MainActivity.this, R.string.default_toolbar_color)
                            .preselect(PrefUtils.getDefaultToolbarColor(MainActivity.this))
                            .customButton(R.string.custom)
                            .doneButton(R.string.done)
                            .cancelButton(android.R.string.cancel)
                            .show();
                } catch (IllegalStateException e) {
                    Crashlytics.logException(e);
                }
            }
        });

        mDefaultToolbarColorView.setBackgroundColor(PrefUtils.getDefaultToolbarColor(MainActivity.this));

        //DYNAMIC TOOLBAR COLOR
        final ViewGroup dynamicToolbarLinear = (ViewGroup) findViewById(R.id.linear_dynamic_toolbar_color);
        final SwitchCompat dynamicToolbarSwitch = (SwitchCompat) dynamicToolbarLinear.getChildAt(2);

        dynamicToolbarSwitch.setChecked(PrefUtils.isDynamicToolbar(this));

        dynamicToolbarLinear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean checked = !dynamicToolbarSwitch.isChecked();
                PrefUtils.setDynamicToolbar(MainActivity.this, checked);
                dynamicToolbarSwitch.setChecked(checked);
            }
        });

        //DYNAMIC APP-BASED TOOLBAR COLOR
        final ViewGroup appBasedToolbarLinear = (ViewGroup) findViewById(R.id.linear_dynamic_app_toolbar_color);
        mPageBasedToolbarSwitch = (SwitchCompat) appBasedToolbarLinear.getChildAt(2);

        //state set in invalidateStates

        appBasedToolbarLinear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean checked = !mPageBasedToolbarSwitch.isChecked();
                if (checked && !isAccessibilityServiceEnabled(MainActivity.this)) {
                    new MaterialDialog.Builder(MainActivity.this)
                            .positiveText(android.R.string.ok)
                            .content(R.string.enable_service_page_toolbar)
                            .show();
                } else {
                    PrefUtils.setDynamicAppBasedToolbar(MainActivity.this, checked);
                    mPageBasedToolbarSwitch.setChecked(checked);
                }
            }
        });

        //FLOATING WINDOW SWITCH
        final ViewGroup floatingWindowLinear = (ViewGroup) findViewById(R.id.linear_preload_window);
        final SwitchCompat floatingWindowSwitch = (SwitchCompat) floatingWindowLinear.getChildAt(1);
        floatingWindowSwitch.setChecked(FloatingMonitorService.get() != null);

        floatingWindowLinear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean checked = onFloatingWindowSwitchChange(!floatingWindowSwitch.isChecked());
                floatingWindowSwitch.setChecked(checked);
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

        if (PrefUtils.isFirstRun(this)) {
            PrefUtils.setVersionCode(this, BuildConfig.VERSION_CODE);
        }

        processIntent();

        DbUtil.initializeBlacklist(this);
        MainAccessibilityService.updateFilterList();

        invalidateStates();
    }

    private void processIntent() {
        if (BuildConfig.VERSION_CODE > PrefUtils.getVersionCode(this) ||
                getIntent().getBooleanExtra(EXTRA_SHOW_CHANGELOG, false)) {
            showChangelog();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        processIntent();
    }

    private void setupAnimationStyle() {
        final Spinner spinner = (Spinner) findViewById(R.id.spinner_animation);
        ArrayAdapter<AnimationStyle> adapter = new ArrayAdapter<AnimationStyle>(this,
                R.layout.spinner_dropdown, AnimationStyle.values()) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view;

                if (convertView == null) {
                    view = getLayoutInflater().inflate(R.layout.spinner_dropdown_icon, parent, false);
                } else {
                    view = convertView;
                }

                TextView text = (TextView) view.findViewById(android.R.id.text1);
                ImageView imageView = (ImageView) view.findViewById(R.id.image_app);

                text.setText(getItem(position).name);
                imageView.setImageResource(getItem(position).icon);

                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                return getView(position, convertView, parent);
            }

            @Override
            public long getItemId(int position) {
                return getItem(position).id;
            }
        };
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                PrefUtils.setAnimationStyle(MainActivity.this, AnimationStyle.valueWithId((int) id));
                spinner.setDropDownVerticalOffset(Utils.dp2px(MainActivity.this, spinner.getSelectedItemPosition() * -48));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //Ignore
            }
        });
        spinner.setSelection(AnimationStyle.valueWithId(PrefUtils.getAnimationStyle(MainActivity.this)).ordinal());
        spinner.setDropDownVerticalOffset(Utils.dp2px(this, spinner.getSelectedItemPosition() * -48));
    }

    private void showChangelog() {
        ChangelogDialog.newInstance().show(getFragmentManager(), "CHANGELOG_DIALOG");

        PrefUtils.setVersionCode(this, BuildConfig.VERSION_CODE);
    }

    boolean onFloatingWindowSwitchChange(boolean checked) {
        Intent service = new Intent(MainActivity.this, FloatingMonitorService.class);

        if (checked) {
            if (FloatingMonitorService.get() == null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(MainActivity.this)) {
                    new MaterialDialog.Builder(MainActivity.this)
                            .content(R.string.dialog_draw_overlay)
                            .positiveText(R.string.open_settings)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @TargetApi(Build.VERSION_CODES.M)
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    try {
                                        //Open the current default browswer App Info page
                                        openSettings(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, BuildConfig.APPLICATION_ID);
                                    } catch (ActivityNotFoundException ignored) {
                                        Crashlytics.logException(ignored);
                                        Toast.makeText(MainActivity.this, R.string.open_settings_failed_overlay, Toast.LENGTH_LONG).show();
                                    }
                                }
                            })
                            .show();
                    return false;
                } else {
                    startService(service);
                }
            }
            return true;
        } else {
            stopService(service);
            return false;
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
            case R.id.menu_main_changelog:
                showChangelog();
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
                .linkColorRes(R.color.blue_800)
                .positiveColorRes(R.color.blue_800)
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
            viewUrlIntent.setClass(MainActivity.this, BrowserLauncherActivity.class);
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
                    .positiveColorRes(R.color.blue_800)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            try {
                                //Open the current default browswer App Info page
                                openSettings(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, current);
                            } catch (ActivityNotFoundException ignored) {
                                Crashlytics.logException(ignored);
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
        mPageBasedToolbarSwitch.setChecked(accessibilityServiceEnabled && PrefUtils.isDynamicAppBasedToolbar(this));
        mQuickSwitch.setChecked(PrefUtils.isQuickSwitch(this)
                && (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(MainActivity.this)));

        boolean isSetAsDefault = isSetAsDefault();

        Drawable drawable = DrawableCompat.wrap(mSetDefaultImage.getDrawable().mutate());
        DrawableCompat.setTintMode(drawable, PorterDuff.Mode.SRC_ATOP);
        DrawableCompat.setTint(drawable, ContextCompat.getColor(this, isSetAsDefault ? R.color.green_500 : R.color.blue_800));
        mSetDefaultImage.setImageDrawable(drawable);
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


    @Override
    public void onColorSelection(@NonNull ColorChooserDialog dialog, @ColorInt int selectedColor) {
        PrefUtils.setDefaultToolbarColor(MainActivity.this, selectedColor);

        mDefaultToolbarColorView.setBackgroundColor(selectedColor);
    }
}
