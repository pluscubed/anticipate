package com.pluscubed.anticipate.quickswitch;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
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
    BubbleViewHolder mViewHolder;
    Animator mDiscardAnimatingIn;
    Animator mDiscardAnimatingOut;
    private float mInitialTouchX;
    private float mInitialTouchY;
    private int mInitialX;
    private int mInitialY;
    private long mStartClickTime;
    private boolean mIsClick;

    public BubbleOnTouchListener(QuickSwitchService quickSwitchService, String url, BubbleViewHolder holder) {
        mQuickSwitchService = quickSwitchService;
        mBubbleWidth = quickSwitchService.getResources().getDimensionPixelSize(R.dimen.bubble_size);
        mBubbleBgPadding = quickSwitchService.getResources().getDimensionPixelSize(R.dimen.bubble_bg_padding);
        mViewHolder = holder;

        mUrl = url;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) mViewHolder.root.getLayoutParams();

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

                    mQuickSwitchService.getWindowManager().updateViewLayout(mViewHolder.root, params);
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
                    mQuickSwitchService.animateViewToSideSlot(mViewHolder.root);

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
        if (mQuickSwitchService.getDiscardLayout().getAlpha() != 1 && mDiscardAnimatingIn == null) {

            if (mDiscardAnimatingOut != null) {
                mDiscardAnimatingOut.cancel();
                mDiscardAnimatingOut = null;
            }

            mDiscardAnimatingIn = ObjectAnimator.ofFloat(mQuickSwitchService.getDiscardLayout(), View.ALPHA, 1f);
            mDiscardAnimatingIn.setDuration(200)
                    .addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            mDiscardAnimatingIn = null;
                        }
                    });
            mDiscardAnimatingIn.start();
        }
    }

    private void animateOutDiscard() {
        if (mQuickSwitchService.getDiscardLayout().getAlpha() != 0 && mDiscardAnimatingOut == null) {
            if (mDiscardAnimatingIn != null) {
                mDiscardAnimatingIn.cancel();
                mDiscardAnimatingIn = null;
            }
            mDiscardAnimatingOut = ObjectAnimator.ofFloat(mQuickSwitchService.getDiscardLayout(), View.ALPHA, 0f);
            mDiscardAnimatingOut.setDuration(200)
                    .addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            mDiscardAnimatingOut = null;
                        }
                    });
            mDiscardAnimatingOut.start();
        }
    }
}
