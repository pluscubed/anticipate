package com.pluscubed.anticipate;

import android.app.Application;

import com.afollestad.inquiry.Inquiry;
import com.bumptech.glide.Glide;
import com.pluscubed.anticipate.glide.AppIconLoader;
import com.pluscubed.anticipate.perapp.AppInfo;
import com.pluscubed.anticipate.perapp.PerAppListActivity;

import java.io.InputStream;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Inquiry.init(this, PerAppListActivity.DB, 1);

        Glide.get(this)
                .register(AppInfo.class, InputStream.class, new AppIconLoader.Loader());
    }
}
