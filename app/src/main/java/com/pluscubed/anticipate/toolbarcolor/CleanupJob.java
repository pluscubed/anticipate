package com.pluscubed.anticipate.toolbarcolor;

import android.support.annotation.NonNull;

import com.evernote.android.job.Job;

public class CleanupJob extends Job {
    public static final String TAG = "CleanupJob";

    @NonNull
    @Override
    protected Result onRunJob(Params params) {
        WebsiteToolbarDbUtil.cleanup();

        return Result.SUCCESS;
    }
}
