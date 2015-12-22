package com.pluscubed.anticipate;


import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.TextView;

public class MainService extends Service {

    private static MainService sSharedService;
    TextView mTextView;
    private Handler mHandler;
    private WindowManager mWindowManager;

    public static MainService get() {
        return sSharedService;
    }

    public void addUrl(final String url) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mTextView.append((mTextView.getText().length() == 0 ? "" : "\n") + url);
            }
        });

    }

    public void clear() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mTextView.setText("");
            }
        });
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mHandler = new Handler();

        Intent notificationIntent = new Intent(this, MainService.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new Notification.Builder(this)
                .setContentTitle("Anticipate")
                .setContentText("Tap to close.")
                .setPriority(Notification.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(42, notification);

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        /*ImageView chatHead = new ImageView(this);
        chatHead.setImageResource(R.drawable.earth);*/


        mTextView = (TextView) LayoutInflater.from(this).inflate(R.layout.floating_monitor, null, false);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
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
