package com.pluscubed.anticipate;


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

public class FloatingWindowService extends Service {

    private static FloatingWindowService sSharedService;

    String mOldDisplayed="";
    String mDisplayed="";

    TextView mTextView;
    private WindowManager mWindowManager;

    public static FloatingWindowService get() {
        return sSharedService;
    }

    public void setText(final String url) {
        mOldDisplayed = mDisplayed;
        mDisplayed=url;

        if(!mOldDisplayed.equals(mDisplayed))
            mTextView.setText(mDisplayed);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Intent notificationIntent = new Intent(this, FloatingWindowService.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.anticipate_url_monitor))
                .setContentText(getString(R.string.notification_tap_to_close))
                .setPriority(Notification.PRIORITY_MIN)
                .setSmallIcon(R.drawable.earth)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        /*ImageView chatHead = new ImageView(this);
        chatHead.setImageResource(R.drawable.earth);*/


        mTextView = (TextView) LayoutInflater.from(this).inflate(R.layout.floating_monitor, null, false);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE| WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.BOTTOM | Gravity.START;
        params.x = 0;
        params.y = 0;

        mWindowManager.addView(mTextView, params);


        //mTextView.setTextColor(Color.BLACK);


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
