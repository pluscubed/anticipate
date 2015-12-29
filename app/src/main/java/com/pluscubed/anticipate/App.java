package com.pluscubed.anticipate;

import android.app.Application;

import com.afollestad.inquiry.Inquiry;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Inquiry.init(this, PerAppListActivity.DB, 1);
    }
}
