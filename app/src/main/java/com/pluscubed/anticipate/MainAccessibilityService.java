package com.pluscubed.anticipate;

import android.accessibilityservice.AccessibilityService;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsCallback;
import android.support.customtabs.CustomTabsService;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.NotificationCompat;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.github.florent37.glidepalette.BitmapPalette;
import com.github.florent37.glidepalette.GlidePalette;
import com.pluscubed.anticipate.customtabs.util.CustomTabConnectionHelper;
import com.pluscubed.anticipate.filter.AppInfo;
import com.pluscubed.anticipate.filter.DbUtil;
import com.pluscubed.anticipate.util.LimitedQueue;
import com.pluscubed.anticipate.util.PrefUtils;
import com.pluscubed.anticipate.util.ScrimUtil;
import com.pluscubed.anticipate.widget.ProgressWheel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rx.Observable;
import rx.SingleSubscriber;
import rx.functions.Func1;

public class MainAccessibilityService extends AccessibilityService {

    public static final String TAG = "Accesibility";
    public static final String LINK_REG_EX = "((?:[a-z][\\w-]+:(?:\\/{1,3}|[a-z0-9%])|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}/)(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\))+(?:\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:'\".,<>?«»“”‘’]))";
    public static final int NOTIFICATION_CHANGELOG = 2;

    private static MainAccessibilityService sSharedService;
    List<String> mFilterList;
    boolean mBlacklistMode;
    LimitedQueue<AppUsageEntry> mAppsUsed;

    LinkedHashMap<String, BubbleViewHolder> mQueuedWebsites;
    View mDiscardLayout;
    WindowManager mWindowManager;
    boolean mPendingPageLoadStart;
    Handler mMainHandler;
    private View mDiscardScrim;
    private ImageView mDiscardBubble;
    private CustomTabConnectionHelper mCustomTabActivityHelper;

    public static MainAccessibilityService get() {
        return sSharedService;
    }

    public static void updateFilterList() {
        if (MainAccessibilityService.get() != null) {
            MainAccessibilityService.get().updateBlackWhitelistInternal();
        }
    }

    private static void log(String info) {
        if (BuildConfig.DEBUG)
            Log.i(TAG, info);
    }

    public CustomTabConnectionHelper getCustomTabActivityHelper() {
        return mCustomTabActivityHelper;
    }

    public void addQueuedWebsite(String url) {
        if (!mQueuedWebsites.containsKey(url)) {
            addBubble(url);
        } else {
            mWindowManager.removeView(mQueuedWebsites.get(url).root);
            mQueuedWebsites.remove(url);
        }
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        sSharedService = this;

        mMainHandler = new Handler(getMainLooper());

        mAppsUsed = new LimitedQueue<>(10);
        mQueuedWebsites = new LinkedHashMap<>();

        mCustomTabActivityHelper = new CustomTabConnectionHelper();
        mCustomTabActivityHelper.setCustomTabsCallback(new TabsNavigationCallback());
        boolean success = mCustomTabActivityHelper.bindCustomTabsService(this);
        log("onServiceConnected: " + success);

        DbUtil.initializeBlacklist(this);

        updateFilterList();

        if (BuildConfig.VERSION_CODE > PrefUtils.getVersionCode(this)) {
            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

            Notification notification = new NotificationCompat.Builder(this)
                    .setContentTitle(getString(R.string.anticipate_update) + BuildConfig.VERSION_NAME)
                    .setContentText(getString(R.string.anticipate_update_desc))
                    .setSmallIcon(R.drawable.ic_trending_up_black_24dp)
                    .setContentIntent(pendingIntent)
                    .build();

            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.notify(NOTIFICATION_CHANGELOG, notification);
        }

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

    }

    @SuppressLint("InflateParams")
    private void addBubble(final String url) {
        LayoutInflater inflater = LayoutInflater.from(this);

        if (mDiscardLayout == null) {
            mDiscardLayout = inflater.inflate(R.layout.bubble_discard_bg, null);
            final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.LEFT | Gravity.TOP;

            mWindowManager.addView(mDiscardLayout, params);

            mDiscardScrim = mDiscardLayout.findViewById(R.id.bubble_discard_bg_scrim);
            mDiscardScrim.setBackground(ScrimUtil.makeCubicGradientScrimDrawable(0xaa000000, 8, Gravity.BOTTOM));

            mDiscardBubble = (ImageView) mDiscardLayout.findViewById(R.id.bubble_discard_bg_bubble);

            mDiscardLayout.setAlpha(0);
        }

        final BubbleViewHolder holder = new BubbleViewHolder();
        holder.root = inflater.inflate(R.layout.bubble_quick_switch, null);
        holder.icon = (ImageView) holder.root.findViewById(R.id.bubble_quick_switch_icon);
        holder.progress = (ProgressWheel) holder.root.findViewById(R.id.bubble_quick_switch_progress);

        holder.progress.setBarColor(PrefUtils.getDefaultToolbarColor(this));

        final int bubbleWidth = getResources().getDimensionPixelSize(R.dimen.floating_size);

        holder.root.setOnTouchListener(new BubbleOnTouchListener(bubbleWidth, url));

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                bubbleWidth,
                bubbleWidth,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);

        Uri faviconUri = Uri.parse("http://www.google.com/s2/favicons?domain_url=" + url);
        Glide.with(this)
                .load(faviconUri)
                .placeholder(R.mipmap.ic_launcher)
                .error(R.mipmap.ic_launcher)
                .listener(GlidePalette.with(faviconUri.toString())
                        .intoCallBack(new BitmapPalette.CallBack() {
                            @Override
                            public void onPaletteLoaded(@Nullable Palette palette) {
                                holder.progress.setBarColor(palette.getVibrantColor(PrefUtils.getDefaultToolbarColor(MainAccessibilityService.this)));
                            }
                        }))
                .into(holder.icon);

        params.gravity = Gravity.LEFT | Gravity.TOP;

        final Point size = new Point();
        mWindowManager.getDefaultDisplay().getSize(size);
        params.x = size.x;
        params.y = size.y / 3;

        mWindowManager.addView(holder.root, params);

        mQueuedWebsites.put(url, holder);


        int endX = size.x - bubbleWidth * 4 / 5;
        ValueAnimator valueAnimator = ValueAnimator.ofInt(params.x, endX)
                .setDuration(300);
        valueAnimator.setInterpolator(new OvershootInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                WindowManager.LayoutParams params = (WindowManager.LayoutParams) holder.root.getLayoutParams();
                params.x = (int) animation.getAnimatedValue();

                mWindowManager.updateViewLayout(holder.root, params);
            }
        });
        valueAnimator.start();

        mPendingPageLoadStart = true;
    }

    void animateViewToSideSlot(final View view) {
        int bubbleWidth = getResources().getDimensionPixelSize(R.dimen.floating_size);

        Point size = new Point();
        mWindowManager.getDefaultDisplay().getSize(size);

        WindowManager.LayoutParams params = (WindowManager.LayoutParams) view.getLayoutParams();
        int endX;
        if (params.x + bubbleWidth / 2 >= size.x / 2) {
            endX = size.x - bubbleWidth * 4 / 5;
        } else {
            endX = -bubbleWidth / 5;
        }

        int endY;
        endY = params.y;

        ValueAnimator valueAnimator = ValueAnimator.ofInt(params.x, endX)
                .setDuration(300);
        valueAnimator.setInterpolator(new OvershootInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                WindowManager.LayoutParams params = (WindowManager.LayoutParams) view.getLayoutParams();
                params.x = (int) animation.getAnimatedValue();
            }
        });

        ValueAnimator valueAnimator2 = ValueAnimator.ofInt(params.y, endY)
                .setDuration(300);
        valueAnimator2.setInterpolator(new OvershootInterpolator());
        valueAnimator2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                WindowManager.LayoutParams params = (WindowManager.LayoutParams) view.getLayoutParams();
                params.y = (int) animation.getAnimatedValue();

                mWindowManager.updateViewLayout(view, params);
            }
        });

        AnimatorSet set = new AnimatorSet();
        set.playTogether(valueAnimator, valueAnimator2);
        set.start();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        sSharedService = null;
        mCustomTabActivityHelper.unbindCustomTabsService(this);

        return super.onUnbind(intent);
    }

    public int getLastAppPrimaryColor() {
        //http://stackoverflow.com/questions/27121919/is-it-possible-to-get-another-applications-primary-color-like-lollipops-recent

        int defaultToolbarColor = PrefUtils.getDefaultToolbarColor(this);

        PackageManager pm = getPackageManager();

        String firstViablePackageName = null;
        int[] attrs = null;
        Resources res = null;

        for (Iterator<AppUsageEntry> iterator = mAppsUsed.descendingIterator(); iterator.hasNext(); ) {
            AppUsageEntry entry = iterator.next();

            if (!entry.packageName.equals(PrefUtils.getChromeApp(MainAccessibilityService.this)) &&
                    !entry.packageName.startsWith("com.android.systemui") &&
                    !entry.packageName.startsWith("android")) {

                if (firstViablePackageName == null) {
                    firstViablePackageName = entry.packageName;

                    // Retrieve the Resources from the app
                    try {
                        res = pm.getResourcesForApplication(firstViablePackageName);
                    } catch (PackageManager.NameNotFoundException e) {
                        return defaultToolbarColor;
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        attrs = new int[]{
                                res.getIdentifier("colorPrimary", "attr", firstViablePackageName),
                                android.R.attr.colorPrimary
                        };
                    } else {
                        attrs = new int[]{
                                res.getIdentifier("colorPrimary", "attr", firstViablePackageName)
                        };
                    }

                }

                try {
                    if (firstViablePackageName != null && firstViablePackageName.equals(entry.packageName)) {
                        final Resources.Theme theme = res.newTheme();
                        ComponentName cn = ComponentName.unflattenFromString(firstViablePackageName + "/" + entry.activityName);
                        theme.applyStyle(pm.getActivityInfo(cn, 0).theme, false);

                        // Obtain the colorPrimary color from the attrs
                        TypedArray a = theme.obtainStyledAttributes(attrs);
                        int colorPrimary = a.getColor(0, a.getColor(1, defaultToolbarColor));
                        a.recycle();

                        if (colorPrimary == ContextCompat.getColor(this, R.color.primary_material_light)
                                || colorPrimary == ContextCompat.getColor(this, R.color.primary_material_dark)) {
                            //Assume the primary color was left as default, therefore use user default color
                            colorPrimary = defaultToolbarColor;
                        }

                        return colorPrimary;
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    //ignore, continue
                }
            }
        }

        return defaultToolbarColor;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        sSharedService = this;

        log("====onAccessibilityEvent===");
        log("=type: " + AccessibilityEvent.eventTypeToString(event.getEventType()));

        if (mFilterList == null) {
            return;
        }

        CharSequence packageName = event.getPackageName();

        if (packageName != null) {
            String appId = packageName.toString();
            log("=appId: " + appId);

            if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                String activityName = event.getClassName().toString();

                boolean redundant = false;
                if (mAppsUsed.size() > 0) {
                    String lastPackageName = mAppsUsed.peekLast().packageName;
                    redundant = appId.equals(lastPackageName);
                }
                if (!redundant) {
                    AppUsageEntry entry = new AppUsageEntry();
                    entry.activityName = activityName;
                    entry.packageName = appId;

                    mAppsUsed.add(entry);
                }

                log("=activityName: " + activityName);
            }



            if ((mBlacklistMode && mFilterList.contains(appId)) ||
                    (!mBlacklistMode && !mFilterList.contains(appId))) {
                log("=excluded");
                return;
            }
        }


        String allText = getAllText(getRootInActiveWindow(), 0);

        Log.v(TAG, allText);

        Pattern pattern = Pattern.compile(LINK_REG_EX);
        Matcher matcher = pattern.matcher(allText);


        Uri firstUrl = null;
        String floatingWindowText = "";
        List<Bundle> possibleUrls = new ArrayList<>();

        while (matcher.find()) {
            String url = matcher.group(0);

            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://" + url;
            }

            Uri uri = Uri.parse(url);

            if (firstUrl == null) {
                firstUrl = uri;

                floatingWindowText += uri;
            } else {
                Bundle bundle = new Bundle();
                bundle.putParcelable(CustomTabsService.KEY_URL, uri);
                possibleUrls.add(bundle);

                floatingWindowText += "\n" + uri;
            }

            log("Preload URL: " + url);
        }


        if (firstUrl != null) {
            boolean success = mCustomTabActivityHelper.mayLaunchUrl(firstUrl, null, possibleUrls);

            if (FloatingMonitorService.get() != null) {
                FloatingMonitorService.get().setText(floatingWindowText);
            }

            log("Preload URL: " + success);
        }


    }

    void updateBlackWhitelistInternal() {
        DbUtil.getPerAppListApps(this).flatMapObservable(new Func1<List<AppInfo>, Observable<AppInfo>>() {
            @Override
            public Observable<AppInfo> call(List<AppInfo> appInfos) {
                return Observable.from(appInfos);
            }
        }).map(new Func1<AppInfo, String>() {
            @Override
            public String call(AppInfo appInfo) {
                return appInfo.packageName;
            }
        }).toList().toSingle()
                .subscribe(new SingleSubscriber<List<String>>() {

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onSuccess(List<String> strings) {
                        mFilterList = strings;
                        mBlacklistMode = PrefUtils.isBlacklistMode(MainAccessibilityService.this);
                    }
                });


    }

    private String getAllText(AccessibilityNodeInfo source, int depth) {
        if (depth > 50) {
            return "";
        }

        String string = "";

        if (source == null) {
            return string;
        }

        if (source.getText() != null) {
            String text = source.getText().toString();
            text = text.replace("\n", " ");
            string += text + " ";

            log("Text: " + text);
        }

        for (int i = 0; i < source.getChildCount(); i++) {
            string += getAllText(source.getChild(i), depth + 1);
        }

        source.recycle();

        return string;
    }

    @Override
    public void onInterrupt() {

    }

    private class BubbleViewHolder {
        View root;
        ImageView icon;
        ProgressWheel progress;
        boolean done;

        BubbleViewHolder() {
        }
    }

    private class AppUsageEntry {
        String packageName;
        String activityName;

        AppUsageEntry() {
        }
    }

    private class TabsNavigationCallback extends CustomTabsCallback {
        TabsNavigationCallback() {
        }

        @Override
        public void onNavigationEvent(int navigationEvent, Bundle extras) {
            super.onNavigationEvent(navigationEvent, extras);

            switch(navigationEvent){
                case TAB_SHOWN:
                    break;
                case TAB_HIDDEN:
                    break;
                case NAVIGATION_FINISHED:
                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainAccessibilityService.this, "Page finished", Toast.LENGTH_SHORT).show();
                        }
                    });

                    for (BubbleViewHolder holder : mQueuedWebsites.values()) {
                        if (!holder.done) {
                            ProgressWheel progress = holder.progress;
                            progress.setProgress(1);
                            holder.done = true;
                            break;
                        }
                    }
                    break;
                case NAVIGATION_STARTED:
                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainAccessibilityService.this, "Page started", Toast.LENGTH_SHORT).show();
                        }
                    });

                    if (mPendingPageLoadStart) {
                        BrowserLauncherActivity.moveToBack();
                        mPendingPageLoadStart = false;
                    }
                    break;
            }
        }
    }

    private class BubbleOnTouchListener implements View.OnTouchListener {
        private final int bubbleWidth;
        private final String url;
        float initialTouchX;
        float initialTouchY;
        int initialX;
        int initialY;

        long startClickTime;
        boolean isClick;

        boolean discardAnimatingIn;
        boolean discardAnimatingOut;

        public BubbleOnTouchListener(int bubbleWidth, String url) {
            this.bubbleWidth = bubbleWidth;
            this.url = url;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            WindowManager.LayoutParams params = (WindowManager.LayoutParams) v.getLayoutParams();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();

                    initialX = params.x;
                    initialY = params.y;

                    startClickTime = System.currentTimeMillis();

                    isClick = true;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dX = event.getRawX() - initialTouchX;
                    float dY = event.getRawY() - initialTouchY;
                    if ((isClick && (Math.abs(dX) > 10 || Math.abs(dY) > 10))
                            || System.currentTimeMillis() - startClickTime > ViewConfiguration.getLongPressTimeout()) {
                        isClick = false;
                    }

                    if (!isClick) {
                        animateInDiscard();

                        int[] discardLocation = new int[2];
                        mDiscardBubble.getLocationOnScreen(discardLocation);

                        if (isTouchInDiscard(event, discardLocation[0], discardLocation[1])) {
                            //In discard bubble
                            params.x = (int) mDiscardBubble.getX();
                            params.y = (int) mDiscardBubble.getY();
                        } else {
                            params.x = (int) (dX + initialX);
                            params.y = (int) (dY + initialY);
                        }

                        mWindowManager.updateViewLayout(v, params);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    int[] discardLocation = new int[2];
                    mDiscardBubble.getLocationOnScreen(discardLocation);

                    if (isClick && System.currentTimeMillis() - startClickTime <= ViewConfiguration.getLongPressTimeout()) {
                        Intent intent = new Intent(MainAccessibilityService.this, BrowserLauncherActivity.class);
                        intent.setData(Uri.parse(url));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra(BrowserLauncherActivity.EXTRA_ADD_QUEUE, false);
                        startActivity(intent);

                        mQueuedWebsites.remove(url);
                        mWindowManager.removeView(v);
                    } else if (isTouchInDiscard(event, discardLocation[0], discardLocation[1])) {
                        mQueuedWebsites.remove(url);
                        mWindowManager.removeView(v);

                        animateOutDiscard();
                    } else {
                        animateViewToSideSlot(v);

                        animateOutDiscard();
                    }
                    return true;
            }
            return false;
        }

        private boolean isTouchInDiscard(MotionEvent event, float discardX, float discardY) {
            return event.getRawX() >= discardX && event.getRawX() <= discardX + bubbleWidth
                    && event.getRawY() >= discardY && event.getRawY() <= discardY + bubbleWidth;
        }

        private void animateInDiscard() {
            if (!discardAnimatingIn) {
                if (discardAnimatingOut) {
                    mDiscardLayout.clearAnimation();
                }
                mDiscardLayout.animate().alpha(1).setDuration(200).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        discardAnimatingIn = false;
                    }
                }).start();
                discardAnimatingIn = true;
            }
        }

        private void animateOutDiscard() {
            if (!discardAnimatingOut) {
                if (discardAnimatingIn) {
                    mDiscardLayout.clearAnimation();
                }
                discardAnimatingOut = true;
                mDiscardLayout.animate().alpha(0).setDuration(200).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        discardAnimatingOut = false;
                    }
                }).start();
            }
        }
    }
}
