package com.pluscubed.anticipate;

import android.app.Application;

import com.afollestad.inquiry.Inquiry;
import com.bumptech.glide.Glide;
import com.crashlytics.android.Crashlytics;
import com.pluscubed.anticipate.filter.AppInfo;
import com.pluscubed.anticipate.filter.DbUtil;
import com.pluscubed.anticipate.glide.AppIconLoader;

import java.io.InputStream;

import io.fabric.sdk.android.Fabric;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        if (!BuildConfig.DEBUG) {
            Fabric.with(this, new Crashlytics());
        }

        Inquiry.init(this, DbUtil.DB, 1);

        Glide.get(this)
                .register(AppInfo.class, InputStream.class, new AppIconLoader.Loader());
    }
}
