package com.pluscubed.anticipate;

import android.app.Application;
import android.net.Uri;

import com.afollestad.inquiry.Inquiry;
import com.bumptech.glide.Glide;
import com.crashlytics.android.Crashlytics;
import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.pluscubed.anticipate.filter.AppInfo;
import com.pluscubed.anticipate.glide.AppIconLoader;
import com.pluscubed.anticipate.glide.FaviconLoader;
import com.pluscubed.anticipate.toolbarcolor.CleanupJob;
import com.squareup.leakcanary.LeakCanary;

import java.io.InputStream;

import io.fabric.sdk.android.Fabric;

public class App extends Application {

    public static final String DB = "Anticipate";

    @Override
    public void onCreate() {
        super.onCreate();

        if (!BuildConfig.DEBUG) {
            Fabric.with(this, new Crashlytics());
        }

        LeakCanary.install(this);

        Inquiry.init(this, DB, 1);

        Glide.get(this)
                .register(AppInfo.class, InputStream.class, new AppIconLoader.Factory());
        Glide.get(this)
                .register(Uri.class, InputStream.class, new FaviconLoader.Factory());

        JobManager.create(this)
            .addJobCreator(new JobCreator() {
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
