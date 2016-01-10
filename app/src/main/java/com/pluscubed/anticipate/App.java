package com.pluscubed.anticipate;

import android.app.Application;

import com.afollestad.inquiry.Inquiry;
import com.bumptech.glide.Glide;
import com.crashlytics.android.Crashlytics;
import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.pluscubed.anticipate.filter.AppInfo;
import com.pluscubed.anticipate.glide.AppIconLoader;
import com.pluscubed.anticipate.toolbarcolor.CleanupJob;

import java.io.InputStream;

import io.fabric.sdk.android.Fabric;

public class App extends Application {

    public static final String DB = "Anticipate";

    @Override
    public void onCreate() {
        super.onCreate();

        Inquiry.init(this, DB, 1);

        if (!BuildConfig.DEBUG) {
            Fabric.with(this, new Crashlytics());
        }

        Glide.get(this)
                .register(AppInfo.class, InputStream.class, new AppIconLoader.Loader());

        JobManager.create(this, new JobCreator() {
            @Override
            public Job create(String tag) {
                switch (tag) {
                    case CleanupJob.TAG:
                        return new CleanupJob();
                    default:
                        throw new RuntimeException("Cannot find job for tag " + tag);
                }
            }
        });

        JobManager.instance().cancelAll();

        new JobRequest.Builder(CleanupJob.TAG)
                //1 day
                .setPeriodic(86400000)
                .build()
                .schedule();
    }
}
