package com.pluscubed.anticipate;


import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.TextView;

public class FloatingMonitorService extends Service {

    public static final int NOTIFICATION_FLOATING_WINDOW = 1;
    public static final int PENDING_CLOSE = 5;
    private static FloatingMonitorService sSharedService;

    String mOldDisplayed = "";
    String mDisplayed = "";

    TextView mTextView;
    private WindowManager mWindowManager;

    public static FloatingMonitorService get() {
        return sSharedService;
    }

    public void setText(final String url) {
        mOldDisplayed = mDisplayed;
        mDisplayed = url;

        if (!mOldDisplayed.equals(mDisplayed))
            mTextView.setText(mDisplayed);
    }

    @SuppressLint("InflateParams")
    @Override
    public void onCreate() {
        super.onCreate();

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, PENDING_CLOSE, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.anticipate_url_monitor))
                .setContentText(getString(R.string.notification_tap_to_close))
                .setPriority(Notification.PRIORITY_MIN)
                .setSmallIcon(R.drawable.ic_public_white_24dp)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(NOTIFICATION_FLOATING_WINDOW, notification);

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        mTextView = (TextView) LayoutInflater.from(this).inflate(R.layout.floating_monitor, null, false);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.BOTTOM | Gravity.START;
        params.x = 0;
        params.y = 0;

        mWindowManager.addView(mTextView, params);


        sSharedService = this;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sSharedService = null;

        mWindowManager.removeView(mTextView);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
