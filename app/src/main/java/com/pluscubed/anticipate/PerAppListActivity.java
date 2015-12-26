package com.pluscubed.anticipate;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.inquiry.Inquiry;
import com.afollestad.inquiry.annotations.Column;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import rx.Single;
import rx.SingleSubscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class PerAppListActivity extends AppCompatActivity {


    public static final String TABLE_EXCLUDED_APPS = "ExcludedApps";

    static final String[] DEFAULT_BLACKLISTED_APPS = {"com.android.systemui", "com.google.android.googlequicksearchbox",};

    List<AppPackage> mPerAppList;
    ExcludedAdapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_excluded);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);

        mPerAppList = new ArrayList<>();

        Inquiry.init(this, TABLE_EXCLUDED_APPS, 1);
        Single.create(new Single.OnSubscribe<List<AppPackage>>() {
            @Override
            public void call(SingleSubscriber<? super List<AppPackage>> singleSubscriber) {
                AppPackage[] all = Inquiry.get().selectFrom(TABLE_EXCLUDED_APPS, AppPackage.class)
                        .all();
                if (all != null) {
                    if (all.length == 0) {
                        AppPackage[] defaultBlacklistApps = new AppPackage[DEFAULT_BLACKLISTED_APPS.length];
                        for (int i = 0; i < DEFAULT_BLACKLISTED_APPS.length; i++) {
                            String packageName = DEFAULT_BLACKLISTED_APPS[i];
                            AppPackage appPackage = new AppPackage();
                            appPackage.excludedAppPackage = packageName;
                            defaultBlacklistApps[i] = appPackage;
                        }

                        Long[] ids = Inquiry.get().insertInto(TABLE_EXCLUDED_APPS, AppPackage.class)
                                .values(defaultBlacklistApps)
                                .run();

                        for (int i = 0; i < ids.length; i++) {
                            defaultBlacklistApps[i].id = ids[i];
                        }

                        singleSubscriber.onSuccess(new ArrayList<>(Arrays.asList(defaultBlacklistApps)));
                    } else {
                        singleSubscriber.onSuccess(new ArrayList<>(Arrays.asList(all)));
                    }
                } else {
                    singleSubscriber.onError(new Exception("Unknown error"));
                }
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleSubscriber<List<AppPackage>>() {
                    @Override
                    public void onSuccess(List<AppPackage> value) {
                        mPerAppList = value;

                        mAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onError(Throwable error) {

                    }
                });

        RecyclerView view = (RecyclerView) findViewById(R.id.recyclerview_excluded);
        mAdapter = new ExcludedAdapter();
        view.setAdapter(mAdapter);
        view.setLayoutManager(new LinearLayoutManager(this));

        FloatingActionButton button = (FloatingActionButton) findViewById(R.id.fab_add_excluded);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PerAppListActivity.this, AddAppDialogActivity.class);
                startActivity(intent);
            }
        });
    }

    public static class AppPackage {
        @Column(name = "_id", primaryKey = true, notNull = true, autoIncrement = true)
        public long id;

        @Column(name = "excluded_package_name")
        public String excludedAppPackage;

        public AppPackage() {

        }
    }

    private class ExcludedAdapter extends RecyclerView.Adapter<ExcludedAdapter.ViewHolder> {

        public ExcludedAdapter() {
            super();
            setHasStableIds(true);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_excluded_app, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            AppPackage app = mPerAppList.get(position);

            try {
                ApplicationInfo info = getPackageManager().getApplicationInfo(app.excludedAppPackage, 0);

                holder.icon.setImageDrawable(info.loadIcon(getPackageManager()));
                holder.title.setText(info.loadLabel(getPackageManager()));
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            holder.desc.setText(app.excludedAppPackage);

        }

        @Override
        public int getItemCount() {
            return mPerAppList.size();
        }

        @Override
        public long getItemId(int position) {
            return mPerAppList.get(position).id;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView icon;
            TextView title;
            TextView desc;

            public ViewHolder(View itemView) {
                super(itemView);

                icon = (ImageView) itemView.findViewById(R.id.image_excluded_app);
                title = (TextView) itemView.findViewById(R.id.text_excluded_name);
                desc = (TextView) itemView.findViewById(R.id.text_excluded_desc);

                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                });
            }
        }
    }
}
