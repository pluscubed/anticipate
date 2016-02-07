package com.pluscubed.anticipate.quickswitch;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;

import com.pluscubed.anticipate.BrowserLauncherActivity;
import com.pluscubed.anticipate.R;

import java.util.List;

class BubbleOnTouchListener implements View.OnTouchListener {

    final QuickSwitchService mQuickSwitchService;
    private final int mBubbleWidth;
    private final String mUrl;
    private final int mBubbleBgPadding;
    boolean mDiscardAnimatingIn;
    boolean mDiscardAnimatingOut;
    private float mInitialTouchX;
    private float mInitialTouchY;
    private int mInitialX;
    private int mInitialY;
    private long mStartClickTime;
    private boolean mIsClick;

    public BubbleOnTouchListener(QuickSwitchService quickSwitchService, String url) {
        mQuickSwitchService = quickSwitchService;
        mBubbleWidth = quickSwitchService.getResources().getDimensionPixelSize(R.dimen.bubble_size);
        mBubbleBgPadding = quickSwitchService.getResources().getDimensionPixelSize(R.dimen.bubble_bg_padding);

        mUrl = url;
    }

    @Override
    public synchronized boolean onTouch(View v, MotionEvent event) {
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) v.getLayoutParams();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mInitialTouchX = event.getRawX();
                mInitialTouchY = event.getRawY();

                mInitialX = params.x;
                mInitialY = params.y;

                mStartClickTime = System.currentTimeMillis();

                mIsClick = true;
                return true;
            case MotionEvent.ACTION_MOVE:
                float dX = event.getRawX() - mInitialTouchX;
                float dY = event.getRawY() - mInitialTouchY;
                if ((mIsClick && (Math.abs(dX) > 10 || Math.abs(dY) > 10))
                        || System.currentTimeMillis() - mStartClickTime > ViewConfiguration.getLongPressTimeout()) {
                    mIsClick = false;
                }

                if (!mIsClick) {
                    animateInDiscard();

                    int[] discardLocation = new int[2];
                    mQuickSwitchService.getDiscardBubble().getLocationOnScreen(discardLocation);

                    if (isTouchInDiscard(event, discardLocation[0], discardLocation[1])) {
                        //In discard bubble
                        params.x = (int) mQuickSwitchService.getDiscardBubble().getX() - mBubbleBgPadding;
                        params.y = (int) mQuickSwitchService.getDiscardBubble().getY() - mBubbleBgPadding;
                    } else {
                        params.x = (int) (dX + mInitialX);
                        params.y = (int) (dY + mInitialY);
                    }

                    mQuickSwitchService.getWindowManager().updateViewLayout(v, params);
                }
                return true;
            case MotionEvent.ACTION_UP:
                int[] discardLocation = new int[2];
                mQuickSwitchService.getDiscardBubble().getLocationOnScreen(discardLocation);

                if (mIsClick && System.currentTimeMillis() - mStartClickTime <= ViewConfiguration.getLongPressTimeout()) {
                    Intent intent = new Intent(mQuickSwitchService, BrowserLauncherActivity.class);
                    intent.setData(Uri.parse(mUrl));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra(BrowserLauncherActivity.EXTRA_ADD_QUEUE, false);
                    mQuickSwitchService.startActivity(intent);

                    mQuickSwitchService.removeUrl(mUrl);
                } else if (isTouchInDiscard(event, discardLocation[0], discardLocation[1])) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        ActivityManager manager = (ActivityManager) mQuickSwitchService.getSystemService(Context.ACTIVITY_SERVICE);
                        List<ActivityManager.AppTask> appTasks;
                        appTasks = manager.getAppTasks();

                        for (ActivityManager.AppTask task : appTasks) {
                            Intent baseIntent = task.getTaskInfo().baseIntent;
                            if (baseIntent.getDataString() != null && baseIntent.getDataString().equals(mUrl)) {
                                task.finishAndRemoveTask();
                            }
                        }
                    }

                    mQuickSwitchService.removeUrl(mUrl);

                    animateOutDiscard();
                } else {
                    mQuickSwitchService.animateViewToSideSlot(v);

                    animateOutDiscard();
                }
                return true;
        }
        return false;
    }

    private boolean isTouchInDiscard(MotionEvent event, float discardX, float discardY) {
        return event.getRawX() >= discardX && event.getRawX() <= discardX + mBubbleWidth
                && event.getRawY() >= discardY && event.getRawY() <= discardY + mBubbleWidth;
    }

    private void animateInDiscard() {
        if (mQuickSwitchService.getDiscardLayout().getAlpha() != 1 && !mDiscardAnimatingIn) {
            mDiscardAnimatingIn = true;
            mDiscardAnimatingOut = false;
            mQuickSwitchService.getDiscardLayout().clearAnimation();
            if (!mQuickSwitchService.getDiscardLayout().isShown()) {
                mQuickSwitchService.getWindowManager().addView(mQuickSwitchService.getDiscardLayout(), mQuickSwitchService.getDiscardLayout().getLayoutParams());
            }
            mQuickSwitchService.getDiscardLayout().animate().alpha(1).setDuration(200).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mDiscardAnimatingIn = false;
                }
            }).start();
        }
    }

    private void animateOutDiscard() {
        if (mQuickSwitchService.getDiscardLayout().getAlpha() != 0 && !mDiscardAnimatingOut) {
            mDiscardAnimatingOut = true;
            mDiscardAnimatingIn = false;
            mQuickSwitchService.getDiscardLayout().clearAnimation();
            mQuickSwitchService.getDiscardLayout().animate().alpha(0).setDuration(200).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mDiscardAnimatingOut = false;
                    if (!mDiscardAnimatingIn) {
                        if (mQuickSwitchService.getDiscardLayout().isShown()) {
                            mQuickSwitchService.getWindowManager().removeView(mQuickSwitchService.getDiscardLayout());
                        }
                    }
                }
            }).start();
        }
    }
}
