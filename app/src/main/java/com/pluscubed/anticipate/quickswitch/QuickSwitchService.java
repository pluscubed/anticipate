package com.pluscubed.anticipate.quickswitch;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsCallback;
import android.support.v7.app.NotificationCompat;
import android.support.v7.graphics.Palette;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.github.florent37.glidepalette.BitmapPalette;
import com.github.florent37.glidepalette.GlidePalette;
import com.pluscubed.anticipate.BrowserLauncherActivity;
import com.pluscubed.anticipate.MainAccessibilityService;
import com.pluscubed.anticipate.R;
import com.pluscubed.anticipate.customtabs.util.CustomTabConnectionHelper;
import com.pluscubed.anticipate.util.PrefUtils;
import com.pluscubed.anticipate.util.ScrimUtil;
import com.pluscubed.anticipate.widget.ProgressWheel;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

public class QuickSwitchService extends Service {

    public static final String EXTRA_STOP = "com.pluscubed.anticipate.EXTRA_STOP";
    public static final String EXTRA_FINISH_LOADING = "com.pluscubed.anticipate.EXTRA_FINISH_LOADING";
    public static final String EXTRA_CHECK_BUBBLES = "com.pluscubed.anticipate.EXTRA_CHECK_BUBBLES";

    public static final int NOTIFICATION_FLOATING_WINDOW = 23;
    public static final int PENDING_STOP = 6;

    private static QuickSwitchService sSharedService;
    LinkedHashMap<String, BubbleViewHolder> mQuickSwitchWebsites;
    WindowManager mWindowManager;
    boolean mPendingPageLoadStart;

    private View mDiscardLayout;
    private ImageView mDiscardBubble;
    private View mDiscardAlphaLayout;

    private boolean mUsingAccessibility;
    private CustomTabConnectionHelper mCustomTabConnectionHelper;

    public static QuickSwitchService get() {
        return sSharedService;
    }

    public WindowManager getWindowManager() {
        return mWindowManager;
    }

    public View getDiscardAlphaLayout() {
        return mDiscardAlphaLayout;
    }

    public CustomTabConnectionHelper getCustomTabConnectionHelper() {
        return mCustomTabConnectionHelper;
    }

    @SuppressLint("InflateParams")
    private void addBubble(final String url, boolean loaded) {
        LayoutInflater inflater = LayoutInflater.from(this);
        initDiscard(inflater);
        if (!mDiscardLayout.isShown()) {
            mWindowManager.addView(mDiscardLayout, mDiscardLayout.getLayoutParams());
        }

        final BubbleViewHolder holder = new BubbleViewHolder();
        holder.root = inflater.inflate(R.layout.bubble_quick_switch, null);
        holder.icon = (ImageView) holder.root.findViewById(R.id.bubble_quick_switch_icon);
        holder.progress = (ProgressWheel) holder.root.findViewById(R.id.bubble_quick_switch_progress);

        final int defaultToolbarColor = PrefUtils.getDefaultToolbarColor(this);
        holder.progress.setBarColor(defaultToolbarColor);

        final int bubbleWidth = getResources().getDimensionPixelSize(R.dimen.bubble_size_padding);

        holder.icon.setOnTouchListener(new BubbleOnTouchListener(this, url, holder));

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                bubbleWidth,
                bubbleWidth,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);

        Uri faviconUri = Uri.parse(url);
        Glide.with(this)
                .load(faviconUri)
                .placeholder(R.mipmap.ic_launcher)
                .error(R.mipmap.ic_launcher)
                .listener(GlidePalette.with(faviconUri.toString())
                        .intoCallBack(new BitmapPalette.CallBack() {
                            @Override
                            public void onPaletteLoaded(@Nullable Palette palette) {

                                int color = palette.getVibrantColor(defaultToolbarColor);

                                if (color == defaultToolbarColor) {
                                    List<Palette.Swatch> swatches = palette.getSwatches();
                                    if (swatches.size() > 0) {
                                        Palette.Swatch most = Collections.max(swatches, new Comparator<Palette.Swatch>() {
                                            @Override
                                            public int compare(Palette.Swatch lhs, Palette.Swatch rhs) {
                                                if (lhs.getPopulation() < rhs.getPopulation()) {
                                                    return -1;
                                                } else if (lhs.getPopulation() == rhs.getPopulation()) {
                                                    return 0;
                                                } else {
                                                    return 1;
                                                }
                                            }
                                        });
                                        color = most.getRgb();
                                    }
                                }

                                holder.progress.setBarColor(color);
                            }
                        }))
                .into(holder.icon);

        params.gravity = Gravity.LEFT | Gravity.TOP;

        final Point size = new Point();
        mWindowManager.getDefaultDisplay().getSize(size);
        params.x = size.x;
        params.y = size.y / 3;

        mWindowManager.addView(holder.root, params);

        mQuickSwitchWebsites.put(url, holder);


        int endX = size.x - bubbleWidth * 7 / 10;
        ValueAnimator valueAnimator = ValueAnimator.ofInt(params.x, endX)
                .setDuration(300);
        valueAnimator.setInterpolator(new OvershootInterpolator(1.5f));
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                WindowManager.LayoutParams params = (WindowManager.LayoutParams) holder.root.getLayoutParams();
                params.x = (int) animation.getAnimatedValue();

                mWindowManager.updateViewLayout(holder.root, params);
            }
        });
        valueAnimator.start();

        if (!loaded) {
            if (MainAccessibilityService.get() == null) {
                mPendingPageLoadStart = true;
            } else {
                MainAccessibilityService.get().pendLoadStart();
            }
        } else {
            holder.progress.setInstantProgress(1);
            holder.done = true;
        }
    }

    private void initDiscard(LayoutInflater inflater) {
        if (mDiscardLayout == null) {
            mDiscardLayout = inflater.inflate(R.layout.bubble_discard_bg, null);
            final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.LEFT | Gravity.TOP;

            mDiscardLayout.setLayoutParams(params);

            View discardScrim = mDiscardLayout.findViewById(R.id.bubble_discard_bg_scrim);
            discardScrim.setBackground(ScrimUtil.makeCubicGradientScrimDrawable(0xaa000000, 8, Gravity.BOTTOM));

            mDiscardBubble = (ImageView) mDiscardLayout.findViewById(R.id.bubble_discard_bg_bubble);
            mDiscardAlphaLayout = mDiscardLayout.findViewById(R.id.bubble_discard_bg_alpha);

            mDiscardAlphaLayout.setAlpha(0);
        }
    }

    void animateViewToSideSlot(final View view) {
        int bubbleWidth = getResources().getDimensionPixelSize(R.dimen.bubble_size_padding);

        Point size = new Point();
        mWindowManager.getDefaultDisplay().getSize(size);

        WindowManager.LayoutParams params = (WindowManager.LayoutParams) view.getLayoutParams();
        int endX;
        if (params.x + bubbleWidth / 2 >= size.x / 2) {
            endX = size.x - bubbleWidth * 7 / 10;
        } else {
            endX = -bubbleWidth * 3 / 10;
        }

        int endY;
        endY = params.y;

        ValueAnimator valueAnimator = ValueAnimator.ofInt(params.x, endX)
                .setDuration(300);
        valueAnimator.setInterpolator(new OvershootInterpolator(1.5f));
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                WindowManager.LayoutParams params = (WindowManager.LayoutParams) view.getLayoutParams();
                params.x = (int) animation.getAnimatedValue();
            }
        });

        ValueAnimator valueAnimator2 = ValueAnimator.ofInt(params.y, endY)
                .setDuration(300);
        valueAnimator2.setInterpolator(new OvershootInterpolator(1.5f));
        valueAnimator2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                WindowManager.LayoutParams params = (WindowManager.LayoutParams) view.getLayoutParams();
                params.y = (int) animation.getAnimatedValue();

                if (view.isShown()) {
                    mWindowManager.updateViewLayout(view, params);
                }
            }
        });

        AnimatorSet set = new AnimatorSet();
        set.playTogether(valueAnimator, valueAnimator2);
        set.start();
    }


    @Override
    public void onCreate() {
        super.onCreate();

        sSharedService = this;

        mQuickSwitchWebsites = new LinkedHashMap<>();
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        Intent notificationIntent = new Intent(this, getClass());
        notificationIntent.putExtra(EXTRA_STOP, true);
        PendingIntent pendingIntent = PendingIntent.getService(this, PENDING_STOP, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.bubble_notif))
                .setContentText(getString(R.string.bubble_notif_desc))
                .setPriority(Notification.PRIORITY_MIN)
                .setSmallIcon(R.drawable.ic_swap_vertical_circle_black_24dp)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(NOTIFICATION_FLOATING_WINDOW, notification);

        mUsingAccessibility = true;
    }

    private void updateUsingAccessibility() {
        if (MainAccessibilityService.get() == null && mUsingAccessibility) {
            mUsingAccessibility = false;
            mCustomTabConnectionHelper = new CustomTabConnectionHelper();
            mCustomTabConnectionHelper.setCustomTabsCallback(new TabsNavigationCallback());
            mCustomTabConnectionHelper.bindCustomTabsService(this);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        sSharedService = null;

        if (mCustomTabConnectionHelper != null) {
            mCustomTabConnectionHelper.unbindCustomTabsService(this);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getBooleanExtra(EXTRA_STOP, false)) {
            stopSelf();
            return super.onStartCommand(intent, flags, startId);
        }

        updateUsingAccessibility();

        if (intent != null && intent.getBooleanExtra(EXTRA_FINISH_LOADING, false)) {
            finishLoadingBubble();
            return super.onStartCommand(intent, flags, startId);
        }

        if (intent != null && intent.getBooleanExtra(EXTRA_CHECK_BUBBLES, false)) {
            checkBubbles();
            return super.onStartCommand(intent, flags, startId);
        }


        String url = intent != null ? intent.getDataString() : null;
        if (!mQuickSwitchWebsites.containsKey(url)) {
            addBubble(url, false);
        } else {
            removeUrl(url);
        }

        checkExit();

        return super.onStartCommand(intent, flags, startId);
    }

    void checkBubbles() {
        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {

            Set<String> taskOpenUrls = new HashSet<>();

            ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            for (ActivityManager.AppTask task : manager.getAppTasks()) {
                Intent baseIntent = task.getTaskInfo().baseIntent;
                String url = baseIntent.getDataString();
                if (url != null && task.getTaskInfo().id > -1) {
                    taskOpenUrls.add(url);
                }
            }

            for (Iterator<String> iterator = mQuickSwitchWebsites.keySet().iterator(); iterator.hasNext(); ) {
                String quickSwitchUrl = iterator.next();
                if (!taskOpenUrls.contains(quickSwitchUrl)) {
                    removeUrl(quickSwitchUrl, false);
                    iterator.remove();
                }
            }


            for (String taskOpenUrl : taskOpenUrls) {
                if (!mQuickSwitchWebsites.containsKey(taskOpenUrl)) {
                    addBubble(taskOpenUrl, true);
                }
            }
        }
    }

    void removeUrl(String url) {
        removeUrl(url, true);
    }

    void removeUrl(String url, boolean removeFromList) {
        mWindowManager.removeView(mQuickSwitchWebsites.get(url).root);
        if (removeFromList) {
            mQuickSwitchWebsites.remove(url);
        }

        checkExit();
    }

    private void checkExit() {
        if (mQuickSwitchWebsites.size() == 0 && mUsingAccessibility) {
            mWindowManager.removeView(mDiscardLayout);
            stopSelf();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public ImageView getDiscardBubble() {
        return mDiscardBubble;
    }

    void finishLoadingBubble() {
        for (BubbleViewHolder holder : mQuickSwitchWebsites.values()) {
            if (!holder.done) {
                holder.progress.setProgress(1);
                holder.done = true;
                break;
            }
        }
    }

    private class TabsNavigationCallback extends CustomTabsCallback {
        TabsNavigationCallback() {
        }

        @Override
        public void onNavigationEvent(int navigationEvent, Bundle extras) {
            super.onNavigationEvent(navigationEvent, extras);

            switch (navigationEvent) {
                case TAB_SHOWN:
                    break;
                case TAB_HIDDEN:
                    checkBubbles();
                    break;
                case NAVIGATION_FINISHED:
                    finishLoadingBubble();
                    break;
                case NAVIGATION_STARTED:
                    if (mPendingPageLoadStart) {
                        BrowserLauncherActivity.moveInstanceToBack();
                        mPendingPageLoadStart = false;
                    }
                    break;
            }
        }
    }
}
